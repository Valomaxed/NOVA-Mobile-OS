# Minimal UI and answer capacity update

Implemented 21 October 2026 in the existing checkout. Changes remain uncommitted.

## Interface

The light theme now uses a soft grey canvas, rounded white cards, blue actions and selection, and system sans-serif text. Red is reserved for errors. Shared cards use 24dp corners; buttons use pill shapes with clipped touch feedback and disabled states. Navigation keeps native selection semantics and its large-font label behavior.

Actions, Ask, and Settings use quieter headings and rounded surfaces. The command bar has no decorative shadow. The empty app-control tree shows an honest empty state instead of illustrative WhatsApp controls. Unavailable hardware readouts were removed from Settings. Model filenames wrap within their available width so status labels retain space.

This is a One UI–inspired light theme, not a Samsung asset or framework integration. Dark mode and device visual verification are not part of this local pass.

## Answer and context limits

| Capacity | Before | Updated |
| --- | --- | --- |
| Default returned answer budget | 512 tokens | 1,024 tokens |
| Backend maximum returned answer budget | 1,024 tokens | 1,536 tokens when explicitly requested |
| Generic answer text cap, before attribution | 2,400 characters | 6,000 characters |
| Retrieved source window | 2,400 characters | 8,000 characters |
| Grounded prompt source selection ceiling | 6,000 characters | 12,000 characters, then fitted with the model tokenizer |
| Recommended Gemma bundle context | 2,048 total tokens | 4,096 total tokens with the new bundle |

The recommended download is `Gemma3-1B-IT_multi-prefill-seq_q4_block128_ekv4096.task`, approximately 689 MB. Its filename and context are listed in the [publisher's model repository](https://huggingface.co/litert-community/Gemma3-1B-IT/tree/main). Existing `Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task` installations remain supported with a 2,048-token limit. The earlier file is retained. The download catalog and helper script point to the new bundle. No model was downloaded or installed on a phone during this pass.

Settings now reports the available bundle's capacity rather than offering ineffective 4K–128K selectors. It reports “Not installed” if no Gemma bundle is available. File availability does not prove successful native inference.

As the [MediaPipe Android guide](https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android) explains, the context limit includes input and output. The prompt fitter measures the completed prompt with the backend tokenizer, retains the question/instructions, trims source text as needed, and reserves the requested answer budget plus 64 control tokens. Impossible budgets fall back to extractive answers. Returned output is capped after generation because the current API exposes a total context limit rather than a separate decode limit.

Character ceilings and token ceilings are different: a long source window is reduced when necessary to fit the token budget. Answers can end earlier than their budget. No claim is made that every response will be twice as long or more accurate. The larger bundle and context may use more RAM and take longer; device measurements are still needed.

The embedding model, 128,000-character extraction limit, digital PDF limit, and OCR limit are unchanged by this update.

## Validation

- 218 local tests passed: ai-core 44, storage 98, ai-native 74, app 2; zero failures, errors, or skips.
- New tests cover answer-space reservation for both context sizes, intact prompt instructions, impossible budgets, Unicode boundaries, and answers/source excerpts beyond the previous caps.
- Debug APK built successfully at `app/build/outputs/apk/debug/app-debug.apk`.
- Local logs are under `tmp/one-ui-refresh/`. Tests use controlled model responses; actual Gemma inference and phone rendering have not been verified.
