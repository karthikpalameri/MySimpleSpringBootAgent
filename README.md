# AI-Powered XPath Finder Service

A **simple, modular, and reusable** Spring Boot service that uses local LLMs (via langchain4j) to intelligently find XPath and CSS selectors for web elements when Selenium tests fail.

## ✨ Features

- 🤖 **AI-Powered Selector Generation** - Uses local LLMs to analyze HTML and suggest selectors
- 🎯 **Structured Output** - Automatic JSON extraction to POJOs (no regex parsing!)
- 🔄 **Multiple Provider Support** - Works with Ollama, LMStudio, or any OpenAI-compatible API
- 📊 **Confidence Scoring** - AI provides confidence levels for each suggestion
- 🛠️ **Multiple Alternatives** - Returns both XPath and CSS selector options
- 📝 **Built-in Observability** - ChatModelListener for debugging and monitoring
- 🚀 **Zero Boilerplate** - Uses `@AiService` for declarative AI interactions

## 🏗️ Architecture

This project follows **langchain4j best practices** for a clean, modular architecture:

```
src/main/java/com/simple/MySimpleSpringBootAgent/
├── aiservice/
│   └── XPathFinderAI.java              # @AiService interface (auto-implemented!)
├── controller/
│   └── XPathFinderController.java      # REST endpoints
├── dto/
│   ├── XPathAnalysisResult.java        # AI response POJO with @Description
│   ├── XPathFinderRequest.java         # HTTP request DTO
│   └── XPathFinderResponse.java        # HTTP response DTO (for compatibility)
└── config/
    └── XPathFinderChatModelListener.java # Observability listener
```

### Key Design Principles

