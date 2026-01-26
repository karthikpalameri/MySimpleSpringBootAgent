# Flow Diagrams - Visual Guide for New Users

> **Note**: For best viewing of Mermaid diagrams, view this on GitHub.com or use an IDE plugin like Mermaid Viewer
> For IntelliJ, see setup instructions at the end of this file.

## 1️⃣ Main Process Flow (How the System Works)

```mermaid
sequenceDiagram
    participant User as User/Client
    participant API as Spring Boot API
    participant HTML as HtmlPreprocessor
    participant Tools as DomQueryTools
    participant AI as Claude AI
    participant Response as ResponseMapper

    User->>API: POST /analyze<br/>(locator, htmlContent,<br/>description, url)
    API->>API: ✓ Validate request
    API->>HTML: Preprocess HTML<br/>(clean, optimize)
    HTML-->>API: Jsoup Document<br/>(cleaned HTML)
    API->>Tools: Store in ThreadLocal<br/>(accessible to AI)

    API->>AI: Call analyzeLocator()<br/>ONLY sends:<br/>- locator string<br/>- description<br/>- pageUrl<br/>(NO HTML!)

    Note over AI: AI analyzes without full HTML
    AI->>Tools: findByXPath(xpath)<br/>Query the stored HTML
    Tools-->>AI: Results

    AI->>Tools: getAllInteractiveElements()<br/>See what's available
    Tools-->>AI: List of buttons/inputs

    AI->>Tools: findByCss(selector)<br/>Test alternatives
    Tools-->>AI: Match found

    AI-->>API: LocatorAnalysisResult<br/>(with recommendations)
    API->>Tools: clearDocument()<br/>(cleanup)
    API->>Response: toResponse(aiResult)
    Response-->>API: LocatorAnalysisResponse
    API-->>User: 200 OK + JSON response
```

**What This Shows:**
- User sends request with HTML + locator
- HTML is cleaned and stored (not sent to AI!)
- AI receives only 3 parameters: locator, description, pageUrl
- AI uses tools to query the stored HTML
- Response is mapped and returned to user
- ThreadLocal is cleaned up after response

---

## 2️⃣ Architecture Overview

```mermaid
graph TB
    subgraph "🌐 Controller Layer"
        API["LocatorController<br/>(HTTP endpoints)"]
    end

    subgraph "🛠️ Validation & Formatting"
        VAL["LocatorRequestValidator<br/>(Input validation)"]
        MAP["LocatorResponseMapper<br/>(Format output)"]
    end

    subgraph "📝 HTML Processing"
        PRE["HtmlPreprocessor<br/>(Clean & optimize)"]
        MIN["HtmlMinificationService<br/>(Reduce size)"]
        UTIL["HtmlUtilityService<br/>(Utilities)"]
    end

    subgraph "🔧 DOM Tools"
        DOM["DomQueryTools<br/>(Query DOM with tools)"]
    end

    subgraph "🤖 AI Layer"
        AI["LocatorAnalyzerAI<br/>(Claude via LangChain4j)"]
    end

    API -->|Validates| VAL
    API -->|Preprocesses| PRE
    PRE --> MIN
    PRE --> UTIL
    API -->|Stores| DOM
    API -->|Calls| AI
    AI -->|Uses tools| DOM
    API -->|Formats| MAP

    style API fill:#e3f2fd,color:#000
    style AI fill:#fff3e0,color:#000
    style DOM fill:#e8f5e9,color:#000
    style PRE fill:#f3e5f5,color:#000
    style MAP fill:#fce4ec,color:#000
```

**Components:**
- **LocatorController**: Handles all HTTP requests
- **Validators**: Check input is valid
- **HtmlPreprocessor**: Cleans HTML, removes scripts/styles
- **DomQueryTools**: Provides tools for AI to query HTML
- **LocatorAnalyzerAI**: Claude AI interface
- **ResponseMapper**: Formats response JSON

---

## 3️⃣ Step-by-Step Processing

