# Draftly API Reference

Base URL: `http://localhost:8080`

Authentication: OAuth2 session-based (Google login). All endpoints require a valid session cookie (`JSESSIONID`). The frontend must include credentials (`credentials: 'include'`).

---

## Threads

### List Threads

Returns all email threads for the authenticated user.

```
GET /api/v1/threads
```

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "gmailThreadId": "191234567890abcdef",
    "subject": "Q4 Budget Review",
    "lastSynchronizedAt": "2025-05-31T10:30:00Z"
  },
  {
    "id": 2,
    "gmailThreadId": "19fedcba0987654321",
    "subject": "Design Feedback",
    "lastSynchronizedAt": "2025-05-31T09:15:00Z"
  }
]
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Internal database ID |
| `gmailThreadId` | String | Gmail's thread identifier |
| `subject` | String | Email subject line |
| `lastSynchronizedAt` | DateTime (ISO-8601) | Last sync timestamp |

---

### Get Thread Detail

Returns a single thread with all its messages, sorted newest-first.

```
GET /api/v1/threads/{id}
```

**Path Parameters**

| Name | Type | Description |
|------|------|-------------|
| `id` | Long | Internal thread ID |

**Response `200 OK`**
```json
{
  "id": 1,
  "gmailThreadId": "191234567890abcdef",
  "subject": "Q4 Budget Review",
  "lastSynchronizedAt": "2025-05-31T10:30:00Z",
  "messages": [
    {
      "messageId": "1912345abc",
      "from": "Alice Johnson <alice@example.com>",
      "to": "Bob Smith <bob@example.com>",
      "subject": "Re: Q4 Budget Review",
      "date": "Fri, 30 May 2025 16:45:00 +0000",
      "snippet": "Sounds good, let's finalize on Monday.",
      "body": "Sounds good, let's finalize on Monday.\n\nBest,\nAlice"
    },
    {
      "messageId": "1912345def",
      "from": "Bob Smith <bob@example.com>",
      "to": "Alice Johnson <alice@example.com>",
      "subject": "Q4 Budget Review",
      "date": "Fri, 30 May 2025 14:00:00 +0000",
      "snippet": "Please review the attached Q4 budget proposal...",
      "body": "Please review the attached Q4 budget proposal..."
    }
  ]
}
```

**Response Fields**

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Internal thread ID |
| `gmailThreadId` | String | Gmail thread identifier |
| `subject` | String | Thread subject |
| `lastSynchronizedAt` | DateTime (ISO-8601) | Last sync timestamp |
| `messages` | Array | List of messages in the thread |

**Message Fields**

| Field | Type | Description |
|-------|------|-------------|
| `messageId` | String | Gmail message ID |
| `from` | String | Sender name and email |
| `to` | String | Recipient name and email |
| `subject` | String | Message subject |
| `date` | String | RFC 2822 date header |
| `snippet` | String | Plain-text summary (Gmail snippet) |
| `body` | String or null | Decoded email body text (from Base64url MIME), null if unavailable |

**Response `404 Not Found`** — Returned when the thread does not exist or does not belong to the authenticated user.

---

### Generate Draft

Generates an AI-powered draft reply for a thread. The latest message in the thread is used as the email to reply to. The user's 3 most recent sent emails are used as writing style samples.

```
POST /api/v1/threads/{id}/generate-draft
```

**Path Parameters**

| Name | Type | Description |
|------|------|-------------|
| `id` | Long | Internal thread ID |

**Request Body**