- **KISS (Keep It Simple, Stupid)** - No complex prompt building or response parsing
- **DRY (Don't Repeat Yourself)** - Reusable AI service interface
- **Modular** - Easy to add new AI features by just adding methods to `@AiService`
- **Declarative** - Using annotations instead of imperative code

## 📋 Prerequisites

1. **Java 17+**
2. **Maven 3.6+**
3. **Local LLM** (choose one):
   - [Ollama](https://ollama.ai/) with a model (recommended: `llama3.2`)
   - [LMStudio](https://lmstudio.ai/) with a loaded model

## 🚀 Quick Start

### 1. Install Ollama (Recommended)

```bash
# macOS/Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Pull a model
ollama pull llama3.2

# Verify it's running
ollama list
```

### 2. Configure the Application

The application is configured via `application.properties` using **Spring Boot autoconfiguration**:

**For Ollama (default):**
```properties
langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.chat-model.model-name=llama3.2
langchain4j.ollama.chat-model.temperature=0.3
langchain4j.ollama.chat-model.timeout=300s
```

**For LMStudio:**
```properties
# Comment out Ollama config and uncomment these:
langchain4j.open-ai.chat-model.base-url=http://localhost:1234/v1
langchain4j.open-ai.chat-model.api-key=not-needed
langchain4j.open-ai.chat-model.model-name=local-model
langchain4j.open-ai.chat-model.temperature=0.3
```

### 3. Run the Application

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

The service starts on `http://localhost:8080`

### 4. Test It

```bash
# Health check
curl http://localhost:8080/api/xpath/health

# Test endpoint with sample HTML
curl http://localhost:8080/api/xpath/test
```

## 📡 API Endpoints

### Health Check
```http
GET /api/xpath/health
```

### Find XPath/CSS Selectors
```http
POST /api/xpath/find
Content-Type: application/json

{
  "htmlContent": "<html>...</html>",
  "elementDescription": "search box",
  "failedXPath": "//*[@id='wrongId']",
  "pageUrl": "https://www.amazon.in",
  "additionalContext": "It's in the header navigation"
}
```

**Response:**
```json
{
  "primaryXPath": "//*[@id='twotabsearchtextbox']",
  "suggestedXPaths": [
    "//*[@id='twotabsearchtextbox']",
    "//input[@type='text' and @name='field-keywords']",
    "//input[contains(@class, 'search-input')]"
  ],
  "alternativeSelectors": [
    "input#twotabsearchtextbox",
    "input[name='field-keywords']"
  ],
  "confidence": 95,
  "explanation": "Found the search input using ID selector which is most reliable",
  "elementFound": true,
  "warnings": null
}
```

### Test Endpoint
```http
GET /api/xpath/test
```

## 🔧 How It Works

### The Magic of @AiService

Instead of manually building prompts and parsing responses, we use langchain4j's `@AiService`:

```java
@AiService
public interface XPathFinderAI {

    @SystemMessage("""
        You are an expert web automation engineer...
        """)
    @UserMessage("""
        Find XPath for: {{elementDescription}}
        HTML: {{htmlContent}}
        Failed XPath: {{failedXPath}}
        """)
    XPathAnalysisResult findSelectors(
        @V("elementDescription") String elementDescription,
        @V("htmlContent") String htmlContent,
        @V("failedXPath") String failedXPath,
        @V("pageUrl") String pageUrl
    );
}
```

**What happens behind the scenes:**
1. langchain4j generates the implementation at runtime
2. Interpolates variables into the prompt template
3. Calls the LLM (Ollama/LMStudio)
4. **Automatically parses JSON response to `XPathAnalysisResult` POJO**
5. No regex, no manual parsing - it just works! ✨

### Structured Output with @Description

```java
public class XPathAnalysisResult {
    @Description("The best and most reliable XPath selector found")
    private String primaryXPath;

    @Description("Overall confidence 0-100")
    private Integer confidence;

    @Description("Whether the element was found")
    private Boolean elementFound;

    // ... more fields
}
```

The `@Description` annotations guide the LLM to produce correctly structured JSON.

## 📊 Observability

All LLM interactions are automatically logged via `ChatModelListener`:

```
================================================================================
LLM REQUEST
================================================================================
Model: llama3.2
Messages: 2
  - SYSTEM: You are an expert web automation engineer...
  - USER: Find XPath for: search box...
Temperature: 0.3
================================================================================

================================================================================
LLM RESPONSE
================================================================================
Response: {"primaryXPath": "//*[@id='searchBox']", ...}
Token Usage:
  - Input: 245
  - Output: 87
  - Total: 332
================================================================================
```

## 🎯 Usage Example from Selenium

```java
// In your Selenium test
XPathFinderClient client = new XPathFinderClient();

try {
    // Try to find element
    driver.findElement(By.xpath(oldXPath));
} catch (NoSuchElementException e) {
    // Element not found, ask AI for help
    XPathFinderResponse response = client.findXPath(
        driver,
        "search box",
        oldXPath
    );

    // Use AI-suggested XPath
    WebElement element = driver.findElement(
        By.xpath(response.getPrimaryXPath())
    );
}
```

## 🔄 Switching Between Ollama and LMStudio

### Option 1: Edit `application.properties`

Comment/uncomment the appropriate configuration section.

### Option 2: Use Spring Profiles

Create `application-ollama.properties`:
```properties
langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.chat-model.model-name=llama3.2
```

Create `application-lmstudio.properties`:
```properties
langchain4j.open-ai.chat-model.base-url=http://localhost:1234/v1
langchain4j.open-ai.chat-model.api-key=not-needed
```

Run with profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=ollama
# or
mvn spring-boot:run -Dspring-boot.run.profiles=lmstudio
```

## 🎓 Adding New AI Features

Thanks to the modular design, adding new AI features is **trivial**:

```java
@AiService
public interface XPathFinderAI {

    // Existing method
    XPathAnalysisResult findSelectors(...);

    // NEW: Add element validation
    @SystemMessage("You are a web automation expert.")
    @UserMessage("Is this XPath {{xpath}} valid for HTML: {{html}}?")
    Boolean validateXPath(@V("xpath") String xpath, @V("html") String html);

    // NEW: Suggest improvements
    @SystemMessage("You are a web automation expert.")
    @UserMessage("Improve this XPath: {{xpath}}")
    String improveXPath(@V("xpath") String xpath);
}
```

That's it! No service classes, no parsing, no boilerplate. Just add methods.

## 📦 Dependencies

```xml
<!-- langchain4j Spring Boot Starters -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🐛 Troubleshooting

### Ollama Not Found
```bash
# Check if running
curl http://localhost:11434/api/tags

# Start Ollama
ollama serve
```

### LMStudio Connection Error
- Ensure local server is running in LMStudio UI
- Check port (default: 1234)
- Verify model is loaded

### JSON Parsing Errors
- Lower temperature (makes output more structured):
  ```properties
  langchain4j.ollama.chat-model.temperature=0.1
  ```
- Use a more capable model (llama3.2 works well)

### Slow Responses
- Use smaller models
- Reduce HTML size before sending
- Increase timeout if needed

## 📝 Configuration Reference

| Property | Description | Default |
|----------|-------------|---------|
| `langchain4j.ollama.chat-model.base-url` | Ollama API URL | `http://localhost:11434` |
| `langchain4j.ollama.chat-model.model-name` | Model to use | `llama3.2` |
| `langchain4j.ollama.chat-model.temperature` | Randomness (0-1) | `0.3` |
| `langchain4j.ollama.chat-model.timeout` | Request timeout | `300s` |
| `langchain4j.ollama.chat-model.log-requests` | Log requests | `true` |
| `langchain4j.ollama.chat-model.log-responses` | Log responses | `true` |

## 🎨 Project Highlights

### Before Refactoring (Old Approach)
- ❌ 150+ lines of manual prompt building
- ❌ Complex regex parsing
- ❌ Brittle response extraction
- ❌ Hard to extend
- ❌ No type safety

### After Refactoring (New Approach)
- ✅ ~50 lines for AI service interface
- ✅ Automatic JSON extraction
- ✅ Type-safe POJOs
- ✅ Add features by adding methods
- ✅ Follows langchain4j best practices

## 🚀 Future Enhancements

Easy to add thanks to modular design:

- [ ] Streaming responses with `Flux<String>`
- [ ] Conversation memory for multi-turn debugging
- [ ] RAG for documentation-based selector suggestions
- [ ] Tools for actually testing selectors
- [ ] Batch processing of multiple elements

## 📚 Learn More

- [langchain4j Documentation](https://docs.langchain4j.dev/)
- [langchain4j Examples](https://github.com/langchain4j/langchain4j-examples)
- [Ollama Documentation](https://ollama.ai/docs)
- [LMStudio Documentation](https://lmstudio.ai/docs)

## 📄 License

MIT

## 🤝 Contributing

Issues and pull requests welcome!

---

**Built with ❤️ using langchain4j and Spring Boot**
# MySimpleSpringBootAgent
