# Simple Flow Guide - ASCII Diagrams (Works Everywhere!)

This guide shows the flow in simple ASCII format that works in IntelliJ without any plugins.

---

## 1️⃣ How a Request Flows Through the System

```
┌─────────────────────────────────────────────────────────────────────┐
│  Client Sends POST /api/locators/analyze                            │
│  {                                                                   │
│    "locator": "//*[@id='search']",                                  │
│    "htmlContent": "<html>...</html>",                               │
│    "elementDescription": "Search button",                           │
│    "pageUrl": "https://example.com"                                 │
│  }                                                                   │
└────────────────┬────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  LocatorController receives request                                  │
│  📋 Validates input (locator, htmlContent required)                 │
└────────────────┬────────────────────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
   ✅ Valid          ❌ Invalid
        │                 │
        │                 ├─→ Return 400 Bad Request
        │                 │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│  HtmlPreprocessor.preprocessHtml()                                   │
│  • Remove <script> tags                                              │
│  • Remove <style> tags                                               │
│  • Remove comments                                                   │
│  • Clean whitespace                                                  │
│  Result: Jsoup Document (optimized, ~90% smaller)                   │
└────────────────┬────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  DomQueryTools.setDocument(doc)                                      │
│  • Store cleaned HTML in ThreadLocal                                │
│  • Make available to AI tools                                        │
│  • Keep it on server (secure!)                                      │
└────────────────┬────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  LocatorAnalyzerAI.analyzeLocator()                                  │
│  ❌ NOT sending: htmlContent (too big!)                             │
│  ✅ Sending only:                                                   │
│    • failedLocator: "//*[@id='search']"                             │
│    • elementDescription: "Search button"                             │
│    • pageUrl: "https://example.com"                                 │
│                                                                      │
│  Why? AI gets 3 small params + tools to query the HTML!            │
└────────────────┬────────────────────────────────────────────────────┘
                 │
                 ▼
        ┌─────────────────────────────────────────┐
        │  Claude AI Analysis                      │
        │  (with tool calling enabled)             │
        └─────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
    Step 1:         Step 2:          Step 3:
  Test Failed    See What's      Find Target
    Locator      Available       Element
        │               │              │
    Call tool:      Call tool:    Call tool:
  findByXPath()  getAllInteractive findByText()
        │               │              │
    No match      50+ elements     Found it!
        │               │              │
        └───────────────┼──────────────┘
                        │
                        ▼
        ┌─────────────────────────────────────────┐
        │  Step 4: Test Alternatives              │
        │  • Call findById('search') ✓ Works      │
        │  • Call findByCss('input.search') ✓     │
        │  • Call findByXPath('//input[@id...]')  │
        │                                          │
        │  Step 5: Score & Rank                   │
        │  By.id = most stable → 🥇 RECOMMENDED  │
        │  By.css = flexible → 🥈 Alternative    │
        │  By.xpath = brittle → 🥉 Last resort   │
        └─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────┐
│  AI Returns LocatorAnalysisResult                                    │
│  {                                                                   │
│    elementFound: true,                                               │
│    recommendedLocator: "search",                                     │
│    recommendedLocatorType: "ID",                                     │
│    alternatives: {                                                   │
│      CSS_SELECTOR: "input.search",                                   │
│      XPATH: "//*[@id='search']"                                      │
│    },                                                                │
│    confidence: 0.95,                                                 │
│    explanation: "..."                                                │
│  }                                                                   │
└────────────────┬────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  LocatorResponseMapper.toResponse()                                  │
│  Convert LocatorAnalysisResult to LocatorAnalysisResponse            │
│  (Same data, different format for client)                            │
└────────────────┬────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  DomQueryTools.clearDocument()                                       │
│  • Remove HTML from ThreadLocal                                      │
│  • Free up memory                                                    │
│  • Clean up after ourselves                                          │
└────────────────┬────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Return 200 OK + JSON Response to Client                             │
│  {                                                                   │
│    "elementFound": true,                                             │
│    "recommendedLocator": "search",                                   │
│    "recommendedLocatorType": "ID",                                   │
│    "alternatives": {...},                                            │
│    "confidence": 0.95,                                               │
│    "explanation": "..."                                              │
│  }                                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2️⃣ Architecture Components

```
┌─────────────────────────────────────────────────────────────────┐
│                    HTTP Request / Response                        │
└─────────┬───────────────────────────────────────────────┬────────┘
          │                                               │
          ▼                                               ▲
     ┌─────────────────────────────────────────────┐     │
     │  LocatorController                          │     │
     │  • Receives POST /api/locators/analyze      │─────┘
     │  • Orchestrates the flow                    │
     └──┬──────────────────────────────────────────┘
        │
        │ Validates
        ▼
     ┌─────────────────────────────────────────────┐
     │  LocatorRequestValidator                    │
     │  • Check locator not empty                  │
     │  • Check htmlContent not empty              │
     │  • Return validation errors                 │
     └──┬──────────────────────────────────────────┘
        │
        │ Preprocesses
        ▼
     ┌─────────────────────────────────────────────┐
     │  HtmlPreprocessor                           │
     │  ├─ Uses HtmlMinificationService            │
     │  ├─ Uses HtmlUtilityService                 │
     │  └─ Returns cleaned Jsoup Document          │
     └──┬──────────────────────────────────────────┘
        │
        │ Stores
        ▼
     ┌─────────────────────────────────────────────┐
     │  DomQueryTools (ThreadLocal Storage)         │
     │  • Store HTML for this thread only          │
     │  • Make available to tools                  │
     │  • Thread-safe (each thread has own copy)   │
     └──┬──────────────────────────────────────────┘
        │
        │ Calls
        ▼
     ┌─────────────────────────────────────────────┐
     │  LocatorAnalyzerAI (Claude via LangChain4j) │
     │  ├─ With tool calling enabled               │
     │  ├─ Has access to 6 tools                   │
     │  └─ Returns analysis result                 │
     └──┬──────────────────────────────────────────┘
        │                      ▲
        │                      │
        └──────────────────────┘
           Uses tools to
          query DomQueryTools
        (findByXPath, findByCss,
         findById, etc.)
        │
        │ Converts result
        ▼
     ┌─────────────────────────────────────────────┐
     │  LocatorResponseMapper                      │
     │  • Convert to response format               │
     │  • Return to client                         │
     └─────────────────────────────────────────────┘
