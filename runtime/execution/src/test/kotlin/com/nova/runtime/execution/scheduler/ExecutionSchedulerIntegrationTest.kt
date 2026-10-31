package com.nova.runtime.execution.scheduler

import com.nova.runtime.events.InMemoryEventBus
import com.nova.runtime.events.execution.ExecutionEvents
import com.nova.runtime.execution.events.ExecutionEventPublisher
import com.nova.runtime.execution.history.NoOpExecutionHistoryRecorder
import com.nova.runtime.execution.metrics.ExecutionMetrics
import com.nova.runtime.execution.model.ExecutionConfig
import com.nova.runtime.execution.monitor.DefaultExecutionMonitor
import com.nova.runtime.execution.queue.DefaultQueueManager
import com.nova.runtime.execution.retry.DefaultRetryManager
import com.nova.runtime.execution.rollback.DefaultRollbackManager
import com.nova.runtime.execution.worker.ActionExecutor
import com.nova.runtime.execution.worker.NodeExecutionOutcome
import com.nova.runtime.models.ActionNode
import com.nova.runtime.models.Nag
import com.nova.runtime.models.Priority
import com.nova.runtime.models.contracts.ActionPolicyGate
import com.nova.runtime.models.contracts.ActionPolicyRequest
import com.nova.runtime.models.contracts.ExecutionRequest
import com.nova.runtime.models.contracts.ExecutionResult
import com.nova.runtime.models.contracts.PolicyDecision
import com.nova.runtime.models.contracts.PolicyDecisionType
import com.nova.runtime.models.contracts.PolicyEvaluationResult
import com.nova.runtime.utils.logging.StructuredLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExecutionSchedulerIntegrationTest {
    private val logger = StructuredLogger()
    private val eventBus = InMemoryEventBus(logger)

    @Test
    fun execute_runsDagInDependencyOrder() =
        runTest {
            val order = ConcurrentHashMap.newKeySet<String>()
            val scheduler =
                createScheduler(
                    FakeActionExecutor { node ->
                        order.add(node.inputs.getValue("taskKey"))
                        NodeExecutionOutcome.Success(node.outputs)
                    },
                )

            val nodeA = node("a", emptyList())
            val nodeB = node("b", listOf(nodeA.id))
            val nodeC = node("c", listOf(nodeB.id))
            val graph = graphOf(listOf(nodeA, nodeB, nodeC))

            val result =
                scheduler.execute(
                    request = ExecutionRequest(graph = graph, traceId = UUID.randomUUID()),
                    executionId = UUID.randomUUID(),
                    pauseFlag = java.util.concurrent.atomic.AtomicBoolean(false),
                    cancelFlag = java.util.concurrent.atomic.AtomicBoolean(false),
                )

            assertIs<ExecutionResult.Success>(result)
            assertEquals(3, result.completedNodes)
            assertEquals(listOf("a", "b", "c"), order.toList())
        }

    @Test
    fun execute_parallelIndependentNodesRun() =
        runTest {
            val scheduler =
                createScheduler(
                    FakeActionExecutor { node ->
                        NodeExecutionOutcome.Success(node.outputs)
                    },
                )

            val nodeA = node("a", emptyList())
            val nodeB = node("b", emptyList())
            val nodeC = node("c", listOf(nodeA.id, nodeB.id))
            val graph = graphOf(listOf(nodeA, nodeB, nodeC))

            val result =
                scheduler.execute(
                    request = ExecutionRequest(graph = graph, traceId = UUID.randomUUID()),
                    executionId = UUID.randomUUID(),
                    pauseFlag = java.util.concurrent.atomic.AtomicBoolean(false),
                    cancelFlag = java.util.concurrent.atomic.AtomicBoolean(false),
                )

            assertIs<ExecutionResult.Success>(result)
            assertEquals(3, result.completedNodes)
        }

    @Test
    fun execute_retriesThenSucceeds() =
        runTest {
            val attempts = ConcurrentHashMap<String, Int>()
            val scheduler =
                createScheduler(
                    FakeActionExecutor { node ->
                        val count = attempts.merge(node.inputs.getValue("taskKey"), 1, Int::plus) ?: 1
                        if (node.inputs.getValue("taskKey") == "flaky" && count < 2) {
                            NodeExecutionOutcome.Failure(
                                error =
                                    com.nova.runtime.models.RuntimeError(
                                        code = "TEMP",
                                        category = com.nova.runtime.models.ErrorCategory.EXECUTION,
                                        severity = com.nova.runtime.models.ErrorSeverity.LOW,
                                        recoverable = true,
                                        userVisibleMessage = "temporary",
                                    ),
                                retryable = true,
                            )
                        } else {
                            NodeExecutionOutcome.Success(node.outputs)
                        }
                    },
                )

            val flaky = node("flaky", emptyList()).copy(retryPolicy = "fixed:2:1")
            val graph = graphOf(listOf(flaky))

            val result =
                scheduler.execute(
                    request = ExecutionRequest(graph = graph, traceId = UUID.randomUUID()),
                    executionId = UUID.randomUUID(),
                    pauseFlag = java.util.concurrent.atomic.AtomicBoolean(false),
                    cancelFlag = java.util.concurrent.atomic.AtomicBoolean(false),
                )

            assertIs<ExecutionResult.Success>(result)
            assertTrue((attempts["flaky"] ?: 0) >= 2)
        }

    @Test
    fun execute_failureTriggersRollbackAndGraphFailedEvent() =
        runTest {
            val scheduler =
                createScheduler(
                    FakeActionExecutor { node ->
                        if (node.inputs["taskKey"] == "fail") {
                            NodeExecutionOutcome.Failure(
                                error =
                                    com.nova.runtime.models.RuntimeError(
                                        code = "HARD_FAIL",
                                        category = com.nova.runtime.models.ErrorCategory.EXECUTION,
                                        severity = com.nova.runtime.models.ErrorSeverity.HIGH,
                                        recoverable = false,
                                        userVisibleMessage = "failed",
                                    ),
                                retryable = false,
                            )
                        } else {
                            NodeExecutionOutcome.Success(node.outputs)
                        }
                    },
                )

            val ok = node("ok", emptyList()).copy(rollbackPolicy = "compensate")
            val bad = node("fail", listOf(ok.id)).copy(retryPolicy = "none")
            val graph = graphOf(listOf(ok, bad))
            val traceId = UUID.randomUUID()

            val result =
                scheduler.execute(
                    request = ExecutionRequest(graph = graph, traceId = traceId),
                    executionId = UUID.randomUUID(),
                    pauseFlag = java.util.concurrent.atomic.AtomicBoolean(false),
                    cancelFlag = java.util.concurrent.atomic.AtomicBoolean(false),
                )

            assertIs<ExecutionResult.Failure>(result)
            val eventTypes = eventBus.publishedEvents().map { it.eventType }
            assertTrue(ExecutionEvents.GRAPH_FAILED in eventTypes)
            assertTrue(ExecutionEvents.NODE_ROLLED_BACK in eventTypes)
        }

    @Test
    fun cancelWhilePaused_wakesWithoutResumeOrActionExecution() =
        runTest {
            var actions = 0
            val scheduler =
                createScheduler(
                    FakeActionExecutor { node ->
                        actions++
                        NodeExecutionOutcome.Success(node.outputs)
                    },
                )
            val control = MutableStateFlow(ExecutionControlState(paused = true))
            val result =
                async {
                    scheduler.execute(
                        request = ExecutionRequest(graphOf(listOf(node("a", emptyList()))), UUID.randomUUID()),
                        executionId = UUID.randomUUID(),
                        pauseFlag = java.util.concurrent.atomic.AtomicBoolean(true),
                        cancelFlag = java.util.concurrent.atomic.AtomicBoolean(false),
                        controlState = control,
                    )
                }
            runCurrent()
            assertFalse(result.isCompleted)
            assertEquals(0, actions)
            control.value = control.value.copy(cancelled = true)
            runCurrent()
            assertTrue(result.isCompleted)
            assertIs<ExecutionResult.Failure>(result.await())
            assertEquals(0, actions)
        }

    @Test
    fun resumeFromPaused_runsGraphOnce() =
        runTest {
            var actions = 0
            val scheduler =
                createScheduler(
                    FakeActionExecutor { node ->
                        actions++
                        NodeExecutionOutcome.Success(node.outputs)
                    },
                )
            val control = MutableStateFlow(ExecutionControlState(paused = true))
            val result =
                async {
                    scheduler.execute(
                        request = ExecutionRequest(graphOf(listOf(node("a", emptyList()))), UUID.randomUUID()),
                        executionId = UUID.randomUUID(),
                        pauseFlag = java.util.concurrent.atomic.AtomicBoolean(true),
                        cancelFlag = java.util.concurrent.atomic.AtomicBoolean(false),
                        controlState = control,
                    )
                }
            runCurrent()
            assertEquals(0, actions)
            control.value = control.value.copy(paused = false)
            assertIs<ExecutionResult.Success>(result.await())
            assertEquals(1, actions)
        }

    private fun createScheduler(actionExecutor: FakeActionExecutor): ExecutionScheduler =
        ExecutionScheduler(
            dependencyResolver = DefaultDependencyResolver(),
            queueManager = DefaultQueueManager(),
            retryManager = DefaultRetryManager(),
            rollbackManager = DefaultRollbackManager(),
            monitor = DefaultExecutionMonitor(),
            metrics = ExecutionMetrics(),
            eventPublisher = ExecutionEventPublisher(eventBus),
            actionExecutor = actionExecutor,
            actionPolicyGate = AllowAllPolicyGate,
            historyRecorder = NoOpExecutionHistoryRecorder(),
            logger = logger,
            config = ExecutionConfig(workerPoolSize = 2),
        )

    private object AllowAllPolicyGate : ActionPolicyGate {
        override suspend fun evaluateAction(request: ActionPolicyRequest): PolicyEvaluationResult =
            PolicyEvaluationResult.Success(
                PolicyDecision(
                    type = PolicyDecisionType.APPROVED,
                    rationale = "test allow all",
                ),
            )
    }

    private class FakeActionExecutor(
        private val handler: suspend (ActionNode) -> NodeExecutionOutcome,
    ) : ActionExecutor {
        override suspend fun execute(
            node: ActionNode,
            traceId: UUID,
        ): NodeExecutionOutcome = handler(node)

        override suspend fun rollback(
            node: ActionNode,
            traceId: UUID,
        ): NodeExecutionOutcome = NodeExecutionOutcome.Success(emptyMap())
    }

    private fun node(
        key: String,
        dependencies: List<UUID>,
    ): ActionNode =
        ActionNode(
            id = UUID.nameUUIDFromBytes(key.toByteArray()),
            actionType = "execute_capability",
            inputs = mapOf("taskKey" to key),
            outputs = mapOf("taskKey" to key),
            dependencies = dependencies,
            timeoutMs = 1_000L,
            retryPolicy = "none",
            rollbackPolicy = "none",
            executionPriority = Priority.NORMAL,
        )

    private fun graphOf(nodes: List<ActionNode>): Nag =
        Nag(
            graphId = UUID.randomUUID(),
            metadata = emptyMap(),
            taskHierarchy = emptyList(),
            actionNodes = nodes,
            dependencies = nodes.associate { it.id to it.dependencies },
            executionPolicies = emptyMap(),
        )
}
