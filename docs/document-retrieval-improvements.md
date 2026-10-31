# Document extraction and retrieval improvements

This report records the 17 October extraction/retrieval pass. For current answer/context limits and the 21 October UI update, see [Minimal UI and answer capacity update](minimal-ui-and-context-update.md).

Implemented 17 October 2026 in the existing NOVA checkout. Changes are uncommitted and preserve the earlier frontend and runtime work. The revised debug APK is built; it has not been installed or tested on a phone in this pass, as requested.

## Confirmed problems and changes

| Stage | Previous behavior | Revised behavior |
| --- | --- | --- |
| Document vectors | One 512-character filename/summary vector | Discovery vector plus overlapping body passages across the extracted text |
| Encoder window | 128 word pieces including special tokens | 256 including special tokens; body windows contain at most 254 pieces, with up to 32 pieces of overlap |
| Text coverage | 32,000 characters; a single Reader.read could stop early | Up to 128,000 characters, short reads handled correctly, UTF-8/UTF-16 BOM detection |
| Digital PDF pages | First 20 pages, ignoring the caller's page budget | Up to the caller's page budget, default 128; bounded by the text-character budget |
| Scanned PDF pages | Default 16, hard cap 24 | Hard cap remains 24; the increased default page allowance reaches that cap |
| PDF reading order | PDF stream order | Position-sorted text extraction |
| DOCX text | XML tags replaced with spaces | Streaming XML parsing; split text runs are rejoined correctly; entities, paragraph breaks, tabs and table cells are retained |
| OCR weekday tables | Column-major text could lose row/column association | Coordinate-based reconstruction when a multi-cell weekday header is detected; ordinary prose retains the recognizer's reading order |
| Table blanks | Empty pipe/tab cells were dropped, shifting values into the wrong weekday | Interior blank cells and generated leading cells are preserved |
| Generic answer context | Usually the first 300 characters | Query-focused source windows up to 2,400 characters, retaining the matched semantic passage when lexical overlap is absent |
| Generic answer display | 400 characters, with a misleading Menu prefix | Up to 2,400 characters with source paragraphs and attribution; menu/timetable formatting remains specialized |
| Explicit filename | Could answer from another matching document | Exact named-document filtering in semantic and keyword paths; a missing named file does not silently substitute another file |
| Local LLM | 1,024 total input/output tokens; request budget ignored | 2,048 total context tokens, measured prompt-budget checks and a returned-output token budget; oversized prompts fall back to extractive answers |

The model choice and weights have not changed. MiniLM is a short-passage encoder, so document coverage comes from multiple source passages rather than forcing a book into one embedding. Its [model card](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2) documents the default 256-word-piece truncation. MediaPipe's [Android guide](https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android) defines maxTokens as the combined input/output capacity. The installed API was inspected locally before changing its use.

## Retrieval integrity and upgrades

Passage offsets are persisted in existing embedding metadata and reconstructed during startup hydration. Old summary-only documents receive a bounded upgrade allowance during background indexing; manual reimport refreshes a particular document immediately. Failed embedding preparation preserves the old searchable records. If changed text is saved while re-embedding fails, its primary record is marked stale so a later background batch can retry. Valid vectors are not regenerated solely because an extraction-attempt timestamp changed.

New vectors are prepared before publication. Database writes, the document reference update, and in-memory publication are serialized for this indexing path. The publication phase defers cancellation to avoid removing newly referenced vectors. It is not a single crash-atomic transaction spanning Room and the in-memory index; process termination during publication can still require startup hydration. No database schema migration was needed.

Body-passage hits use bounded overfetch followed by document deduplication, so very long documents can still crowd a limited candidate window. This is not an exhaustive per-document nearest-neighbor algorithm or a measured retrieval-recall guarantee.

Readability cleanup preserves Unicode numeric HTML entities, paragraphs, and source wording. Replacement-character/control-character corruption is rejected by the answerability check. A simple source-value check rejects generated numbers or identifiers absent from the retrieved context. These checks reduce specific failure modes; they do not prove every generated statement is entailed or repair arbitrary OCR mistakes.

## Validation

Final Gradle invocation succeeded:

```
:runtime:ai-core:test :runtime:storage:testDebugUnitTest :runtime:ai-native:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
```

214 tests passed, with zero failures, errors, or skipped tests: ai-core 41, storage 98, ai-native 73, app 2. New coverage includes real WordPiece passage budgets and source coverage, late facts beyond the old text/page limits, short stream reads, Unicode BOMs, DOCX text runs and entities, a generated 25-page PDF, weekday geometry and blank columns, exact filename lookup, persisted passage offsets and replacement, failed/missing-tokenizer recovery, stale-index upgrade selection, longer source-grounded answers, rejected fabricated quantities, and a semantic-only late passage surviving answer formatting.

The PDF fixture required Android library assets in Robolectric tests; native-module test resources are now enabled so PDFBox uses its actual bundled font resources. Local orchestration/retrieval fixtures use controlled or hash embeddings; the passage-boundary tests use the real bundled WordPiece vocabulary. This is not a Windows execution of the Android ONNX model or a real-device OCR/model-quality benchmark.

Diff whitespace checks passed. The final log and per-suite counts are in tmp/retrieval-tests/release-check.log and tmp/retrieval-tests/test-results.csv. The APK is app/build/outputs/apk/debug/app-debug.apk.

## Practical limits

- Existing records must be upgraded before they gain body-passage retrieval. Background batches are bounded; a large existing library is not rebuilt instantaneously.
- Coverage remains capped at 128,000 extracted characters, 128 digital PDF pages and 24 scanned OCR pages by default. This is not unlimited document reading.
- Source text is extracted and stored; the LLM receives selected passages, not the whole 128,000-character document at once. Longer answers are supported, but the MediaPipe task still has a bounded context.
- The MediaPipe API exposes a total context limit rather than a separate generation limit. Returned output is constrained to the requested budget, up to 1,024 tokens; generation itself is bounded by the 2,048-token engine context. Physical memory, latency and quality of this changed configuration need device measurements.
- Grounded answer rewriting requires the separate Gemma task bundle. Availability of the legacy ONNX model files alone does not establish that this generative path is available. Readable extractive fallback remains supported.
- OCR still uses the existing Latin-script recognizer. Rotated, low-resolution, merged-cell, multi-column and non-Latin documents need representative on-device evaluation. No claim is made that every previously garbled document is now understood correctly.
- More passage vectors increase indexing work, storage and the final in-memory index footprint. No speed, battery, thermal or memory improvement percentage is claimed.
- Test examples are synthetic and local. A representative failing document and question are still needed for a real-world quality comparison.