```mermaid
graph TD
    A["1️⃣ Request Arrives<br/>locator, html, description, url"]
    --> B["2️⃣ Validate<br/>Check all required fields"]
    --> C{"Valid?"}

    C -->|❌ No| D["Return 400<br/>Bad Request"]
    C -->|✅ Yes| E["3️⃣ Preprocess HTML<br/>Remove scripts/styles<br/>Clean up text"]

    E --> F["4️⃣ Store in ThreadLocal<br/>Make available to tools"]
    --> G["5️⃣ Call AI<br/>with 3 parameters only"]

    G --> H["🤖 AI Analysis<br/>Uses tools to query HTML"]

    H --> I["6️⃣ Format Response<br/>Convert to JSON"]
    --> J["7️⃣ Cleanup<br/>Clear ThreadLocal"]
    --> K["8️⃣ Return 200 OK<br/>with recommendations"]

    D --> L["Clean up resources"]
    K --> M["✅ Complete"]
    L --> M

    style A fill:#bbdefb
    style E fill:#c8e6c9
    style G fill:#ffe0b2
    style H fill:#f8bbd0
    style K fill:#b2dfdb
    style M fill:#c8e6c9
```

**Key Points:**
1. Request validation happens first
2. HTML is preprocessed (optimized, not sent to AI yet!)
3. Cleaned HTML stored in ThreadLocal
4. AI called with only 3 small parameters
5. AI uses tools to query the stored HTML
6. Response formatted and sent back
7. Resources cleaned up

---

## 4️⃣ AI Tool Capabilities

```mermaid
graph LR
    AI["🤖 Claude AI"]

    T1["🔍 findByXPath<br/>Test XPath<br/>Example: //*[@id='x']"]
    T2["🎨 findByCss<br/>Test CSS<br/>Example: button.active"]
    T3["🆔 findById<br/>Find by ID<br/>Example: search-btn"]
    T4["🏷️ findByAttribute<br/>Find by attr<br/>Example: data-testid"]
    T5["📝 findByText<br/>Find by text<br/>Example: 'Click'"]
    T6["📋 getAllInteractive<br/>List all buttons/<br/>inputs/links"]

    AI -->|calls| T1
    AI -->|calls| T2
    AI -->|calls| T3
    AI -->|calls| T4
    AI -->|calls| T5
    AI -->|calls| T6

    style AI fill:#fff3e0
    style T1 fill:#e8f5e9
    style T2 fill:#e8f5e9
    style T3 fill:#e8f5e9
    style T4 fill:#e8f5e9
    style T5 fill:#e8f5e9
    style T6 fill:#e8f5e9
```

**Available Tools for AI:**

| Tool | Purpose | Example |
|------|---------|---------|
| `findByXPath()` | Test XPath expressions | `//*[@id='login']` |
| `findByCss()` | Test CSS selectors | `button.submit-btn` |
| `findById()` | Find by ID attribute | `search-input` |
| `findByAttribute()` | Find by any attribute | `data-testid='submit'` |
| `findByText()` | Find by visible text | `"Click here"` |
| `getAllInteractiveElements()` | List all clickable/input elements | Returns JSON array |

---

## 5️⃣ AI Analysis Strategy

```mermaid
sequenceDiagram
    participant AI as Claude AI
    participant Tools

    Note over AI: Step 1: Understand the problem
    AI->>Tools: Test failed locator
    Tools-->>AI: Result: No matches (broken)

    Note over AI: Step 2: See available elements
    AI->>Tools: getAllInteractiveElements()
    Tools-->>AI: 50+ buttons/inputs/links with IDs/classes

    Note over AI: Step 3: Find target element
    AI->>Tools: findByText('element description')
    Tools-->>AI: Found! Here are its attributes

    Note over AI: Step 4: Test alternatives
    AI->>Tools: findById('target-id')
    Tools-->>AI: ✓ Works!

    AI->>Tools: findByCss('button.target')
    Tools-->>AI: ✓ Works!

    AI->>Tools: findByXPath('//button[@id=...]')
    Tools-->>AI: ✓ Works!

    Note over AI: Choose best (By.id)<br/>most stable & performant
    AI-->>AI: Return recommendations
```

**Analysis Steps:**
1. Test the original failed locator
2. List all available interactive elements
3. Search for element by description
4. Test multiple alternative locators
5. Score by reliability
6. Return best + alternatives