```json
{
  "tone": "CONCISE"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `tone` | String | Yes | One of: `CONCISE`, `FORMAL`, `FRIENDLY` |

**Tone Descriptions**

| Tone | Behavior |
|------|----------|
| `CONCISE` | Keeps response under 2-3 sentences. Short, direct, action-oriented. |
| `FORMAL` | Uses high-caliber corporate vernacular. Structured, respectful, professional. |
| `FRIENDLY` | Warm, welcoming, conversational, light-hearted while preserving professional boundaries. |

**Response `200 OK`**
```json
{
  "id": 10,
  "threadId": 1,
  "gmailThreadId": "191234567890abcdef",
  "subject": "Q4 Budget Review",
  "suggestedBody": "Hi Alice, I've reviewed the proposal and everything looks good. Let's connect Monday morning to finalize.",
  "currentTone": "CONCISE",
  "status": "PENDING",
  "updatedAt": "2025-05-31T11:00:00Z"
}
```

**Response Fields**

Same as [`DraftResponseDto`](#draftresponsebody).

**Response `404 Not Found`** — Thread does not exist or does not belong to the user.

---

## Drafts

### List Pending Drafts

Returns all drafts with status `PENDING`.

```
GET /api/v1/drafts/pending
```

**Response `200 OK`**
```json
[
  {
    "id": 10,
    "threadId": 1,
    "gmailThreadId": "191234567890abcdef",
    "subject": "Q4 Budget Review",
    "suggestedBody": "Hi Alice, I've reviewed the proposal...",
    "currentTone": "CONCISE",
    "status": "PENDING",
    "updatedAt": "2025-05-31T11:00:00Z"
  }
]
```

---

### Edit Draft Body

Manually edits the body content of a draft. The draft status is set to `EDITED`.

```
PUT /api/v1/drafts/{id}/body
```

**Path Parameters**

| Name | Type | Description |
|------|------|-------------|
| `id` | Long | Internal draft ID |

**Request Body**

```json
{
  "updatedBody": "Hi Alice, reviewed and approved. See you Monday."
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `updatedBody` | String | Yes | New draft body text |

**Response `200 OK`**
```json
{
  "id": 10,
  "threadId": 1,
  "gmailThreadId": "191234567890abcdef",
  "subject": "Q4 Budget Review",
  "suggestedBody": "Hi Alice, reviewed and approved. See you Monday.",
  "currentTone": "CONCISE",
  "status": "EDITED",
  "updatedAt": "2025-05-31T11:05:00Z"
}
```

**Response `404 Not Found`** — Draft does not exist.

---

### Regenerate Draft Tone

Marks the existing draft as `REJECTED` and generates a fresh draft with a new tone. The original email body must be re-supplied to preserve generation context.

```
POST /api/v1/drafts/{id}/tone
```

**Path Parameters**

| Name | Type | Description |
|------|------|-------------|
| `id` | Long | Internal draft ID |

**Request Body**

```json
{
  "requestedTone": "FORMAL",
  "incomingEmailBody": "Hi Bob, please review the attached Q4 budget proposal and let me know your thoughts by Monday."
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `requestedTone` | String | Yes | One of: `CONCISE`, `FORMAL`, `FRIENDLY` |
| `incomingEmailBody` | String | Yes | The original email body being replied to (needed for re-generation context) |

**Response `200 OK`**
```json
{
  "id": 11,
  "threadId": 1,
  "gmailThreadId": "191234567890abcdef",
  "subject": "Q4 Budget Review",
  "suggestedBody": "Dear Alice, I have reviewed the budget proposal thoroughly and find it satisfactory. I suggest we convene on Monday to discuss final approval.",
  "currentTone": "FORMAL",
  "status": "PENDING",
  "updatedAt": "2025-05-31T11:10:00Z"
}
```

**Notes:**
- The old draft's status becomes `REJECTED`.
- A new draft is created with `PENDING` status and a fresh AI generation.
- The returned draft has a new `id`.

---

### Send Draft

Sends the draft as a reply email via the Gmail API. The email is threaded correctly using `In-Reply-To` and `References` MIME headers.

```
POST /api/v1/drafts/{id}/send
```

**Path Parameters**

| Name | Type | Description |
|------|------|-------------|
| `id` | Long | Internal draft ID |

**Headers**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `X-Idempotency-Key` | String | Yes | Client-generated unique key (e.g., UUID v4) to prevent duplicate sends |

**Response `200 OK`**
```json
{
  "success": true,
  "message": "Email successfully sent.",
  "gmailMessageId": "1912345xyz"
}
```

**Response Fields**

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Whether the email was sent |
| `message` | String | Human-readable status message |
| `gmailMessageId` | String or null | Gmail message ID of the sent email (present only on success) |

**Response `400 Bad Request`**
```json
{
  "success": false,
  "message": "Missing required mandatory HTTP Header constraint: X-Idempotency-Key"
}
```
Returned when the `X-Idempotency-Key` header is missing or blank.

**Response `409 Conflict`**
```json
{
  "success": false,
  "message": "Execution aborted: This specific draft has already been successfully dispatched."
}
```
Returned when the draft has already been sent (`status = APPROVED`).

```json
{
  "success": false,
  "message": "Duplicate execution caught via Idempotency Token identification."
}
```
Returned when the same `X-Idempotency-Key` was used for a previous successful send.

---

## Common DTOs

### DraftResponseBody

Returned by draft-related endpoints.

```json
{
  "id": 10,
  "threadId": 1,
  "gmailThreadId": "191234567890abcdef",
  "subject": "Q4 Budget Review",
  "suggestedBody": "Hi Alice, I've reviewed the proposal...",
  "currentTone": "CONCISE",
  "status": "PENDING",
  "updatedAt": "2025-05-31T11:00:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Draft ID |
| `threadId` | Long | Parent thread ID |
| `gmailThreadId` | String | Gmail thread identifier |
| `subject` | String | Thread subject |
| `suggestedBody` | String | AI-generated draft body |
| `currentTone` | String | Draft tone: `CONCISE`, `FORMAL`, or `FRIENDLY` |
| `status` | String | Draft status: `PENDING`, `EDITED`, `APPROVED`, or `REJECTED` |
| `updatedAt` | DateTime (ISO-8601) | Last update timestamp |

---

## Error Responses

All error responses are returned as JSON with the appropriate HTTP status code.

| Status Code | Meaning |
|-------------|---------|
| `200` | Success |
| `400` | Bad Request (missing/invalid parameters) |
| `404` | Not Found (resource does not exist or access denied) |
| `409` | Conflict (idempotency violation or resource already processed) |

Spring Security redirects unauthenticated requests to Google's OAuth2 login page rather than returning a JSON error.

---

## Common Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Content-Type` | For requests with body | `application/json` |
| `X-Idempotency-Key` | For `POST /api/v1/drafts/{id}/send` only | UUID or unique string |

The session cookie (`JSESSIONID`) is handled automatically by the browser when credentials are included.