```

---

## 3️⃣ Key Insight: Why HTML is NOT Sent to AI

```
TRADITIONAL APPROACH (❌ WASTEFUL)
───────────────────────────────────

Request: "Find alternative for //*[@id='x']"
         + Full HTML (50KB)
         + All page data

         ▼ Network upload

API Server: Receives 50KB HTML
            Sends to AI: Full HTML

Claude AI: Processes 50KB
          Costs: High tokens

TOTAL COST: $$$$$ (wasted tokens)


OUR SMART APPROACH (✅ EFFICIENT)
──────────────────────────────────

Request: "Find alternative for //*[@id='x']"
         + Full HTML (50KB)
         + Description: "Search button"
         + URL: "https://..."

         ▼ Network upload

API Server: Receives HTML
            Preprocesses → 5KB (cleaned)
            Stores in ThreadLocal

            Sends to AI:
            • locator: "//*[@id='x']"
            • description: "Search button"
            • pageUrl: "..."
            ❌ No HTML sent!

Claude AI: Uses 3 small params
          Calls tools when needed:
          • "findByText('Search button')"
          • Tool queries ThreadLocal HTML
          • Returns matches

          Only charged for:
          • 3 params (small)
          • Tool calls (small responses)

TOTAL COST: $ (efficient!)
```

---

## 4️⃣ What Tools AI Can Use

```
AI has access to these tools to query the stored HTML:

┌─ findByXPath(xpath) ─────────────────────────────────────┐
│  Usage: Test XPath expressions                           │
│  Example: findByXPath("//*[@id='login']")               │
│  Returns: List of matching elements                      │
└──────────────────────────────────────────────────────────┘

┌─ findByCss(selector) ────────────────────────────────────┐
│  Usage: Test CSS selectors                               │
│  Example: findByCss("button.submit-btn")                │
│  Returns: List of matching elements                      │
└──────────────────────────────────────────────────────────┘

┌─ findById(id) ───────────────────────────────────────────┐
│  Usage: Find element by ID                               │
│  Example: findById("search-input")                       │
│  Returns: Element (if exists) or null                    │
└──────────────────────────────────────────────────────────┘

