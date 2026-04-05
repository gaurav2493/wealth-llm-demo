# Requirements Document

## Introduction

This feature adds an Admin Chat screen to the Wealth Management Platform. The chat interface allows authenticated admins to query application data using natural language messages. The Chat_Service interprets admin queries and returns structured data from the backend — including clients, schemes, transactions, holdings, and reports. The backend exposes a new chat API endpoint that parses the admin's message, determines the intent, calls the appropriate existing services/repositories, and returns a formatted response. The frontend adds a new "Chat" tab with a conversational UI.

## Glossary

- **Admin**: The authenticated user who operates the Wealth Management Platform
- **Chat_Screen**: The frontend tab that displays the conversational chat interface
- **Chat_Service**: The backend service that interprets admin messages and retrieves the requested data
- **Chat_Controller**: The backend REST controller that exposes the chat API endpoint
- **Intent**: The determined purpose of an admin's chat message (e.g., list clients, get holdings)
- **Chat_Message**: A single message in the conversation, either from the Admin or from the Chat_Service
- **Query_Result**: The structured data returned by the Chat_Service in response to an admin query
- **Supported_Query**: A data query type that the Chat_Service can interpret and fulfill (clients, schemes, transactions, holdings, reports)

## Requirements

### Requirement 1: Chat Screen Access

**User Story:** As an admin, I want to access a chat screen from the main navigation, so that I can query data conversationally.

#### Acceptance Criteria

1. WHEN the Admin clicks the "Chat" navigation link, THE Chat_Screen SHALL display a chat interface with a message input field and a conversation history area
2. WHILE the Admin is not authenticated, THE Chat_Screen SHALL remain inaccessible and redirect to the login page
3. THE Chat_Screen SHALL display a welcome message indicating the types of queries the Admin can perform

### Requirement 2: Send Chat Message

**User Story:** As an admin, I want to type a natural language message and send it, so that I can request data without navigating through multiple screens.

#### Acceptance Criteria

1. WHEN the Admin submits a chat message, THE Chat_Screen SHALL display the message in the conversation history as a sent message
2. WHEN the Admin submits a chat message, THE Chat_Screen SHALL send the message to the Chat_Controller via a POST request to `/api/chat`
3. WHEN the Admin submits an empty message, THE Chat_Screen SHALL prevent submission and keep focus on the input field
4. WHILE a chat request is in progress, THE Chat_Screen SHALL display a loading indicator and disable the send button

### Requirement 3: Intent Recognition

**User Story:** As an admin, I want the system to understand my data queries expressed in natural language, so that I get the correct data without using exact commands.

#### Acceptance Criteria

1. WHEN the Chat_Service receives a message containing client-related keywords (e.g., "clients", "client list", "show clients"), THE Chat_Service SHALL resolve the Intent to retrieve client data
2. WHEN the Chat_Service receives a message containing scheme-related keywords (e.g., "schemes", "funds", "show schemes"), THE Chat_Service SHALL resolve the Intent to retrieve scheme data
3. WHEN the Chat_Service receives a message containing transaction-related keywords (e.g., "transactions", "trades", "buys", "sells"), THE Chat_Service SHALL resolve the Intent to retrieve transaction data
4. WHEN the Chat_Service receives a message containing holdings-related keywords (e.g., "holdings", "portfolio", "positions"), THE Chat_Service SHALL resolve the Intent to retrieve holdings data
5. WHEN the Chat_Service receives a message containing report-related keywords (e.g., "report", "capital gains", "generate report"), THE Chat_Service SHALL resolve the Intent to provide report download information
6. IF the Chat_Service cannot determine the Intent from the message, THEN THE Chat_Service SHALL return a response listing the Supported_Query types

### Requirement 4: Client Data Query

**User Story:** As an admin, I want to ask for client information in the chat, so that I can quickly see client details.

#### Acceptance Criteria

1. WHEN the Intent is to retrieve client data, THE Chat_Service SHALL return the list of all clients with their id, name, email, and phone
2. WHEN the Admin message includes a specific client name, THE Chat_Service SHALL filter the results to clients matching that name
3. THE Chat_Screen SHALL display client Query_Result in a readable tabular format within the conversation history

