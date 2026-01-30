#!/bin/bash

# Test scenarios for the refactored LangChain4j Tool Calling architecture
# Run these tests after starting the application with: mvn spring-boot:run

BASE_URL="http://localhost:8080/api/locators"

echo "=================================="
echo "Testing Refactored Architecture"
echo "=================================="
echo ""

# S1: Simple ID Match
echo "S1: Simple ID Match - //*[@id='search']"
curl -X POST "$BASE_URL/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "locator": "//*[@id=\"search\"]",
    "htmlContent": "<html><body><input id=\"search\" name=\"q\" /></body></html>",
    "elementDescription": "search box",
    "pageUrl": "https://example.com"
  }' | jq .
echo ""
echo "Expected: LLM should use findById tool and suggest By.id(\"search\") as best option"
echo ""
echo "=================================="
echo ""

# S2: Typo in Locator (fuzzy matching)
echo "S2: Typo in Locator - //*[@id='searchxyz'] (actual id is 'search')"
curl -X POST "$BASE_URL/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "locator": "//*[@id=\"searchxyz\"]",
    "htmlContent": "<html><body><input id=\"search\" name=\"q\" class=\"search-input\" /></body></html>",
    "elementDescription": "search box",
    "pageUrl": "https://example.com"
  }' | jq .
echo ""
echo "Expected: LLM should use getAllInteractiveElements, recognize typo, suggest By.id(\"search\") or By.name(\"q\")"
echo ""
echo "=================================="
echo ""

# S3: Contains Function
echo "S3: XPath with contains() - //div[contains(@class,'nav-')]"
curl -X POST "$BASE_URL/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "locator": "//div[contains(@class, \"nav-\")]",
    "htmlContent": "<html><body><div class=\"nav-menu\" id=\"mainNav\"><a href=\"/home\">Home</a></div></body></html>",
    "elementDescription": "navigation menu",
    "pageUrl": "https://example.com"
  }' | jq .
echo ""
echo "Expected: LLM should use findByXPath to test, then suggest By.id(\"mainNav\") or By.className(\"nav-menu\")"
echo ""
echo "=================================="
echo ""

# S4: Text Content Matching
echo "S4: Text Matching - //button[text()='Login']"
curl -X POST "$BASE_URL/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "locator": "//button[text()=\"Login\"]",
    "htmlContent": "<html><body><button id=\"login-btn\" name=\"loginButton\" class=\"btn-primary\">Login</button></body></html>",
    "elementDescription": "login button",
    "pageUrl": "https://example.com"
  }' | jq .
echo ""
echo "Expected: LLM should use findByText(\"Login\"), suggest By.id(\"login-btn\") or By.name(\"loginButton\")"
echo ""
echo "=================================="
echo ""

# S5: Nested XPath with Position
echo "S5: Nested XPath with position - //div[@data-testid='user-menu']//button[2]"
curl -X POST "$BASE_URL/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "locator": "//div[@data-testid=\"user-menu\"]//button[2]",
    "htmlContent": "<html><body><div data-testid=\"user-menu\"><button id=\"profile-btn\">Profile</button><button id=\"logout-btn\">Logout</button></div></body></html>",
    "elementDescription": "logout button",
    "pageUrl": "https://example.com"
  }' | jq .
echo ""
echo "Expected: LLM should use findByAttribute, find second button, suggest By.id(\"logout-btn\") (more stable than position)"
echo ""
echo "=================================="
echo ""

# S6: Complex CSS Selector
echo "S6: Complex CSS - div.class1.class2 > input[type='text']"
curl -X POST "$BASE_URL/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "locator": "div.class1.class2 > input[type=\"text\"]",
    "htmlContent": "<html><body><div class=\"class1 class2\"><input type=\"text\" id=\"field1\" name=\"textField\" /></div></body></html>",
    "elementDescription": "text input",
    "pageUrl": "https://example.com"
  }' | jq .
echo ""
echo "Expected: LLM should use findByCss, suggest By.id(\"field1\") or By.name(\"textField\") (simpler)"
echo ""
echo "=================================="
echo ""

echo "All tests completed!"
echo ""
echo "To run this script:"
echo "1. Start the application: cd /Users/karthikp/Documents/GitHub/MySimpleSpringBootAgent && mvn spring-boot:run"
echo "2. In another terminal: cd /Users/karthikp/Documents/GitHub/MySimpleSpringBootAgent && bash test-scenarios.sh"
echo ""
echo "Note: Ensure LM Studio is running on http://localhost:1234 with a loaded model"
