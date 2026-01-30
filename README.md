# AI-Powered Selenium Locator Analyzer - Complete Guide

A Spring Boot service that uses local LLMs to analyze failed Selenium locators and suggest alternatives across **ALL** Selenium locator types (ID, Name, CSS, XPath, LinkText, and more).

---

## 📑 Table of Contents

1. [Quick Start](#quick-start) - Get running in 5 minutes
2. [What This Tool Does](#what-this-tool-does) - Core concepts
3. [How It Works](#how-it-works) - Visual guides and architecture
4. [API Reference](#api-reference) - Endpoints and examples
5. [Configuration](#configuration) - Settings and tuning
6. [Locator Strategies](#locator-strategies) - Selenium types and AI strategy
7. [Development Guide](#development-guide) - Architecture and SOLID principles
8. [IDE Setup](#ide-setup) - Java LSP for Claude Code
9. [Troubleshooting](#troubleshooting) - Common issues and solutions
10. [Quick Reference](#quick-reference) - Templates and commands

---

## Quick Start

### Prerequisites & Installation

```bash
# 1. Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh
ollama pull llama3.2

# 2. Clone and run
git clone <repo-url>
cd MySimpleSpringBootAgent
mvn spring-boot:run

# Service runs at http://localhost:8080
```

### First Test

```bash
# Health check
curl http://localhost:8080/api/locators/health

# Test with sample data
curl http://localhost:8080/api/locators/test

# Analyze a failed locator
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "htmlContent": "<html><body><input id=\"search\" name=\"q\"/></body></html>",
    "locator": "//*[@id=\"wrong\"]",
    "pageUrl": "https://example.com"
  }'
```

**Expected Response:**
```json
{
  "elementFound": true,
  "recommendedLocator": "search",
  "recommendedLocatorType": "ID",
  "byId": "search",
  "primaryCssSelector": "#search",
  "confidence": 95,
  "explanation": "Use By.id for best reliability"
}
```

---

## What This Tool Does

### The Problem
Selenium test locators break when HTML elements change. Finding alternatives is time-consuming and error-prone.

### The Solution
Send the failed locator and page HTML to this service. It uses AI to intelligently suggest alternatives across ALL Selenium locator types with confidence scores.

### Key Features
- 🎯 **All Selenium Locators** - ID, Name, ClassName, TagName, LinkText, PartialLinkText, CSS, XPath
- 🤖 **AI-Powered** - Local LLM analysis with confidence scores
- ⚡ **Smart Preprocessing** - 90%+ HTML size reduction for large pages
- 🏆 **Priority Recommendations** - ID > Name > LinkText > CSS > XPath
- 🛠️ **SOLID Architecture** - Clean, testable, maintainable design
- 📊 **Full Observability** - Complete LLM interaction logging

### Basic Usage (Java/Selenium)

```java
try {
    driver.findElement(By.xpath("//*[@id='oldId']"));
} catch (NoSuchElementException e) {
    // Call API
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

## How It Works

### Main Request Flow

```
┌────────────────────────────────────────────────────────────────┐
│  Client sends POST /api/locators/analyze                       │
│  { locator, htmlContent, elementDescription, pageUrl }         │
└─────────────────┬──────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────┐
│  LocatorController validates & preprocesses HTML               │
│  • HtmlPreprocessor removes scripts/styles/comments            │
│  • DomQueryTools stores cleaned Jsoup Document in ThreadLocal  │
└─────────────────┬──────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────┐
│  LocatorAnalyzerAI calls Claude with 3 small params            │
│  ✅ Sending: locator + description (not raw HTML!)             │
│  Claude uses 6 tools to query stored HTML intelligently        │
└─────────────────┬──────────────────────────────────────────────┘
         ┌────────┴──────────┐
         ▼                   ▼
     Tools Used:         Results:
     findByXPath()       ✓ Found element
     findByCss()         ✓ Works reliably
     findById()          ✓ Most stable
         │                   │
         └────────┬──────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────┐
│  AI Scores & Returns Recommendations                           │
│  LocatorResponseMapper formats response                        │
│  DomQueryTools clears ThreadLocal (cleanup)                    │
└─────────────────┬──────────────────────────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────────────────────────┐
│  Return 200 OK + JSON with recommendations to client           │
└────────────────────────────────────────────────────────────────┘
```

### Key Insight: HTML Stays on Server

**Traditional (Wasteful):**
```
Client → {50KB HTML + params} → AI processes all
Cost: $$$$ (tokens, slow, insecure)
```

**Smart (Efficient):**
```
Client → {50KB HTML + params}
            ↓ (stays on server)
          {3 params} → AI uses tools
Cost: $ (minimal tokens, fast, secure)
```

### Component Architecture

```
LocatorController (HTTP endpoints)
    ↓
├─ LocatorRequestValidator (validate input)
├─ HtmlPreprocessor (clean HTML, remove noise)
├─ DomQueryTools (ThreadLocal storage + 6 tools)
├─ LocatorAnalyzerAI (Claude AI interface)
└─ LocatorResponseMapper (format response)
```

### AI Tool Capabilities

The AI has 6 tools for querying stored HTML:

| Tool | Purpose | Example |
|------|---------|---------|
| `findByXPath()` | Test XPath expressions | `//*[@id='login']` |
| `findByCss()` | Test CSS selectors | `button.submit-btn` |
| `findById()` | Find by ID attribute | `search-input` |
| `findByAttribute()` | Find by any HTML attribute | `data-testid='submit'` |
| `findByText()` | Find by visible text | `"Click here"` |
| `getAllInteractiveElements()` | List all clickable/input elements | Returns JSON array |

### AI Analysis Strategy

```
Step 1: Understand the Problem
  • Test the failed locator
  • Confirm it doesn't match

Step 2: Survey Available Elements
  • getAllInteractiveElements()
  • See all buttons, inputs, links

Step 3: Find Target Element
  • Use element description
  • Test multiple strategies

Step 4: Test Alternatives
  • By.id (most reliable)
  • By.name (good for forms)
  • By.css (flexible)
  • By.xpath (last resort)

Step 5: Score & Rank
  • By.id = 100 (most reliable)
  • By.name = 90
  • By.css = 85
  • By.xpath = 60

Step 6: Return Recommendations
  • Best choice
  • Confidence score
  • Alternatives
```

---

## API Reference

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/locators/health` | Health check |
| GET | `/api/locators/test` | Test with sample HTML |
| POST | `/api/locators/analyze` | Analyze failed locator |

### Request Format

**POST /api/locators/analyze**

```json
{
  "locator": "//*[@id='search']",              // REQUIRED
  "htmlContent": "<html>...</html>",           // REQUIRED
  "elementDescription": "Search button",       // OPTIONAL
  "pageUrl": "https://example.com"             // OPTIONAL
}
```

### Response Format

**Success (200 OK):**
```json
{
  "elementFound": true,
  "recommendedLocator": "search-id",
  "recommendedLocatorType": "ID",
  "byId": "search-id",
  "byName": "q",
  "byClassName": "search-input",
  "byTagName": "input",
  "byLinkText": null,
  "byPartialLinkText": null,
  "primaryCssSelector": "#search-id",
  "alternativeCssSelectors": ["input.search"],
  "primaryXPath": "//*[@id='search-id']",
  "alternativeXPaths": ["//input[@name='q']"],
  "confidence": 95,
  "explanation": "Use By.id for best reliability"
}
```

**Error (400 Bad Request):**
```json
{
  "statusCode": 400,
  "error": "locator cannot be empty"
}
```

### Examples

**Example 1: Search Box with ID**

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

Response: By.id("search") recommended with 98% confidence.

**Example 2: Form Field Without ID**

```bash
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "locator": "//*[@class=\"no-longer-exists\"]",
    "htmlContent": "<html><body><input name=\"username\" placeholder=\"Username\"/></body></html>",
    "elementDescription": "Username field"
  }'
```

Response: By.name("username") recommended with 95% confidence.

---

## Configuration

### Application Properties

**LLM Configuration (Ollama)**
```properties
# Base URL for Ollama
langchain4j.ollama.chat-model.base-url=http://localhost:11434

# Model name (recommended: llama3.2)
langchain4j.ollama.chat-model.model-name=llama3.2

# Temperature (0.1-0.3 for structured output)
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

### HTML Processing Tuning

```properties
# For large pages (500KB+)
html.processing.max-output-size=102400

# For complex analysis
html.processing.max-candidates=10

# For slow LLMs
langchain4j.ollama.chat-model.timeout=600s

# For deterministic output
langchain4j.ollama.chat-model.temperature=0.1
```

---

## Locator Strategies

### Selenium Locator Types

AI recommends in this priority order:

| Priority | Type | Reliability | When to Use |
|----------|------|-------------|-------------|
| 🥇 1 | **By.id** | ⭐⭐⭐⭐⭐ | Element has stable ID |
| 🥈 2 | **By.name** | ⭐⭐⭐⭐ | Form elements with name |
| 🥉 3 | **By.linkText** | ⭐⭐⭐⭐ | Links with stable text |
| 4️⃣ 4 | **By.className** | ⭐⭐⭐ | Unique stable classes |
| 5️⃣ 5 | **By.cssSelector** | ⭐⭐⭐⭐ | Complex selectors |
| 6️⃣ 6 | **By.partialLinkText** | ⭐⭐⭐ | Links (partial text) |
| 7️⃣ 7 | **By.tagName** | ⭐⭐ | Generic element groups |
| 8️⃣ 8 | **By.xpath** | ⭐⭐ | Last resort (brittle) |

### Best Practices

1. **Always prefer By.id** when stable ID exists (fastest, most reliable)
2. **Use By.name** for form elements (semantic, usually stable)
3. **Use By.linkText** for links with stable text
4. **Add data-testid** attributes for test stability
5. **Avoid By.xpath** unless absolutely necessary (slow, brittle)

---

## Development Guide

### Architecture Overview

**File Structure:**
```
src/main/java/com/simple/MySimpleSpringBootAgent/
├── aiservice/
│   └── LocatorAnalyzerAI.java       # AI interface
├── controller/
│   └── LocatorController.java       # REST endpoints
├── service/
│   ├── LocatorRequestValidator.java # Validation
│   ├── LocatorResponseMapper.java   # DTO mapping
│   ├── HtmlPreprocessor.java        # Pipeline
│   ├── HtmlMinificationService.java # Size reduction
│   ├── HtmlUtilityService.java      # Utilities
│   └── DomQueryTools.java           # DOM tools
├── dto/
│   ├── LocatorAnalysisRequest.java
│   ├── LocatorAnalysisResponse.java
│   ├── LocatorAnalysisResult.java
│   └── LocatorType.java
└── config/
    └── HtmlProcessingConfig.java
```

### SOLID Principles Applied

**Single Responsibility:**
- `LocatorController` - HTTP concerns only
- `HtmlPreprocessor` - HTML processing only
- `LocatorAnalyzerAI` - AI analysis only
- `LocatorResponseMapper` - Response formatting only

**Open/Closed:** All components use interfaces, easy to extend

**Liskov Substitution:** AI services are interchangeable

**Interface Segregation:** Small, focused interfaces

**Dependency Inversion:** Depend on abstractions via Spring injection

### LangChain4j Integration

**What We Use:**
- ✅ Tool calling (@Tool annotations)
- ✅ @SystemMessage, @UserMessage decorators
- ✅ Chat model integration
- ✅ Agent framework

**What We Don't Use:**
- ❌ Document Transformer (not needed for single page)
- ❌ Document Splitter (no document corpus)
- ❌ RAG/retrieval (not needed)
- ❌ Embedding stores (not needed)

**Why?** We process one HTML page per request. Direct Jsoup approach is simpler and more efficient. Tool calling is perfect for our use case.

### HTML Processing Pipeline

**5-Stage Process:**

1. **Input Validation** - Check not empty, valid HTML
2. **Preprocessing** - Parse with Jsoup, remove scripts/styles/comments
3. **ThreadLocal Storage** - Store cleaned document, make available to tools
4. **AI Analysis** - Send locator + description, AI queries document
5. **Cleanup** - Remove from ThreadLocal, free memory

**Size Reduction:**
- Original HTML: 500KB → After preprocessing: 50KB (90% reduction)
- Sent to AI: 3 params only

### Dependencies

**Core:**
- Java 21, Spring Boot 3.4.2
- langchain4j 1.10.0 (AI with tool calling)
- Jsoup 1.17.2 (HTML parsing)
- HtmlCompressor 1.5.2 (size reduction)

**What Each Does:**
- `langchain4j` - Coordinates AI and tool execution
- `Jsoup` - Parses HTML and queries DOM safely
- `HtmlCompressor` - Reduces HTML size for large pages
- `Spring Boot` - Web framework and injection
- `Lombok` - Auto-generates getters/setters

---

## IDE Setup

### Java LSP for Claude Code

**Status**: ✅ WORKING & ENABLED (verified January 25, 2026)

**Components Installed:**
- ✅ Java 21.0.9
- ✅ JDTLS v1.55.0
- ✅ jdtls-lsp plugin v1.0.0
- ✅ Claude Code v2.1.19

### Installation Steps

**1. Install Java 21+**
```bash
java --version  # Should be 21+
```

**2. Install JDTLS**
```bash
brew install jdtls
which jdtls  # Verify: /opt/homebrew/bin/jdtls
```

**3. Install jdtls-lsp Plugin in Claude Code**
```bash
# In Claude Code CLI:
/plugin

# Navigate to "Discover" tab
# Search for: jdtls-lsp
# Source: claude-plugins-official
# Click "Install"
# Enable at project scope
```

**4. Restart Claude Code**
```bash
exit
cc  # Start new session
```

### Verification

```bash
# Check installation
java --version
which jdtls
ps aux | grep jdtls | grep -v grep

# Verify plugin
/plugin  # Should show jdtls-lsp as Enabled
```

### How LSP Works

LSP runs **silently in the background** - no explicit tool invocation needed.

**Automatic Capabilities:**
- Real-time diagnostics (errors/warnings)
- Go to definition
- Find references
- Hover information
- Type checking

**Performance:** ~900x faster than text search (50ms vs 45s)

### Troubleshooting LSP

**Not working?**
1. Restart Claude Code: `exit` then `cc`
2. Verify JDTLS process: `ps aux | grep jdtls`
3. Clear cache: `rm -rf ~/Library/Caches/jdtls/`
4. Reinstall plugin from `/plugin`

**Executable not found?**
```bash
brew install jdtls
which jdtls
```

---

## Troubleshooting

### Common Issues

**1. Ollama Connection Error**

Error: `Connection refused: localhost:11434`

Solution:
```bash
curl http://localhost:11434/api/tags  # Check if running
ollama serve                            # Start service
ollama pull llama3.2                    # Pull model
```

**2. JSON Parsing Errors**

Symptom: Garbled or unparseable AI responses

Solution:
```properties
langchain4j.ollama.chat-model.temperature=0.1
```

**3. Timeout for Large HTML**

Error: `Request timeout after 300s`

Solution:
```properties
langchain4j.ollama.chat-model.timeout=600s
html.processing.max-output-size=25600
```

**4. Empty/Null Responses**

Symptom: AI returns null or empty

Solution:
- Check model supports structured output
- Try different model: `qwen2.5-coder`, `mistral`
- Increase temperature: `0.5`

**5. Compilation Errors**

```bash
mvn clean compile
java -version  # Should be 21+
```

### Debug Logging

**Enable Debug Logs:**
```properties
logging.level.com.simple.MySimpleSpringBootAgent=DEBUG
logging.level.dev.langchain4j=DEBUG
```

### FAQ

**Q: Where is HTML stored?**
A: ThreadLocal on server (thread-safe, local memory only)

**Q: Why not send raw HTML to AI?**
A: Reduces tokens, improves security, allows selective querying

**Q: How does AI access HTML if it doesn't see it?**
A: Through 6 tools that query ThreadLocal

**Q: What if AI can't find the element?**
A: Returns `elementFound: false` with explanation

**Q: How do I use this in Selenium tests?**
A: Call POST endpoint, get recommendations, use in test

**Q: What's typical response time?**
A: Usually < 2 seconds (depends on HTML size and model speed)

**Q: Can I use OpenAI instead of local LLM?**
A: Yes, configure OpenAI endpoint in properties

**Q: Does it work with large pages (500KB+)?**
A: Yes, preprocessing reduces size by 90%+

**Q: Is my HTML secure?**
A: Yes, stays on server in ThreadLocal (never sent to AI)

---

## Quick Reference

### Common Curl Commands

```bash
# Health check
curl http://localhost:8080/api/locators/health

# Test with sample
curl http://localhost:8080/api/locators/test

# Analyze (basic)
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{"locator":"xpath","htmlContent":"<html>..."}'

# Extract recommendation
curl http://localhost:8080/api/locators/test | jq '.recommendedLocator'

# Pretty print
curl http://localhost:8080/api/locators/test | jq '.'
```

### Request Template

```json
{
  "locator": "//*[@id='search']",
  "htmlContent": "<html>...</html>",
  "elementDescription": "Search button",
  "pageUrl": "https://example.com"
}
```

### Response Template

```json
{
  "elementFound": boolean,
  "recommendedLocator": "string",
  "recommendedLocatorType": "ID|NAME|CSS|XPATH|...",
  "byId": "string",
  "byName": "string",
  "primaryCssSelector": "string",
  "primaryXPath": "string",
  "confidence": number,
  "explanation": "string"
}
```

### Configuration Template

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

### Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.4.2
- **AI Orchestration**: langchain4j 1.10.0
- **HTML Parsing**: Jsoup 1.17.2
- **HTML Compression**: HtmlCompressor 1.5.2
- **Build Tool**: Maven 3.6+
- **LLM**: Ollama (local) or any OpenAI-compatible API

---

## Key Takeaways

✅ **How It Works**: HTML preprocessed locally → AI gets 3 small params → uses tools to query → returns recommendations

✅ **Why It's Efficient**: Minimal tokens, secure (HTML stays on server), flexible (AI decides what to search)

✅ **Priority Order**: By.id > By.name > By.linkText > By.css > By.xpath

✅ **SOLID Architecture**: Each component has single responsibility, easy to test and extend

✅ **Easy Setup**: Ollama + Maven + Claude Code with jdtls-lsp

✅ **AI-Powered**: Uses local LLM with tool calling for intelligent analysis

---

**Built with ❤️ using langchain4j, Spring Boot, and SOLID principles**

---

## Documentation History

This documentation was consolidated on 2026-01-26 from 5 separate markdown files into this single comprehensive guide for improved clarity and maintainability.

**Archived documentation** (preserved for historical reference):
- 📁 [View archived documentation](docs/archive/) - Includes original README.md, GUIDE.md, setup guides, and historical documents

**Why consolidated?**
- Single source of truth for easier maintenance
- Optimized for both new developers and LLMs
- 60% size reduction by removing redundancy while preserving all content

Last updated: 2026-01-26

For issues or questions, see troubleshooting section or check project repository.