---

## 6️⃣ Why HTML is NOT Sent to AI

**Traditional Approach (Wasteful):**
```
Request → Controller → AI
          ↓
    (HTML 50KB + parameters)
          ↓
    AI processes (expensive!)
```

**Smart Approach (Efficient):**
```
Request → Controller → Store HTML locally in ThreadLocal
                              ↓
                    AI (gets 3 params only)
                              ↓
                    AI calls tools to query HTML
                              ↓
                    Tools access ThreadLocal (no network!)
```

**Benefits:**
- ✅ **Cost**: Only query what's needed
- ✅ **Speed**: Smaller API payloads
- ✅ **Privacy**: HTML stays on server
- ✅ **Flexibility**: AI decides what to search

---

## 7️⃣ Request Data Flow

**What Gets Sent in Request:**

```
POST /api/locators/analyze
{
  ✅ REQUIRED:
  - locator: "//*[@id='search']"     ← Failed locator
  - htmlContent: "<html>...</html>"  ← Full page HTML

  ➕ OPTIONAL:
  - elementDescription: "Search button"
  - pageUrl: "https://example.com"
}
```

**What AI Actually Receives:**
```
analyzeLocator(
  failedLocator: "//*[@id='search']",
  elementDescription: "Search button",
  pageUrl: "https://example.com"
)
↓
❌ htmlContent NOT sent directly
✅ But available via tools!
```

---

## 8️⃣ Response Data Flow

**What You Get Back:**

```json
{
  "elementFound": true,                    ← Found the element?
  "recommendedLocator": "search-id",       ← Best locator string
  "recommendedLocatorType": "ID",          ← Strategy: ID, CSS, XPATH, etc.
  "alternatives": {                        ← Other working options
    "CSS_SELECTOR": "input#search-id",
    "XPATH": "//*[@id='search-id']",
    "NAME": "q"
  },
  "confidence": 0.95,                      ← 95% confident
  "explanation": "Found input with stable ID..."
}
```

---

## 9️⃣ Complete Request Lifecycle

```mermaid
graph TD
    A["📥 Client POST<br/>/api/locators/analyze"]
    --> B["Spring receives<br/>LocatorAnalysisRequest"]
    --> C["LocatorController<br/>analyzeLocator()"]
    --> D["LocatorRequestValidator<br/>isValid()"]

    D --> E{"Input valid?"}
    E -->|❌ No| F["Return 400<br/>+ error message"]
    E -->|✅ Yes| G["HtmlPreprocessor<br/>preprocessHtml()"]

    G --> H["Clean & minify<br/>Jsoup Document"]
    --> I["DomQueryTools<br/>setDocument()"]
    --> J["ThreadLocal Storage<br/>Ready for AI tools"]

    J --> K["LocatorAnalyzerAI<br/>analyzeLocator()"]

    K --> L["AI calls tools<br/>findByXPath()<br/>findByCss()<br/>getAllInteractive()"]
    --> M["AI Analysis<br/>Test alternatives<br/>Score reliability"]

    M --> N["LocatorAnalysisResult<br/>Best recommendation"]

    N --> O["LocatorResponseMapper<br/>toResponse()"]
    --> P["LocatorAnalysisResponse<br/>JSON format"]

    Q["DomQueryTools<br/>clearDocument()"]
    R["Return 200 OK<br/>+ Response JSON"]

    F --> S["🧹 Cleanup<br/>Clear ThreadLocal"]
    P --> Q
    Q --> R
    S --> T["✅ Request Complete"]
    R --> T

    style A fill:#c8e6c9
    style K fill:#fff3e0
    style M fill:#fff3e0
    style R fill:#c8e6c9
    style T fill:#a5d6a7
```

---

## 🔟 Error Handling

