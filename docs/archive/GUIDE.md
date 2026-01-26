# Complete Guide to AI-Powered Selenium Locator Analyzer

A comprehensive guide for understanding, configuring, and using the Selenium Locator Analyzer service.

## Table of Contents

1. [Getting Started](#getting-started)
   - [Quick Start (5 minutes)](#quick-start-5-minutes)
   - [What This Tool Does](#what-this-tool-does)
   - [Basic Usage](#basic-usage)

2. [How It Works (Visual Guide)](#how-it-works-visual-guide)
   - [The Main Flow](#the-main-flow)
   - [Component Architecture](#component-architecture)
   - [AI Tool Capabilities](#ai-tool-capabilities)
   - [Complete Request Lifecycle](#complete-request-lifecycle)

3. [API Reference](#api-reference)
   - [Endpoints](#endpoints)
   - [Request Format](#request-format)
   - [Response Format](#response-format)
   - [Usage Examples](#usage-examples)

4. [Architecture & Design](#architecture--design)
   - [Component Overview](#component-overview)
   - [SOLID Principles](#solid-principles)
   - [LangChain4j Integration](#langchain4j-integration)
   - [HTML Processing Pipeline](#html-processing-pipeline)

5. [Configuration](#configuration)
   - [Application Properties](#application-properties)
   - [LLM Setup](#llm-setup)
   - [HTML Processing Tuning](#html-processing-tuning)

6. [Locator Strategies](#locator-strategies)
   - [Selenium Locator Types](#selenium-locator-types)
   - [AI Analysis Strategy](#ai-analysis-strategy)

7. [Troubleshooting](#troubleshooting)
   - [Common Issues](#common-issues)
   - [Debug Logging](#debug-logging)
   - [FAQ](#faq)

8. [Advanced Topics](#advanced-topics)
   - [Best Practices](#best-practices)
   - [Testing Strategies](#testing-strategies)
   - [Production Deployment](#production-deployment)

9. [Appendices](#appendices)
   - [Viewing Diagrams](#viewing-diagrams)
   - [Dependencies](#dependencies)
   - [Quick Reference](#quick-reference)

---

## Getting Started

### Quick Start (5 minutes)

#### Installation

```bash
# 1. Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh
ollama pull llama3.2

# 2. Clone and navigate
git clone <repo-url>
cd MySimpleSpringBootAgent

# 3. Run the service
mvn spring-boot:run
```

Service runs at `http://localhost:8080`

#### First Test

```bash
# Health check
curl http://localhost:8080/api/locators/health

# Test endpoint
curl http://localhost:8080/api/locators/test

# Analyze a locator
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "htmlContent": "<html><body><input id=\"search\" name=\"q\"/></body></html>",
    "locator": "//*[@id=\"wrong\"]",
    "pageUrl": "https://example.com"
  }'
```

**Response:**
```json
{
  "recommendedLocatorType": "ID",
  "recommendedLocator": "search",
  "byId": "search",
  "byName": "q",
  "primaryCssSelector": "#search",
  "primaryXPath": "//*[@id='search']",
  "confidence": 95,
  "elementFound": true,
  "explanation": "Use By.id for best reliability"
}
```

### What This Tool Does

**The Problem:**
Selenium test locators break when elements change. Finding alternatives is time-consuming and error-prone.

**The Solution:**
Send the failed locator and page HTML to this service. It uses AI to intelligently suggest alternatives across ALL Selenium locator types.

**Key Benefits:**
- 🎯 **All Locator Types** - ID, Name, CSS, XPath, LinkText, and more
- 🤖 **AI-Powered** - Local LLM analysis with confidence scores
- ⚡ **Smart Preprocessing** - 90%+ HTML size reduction for large pages
- 🏆 **Priority Recommendations** - ID > Name > CSS > XPath
- 🛠️ **SOLID Architecture** - Clean, maintainable code
- 📊 **Full Observability** - Complete LLM interaction logging

### Basic Usage

**Curl Example:**
```bash
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "locator": "//*[@id=\"old-id\"]",
    "htmlContent": "$(cat page.html)",
    "elementDescription": "Search button",
    "pageUrl": "https://example.com"
  }'
```

**Java/Selenium Integration:**
```java
try {
    driver.findElement(By.xpath("//*[@id='oldId']"));
} catch (NoSuchElementException e) {
    // Call service
    LocatorAnalysisResponse response = analyzeLocator(
        driver.getPageSource(),
        "//*[@id='oldId']",
        driver.getCurrentUrl()
    );

    // Use recommended locator
    WebElement element = switch (response.getRecommendedLocatorType()) {
        case "ID" -> driver.findElement(By.id(response.getRecommendedLocator()));
        case "NAME" -> driver.findElement(By.name(response.getRecommendedLocator()));
        case "CSS_SELECTOR" -> driver.findElement(By.cssSelector(response.getRecommendedLocator()));
        default -> driver.findElement(By.xpath(response.getRecommendedLocator()));
    };
}
```

---

## How It Works (Visual Guide)

### The Main Flow

```
┌────────────────────────────────────────────────────────────────┐
│  Client sends POST /api/locators/analyze                       │
│  {                                                             │
│    "locator": "//*[@id='search']",                            │
│    "htmlContent": "<html>...</html>",                         │
│    "elementDescription": "Search button",                      │
│    "pageUrl": "https://example.com"                           │
│  }                                                             │
└─────────────────┬──────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────┐
│  LocatorController receives & validates request                │
│  📋 Check locator not empty                                    │
│  📋 Check htmlContent not empty                                │
└─────────────────┬──────────────────────────────────────────────┘
         ┌────────┴──────────┐
         ▼                   ▼
    ✅ Valid          ❌ Invalid
         │                   │
         │                   └─→ Return 400 Bad Request
         │
         ▼
┌────────────────────────────────────────────────────────────────┐
│  HtmlPreprocessor.preprocessHtml()                             │
│  • Remove <script> tags                                        │
│  • Remove <style> tags                                         │
│  • Remove comments                                             │
│  • Clean whitespace                                            │
│  • Minify content                                              │
│  Result: Jsoup Document (~90% smaller)                         │
└─────────────────┬──────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────┐
│  DomQueryTools.setDocument(doc)                                │
│  • Store cleaned HTML in ThreadLocal                           │
│  • Make available to AI tools                                  │
│  • Thread-safe (each thread has own copy)                      │
└─────────────────┬──────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────┐
│  LocatorAnalyzerAI.analyzeLocator()                            │
│  ❌ NOT sending: htmlContent (too big!)                        │
│  ✅ Sending only:                                              │
│    • failedLocator: "//*[@id='search']"                        │
│    • elementDescription: "Search button"                        │
│    • pageUrl: "https://example.com"                            │
│                                                                │
│  Why? AI gets 3 small params + tools to query HTML!           │
└─────────────────┬──────────────────────────────────────────────┘
                  │
        ┌─────────┼─────────┐
        ▼         ▼         ▼
    Step 1:   Step 2:   Step 3:
  Test Orig  See What  Find Target
   Locator   Available Element
        │         │         │
    findByXPath  getAll  findByText
        │         │         │
    No match   50+ elem  Found!
        │         │         │
        └────────┬┴────────┘
                 │
                 ▼
        ┌───────────────────────────┐
        │  Step 4-5: Test & Score   │
        │  • findById() ✓ Works     │
        │  • findByCss() ✓ Works    │
        │  • findByXPath() ✓ Works  │
        │                           │
        │  Score: By.id best        │
        └────────────┬──────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────┐
│  AI Returns LocatorAnalysisResult                              │
│  {                                                             │
│    elementFound: true,                                         │
│    recommendedLocator: "search",                               │
│    recommendedLocatorType: "ID",                               │
│    alternatives: {...},                                        │
│    confidence: 0.95                                            │
│  }                                                             │
└─────────────────┬──────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────┐
│  LocatorResponseMapper converts to response format             │
│  LocatorAnalysisResponse {...}                                 │
└─────────────────┬──────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────┐
│  DomQueryTools.clearDocument()                                 │
│  • Remove HTML from ThreadLocal                                │
│  • Free up memory                                              │
│  • Clean up resources                                          │
└─────────────────┬──────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────┐
│  Return 200 OK + JSON Response to Client                       │
│  Ready to use in tests!                                        │
└────────────────────────────────────────────────────────────────┘
```

### Why HTML is NOT Sent to AI

**Key Insight**: You upload HTML once → AI intelligently queries what it needs via tools!

**Traditional Approach (Wasteful):**
```
Request → {locator + 50KB HTML} → AI processes
Cost: $$$$$ (50KB sent to AI, lots of tokens)
```

**Smart Approach (Efficient):**
```
Request → {locator + 50KB HTML}
              ↓ (stays on server)
            {3 params} → AI uses tools to query
Cost: $ (only 3 params to AI, minimal tokens)
```

**Benefits:**
- ✅ **Speed** - Smaller API payloads
- ✅ **Cost** - Fewer tokens consumed
- ✅ **Security** - HTML stays on server
- ✅ **Flexibility** - AI decides what to search

### Component Architecture

```
┌─────────────────────────────────────────┐
│  🌐 LocatorController                   │
│  • HTTP endpoints only                  │
│  • Orchestrates flow                    │
│  • Handles responses                    │
└────┬────────────────────────────────────┘
     │
     ├────→ ┌──────────────────────────────┐
     │      │ LocatorRequestValidator      │
     │      │ • Validate input             │
     │      └──────────────────────────────┘
     │
     ├────→ ┌──────────────────────────────┐
     │      │ HtmlPreprocessor             │
     │      │ • Remove scripts/styles      │
     │      │ • Clean whitespace           │
     │      │ • Minify                     │
     │      └──────────────────────────────┘
     │
     ├────→ ┌──────────────────────────────┐
     │      │ DomQueryTools                │
     │      │ • ThreadLocal storage        │
     │      │ • 6 tool methods             │
     │      │ • Query Jsoup Document       │
     │      └──────────────────────────────┘
     │
     ├────→ ┌──────────────────────────────┐
     │      │ LocatorAnalyzerAI            │
     │      │ • Claude via LangChain4j     │
     │      │ • Uses tools to analyze      │
     │      │ • Returns recommendations    │
     │      └──────────────────────────────┘
     │
     └────→ ┌──────────────────────────────┐
            │ LocatorResponseMapper        │
            │ • Convert to response format │
            │ • Return to client           │
            └──────────────────────────────┘
```

### AI Tool Capabilities

The AI has access to 6 tools for querying the stored HTML:

```
┌────────────────────────────────────────────┐
│  🤖 Claude AI                              │
└─────────────┬────────────────────────────────┘
              │
    ┌─────────┼─────────┬──────────┬──────────┬──────────┐
    │         │         │          │          │          │
    ▼         ▼         ▼          ▼          ▼          ▼

🔍 findByXPath  🎨 findByCss  🆔 findById  🏷️ findByAttribute
Test XPath      Test CSS      Find by ID   Find by attribute
//*[@id='x']    button.active search-btn   data-testid='btn'


📝 findByText           📋 getAllInteractiveElements
Find by visible text    List all buttons/inputs/links
"Click here"           Returns: [{id, class, name, ...}]
```

**Tool Details:**

| Tool | Purpose | Example |
|------|---------|---------|
| `findByXPath()` | Test XPath expressions | `//*[@id='login']` |
| `findByCss()` | Test CSS selectors | `button.submit-btn` |
| `findById()` | Find by ID attribute | `search-input` |
| `findByAttribute()` | Find by any HTML attribute | `data-testid='submit'` |
| `findByText()` | Find by visible text | `"Click here"` |
| `getAllInteractiveElements()` | List all clickable/input elements | Returns JSON array |

### Complete Request Lifecycle

**Error Handling:**

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
Cleanup (clear ThreadLocal)
  │
  ▼
Return 200 OK with recommendations
```

---

## API Reference

### Endpoints

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/api/locators/health` | Health check | `{"status":"UP"}` |
| GET | `/api/locators/test` | Test with sample HTML | Full analysis result |
| POST | `/api/locators/analyze` | Analyze failed locator | Recommendations |

### Request Format

**POST /api/locators/analyze**

```json
{
  "locator": "//*[@id='search']",              // REQUIRED: Failed locator
  "htmlContent": "<html>...</html>",           // REQUIRED: Page HTML
  "elementDescription": "Search button",       // OPTIONAL: What you're looking for
  "pageUrl": "https://example.com"             // OPTIONAL: Page context
}
```

**Field Descriptions:**
- **locator** - The XPath/CSS selector that didn't work (required, not empty)
- **htmlContent** - The full page HTML to analyze (required, not empty)
- **elementDescription** - Brief description of what element you're trying to find (helps AI)
- **pageUrl** - URL of the page being tested (optional, helps with context)

### Response Format

**Success (200 OK):**
```json
{
  "elementFound": true,                          // Was element found?
  "recommendedLocator": "search-id",             // Best locator value
  "recommendedLocatorType": "ID",                // Strategy: ID, CSS, XPATH, etc.
  "byId": "search-id",                           // Locator if using By.id
  "byName": "q",                                 // Locator if using By.name
  "byClassName": "search-input",                 // Locator if using By.className
  "byTagName": "input",                          // Locator if using By.tagName
  "byLinkText": null,                            // Locator if using By.linkText
  "byPartialLinkText": null,                     // Locator if using By.partialLinkText
  "primaryCssSelector": "#search-id",            // Primary CSS selector
  "alternativeCssSelectors": ["input.search"],   // Alternative CSS selectors
  "primaryXPath": "//*[@id='search-id']",        // Primary XPath
  "alternativeXPaths": ["//input[@name='q']"],   // Alternative XPaths
  "confidence": 95,                              // Confidence score (0-100)
  "explanation": "Use By.id for best reliability",
  "warnings": null                               // Any warnings or notes
}
```

**Error (400 Bad Request):**
```json
{
  "statusCode": 400,
  "error": "locator cannot be empty"
}
```

**Error (500 Internal Error):**
```json
{
  "statusCode": 500,
  "error": "Internal server error: AI analysis failed"
}
```

### Usage Examples

**Example 1: Search Box with ID**

Request:
```bash
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "locator": "//*[@id=\"oldId\"]",
    "htmlContent": "<html><body><input id=\"search\" name=\"q\" class=\"search-box\"/></body></html>",
    "elementDescription": "Search input",
    "pageUrl": "https://example.com"
  }'
```

Response:
```json
{
  "elementFound": true,
  "recommendedLocator": "search",
  "recommendedLocatorType": "ID",
  "byId": "search",
  "primaryCssSelector": "#search",
  "confidence": 98,
  "explanation": "Found input with stable ID 'search'"
}
```

**Example 2: Form with No ID**

Request:
```bash
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "locator": "//*[@class=\"no-longer-exists\"]",
    "htmlContent": "<html><body><input name=\"username\" placeholder=\"Username\"/></body></html>",
    "elementDescription": "Username field"
  }'
```

Response:
```json
{
  "elementFound": true,
  "recommendedLocator": "username",
  "recommendedLocatorType": "NAME",
  "byName": "username",
  "primaryCssSelector": "input[name='username']",
  "confidence": 95,
  "explanation": "Use By.name for stable form elements"
}
```

**Example 3: Using with jq**

```bash
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d @request.json \
  | jq '{type: .recommendedLocatorType, locator: .recommendedLocator, confidence: .confidence}'
```

---

## Architecture & Design

### Component Overview

**File Structure:**
```
src/main/java/com/simple/MySimpleSpringBootAgent/
├── aiservice/
│   └── LocatorAnalyzerAI.java           # AI service interface
├── controller/
│   └── LocatorController.java           # REST endpoints
├── service/
│   ├── LocatorRequestValidator.java     # Request validation
│   ├── LocatorResponseMapper.java       # DTO mapping
│   ├── HtmlPreprocessor.java            # Pipeline orchestration
│   ├── HtmlMinificationService.java     # Size reduction
│   ├── HtmlUtilityService.java          # Utilities
│   └── DomQueryTools.java               # DOM query tools
├── dto/
│   ├── LocatorAnalysisRequest.java      # API request
│   ├── LocatorAnalysisResponse.java     # API response
│   ├── LocatorAnalysisResult.java       # AI result
│   └── LocatorType.java                 # Enum
└── config/
    └── HtmlProcessingConfig.java        # Configuration
```

### SOLID Principles

**Our Approach:**

1. **Single Responsibility**
   - `LocatorController` - HTTP concerns only
   - `HtmlPreprocessor` - HTML processing only
   - `LocatorAnalyzerAI` - AI analysis only
   - `LocatorResponseMapper` - Response formatting only

2. **Open/Closed**
   - All components use interfaces
   - Easy to extend with new implementations

3. **Liskov Substitution**
   - AI services are interchangeable
   - Preprocessor strategies can be swapped

4. **Interface Segregation**
   - Small, focused interfaces
   - Components depend on what they need

5. **Dependency Inversion**
   - Depend on abstractions (interfaces)
   - Not on concrete implementations
   - Spring Boot injection

**Recent Refactoring (2026-01-25):**
- Extracted validation from controller
- Separated response mapping
- Split HTML utilities
- Improved testability

### LangChain4j Integration

**What We USE:**
- ✅ Tool calling (@Tool annotations)
- ✅ @SystemMessage, @UserMessage decorators
- ✅ Chat model integration
- ✅ Agent framework for AI coordination

**What We DON'T Use:**
- ❌ Document Transformer (not needed for single page)
- ❌ Document Splitter (no document corpus)
- ❌ RAG/retrieval (not needed)
- ❌ Embedding stores (not needed)

**Why?**
- We process one HTML page per request
- No document splitting needed
- No large corpus to search
- Direct Jsoup approach is simpler and more efficient
- Tool calling is perfect for our use case

**How Tool Calling Works:**

1. AI receives request with locator + description
2. AI calls tools to query stored HTML
3. Tools return results
4. AI analyzes results and scores alternatives
5. AI returns recommendations

Example:
```
AI: "Let me test the original locator"
→ Tool: findByXPath("//*[@id='old']")
← Result: "Not found"

AI: "Let me see available elements"
→ Tool: getAllInteractiveElements()
← Result: "[{id:'new', class:'search'}, ...]"

AI: "Perfect! Let me verify"
→ Tool: findById('new')
← Result: "Found!"

AI: "By.id='new' is the best option"
```

### HTML Processing Pipeline

**5-Stage Processing:**

```
Stage 1: Input Validation
  ├─ Check not empty
  ├─ Check valid HTML
  └─ Return early if OK

Stage 2: Preprocessing
  ├─ Parse with Jsoup
  ├─ Remove <script> tags
  ├─ Remove <style> tags
  ├─ Remove comments
  └─ Clean whitespace

Stage 3: ThreadLocal Storage
  ├─ Store cleaned document
  ├─ Make available to tools
  └─ Thread-safe access

Stage 4: AI Analysis
  ├─ Send locator + description
  ├─ AI queries document via tools
  └─ Get results

Stage 5: Cleanup
  ├─ Remove from ThreadLocal
  ├─ Free memory
  └─ Clean resources
```

**Size Reduction:**
- Original HTML: 500KB
- After preprocessing: 50KB (90% reduction)
- Sent to AI: 3 params only

---

## Configuration

### Application Properties

**LLM Configuration (Ollama)**
```properties
# Base URL for Ollama
langchain4j.ollama.chat-model.base-url=http://localhost:11434

# Model name (recommended: llama3.2)
langchain4j.ollama.chat-model.model-name=llama3.2

# Temperature (0.1-0.3 for structured output, 0.7+ for creative)
langchain4j.ollama.chat-model.temperature=0.3

# Timeout (increase for large HTML)
langchain4j.ollama.chat-model.timeout=300s
```

**LLM Configuration (LMStudio)**
```properties
langchain4j.open-ai.chat-model.base-url=http://localhost:1234/v1
langchain4j.open-ai.chat-model.api-key=not-needed
langchain4j.open-ai.chat-model.model-name=local-model
langchain4j.open-ai.chat-model.temperature=0.3
```

**HTML Preprocessing**
```properties
# Target size for optimized HTML (bytes)
html.processing.max-output-size=51200

# Maximum elements to analyze
html.processing.max-candidates=5
```

### LLM Setup

**Ollama (Recommended)**

```bash
# Install
curl -fsSL https://ollama.ai/install.sh | sh

# Start service
ollama serve

# Pull model
ollama pull llama3.2
```

**LMStudio**
1. Download from https://lmstudio.ai/
2. Load a model
3. Start local server
4. Update configuration to point to `http://localhost:1234/v1`

**Alternative Providers**
- Use OpenAI API
- Use Anthropic API
- Use any OpenAI-compatible endpoint

### HTML Processing Tuning

```properties
# For large pages, increase preprocessing
html.processing.max-output-size=102400     # 100KB

# For complex analysis, increase candidates
html.processing.max-candidates=10

# For slow LLMs, increase timeout
langchain4j.ollama.chat-model.timeout=600s

# For more deterministic output
langchain4j.ollama.chat-model.temperature=0.1
```

---

## Locator Strategies

### Selenium Locator Types

The AI recommends locators in this priority order:

| Priority | Type | Reliability | When to Use |
|----------|------|-------------|-------------|
| 🥇 1 | **By.id** | ⭐⭐⭐⭐⭐ | Element has stable ID (preferred) |
| 🥈 2 | **By.name** | ⭐⭐⭐⭐ | Form elements with name attribute |
| 🥉 3 | **By.linkText** | ⭐⭐⭐⭐ | Links with stable visible text |
| 4️⃣ 4 | **By.className** | ⭐⭐⭐ | Unique stable classes |
| 5️⃣ 5 | **By.cssSelector** | ⭐⭐⭐⭐ | Flexible queries for complex selectors |
| 6️⃣ 6 | **By.partialLinkText** | ⭐⭐⭐ | Links (partial text match) |
| 7️⃣ 7 | **By.tagName** | ⭐⭐ | Generic element groups |
| 8️⃣ 8 | **By.xpath** | ⭐⭐ | Last resort (brittle, slow) |

### AI Analysis Strategy

**Step-by-step Process:**

```
Step 1: Understand the Problem
  ├─ Test the failed locator
  ├─ Confirm it doesn't match
  └─ Understand what went wrong

Step 2: Survey Available Elements
  ├─ getAllInteractiveElements()
  ├─ See all buttons, inputs, links
  └─ Understand options

Step 3: Find Target Element
  ├─ Use element description
  ├─ Test multiple finding strategies
  └─ Locate the correct element

Step 4: Test Alternatives
  ├─ Try By.id (if available)
  ├─ Try By.name (if available)
  ├─ Try By.css (flexible)
  └─ Try By.xpath (last resort)

Step 5: Score & Rank
  ├─ By.id = 100 (most reliable)
  ├─ By.name = 90 (good for forms)
  ├─ By.css = 85 (flexible)
  ├─ By.xpath = 60 (brittle)
  └─ Return best + alternatives

Step 6: Return Recommendations
  ├─ recommendedLocator (best choice)
  ├─ recommendedLocatorType (strategy)
  ├─ alternatives (backup options)
  └─ confidence (0-100 score)
```

---

## Troubleshooting

### Common Issues

**1. Ollama Connection Error**

Error:
```
Connection refused: localhost:11434
```

Solution:
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# If not running, start it
ollama serve

# Pull model if missing
ollama pull llama3.2
```

**2. JSON Parsing Errors**

Symptom: Garbled or unparseable AI responses

Solution:
```properties
# Lower temperature for more structured output
langchain4j.ollama.chat-model.temperature=0.1
```

**3. Timeout for Large HTML**

Error:
```
Request timeout after 300s
```

Solution:
```properties
# Increase timeout
langchain4j.ollama.chat-model.timeout=600s

# Or reduce HTML size (automatic in latest version)
html.processing.max-output-size=25600
```

**4. Empty/Null Responses**

Symptom: AI returns null or empty analysis

Solution:
- Check model supports structured output
- Try different model: `qwen2.5-coder`, `mistral`
- Increase temperature: `0.5`

**5. Compilation Errors**

```bash
# Clean and rebuild
mvn clean compile

# Check Java version
java -version  # Should be 21+
```

### Debug Logging

**Enable Debug Logs:**
```properties
logging.level.com.simple.MySimpleSpringBootAgent=DEBUG
logging.level.dev.langchain4j=DEBUG
```

**Log Output Example:**
```
================================================================================
LLM REQUEST - Locator Analysis
================================================================================
Model: llama3.2
Temperature: 0.3
Messages: 2
  - SYSTEM: You are an expert Selenium automation engineer...
  - USER: Failed locator: //*[@id='wrong']...
================================================================================
AI Response: {
  "elementFound": true,
  "recommendedLocator": "search",
  ...
}
================================================================================
```

### FAQ

**Q: Where is the HTML stored?**
A: In ThreadLocal on the server (thread-safe, local memory only)

**Q: Why doesn't AI see the raw HTML?**
A: To reduce tokens, improve security, and allow selective querying

**Q: How does AI access HTML if it doesn't see it?**
A: Through 6 tools that query ThreadLocal and return results

**Q: What if AI can't find the element?**
A: Returns `elementFound: false` with explanation

**Q: How do I use this in my tests?**
A: Call POST endpoint, get back alternatives, use in Selenium

**Q: What's the response time?**
A: Usually < 2 seconds (depends on HTML size and model speed)

**Q: Can I use OpenAI instead of local LLM?**
A: Yes, configure OpenAI endpoint in properties

**Q: Does it work with large pages (500KB+)?**
A: Yes, preprocessing reduces size by 90%+

**Q: Is my HTML secure?**
A: Yes, stays on server in ThreadLocal (never sent to AI)

**Q: Can I disable preprocessing?**
A: Preprocessing is automatic when needed, can't disable

---

## Advanced Topics

### Best Practices

**Locator Strategy:**
1. **Always prefer By.id** when stable ID exists (fastest, most reliable)
2. **Use By.name** for form elements (semantic, usually stable)
3. **Use By.linkText** for links with stable text
4. **Add data-testid** attributes to your app for test stability
5. **Avoid By.xpath** unless absolutely necessary (slow, brittle)

**API Usage:**
- Include `pageUrl` for better context
- Large HTML automatically preprocessed (no action needed)
- Check `confidence` score before using suggestions
- Try multiple alternatives if preferred locator fails
- Cache recommendations when possible

**Development:**
- Mock `LocatorAnalyzerAI` in unit tests
- Use `@ConfigurationProperties` for custom settings
- Follow SOLID principles when extending
- Add new locator strategies via interface

### Testing Strategies

**Unit Testing:**
```java
@Test
void testAnalyzeLocator_FindsById() {
    // Mock AI service
    when(aiService.analyze(any())).thenReturn(
        new LocatorAnalysisResult()
            .withElementFound(true)
            .withRecommendedLocator("test-id")
    );

    // Test controller
    LocatorAnalysisResponse response = controller.analyzeLocator(request);

    assertEquals("test-id", response.getRecommendedLocator());
}
```

**Integration Testing:**
- Test with actual Ollama instance
- Test HTML preprocessing pipeline
- Test error handling
- Test ThreadLocal cleanup

### Production Deployment

**Considerations:**
- Run Ollama on separate machine for scalability
- Configure API load balancing
- Monitor response times
- Set up logging and alerting
- Regular model updates
- Thread pool tuning

**Docker:**
```dockerfile
FROM eclipse-temurin:21-jre
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Appendices

### Viewing Diagrams

**ASCII Diagrams (Works Everywhere):**
- Open this file in any text editor
- Works in IntelliJ without plugins
- Copy to terminal or documents

**Mermaid Diagrams (Beautiful but Optional):**

**Option 1: GitHub** ✅ Easiest
1. Push to GitHub
2. Open in browser
3. Automatic rendering

**Option 2: IntelliJ**
1. Settings → Plugins → Marketplace
2. Search "Mermaid"
3. Install
4. Restart
5. Open `.md` file

**Option 3: Online Editor**
1. Visit https://mermaid.live
2. Copy diagram code
3. Paste in editor
4. View rendered

**Option 4: VSCode**
1. Built-in support
2. Open `.md` file
3. Click Preview
4. Auto-renders

### Dependencies

**Core Dependencies:**
- **Java 21** - Latest LTS with modern features
- **Spring Boot 3.4.2** - Latest framework
- **langchain4j 1.10.0** - AI orchestration and tool calling
- **Jsoup 1.17.2** - HTML parsing and DOM querying

**Processing:**
- **HtmlCompressor 1.5.2** - HTML minification
- **Apache Commons Lang3** - String utilities
- **Apache Commons Text 1.12.0** - Text operations

**Development:**
- **Lombok** - Reduce boilerplate
- **Maven 3.6+** - Build tool

**What Each Does:**
- `langchain4j` - Coordinates AI analysis and tool execution
- `Jsoup` - Parses HTML and queries DOM safely
- `HtmlCompressor` - Reduces HTML size for large pages
- `Spring Boot` - Web framework and dependency injection
- `Lombok` - Auto-generates getters, setters, constructors

### Quick Reference

**Common Curl Commands:**

```bash
# Health check
curl http://localhost:8080/api/locators/health

# Test with sample
curl http://localhost:8080/api/locators/test

# Analyze (basic)
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{"locator":"xpath","htmlContent":"<html>..."}'

# Analyze (with description)
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "locator":"//*[@id='old']",
    "htmlContent":"<html>...",
    "elementDescription":"Search button",
    "pageUrl":"https://example.com"
  }'

# Extract recommendation
curl http://localhost:8080/api/locators/test | \
  jq '.recommendedLocator'

# Pretty print
curl http://localhost:8080/api/locators/test | jq '.'
```

**Response Template:**
```json
{
  "elementFound": boolean,
  "recommendedLocator": "string",
  "recommendedLocatorType": "ID|NAME|CSS|XPATH|...",
  "byId": "string",
  "byName": "string",
  "byClassName": "string",
  "primaryCssSelector": "string",
  "primaryXPath": "string",
  "confidence": number,
  "explanation": "string"
}
```

**Configuration Template:**
```properties
# LLM
langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.chat-model.model-name=llama3.2
langchain4j.ollama.chat-model.temperature=0.3

# HTML Processing
html.processing.max-output-size=51200
html.processing.max-candidates=5

# Logging
logging.level.com.simple.MySimpleSpringBootAgent=INFO
```

---

**Built with ❤️ using langchain4j, Spring Boot, and SOLID principles**

For questions or issues, see the project repository or check troubleshooting section above.
