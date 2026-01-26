# 📊 Diagrams & Documentation Guide

This project includes comprehensive documentation with visual diagrams to help you understand how the system works.

## 📁 Available Documentation Files

### 1. **SIMPLE_FLOW_GUIDE.md** ⭐ START HERE
   - **Format**: ASCII diagrams (works everywhere!)
   - **View**: Open in IntelliJ - no plugins needed
   - **Contains**:
     - Flow from request to response
     - Component architecture
     - Tool capabilities
     - AI analysis steps
     - Request/response format
     - Error handling
     - Quick checklist
   
   **Best for**: Reading in IDE, understanding flow quickly

---

### 2. **FLOW_DIAGRAMS.md** 
   - **Format**: Mermaid diagrams (beautiful but needs rendering)
   - **View on GitHub**: https://github.com/... (renders automatically)
   - **View in IntelliJ**: Install "Mermaid" plugin from JetBrains marketplace
   - **View online**: Copy diagrams to https://mermaid.live
   - **View in VSCode**: Built-in support
   - **Contains**: 11 detailed diagrams
   
   **Best for**: Visual learners, GitHub viewing

---

### 3. **README.md**
   - **Format**: Mix of text and Mermaid diagrams
   - **Contains**:
     - Quick start guide
     - Visual flow diagrams
     - Component architecture
     - Selenium locator types
     - Usage examples
     - Configuration
   
   **Best for**: First impression, quick reference

---

### 4. **DOCUMENTATION.md**
   - **Format**: Comprehensive technical documentation
   - **Contains**:
     - Detailed API reference
     - Usage examples
     - Architecture deep dive
     - Configuration options
     - Troubleshooting guide
   
   **Best for**: Complete understanding

---

## 🎯 Recommended Reading Order

### For New Users:
1. **README.md** - Get the overview
2. **SIMPLE_FLOW_GUIDE.md** - Understand the flow visually
3. **DOCUMENTATION.md** - Deep dive into details

### For Developers:
1. **SIMPLE_FLOW_GUIDE.md** - Understand architecture
2. **FLOW_DIAGRAMS.md** - Visual component relationships
3. **DOCUMENTATION.md** - Implementation details

### For Integration:
1. **README.md** - Quick start
2. **DOCUMENTATION.md** - API reference
3. Code examples in DOCUMENTATION.md

---

## 🎨 How to View Mermaid Diagrams

### Option 1: GitHub ✅ (Recommended - Easiest)
1. Push code to GitHub
2. Open `.md` files in GitHub web interface
3. Diagrams render automatically

### Option 2: IntelliJ (Local Development)
1. Open IntelliJ Settings
2. Plugins → Marketplace
3. Search for "Mermaid"
4. Install the Mermaid plugin
5. Restart IntelliJ
6. Open any `.md` file → Diagrams render in preview tab

### Option 3: Online Mermaid Editor
1. Go to: https://mermaid.live
2. Create new diagram
3. Copy any diagram code from `.md` file
4. Paste into editor
5. View rendered diagram

### Option 4: VSCode
1. VSCode has built-in Mermaid support
2. Just open any `.md` file
3. Click "Preview" or press Ctrl+Shift+V
4. All diagrams render automatically

---

## 📋 What Each Diagram Shows