```mermaid
graph TD
    A["Request arrives"] --> B{"Valid input?"}

    B -->|❌ Missing fields| C["400 Bad Request<br/>Error: validation failed"]
    B -->|✅ OK| D["Process HTML"]

    D --> E{"Valid HTML?"}
    E -->|❌ Malformed| F["400 Bad Request<br/>Error: invalid HTML"]
    E -->|✅ OK| G["Call AI"]

    G --> H{"AI Success?"}
    H -->|❌ Error| I["500 Internal Error<br/>Error: AI failed"]
    H -->|✅ Success| J["200 OK<br/>Recommendations"]

    C --> K["🧹 Cleanup<br/>Clear ThreadLocal<br/>Close resources"]
    F --> K
    I --> K
    J --> K
    K --> L["✅ End"]

    style J fill:#c8e6c9,color:#000
    style C fill:#ffcdd2,color:#000
    style F fill:#ffcdd2,color:#000
    style I fill:#ffcdd2,color:#000
    style L fill:#a5d6a7,color:#000
```

**Error Scenarios:**
- **400**: Invalid request (missing fields, bad HTML)
- **500**: Server error (AI processing failed)
- All scenarios: Resources are cleaned up properly

---

## 1️⃣1️⃣ Locator Type Preference

The AI prefers locators in this order:

```
🥇 By.ID              ⭐⭐⭐⭐⭐ Most stable
                      (Fast, reliable, unique)

🥈 By.NAME            ⭐⭐⭐⭐ Good for forms
                      (Standard attribute)

🥉 By.LINK_TEXT       ⭐⭐⭐⭐ For hyperlinks
                      (Semantic)

4️⃣  By.CSS_SELECTOR   ⭐⭐⭐⭐ Flexible
                      (Performant)

5️⃣  By.CLASS_NAME     ⭐⭐⭐ If unique
                      (Shared classes risky)

6️⃣  By.XPATH          ⭐⭐ Last resort
                      (Brittle, slow)
```

---

## 📚 Example Usage

### Request Example

```json
{
  "locator": "//*[@id='old-search-id']",
  "htmlContent": "<html>...<input id='new-search' name='q' class='search-input'/></html>",
  "elementDescription": "Search input field on top navigation",
  "pageUrl": "https://example.com/products"
}
```

### Response Example

```json
{
  "elementFound": true,
  "recommendedLocator": "new-search",
  "recommendedLocatorType": "ID",
  "alternatives": {
    "NAME": "q",
    "CLASS_NAME": "search-input",
    "CSS_SELECTOR": "input#new-search",
    "XPATH": "//*[@id='new-search']"
  },
  "confidence": 0.98,
  "explanation": "Element found using By.id() strategy - most reliable and performant. ID 'new-search' is stable and unique."
}
```

---

## 🔧 How to View Mermaid Diagrams

### Option 1: View on GitHub ✅ (Easiest)
1. Push this file to GitHub
2. Open in browser → Diagrams render automatically

### Option 2: IntelliJ with Markdown Plugin
1. Install "Markdown" plugin (built-in)
2. Install "Mermaid" plugin from JetBrains marketplace
3. Restart IntelliJ
4. Open .md file → Diagrams render in preview

### Option 3: Online Mermaid Editor
1. Copy diagram code
2. Go to: https://mermaid.live
3. Paste code → View rendered diagram

### Option 4: VSCode
1. VSCode has built-in Mermaid support
2. Just open the .md file → Previews automatically

---

## 📖 Quick Reference

| Concept | Location |
|---------|----------|
| Main flow | Diagram #1 |
| Components | Diagram #2 |
| Processing steps | Diagram #3 |
| AI tools | Diagram #4 |
| AI strategy | Diagram #5 |
| Why design works | Diagram #6 |
| Request format | Diagram #7 |
| Response format | Diagram #8 |
| Full lifecycle | Diagram #9 |
| Error handling | Diagram #10 |
| Locator preferences | Diagram #11 |

---

## 💡 Key Takeaways

1. **HTML uploaded once** → stored locally
2. **AI gets only 3 params** → locator, description, pageUrl
3. **Tools enable querying** → AI intelligently searches HTML
4. **ThreadLocal storage** → efficient, secure, fast
5. **Clean separation** → controller, validation, processing, AI, response
6. **Full error handling** → all scenarios covered
7. **SOLID principles** → maintainable, testable code

---

**For questions, see README.md or DOCUMENTATION.md**