### Requirement 5: Scheme Data Query

**User Story:** As an admin, I want to ask for scheme information in the chat, so that I can review investment schemes quickly.

#### Acceptance Criteria

1. WHEN the Intent is to retrieve scheme data, THE Chat_Service SHALL return the list of all schemes with their id, name, type, and NAV
2. WHEN the Admin message includes a specific scheme name or type, THE Chat_Service SHALL filter the results to matching schemes
3. THE Chat_Screen SHALL display scheme Query_Result in a readable tabular format within the conversation history

### Requirement 6: Transaction Data Query

**User Story:** As an admin, I want to ask for transaction information in the chat, so that I can review trading activity.

#### Acceptance Criteria

1. WHEN the Intent is to retrieve transaction data, THE Chat_Service SHALL return the list of transactions with id, clientId, schemeId, type, units, amount, and date
2. WHEN the Admin message includes a filter parameter (e.g., a client name or transaction type like "buy" or "sell"), THE Chat_Service SHALL filter the results accordingly
3. THE Chat_Screen SHALL display transaction Query_Result in a readable tabular format within the conversation history

### Requirement 7: Holdings Data Query

**User Story:** As an admin, I want to ask for holdings information in the chat, so that I can see portfolio positions at a glance.

#### Acceptance Criteria

1. WHEN the Intent is to retrieve holdings data, THE Chat_Service SHALL return holdings with clientName, schemeName, units, and holdingValue
2. WHEN the Admin message includes a client name or scheme name filter, THE Chat_Service SHALL pass the corresponding clientId or schemeId filter to the holdings query
3. WHEN the Admin message includes a sort preference (e.g., "sorted by value", "highest value first"), THE Chat_Service SHALL apply the corresponding sort order to the holdings query
4. THE Chat_Screen SHALL display holdings Query_Result in a readable tabular format within the conversation history

### Requirement 8: Report Information Query

**User Story:** As an admin, I want to ask about reports in the chat, so that I can get download links for client reports.

#### Acceptance Criteria

1. WHEN the Intent is to provide report information and the Admin message includes a client name, THE Chat_Service SHALL return download URLs for the available report types (holdings, transactions, capital gains) for that client
2. IF the Intent is to provide report information and the Admin message does not include a client name, THEN THE Chat_Service SHALL respond asking the Admin to specify a client name
3. THE Chat_Screen SHALL render report download URLs as clickable links in the conversation history

### Requirement 9: Chat API Endpoint

**User Story:** As a developer, I want a dedicated chat API endpoint, so that the frontend can send messages and receive structured responses.

#### Acceptance Criteria

1. THE Chat_Controller SHALL expose a POST endpoint at `/api/chat` that accepts a JSON body with a `message` field
2. THE Chat_Controller SHALL return a JSON response containing a `reply` field with the text response and an optional `data` field with structured query results
3. WHILE the Admin session is not authenticated, THE Chat_Controller SHALL reject requests with HTTP 401 status
4. IF the `message` field is missing or empty, THEN THE Chat_Controller SHALL return HTTP 400 with a descriptive error message

### Requirement 10: Conversation History Display

**User Story:** As an admin, I want to see the full conversation history during my session, so that I can reference previous queries and results.

#### Acceptance Criteria

1. THE Chat_Screen SHALL maintain and display all Chat_Message entries for the current browser session
2. THE Chat_Screen SHALL visually distinguish between Admin messages and Chat_Service responses using different alignment and styling
3. WHEN a new Chat_Message is added, THE Chat_Screen SHALL automatically scroll to the most recent message
4. THE Chat_Screen SHALL display timestamps on each Chat_Message

### Requirement 11: Error Handling

**User Story:** As an admin, I want clear error messages when something goes wrong, so that I understand what happened and can retry.

#### Acceptance Criteria

1. IF the Chat_Controller returns an HTTP error status, THEN THE Chat_Screen SHALL display a user-friendly error message in the conversation history
2. IF a network error occurs while sending a chat message, THEN THE Chat_Screen SHALL display a connection error message and re-enable the send button
3. IF the Chat_Service encounters an error while querying data, THEN THE Chat_Service SHALL return a response indicating the query failed with a descriptive reason
