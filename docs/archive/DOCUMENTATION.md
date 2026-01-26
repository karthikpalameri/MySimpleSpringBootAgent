# Selenium Locator Analyzer - Complete Documentation

AI-powered Spring Boot service that analyzes failed Selenium locators and suggests alternatives across **ALL** Selenium locator types using local LLMs.

## Table of Contents
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Usage Examples](#usage-examples)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+
- Local LLM (Ollama with llama3.2 recommended)

### Setup

1. **Install Ollama**
```bash
curl -fsSL https://ollama.ai/install.sh | sh
ollama pull llama3.2
```

2. **Configure** (`src/main/resources/application.properties`)
```properties
langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.chat-model.model-name=llama3.2
langchain4j.ollama.chat-model.temperature=0.3
```

3. **Run**
```bash
mvn spring-boot:run
```

Service runs at `http://localhost:8080`

---

## API Reference

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/locators/health` | Health check |
| GET | `/api/locators/test` | Test with sample HTML |
| POST | `/api/locators/analyze` | Analyze failed locator |

### POST /api/locators/analyze

**Request:**
```json
{
  "htmlContent": "<html>...</html>",
  "locator": "//failed/xpath",
  "pageUrl": "https://example.com"
}
```

**Response:**
```json
{
  "recommendedLocatorType": "ID",
  "recommendedLocator": "searchBox",
  "byId": "searchBox",
  "byName": "q",
  "byClassName": "search-input",
  "byTagName": null,
  "byLinkText": null,
  "byPartialLinkText": null,
  "primaryCssSelector": "#searchBox",
  "alternativeCssSelectors": ["input[name='q']"],
  "primaryXPath": "//*[@id='searchBox']",
  "alternativeXPaths": ["//input[@name='q']"],
  "confidence": 95,
  "elementFound": true,
  "explanation": "Use By.id for best reliability",
  "warnings": "Avoid XPath when ID exists"
}
```

---

## Usage Examples

### Curl Examples

**1. Health Check**
```bash
curl http://localhost:8080/api/locators/health
```

**2. Simple Analysis**
```bash
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "htmlContent": "<html><body><input id=\"search\" name=\"q\" class=\"search-box\"/></body></html>",
    "locator": "//*[@id=\"wrongId\"]",
    "pageUrl": "https://example.com"
  }'
```

**3. Test Endpoint**
```bash
curl http://localhost:8080/api/locators/test
```

**4. Extract with jq**
```bash
curl -X POST http://localhost:8080/api/locators/analyze \
  -H "Content-Type: application/json" \
  -d '{"htmlContent":"<html><body><input id=\"test\"/></body></html>","locator":"wrong"}' \
  | jq -r '.recommendedLocatorType, .recommendedLocator'
```

### Java/Selenium Integration

```java
// When a locator fails
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
        case "CLASS_NAME" -> driver.findElement(By.className(response.getRecommendedLocator()));
        case "LINK_TEXT" -> driver.findElement(By.linkText(response.getRecommendedLocator()));
        case "CSS_SELECTOR" -> driver.findElement(By.cssSelector(response.getRecommendedLocator()));
        case "XPATH" -> driver.findElement(By.xpath(response.getRecommendedLocator()));
        default -> throw new IllegalStateException("Unknown type");
    };

    // Or try with fallbacks
    WebElement el = tryLocators(
        () -> driver.findElement(By.id(response.getById())),
        () -> driver.findElement(By.name(response.getByName())),
        () -> driver.findElement(By.cssSelector(response.getPrimaryCssSelector()))
    );
}
```

### Locator Type Examples

| Scenario | HTML | Failed Locator | Recommended |
|----------|------|----------------|-------------|
| Search box with ID | `<input id="search" name="q"/>` | `xpath: //input[@id='wrong']` | **By.id**: `search` |
| Form field | `<input name="username" id="user"/>` | `css: .wrong` | **By.id**: `user` |
| Link | `<a href="/help">Help Center</a>` | `xpath: //a[@class='wrong']` | **By.linkText**: `Help Center` |
| Button with test-id | `<button data-testid="submit">Submit</button>` | `id: wrong-id` | **By.cssSelector**: `[data-testid='submit']` |
| Dynamic ID | `<button id="btn-123">Click</button>` | `id: btn-456` | **By.cssSelector**: `button:contains('Click')` |

---

## Architecture

### Design Principles
- **SOLID** - Single Responsibility, Interface Segregation, Dependency Inversion
- **DRY** - No code duplication, reusable components
- **Modular** - Interface-based with dependency injection
- **Testable** - All services mockable

### Project Structure
```
src/main/java/com/simple/MySimpleSpringBootAgent/
├── aiservice/
│   └── LocatorAnalyzerAI.java           # AI service interface
├── controller/
│   └── LocatorController.java           # REST endpoints (HTTP only)
├── service/
│   ├── LocatorRequestValidator.java     # Request validation
│   ├── LocatorResponseMapper.java       # DTO mapping
│   ├── LocatorResponseFormatter.java    # Response formatting
│   ├── HtmlPreprocessor.java            # Pipeline orchestration
│   ├── HtmlCleaningService.java         # HTML cleaning
│   ├── HtmlUtilityService.java          # Utilities
│   ├── HtmlResponseGenerator.java       # Response generation
│   ├── LocatorParserService.java        # Locator parsing
│   ├── DomCandidateService.java         # Element matching
│   ├── DomPruningService.java           # DOM pruning
│   └── HtmlMinificationService.java     # Minification
├── dto/
│   ├── LocatorAnalysisRequest.java      # API request
│   ├── LocatorAnalysisResponse.java     # API response
│   ├── LocatorAnalysisResult.java       # AI response
│   ├── LocatorHints.java                # Parsed hints
│   └── ScoredElement.java               # Candidate element
└── config/
    ├── HtmlProcessingConfig.java        # Configuration
    └── LocatorAnalyzerChatModelListener.java # Observability
```