| File | Diagram # | Name | Shows |
|------|-----------|------|-------|
| SIMPLE_FLOW_GUIDE.md | 1 | Request Flow | Complete request lifecycle |
| SIMPLE_FLOW_GUIDE.md | 2 | Components | All system components |
| SIMPLE_FLOW_GUIDE.md | 3 | Tools | AI tool capabilities |
| SIMPLE_FLOW_GUIDE.md | 4 | AI Strategy | How AI analyzes |
| SIMPLE_FLOW_GUIDE.md | 5 | Why Smart | Efficiency explanation |
| SIMPLE_FLOW_GUIDE.md | 6 | Request Format | Input structure |
| SIMPLE_FLOW_GUIDE.md | 7 | Response Format | Output structure |
| SIMPLE_FLOW_GUIDE.md | 8 | Error Handling | Error scenarios |
| SIMPLE_FLOW_GUIDE.md | 9 | File Organization | Code structure |
| FLOW_DIAGRAMS.md | 1 | Main Process | Sequence of operations |
| FLOW_DIAGRAMS.md | 2 | Architecture | Component dependencies |
| FLOW_DIAGRAMS.md | 3 | Processing Steps | Step-by-step flow |
| FLOW_DIAGRAMS.md | 4 | Tools | Tool capabilities |
| FLOW_DIAGRAMS.md | 5 | AI Strategy | Analysis process |
| FLOW_DIAGRAMS.md | 6 | Why Design Works | Architecture benefits |
| FLOW_DIAGRAMS.md | 7 | Request Data | Input structure |
| FLOW_DIAGRAMS.md | 8 | Response Data | Output structure |
| FLOW_DIAGRAMS.md | 9 | Lifecycle | Full request lifecycle |
| FLOW_DIAGRAMS.md | 10 | Error Handling | Error flows |
| FLOW_DIAGRAMS.md | 11 | Locator Preferences | Priority order |

---

## 🔑 Key Concepts Across All Docs

### The Main Flow
```
Request (locator + HTML)
    ↓
Validate input
    ↓
Preprocess HTML (store on server)
    ↓
Call AI (send only 3 params: locator, description, url)
    ↓
AI uses tools to query stored HTML
    ↓
AI returns best locator + alternatives
    ↓
Response (recommendations + confidence score)
```

### Why This Design?
- ✅ **Efficient**: Only 3 params to AI, not full HTML
- ✅ **Fast**: Smaller payloads, quick processing
- ✅ **Cheap**: Fewer tokens consumed
- ✅ **Secure**: HTML stays on server
- ✅ **Smart**: AI decides what to query

### Core Components
1. **LocatorController** - HTTP endpoint
2. **HtmlPreprocessor** - Cleans HTML
3. **DomQueryTools** - Stores HTML, provides tools
4. **LocatorAnalyzerAI** - Claude AI interface
5. **ResponseMapper** - Formats response

---

## 💡 Quick Reference

| Want to know... | See file | Section |
|-----------------|----------|---------|
| How to get started? | README.md | Quick Start |
| How does request flow? | SIMPLE_FLOW_GUIDE.md | Diagram 1 |
| What components exist? | SIMPLE_FLOW_GUIDE.md | Diagram 2 |
| What tools does AI have? | SIMPLE_FLOW_GUIDE.md | Diagram 3 |
| How does AI analyze? | SIMPLE_FLOW_GUIDE.md | Diagram 4 |
| API endpoint details? | DOCUMENTATION.md | API Reference |
| Configuration? | DOCUMENTATION.md | Configuration |
| Troubleshooting? | DOCUMENTATION.md | Troubleshooting |
| Detailed examples? | DOCUMENTATION.md | Usage Examples |

---

## 📞 Having Issues?

### Diagram Not Rendering?
- Try SIMPLE_FLOW_GUIDE.md (ASCII version)
- Or view FLOW_DIAGRAMS.md on GitHub
- Or copy diagram code to mermaid.live

### Understanding the System?
- Start with README.md
- Then read SIMPLE_FLOW_GUIDE.md
- Then dive into specific sections

### Questions About Code?
- See DOCUMENTATION.md for examples
- Check component files for details

### Setup Issues?
- See README.md Quick Start
- See DOCUMENTATION.md Troubleshooting

---

## 🎯 Summary

- **SIMPLE_FLOW_GUIDE.md** = Best for understanding locally (ASCII, no plugins)
- **FLOW_DIAGRAMS.md** = Best for beautiful visuals (Mermaid, view on GitHub)
- **README.md** = Quick reference
- **DOCUMENTATION.md** = Complete reference

**Start with README.md, then pick the documentation style that works best for you!**
