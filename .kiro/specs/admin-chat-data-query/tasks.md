# Implementation Plan: Admin Chat Data Query

## Overview

Add a conversational chat interface to the Wealth Management Platform. The backend uses an LLM-based intent recognition engine (abstracted behind `LlmProvider`) to interpret admin messages, route to existing repositories, and return structured responses. The frontend adds a "Chat" tab with message input and conversation history. Implementation follows existing Spring Boot + Kotlin patterns and reuses all existing repositories.

## Tasks

- [x] 1. Add dependencies and configuration
  - [x] 1.1 Add Maven dependencies for LLM chat feature
    - Add `io.kotest:kotest-runner-junit5:5.9.1` and `io.kotest:kotest-property:5.9.1` to `pom.xml` test dependencies
    - Add `io.mockk:mockk:1.13.13` to `pom.xml` test dependencies
    - _Requirements: Testing Strategy from design_
  - [x] 1.2 Add LLM configuration properties to `application.properties`
    - Add `app.llm.provider`, `app.llm.api-key`, `app.llm.model`, `app.llm.endpoint`, `app.llm.temperature`, `app.llm.max-tokens` properties
    - API key should reference an environment variable: `${LLM_API_KEY:}`
    - Add corresponding test properties in `backend/src/test/resources/application.properties`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 2. Implement model classes and LlmProvider interface
  - [x] 2.1 Create chat model classes in `backend/src/main/kotlin/com/wealthmgmt/model/Models.kt`
    - Add `ChatRequest(message: String)`, `ChatResponse(reply: String, data: List<Map<String, Any?>>?)`, `Intent` enum (`CLIENTS`, `SCHEMES`, `TRANSACTIONS`, `HOLDINGS`, `REPORT`, `UNKNOWN`), and `IntentResult(intent: Intent, params: Map<String, String?>)` data classes
    - _Requirements: 9.1, 9.2_
  - [x] 2.2 Create `LlmProvider` interface in `backend/src/main/kotlin/com/wealthmgmt/llm/LlmProvider.kt`
    - Define `fun resolveIntent(message: String): IntentResult` method
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 3. Implement LLM provider implementations
  - [x] 3.1 Create `OpenAiLlmProvider` in `backend/src/main/kotlin/com/wealthmgmt/llm/OpenAiLlmProvider.kt`
    - Implement `resolveIntent()` sending POST to `{endpoint}/chat/completions` with system prompt and user message
    - Parse `choices[0].message.content` JSON into `IntentResult`
    - Use `Authorization: Bearer {apiKey}` header
    - Handle malformed LLM responses by returning `IntentResult(UNKNOWN, emptyMap())` and logging a warning
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
  - [x] 3.2 Create `AnthropicLlmProvider` in `backend/src/main/kotlin/com/wealthmgmt/llm/AnthropicLlmProvider.kt`
    - Implement `resolveIntent()` sending POST to `{endpoint}/messages` with `x-api-key` header
    - Parse `content[0].text` JSON into `IntentResult`
    - Handle malformed responses gracefully
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
  - [x] 3.3 Create `GeminiLlmProvider` in `backend/src/main/kotlin/com/wealthmgmt/llm/GeminiLlmProvider.kt`
    - Implement `resolveIntent()` sending POST to `{endpoint}/v1beta/models/{model}:generateContent?key={apiKey}`
    - Parse `candidates[0].content.parts[0].text` JSON into `IntentResult`
    - Handle malformed responses gracefully
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
  - [x] 3.4 Write unit tests for LLM provider implementations
    - Test `OpenAiLlmProvider.resolveIntent()` sends correct HTTP request format (RestTemplate mocked)
    - Test `AnthropicLlmProvider.resolveIntent()` sends correct headers and body format
    - Test `GeminiLlmProvider.resolveIntent()` sends correct URL with API key query param and body format
    - Test all three providers correctly parse valid LLM JSON responses into `IntentResult`
    - Test all three providers handle malformed LLM responses gracefully (return UNKNOWN)
    - Place tests in `backend/src/test/kotlin/com/wealthmgmt/LlmProviderTests.kt`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
  - [x] 3.5 Write property test for LLM response parsing round trip
    - **Property 11: LLM response parsing round trip**
    - Generate random valid `IntentResult` objects, serialize to JSON, parse back through the provider's parser, assert equivalence
    - Place in `backend/src/test/kotlin/com/wealthmgmt/LlmProviderPropertyTests.kt`
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

