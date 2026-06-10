# Gemini Vertex AI Integration — Handoff Context

## Task
A teammate (Ayaan's colleague) committed a `GeminiEngineInvoker` adapter to the
`AdvancedAIUtility` / `GenAiUtility` project (already present in this workspace under
`GenAiUtility/`). The goal: wire the `qsnauthorv2` question-authoring pipeline so that
when the vendor selected is `GEMINI_VERTEX_AI`, questions are generated via Vertex AI
Gemini instead of Azure OpenAI / Mistral.

Original requirements message (from teammate):
- Engine name: `GEMINI_VERTEX_AI`
- DB properties needed per vendor entry: `GEMINI_PROJECT_ID`, `GEMINI_LOCATION`,
  `GEMINI_MODEL_ID`, `GEMINI_CREDENTIALS`, `GEMINI_TEMPERATURE`, `GEMINI_MAX_TOKENS`,
  `GEMINI_TOP_P`

## Reference doc
`Automated_authoring.md` in repo root — full reverse-engineered architecture of the
qsnauthorv2 pipeline (request lifecycle, vendor routing, class responsibilities).

## Analysis performed (already done — do not redo)
Traced the full request flow for `GEMINI_VERTEX_AI`:

```
QuestionGenerationServletV2
  -> QuestionGenerationWSImplV2
    -> QuestionGenerationV2.generateQuestions()
       (GEMINI_VERTEX_AI is NOT in {TCS_iON_AI_ML, QUESTION_GENERATION_OPENSOURCE},
        so it correctly falls into the "else" branch)
       -> QuestionGenerationChatGPT.questionGenerationGPTCall()
         -> TextBasedAuthoring / KnowledgeBasedAuthoring / SmallFileBasedAuthoring
            (their AIvendorCredNodeMap check also routes GEMINI_VERTEX_AI to GPT path)
           -> GPTBasedAuthoring.authoringByGPT()   <-- ONLY THIS FILE NEEDED CHANGES
```

Confirmed via reading:
- `mlframework--ws/qsnauthorv2/QuestionGenerationV2.java` — no change needed
- `mlframework--ws/qsnauthorv2/QuestionGenerationWSImplV2.java` — no change needed
- `mlframework--ws/qsnauthorv2/QuestionGenerationChatGPT.java` — no change needed
- `mlframework/qsnauthorv2/inputtypes/TextBasedAuthoring.java` — no change needed
- `mlframework/qsnauthorv2/engines/HomeGrownBasedAuthoring.java` — not used by Gemini path
- `GenAiUtility/adapter/GeminiEngineInvoker.java` — the actual Gemini adapter (already implemented by teammate, do not modify)
- `GenAiUtility/adapter/EngineInvokerFactory.java` — already has `GEMINI_VERTEX_AI -> GeminiEngineInvoker` wired
- `GenAiUtility/service/EngineInvocationService.java` — entry point: `invokeAdvanceAiEngine(JSONObject payload, Map<String,String> parameterMap)`
- `GenAiUtility/utils/EngineConstants.java` — defines all GEMINI_* property keys and engine return codes (ENGINE_001 = success, ENGINE_429, ENGINE_503, ENGINE_MA_*, ENGINE_SAFETY, etc.)
- `GenAiUtility/utils/GeminiErrorResponseHandler.java` — maps HTTP status -> "ENGINE_xxx" string codes

### Key facts about GeminiEngineInvoker contract
- Input payload: `JSONObject` with keys `"prompt"` (user message) and `"systemPrompt"` (system message) — plain strings, NOT the OpenAI `[{role,content}]` message array format.
- Output: `JSONObject` with keys:
  - `GenAIFrameworkConstants.STATUS_CODE` -> String engine code (`"ENGINE_001"` = success, `"ENGINE_429"` = quota, `"ENGINE_503"` = unavailable, others = blocked/error)
  - `GenAIFrameworkConstants.VENDOR_RESPONSE` -> String, the generated text
  - `GenAIFrameworkConstants.STATUS_MESSAGE` -> String message
  - `"tokens"` -> JSONObject with token usage
  - `"geminiResponse"` -> raw response string
- `EngineInvocationService` is constructed with `inputBean.getAIvendorCredNodeMap()` and called via `invokeAdvanceAiEngine(payload, inputBean.getParameterMap())`. `parameterMap` already contains `"serviceName"` (set in `QuestionGenerationWSImplV2`), which `EngineInvocationService` reads via `GenAIFrameworkConstants.SERVICE_NAME`.

## Change made — ONLY ONE FILE
**`mlframework/qsnauthorv2/engines/GPTBasedAuthoring.java`** — fully edited and currently
in its FINAL state (no further action needed unless new issues surface). Changes:

1. Added 2 imports:
   ```java
   import com.tcs.genai.engine.service.EngineInvocationService;
   import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
   ```

2. In `processQuestionType()`, inside the chunk loop: added `Timestamp startTime` capture
   before the API call, then branch on `GenAIFrameworkConstants.GEMINI_VERTEX_AI.equals(serviceName)`:
   - **Gemini branch**: `buildGeminiPayload(prompt_messages)` -> `new EngineInvocationService(inputBean.getAIvendorCredNodeMap())` -> `invokeAdvanceAiEngine(geminiPayload, inputBean.getParameterMap())` -> `processGeminiResponse(...)`
   - **else branch (unchanged GPT path)**: existing `gptIntegration.invokeAPIwithResponseFormatFromGenAI(...)` -> `processGptResponse(...)`
   - Both branches now pass `inputBean, startTime, endTime, serviceName` to their respective process methods (matches the existing `processGptResponse` signature which already had these params + audit logging via `AuditThirdPartyAPICall.prepareAuditDetailsMap`).

3. Added new private method `buildGeminiPayload(JSONArray promptMessages)`:
   - Iterates the OpenAI-style `[{role, content}]` array
   - `role == "system"` -> `payload.put("systemPrompt", text)`
   - `role == "user"` -> `payload.put("prompt", text)`

4. Added new private method `processGeminiResponse(...)` — signature mirrors
   `processGptResponse` exactly (same params + `prompt_messages, inputBean, startTime,
   endTime, serviceName`):
   - `"ENGINE_001"` -> success path: `postProcessOutput(output, questionType)`, same
     comprehensionmcq handling, `successTypes.add(...)`
   - `"ENGINE_429"` -> `errorCodeCounter.merge("429", ...)`
   - `"ENGINE_503"` -> `errorCodeCounter.merge("503", ...)`
   - anything else (ENGINE_MA_*, ENGINE_SAFETY, ENGINE_400, etc.) -> `errorCodeCounter.merge("400", ...)`
   - On success: `outputBean.setThirdPartyDetails(geminiResult)` — **IMPORTANT**: this
     was just fixed. `setThirdPartyDetails` takes a `JSONObject`, NOT a `String`.
     `geminiResult` (the whole response JSONObject from EngineInvocationService) is
     passed directly — do NOT cast to String.
   - At the end (after try/catch, before return): builds `auditdetailsMap` via
     `AuditThirdPartyAPICall.prepareAuditDetailsMap(orgId, appId, "QuestionGeneration_"+questionType, serviceName, startTime, endTime, engineCode, output, prompt_messages.toString())`
     and adds to `vendorAuditDetailsArray` / `outputBean.setVendorAuditDetails(...)` —
     identical pattern to `processGptResponse`.

The error-code strings ("429", "503", "400") deliberately match what
`authoringByGPT()`'s final switch statement already expects, so the partial-success /
full-failure aggregation logic at the top level needed ZERO changes.

## Current file state
The file `mlframework/qsnauthorv2/engines/GPTBasedAuthoring.java` has been fully edited
and the last known compile issue (`setThirdPartyDetails(JSONObject)` vs `String`) is
fixed. A full copy-paste version of the entire file was provided to the user in this
session for verification against their local copy.

## What's left / not yet done
1. **DB properties** — user needs to set `GEMINI_PROJECT_ID`, `GEMINI_LOCATION`,
   `GEMINI_MODEL_ID`, `GEMINI_CREDENTIALS`, `GEMINI_TEMPERATURE`, `GEMINI_MAX_TOKENS`,
   `GEMINI_TOP_P` under the `GEMINI_VERTEX_AI` vendor entry in
   `gblglobalproperties`/`gblproperties` (or wherever `CommercialVendorDetailsCacheLoader`
   /`UserLevelEngineSelectionService` source vendor credential maps from).
2. **No compile/build was run** — this is a multi-module Java EE project without a
   visible build file in this workspace; user will need to compile/test on their full
   environment (the "other laptop" with the complete codebase + build tooling).
3. **Not verified**: whether `InputBean.getParameterMap()` is populated correctly in
   ALL code paths that reach `GPTBasedAuthoring` (KB / SmallFile / Text) — it's set once
   in `QuestionGenerationWSImplV2` before `generateQuestions()` is called, so it should
   be present, but this hasn't been runtime-tested.
4. **Not verified**: actual end-to-end test against a real Vertex AI endpoint /
   GeminiEngineInvoker — only static code analysis was performed.
5. KB/SmallFile input types (`KnowledgeBasedAuthoring.java`,
   `SmallFileBasedAuthoring.java`) were NOT individually re-read in this session, but
   they follow the identical `authoringByGPT()` call pattern as `TextBasedAuthoring.java`
   (confirmed for Text only) — should work the same way since they all share
   `GPTBasedAuthoring.authoringByGPT()`. Worth a quick read-through if issues arise with
   KB/file-upload + Gemini specifically.