┌─ findByAttribute(name, value) ──────────────────────────┐
│  Usage: Find by any HTML attribute                       │
│  Example: findByAttribute("data-testid", "submit-btn")  │
│  Returns: List of matching elements                      │
└──────────────────────────────────────────────────────────┘

┌─ findByText(text) ───────────────────────────────────────┐
│  Usage: Find element by visible text                     │
│  Example: findByText("Click here")                       │
│  Returns: List of elements with that text               │
└──────────────────────────────────────────────────────────┘

┌─ getAllInteractiveElements() ────────────────────────────┐
│  Usage: List ALL buttons, inputs, links                  │
│  Example: getAllInteractiveElements()                    │
│  Returns: JSON array of all interactive elements         │
│           with their IDs, classes, names, etc.          │
└──────────────────────────────────────────────────────────┘
```

---

## 5️⃣ AI's Analysis Steps

```
When you send: locator="//*[@id='old-id']"
              description="Search button"

Claude AI does this:

STEP 1: Test the Failed Locator
┌────────────────────────────────┐
│ findByXPath("//*[@id='old-id']")│
└─────────┬──────────────────────┘
          │
          ▼
    No matches found ❌
    (This is why it failed!)

STEP 2: See What's Available
┌────────────────────────────────┐
│ getAllInteractiveElements()      │
└─────────┬──────────────────────┘
          │
          ▼
    50+ elements found:
    {
      "id": "new-id",
      "class": "search-btn",
      "name": "q",
      "text": "Search"
    }
    ... more elements ...

STEP 3: Find Target Element
┌────────────────────────────────┐
│ findByText("Search button")      │
└─────────┬──────────────────────┘
          │
          ▼
    Found element with:
    • id = "new-id"
    • class = "search-btn"
    • name = "q"

STEP 4: Test Alternatives
┌─────────────────────────────────────────────┐
│ findById("new-id")              ✓ Works!    │
│ findByCss("input.search-btn")   ✓ Works!    │
│ findByXPath("//input[@id='...']")✓ Works!   │
└─────────────────────────────────────────────┘

STEP 5: Recommend Best Option
┌──────────────────────────────────────────────┐
│ 🥇 By.ID: "new-id"        (most reliable)   │
│ 🥈 By.CSS: ".search-btn"   (flexible)       │
│ 🥉 By.XPATH: "//input..."  (brittle)        │
└──────────────────────────────────────────────┘

RETURNS:
{
  "elementFound": true,
  "recommendedLocator": "new-id",
  "recommendedLocatorType": "ID",
  "confidence": 0.98,
  "alternatives": {...}
}
```

---

## 6️⃣ Locator Type Preferences

```
Best ──────────────────────────────────── Worst

🥇 By.ID
    ▲▲▲▲▲ Most stable & fast
    Perfect when ID exists
    Unique, reliable, unchanged
    [PREFERRED]

🥈 By.NAME
    ▲▲▲▲ Good for form elements
    Standard attribute
    Usually stable
    [GOOD]

🥉 By.LINK_TEXT / PARTIAL_LINK_TEXT
    ▲▲▲▲ For hyperlinks only
    Semantic (uses visible text)
    [GOOD]

4️⃣ By.CSS_SELECTOR
    ▲▲▲▲ Flexible & performant
    Can be brittle if not careful
    [GOOD]

5️⃣ By.CLASS_NAME
    ▲▲▲ Classes often shared
    Can be fragile
    [WEAK]

6️⃣ By.XPATH
    ▲▲ Powerful but brittle
    Breaks with DOM changes
    Performance issues
    [LAST RESORT]
```

---

## 7️⃣ Request & Response Format

```
SENDING REQUEST:
─────────────────

POST /api/locators/analyze
Content-Type: application/json

{
  "locator": "//*[@id='search']",          ← REQUIRED
  "htmlContent": "<html>...</html>",       ← REQUIRED
  "elementDescription": "Search button",   ← optional
  "pageUrl": "https://example.com"         ← optional
}

Size: Small request with large HTML


RECEIVING RESPONSE:
────────────────────

HTTP 200 OK
Content-Type: application/json

