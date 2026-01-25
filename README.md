# AI-Powered Selenium Locator Analyzer

A Spring Boot service that uses local LLMs to analyze failed Selenium locators and suggest alternatives across **ALL** Selenium locator types.

## ✨ Key Features

- 🎯 **All Selenium Locators** - ID, Name, ClassName, TagName, LinkText, PartialLinkText, CSS, XPath
- 🤖 **AI-Powered** - Local LLM analysis with confidence scores and explanations
- ⚡ **Smart Preprocessing** - 90%+ HTML size reduction for large pages
- 🏆 **Priority Recommendations** - ID > Name > LinkText > CSS > XPath
- 🛠️ **SOLID Architecture** - Clean, testable, maintainable design
- 📊 **Built-in Observability** - Full LLM interaction logging

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+
- Ollama with llama3.2

### Setup

```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh
ollama pull llama3.2

# Clone and run
git clone <repo-url>
cd MySimpleSpringBootAgent
mvn spring-boot:run
```

Service runs at `http://localhost:8080`

### Test It

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

## 📖 Documentation

**[→ Full Documentation](DOCUMENTATION.md)** - Complete guide with examples, configuration, and troubleshooting

### Quick Links
- [API Reference](DOCUMENTATION.md#api-reference) - Endpoints and response formats
- [Usage Examples](DOCUMENTATION.md#usage-examples) - Curl and Java/Selenium examples
- [Architecture](DOCUMENTATION.md#architecture) - Design and structure
- [Configuration](DOCUMENTATION.md#configuration) - Settings and options
- [Troubleshooting](DOCUMENTATION.md#troubleshooting) - Common issues and solutions

## 🎯 Selenium Locator Types

| Type | Reliability | When to Use |
|------|-------------|-------------|
| **By.id** | ⭐⭐⭐⭐⭐ | Element has stable ID (preferred) |
| **By.name** | ⭐⭐⭐⭐ | Form elements |
| **By.linkText** | ⭐⭐⭐⭐ | Links with stable text |
| **By.className** | ⭐⭐⭐ | Unique stable classes |
| **By.cssSelector** | ⭐⭐⭐⭐ | Flexible queries |
| **By.xpath** | ⭐⭐ | Last resort (brittle) |

## 💡 Example Usage in Tests

```java
try {
    driver.findElement(By.xpath("//*[@id='oldId']"));
} catch (NoSuchElementException e) {
    // Get AI suggestions
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

## 🏗️ Architecture Highlights

### SOLID Design Principles
- **Single Responsibility** - Each service has one clear purpose
- **Interface Segregation** - Small, focused interfaces
- **Dependency Inversion** - Depend on abstractions, not implementations

### Key Components
- **Controller Layer** - HTTP concerns only
- **Service Layer** - Business logic with interfaces
- **AI Service** - LangChain4j integration
- **HTML Preprocessing** - 5-stage pipeline for size reduction

### Recent Refactoring (2026-01-25)
Refactored to fix SOLID violations:
- Extracted validation, mapping, and formatting from controller
- Split HTML preprocessing utilities into focused services
- Improved testability and maintainability

See [solid-refactoring-summary.md](.serena/memories/solid-refactoring-summary.md) for details.

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:

```properties
# LLM (Ollama)
langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.chat-model.model-name=llama3.2
langchain4j.ollama.chat-model.temperature=0.3

# HTML Preprocessing
html.processing.max-output-size=51200
html.processing.max-candidates=5
```

For LMStudio or other providers, see [Configuration](DOCUMENTATION.md#configuration).

## 🐛 Troubleshooting

**Ollama not running?**
```bash
ollama serve
ollama pull llama3.2
```

**JSON parsing errors?**
```properties
langchain4j.ollama.chat-model.temperature=0.1
```

See [Troubleshooting Guide](DOCUMENTATION.md#troubleshooting) for more.

## 📦 Tech Stack

- Java 21, Spring Boot 3.4.2
- langchain4j 1.10.0
- Jsoup 1.17.2
- HtmlCompressor 1.5.2
- Lombok, Apache Commons

## 📚 Resources

- [Full Documentation](DOCUMENTATION.md)
- [langchain4j Docs](https://docs.langchain4j.dev/)
- [Selenium Docs](https://www.selenium.dev/documentation/)
- [Ollama](https://ollama.ai/)

## 🤝 Contributing

Issues and PRs welcome!

## 📄 License

MIT

---

**Built with ❤️ using langchain4j, Spring Boot, and SOLID principles**