### 5-Stage HTML Preprocessing Pipeline

For large HTML (500KB+), automatic preprocessing reduces size by 90-97%:

```
Stage 1: Locator Parsing
  ↓ Extract IDs, classes, attributes from failed locator

Stage 2: Candidate Discovery
  ↓ Find matching elements (ID → Semantic → Class → Text)

Stage 3: DOM Pruning
  ↓ Keep only relevant subtrees

Stage 4: Context Extraction
  ↓ Limit depth, siblings, nesting

Stage 5: Minification
  ↓ Remove whitespace, compress

Result: 500KB → 20KB (optimized for LLM context)
```

### Selenium Locator Types (Priority Order)

| Priority | Type | Reliability | Performance | When to Use |
|----------|------|-------------|-------------|-------------|
| 1 | **By.id** | ⭐⭐⭐⭐⭐ | ⚡⚡⚡ | Element has stable ID |
| 2 | **By.name** | ⭐⭐⭐⭐ | ⚡⚡⚡ | Form elements |
| 3 | **By.linkText** | ⭐⭐⭐⭐ | ⚡⚡ | Links with stable text |
| 4 | **By.className** | ⭐⭐⭐ | ⚡⚡⚡ | Unique stable classes |
| 5 | **By.cssSelector** | ⭐⭐⭐⭐ | ⚡⚡⚡ | Flexible queries |
| 6 | **By.partialLinkText** | ⭐⭐⭐ | ⚡⚡ | Links (partial match) |
| 7 | **By.tagName** | ⭐⭐ | ⚡⚡⚡ | Groups of elements |
| 8 | **By.xpath** | ⭐⭐ | ⚡ | Last resort (brittle) |

---

## Configuration

### application.properties

**LLM Configuration (Ollama)**
```properties
langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.chat-model.model-name=llama3.2
langchain4j.ollama.chat-model.temperature=0.3
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
html.processing.max-output-size=51200        # Target size (50KB)
html.processing.max-candidates=5             # Max elements analyzed
html.processing.tier-one-score=100           # ID/unique match score
html.processing.tier-two-score=75            # Semantic match score
html.processing.tier-three-score=50          # Class/tag match score
html.processing.max-parent-depth=3           # Ancestor chain depth
html.processing.max-sibling-count=2          # Siblings preserved
html.processing.max-child-depth=2            # Child nesting depth
html.processing.early-return-size=10240      # Skip preprocessing threshold
```

---

## Troubleshooting

### Common Issues

**1. Ollama Connection Error**
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Start Ollama
ollama serve

# Pull model if missing
ollama pull llama3.2
```

**2. JSON Parsing Errors**
- Lower temperature for more structured output:
  ```properties
  langchain4j.ollama.chat-model.temperature=0.1
  ```
- Use more capable model: `llama3.2`, `qwen2.5-coder`

**3. Timeout for Large HTML**
- Preprocessing automatically handles this
- Increase timeout if needed:
  ```properties
  langchain4j.ollama.chat-model.timeout=600s
  ```

**4. Empty/Null Responses**
- Check logs for AI response format
- Verify model supports structured output
- Try different prompt temperature

**5. Compilation Errors**
```bash
# Clean and rebuild
mvn clean compile

# Check Java version
java -version  # Should be 21+
```

### Debugging

**Enable Debug Logging**
```properties
logging.level.com.simple.MySimpleSpringBootAgent=DEBUG
logging.level.dev.langchain4j=DEBUG
```

**Check LLM Interactions**
All LLM calls are logged by `LocatorAnalyzerChatModelListener`:
```
================================================================================
LLM REQUEST - Locator Analysis
================================================================================
Model: llama3.2
Messages: 2
  - SYSTEM: You are an expert Selenium automation engineer...
  - USER: Failed locator: //wrong/xpath...
================================================================================
```

---

## Best Practices

### Locator Strategy
1. **Always prefer By.id** when stable ID exists (fastest, most reliable)
2. **Use By.name** for form elements
3. **Use By.linkText** for links with stable text
4. **Add data-testid** attributes to your app for test stability
5. **Avoid By.xpath** unless absolutely necessary (slow, brittle)

### API Usage
- Include `pageUrl` for better context
- Use preprocessing for large HTML (automatic)
- Check `confidence` score before using suggestions
- Try multiple alternatives if preferred locator fails

### Development
- Mock `LocatorAnalyzerAI` in tests
- Use `@ConfigurationProperties` for settings
- Follow SOLID principles when extending
- Add new locator strategies via interface implementation

---

## Tech Stack

- **Java 21** - Latest LTS
- **Spring Boot 3.4.2** - Modern framework
- **langchain4j 1.10.0** - AI orchestration
- **Jsoup 1.17.2** - HTML parsing
- **HtmlCompressor 1.5.2** - Minification
- **Apache Commons** - Utilities
- **Lombok** - Boilerplate reduction

---

## Resources

- [langchain4j Documentation](https://docs.langchain4j.dev/)
- [Selenium Documentation](https://www.selenium.dev/documentation/)
- [Ollama Documentation](https://ollama.ai/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/)

---

**Built with langchain4j, Spring Boot, and SOLID principles**