- [x] 4. Implement LlmConfig and ChatService
  - [x] 4.1 Create `LlmConfig` in `backend/src/main/kotlin/com/wealthmgmt/config/LlmConfig.kt`
    - Spring `@Configuration` class that reads `app.llm.provider` property and creates the appropriate `LlmProvider` bean
    - Support `openai`, `anthropic`, `gemini` provider values (case-insensitive)
    - Throw `IllegalArgumentException` for unsupported providers
    - Also create a `RestTemplate` bean
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  - [x] 4.2 Create `ChatService` in `backend/src/main/kotlin/com/wealthmgmt/service/ChatService.kt`
    - Inject `LlmProvider`, `ClientRepository`, `SchemeRepository`, `TransactionRepository`, `HoldingRepository`
    - Implement `processMessage(message: String): ChatResponse`
    - Call `llmProvider.resolveIntent(message)` to get `IntentResult`
    - Route based on intent: CLIENTS → `clientRepo.findAll()` with optional name filter, SCHEMES → `schemeRepo.findAll()` with optional name/type filter, TRANSACTIONS → `transactionRepo.findAll()` with optional type/client filter, HOLDINGS → `holdingRepo.getHoldings()` with optional client/scheme/sort params, REPORT → generate report download URLs, UNKNOWN → return help message
    - Wrap all repository calls in try-catch, return error `ChatResponse` on failure
    - Catch LLM provider exceptions and return user-friendly error response
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 4.1, 4.2, 5.1, 5.2, 6.1, 6.2, 7.1, 7.2, 7.3, 8.1, 8.2, 11.3_
  - [x] 4.3 Write property test for intent routing correctness
    - **Property 1: Intent routing correctness**
    - Generate random `Intent` enum values, mock `LlmProvider` to return `IntentResult` with that intent, call `processMessage()`, verify the correct repository was invoked
    - Place in `backend/src/test/kotlin/com/wealthmgmt/ChatServicePropertyTests.kt`
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
  - [x] 4.4 Write property test for response structure invariant
    - **Property 3: Response structure invariant**
    - Generate random non-blank strings, mock `LlmProvider` to return any valid `IntentResult`, assert `processMessage()` returns `ChatResponse` with non-blank `reply`
    - Place in `backend/src/test/kotlin/com/wealthmgmt/ChatServicePropertyTests.kt`
    - **Validates: Requirements 9.2**
  - [x] 4.5 Write property test for error containment
    - **Property 9: Chat service error containment**
    - Mock `LlmProvider` and repositories to throw random exceptions, assert `processMessage()` returns error `ChatResponse` without throwing
    - Place in `backend/src/test/kotlin/com/wealthmgmt/ChatServicePropertyTests.kt`
    - **Validates: Requirements 11.3**