{
  "elementFound": true,
  "recommendedLocator": "search",
  "recommendedLocatorType": "ID",
  "alternatives": {
    "CSS_SELECTOR": "input#search",
    "NAME": "q",
    "XPATH": "//*[@id='search']"
  },
  "confidence": 0.95,
  "explanation": "Found input element..."
}

Size: Small response
Time: Fast!
Cost: Low!


ERROR RESPONSE (400):
──────────────────────

{
  "statusCode": 400,
  "error": "locator cannot be empty"
}

ERROR RESPONSE (500):
──────────────────────

{
  "statusCode": 500,
  "error": "Internal server error: ..."
}
```

---

## 8️⃣ Error Handling

```
Request arrives
      │
      ▼
   Is valid input?
      │
  ┌───┴───┐
  ▼       ▼
 YES      NO ──→ Return 400 Bad Request
  │
  ▼
Process HTML
  │
  ▼
Valid HTML?
  │
  ┌───┴───┐
  ▼       ▼
 YES      NO ──→ Return 400 Bad Request
  │
  ▼
Call AI
  │
  ▼
AI Success?
  │
  ┌───┴───┐
  ▼       ▼
 YES      NO ──→ Return 500 Internal Error
  │
  ▼
Format response
  │
  ▼
Return 200 OK with recommendations


In all cases:
→ Clean up resources (clear ThreadLocal)
→ Log what happened
→ Return appropriate response code
```

---

## 9️⃣ File Organization

```
LocatorController.java
├─ Receives HTTP request
├─ Validates using LocatorRequestValidator
├─ Preprocesses using HtmlPreprocessor
├─ Stores in DomQueryTools
├─ Calls LocatorAnalyzerAI
├─ Formats using LocatorResponseMapper
└─ Returns response

HtmlPreprocessor.java
├─ Removes <script> tags
├─ Removes <style> tags
├─ Uses HtmlMinificationService
├─ Uses HtmlUtilityService
└─ Returns Jsoup Document

DomQueryTools.java
├─ Thread-safe storage (ThreadLocal)
├─ Implements 6 tool methods
├─ Queries Jsoup Document
└─ Called by AI during analysis

LocatorAnalyzerAI.java
├─ Interface for Claude AI
├─ Receives 3 parameters (no HTML)
├─ Tool calling enabled
└─ Returns LocatorAnalysisResult

DTO Classes:
├─ LocatorAnalysisRequest (incoming)
├─ LocatorAnalysisResult (from AI)
├─ LocatorAnalysisResponse (outgoing)
└─ LocatorType enum
```

---

## 🔟 Quick Checklist for New Users

```
When you make a request:

Before sending:
☐ Do you have the failed locator?      e.g., "//*[@id='x']"
☐ Do you have the page HTML?           e.g., driver.getPageSource()
☐ Optional: Element description?       e.g., "Search button"
☐ Optional: Page URL?                  e.g., driver.getCurrentUrl()

The system will:
☐ Validate your input
☐ Preprocess HTML (make it smaller)
☐ Store HTML safely on server
☐ Tell AI about the problem
☐ AI uses tools to query HTML
☐ AI finds best alternative locator
☐ Return recommendations

You will receive:
☐ Best locator strategy (By.id, By.css, etc.)
☐ The actual locator string
☐ Alternative locators
☐ Confidence score (0.0 to 1.0)
☐ Explanation of why this strategy is best

Status codes:
☐ 200 = Success! Use the recommendations
☐ 400 = Invalid request (check your input)
☐ 500 = Server error (check logs)
```

---

## Summary

**The Main Idea:**

1. You send: Broken locator + Page HTML
2. System: Preprocesses HTML, stores locally
3. AI gets: Only the locator + description (NOT HTML)
4. AI uses: Tools to query stored HTML
5. You get: Best alternative locator + alternatives
6. Result: Fast, efficient, low-cost analysis!

**Key Benefits:**
- ✅ Small API payloads (no full HTML to AI)
- ✅ Fast responses (AI is smart about what to query)
- ✅ Low cost (fewer tokens)
- ✅ Secure (HTML stays on server)
- ✅ Flexible (AI decides analysis strategy)

**For visual diagrams, see:**
- FLOW_DIAGRAMS.md (Mermaid diagrams - best on GitHub)
- README.md (Overview with some diagrams)
- This file (ASCII - works everywhere!)