- [x] 5. Checkpoint - Ensure all backend service tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement ChatController
  - [x] 6.1 Create `ChatController` in `backend/src/main/kotlin/com/wealthmgmt/controller/ChatController.kt`
    - `@RestController` with `@RequestMapping("/api/chat")`
    - `@PostMapping` endpoint accepting `ChatRequest` body
    - Validate `request.message` is non-blank; return 400 with `ChatResponse(reply = "Message cannot be empty.")` if empty
    - Delegate to `ChatService.processMessage(message)` and return result
    - Secured by existing Spring Security session auth (no additional config needed since `/api/**` is already authenticated)
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 2.2_
  - [x] 6.2 Write property test for empty/blank message rejection
    - **Property 2: Empty/blank message rejection**
    - Generate random whitespace-only strings (spaces, tabs, newlines, empty), assert the controller returns 400
    - Place in `backend/src/test/kotlin/com/wealthmgmt/ChatControllerTests.kt`
    - **Validates: Requirements 2.3, 9.4**
  - [x] 6.3 Write unit tests for ChatController
    - Test POST `/api/chat` with valid message returns 200 with reply (MockMvc, LlmProvider mocked)
    - Test POST `/api/chat` with empty body returns 400
    - Test POST `/api/chat` without authentication returns 401
    - Place in `backend/src/test/kotlin/com/wealthmgmt/ChatControllerTests.kt`
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [x] 7. Implement data query filtering and response formatting
  - [x] 7.1 Implement filtered query logic in `ChatService`
    - For CLIENTS intent with `clientName` param: filter `clientRepo.findAll()` results by case-insensitive name match
    - For SCHEMES intent with `schemeName`/`schemeType` params: filter `schemeRepo.findAll()` results accordingly
    - For TRANSACTIONS intent with `transactionType`/`clientName` params: filter results, resolve client name to ID for client-specific queries
    - For HOLDINGS intent: resolve `clientName`/`schemeName` to IDs, pass `sortBy`/`sortOrder` to `holdingRepo.getHoldings()`
    - For REPORT intent: resolve `clientName` to client ID, return report download URLs for holdings/transactions/capital-gains; if no client name, ask admin to specify
    - Convert entity results to `List<Map<String, Any?>>` for uniform frontend rendering
    - _Requirements: 4.1, 4.2, 5.1, 5.2, 6.1, 6.2, 7.1, 7.2, 7.3, 8.1, 8.2_
  - [x] 7.2 Write property test for filtered query results
    - **Property 4: Filtered query results contain only matching records**
    - Generate random filter values and entity types, mock `LlmProvider`, seed test data, assert all returned records match the filter criteria
    - Place in `backend/src/test/kotlin/com/wealthmgmt/ChatServicePropertyTests.kt`
    - **Validates: Requirements 4.2, 5.2, 6.2, 7.2**
  - [x] 7.3 Write property test for holdings sort order
    - **Property 5: Holdings sort order**
    - Seed holdings data, mock `LlmProvider` to return HOLDINGS intent with random sort directions, assert returned data is ordered correctly
    - Place in `backend/src/test/kotlin/com/wealthmgmt/ChatServicePropertyTests.kt`
    - **Validates: Requirements 7.3**
  - [x] 7.4 Write property test for report URL correctness
    - **Property 6: Report URL correctness**
    - Seed client data, mock `LlmProvider` to return REPORT intent with valid/null client names, assert response contains 3 URLs with correct client ID or asks for client name
    - Place in `backend/src/test/kotlin/com/wealthmgmt/ChatServicePropertyTests.kt`
    - **Validates: Requirements 8.1, 8.2**
  - [x] 7.5 Write property test for required fields in data query results
    - **Property 7: Data query results include required fields**
    - Generate `IntentResult` for each entity type, assert all returned records contain the required fields for that entity type
    - Place in `backend/src/test/kotlin/com/wealthmgmt/ChatServicePropertyTests.kt`
    - **Validates: Requirements 4.1, 5.1, 6.1, 7.1**

- [x] 8. Checkpoint - Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Implement frontend Chat tab
  - [x] 9.1 Add Chat tab HTML to `backend/src/main/resources/static/index.html`
    - Add `<a href="#" data-tab="chat">Chat</a>` link to the `<nav>` element
    - Add `<section id="tab-chat" class="tab hidden">` with conversation history container (`#chat-messages`), welcome message, message input form with text input and send button
    - _Requirements: 1.1, 1.3, 2.1, 2.3, 2.4, 10.1, 10.2_
  - [x] 9.2 Add Chat tab CSS to `backend/src/main/resources/static/style.css`
    - Add styles for `.chat-container`, `.chat-message.admin`, `.chat-message.system`, `.chat-input-form`, `.chat-loading`, `.chat-timestamp`
    - Style admin messages right-aligned, system messages left-aligned with different background colors
    - _Requirements: 10.2_
  - [x] 9.3 Add Chat tab JavaScript to `backend/src/main/resources/static/app.js`
    - Implement `sendChatMessage()` — sends POST to `/api/chat`, appends admin message and response to conversation history
    - Implement `appendMessage(role, content, data)` — renders a message bubble with timestamp; if `data` is present, renders an HTML table
    - Implement `renderChatTable(data)` — converts `data` array of maps into an HTML `<table>`
    - Implement `renderReportLinks(data)` — renders report URLs as clickable `<a>` tags
    - Handle loading indicator display and send button disable/enable during requests
    - Handle HTTP error responses with user-friendly error message in conversation
    - Handle network errors with connection error message and re-enable send button
    - Prevent empty message submission
    - Auto-scroll to most recent message on new message
    - Display welcome message on chat tab load
    - _Requirements: 1.1, 1.3, 2.1, 2.2, 2.3, 2.4, 4.3, 5.3, 6.3, 7.4, 8.3, 10.1, 10.2, 10.3, 10.4, 11.1, 11.2_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The LLM provider is always mocked in tests — no real LLM API calls during testing
- Frontend property tests (Property 8, 10) can be verified manually or with a lightweight JS test runner
