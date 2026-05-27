# ApnaBuddy – AI Mental Health Companion

![Status](https://img.shields.io/badge/Status-Active-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue)

ApnaBuddy is an advanced AI assistant created to help students manage their mental well-being. Available around the clock, it offers judgment-free support, tracks emotional trends, and guides users to professional help if a crisis is detected.

> **Note:** ApnaBuddy explicitly supplements rather than replaces licensed mental health professionals. It does not provide clinical diagnosis or medical prescriptions.

---

## 📖 Table of Contents

- [Purpose & Motivation](#-purpose--motivation)
- [Key Features](#-key-features)
- [Tech Stack & Architecture](#-tech-stack--architecture)
- [Machine Learning Pipelines](#-machine-learning-pipelines)
- [Advanced Context Management](#-advanced-context-management)
- [How to Run the Project](#-how-to-run-the-project)
- [Developer](#-developer)

---

## 🎯 Purpose & Motivation

Students worldwide face immense pressure, leading to stress and anxiety. Traditional counseling centers are often overbooked, and many individuals hesitate to seek help due to societal judgment. ApnaBuddy bridges this critical gap.

**Why ApnaBuddy was built:**

- **Always Available** – Offers assistance late at night when traditional centers are closed.
- **Judgment-Free** – Chatting with a digital assistant removes the fear of human judgment.
- **Proactive Detection** – Uses machine learning to analyze emotional trends and flag potential crises early.
- **Bilingual Support** – Understands Hinglish, making it natural and accessible for users who mix Hindi and English.

---

## ✨ Key Features

1. **Multi-Layered Safety** – Messages pass through specialized sentiment and risk-detection models before the AI responds.
2. **Hinglish Processing** – Accurately reads mixed-language inputs without confusing the backend models.
3. **Automated Wellness Interventions** – Can trigger interactive exercises (like breathing techniques) automatically if the user is overwhelmed.
4. **Interactive Modules** – Includes journaling, an acceptance therapy game (Fire Game), and drawing therapy (Skribble).
5. **Comprehensive Assessment** – Features a built-in emotional well-being questionnaire.

---

## 💻 Tech Stack & Architecture

ApnaBuddy uses a decoupled, microservices-based hybrid AI architecture.

### Frontend (Mobile App)
| Layer | Technology |
|---|---|
| Framework | React Native, Expo SDK |
| UI / Animations | React Native Reanimated, Expo AV, React Native Canvas |
| Networking / Auth | Axios, Expo Secure Store |

### Backend (Orchestrator)
| Layer | Technology |
|---|---|
| Framework | Java 21, Spring Boot |
| AI Integration | Spring AI |
| Database | PostgreSQL with pgvector |
| Security | JWT Authentication via Google OAuth2 |

### Machine Learning Engine
| Layer | Technology |
|---|---|
| Framework | Python, FastAPI, Uvicorn |
| Libraries | PyTorch, Hugging Face Transformers |

---

## 🧠 Machine Learning Pipelines

The FastAPI service runs multiple models simultaneously for deterministic emotional tracking:

| Model | Purpose |
|---|---|
| `twitter-roberta-base-sentiment-latest` | Sentiment analysis |
| `roberta-base-go_emotions` | Emotion detection |
| `autotrain-deproberta-large-depression` | Depression severity |
| `roberta-base-suicide-prediction-phr` | Suicide risk detection |
| `opus-mt-hi-en` | Hinglish translation |

---

## 🔄 Advanced Context Management

ApnaBuddy optimizes LLM performance, response accuracy, and cost through a highly engineered context management system.

### Dynamic Context Switching (Smart RAG)

The system avoids the high costs and latency of querying vector databases on every request. Instead, it uses **risk-stratified routing**:

- **Low / Moderate Risk** – If ML telemetry detects routine conversation, the system bypasses the vector database completely and uses only short-term sliding window memory.
- **High / Critical Risk** – If telemetry flags elevated risk, the system dynamically switches to deep vector retrieval (RAG). It queries both the user's specific history (`user_vector_store`) and a global knowledge base of CBT protocols (`global_vector_store`) to ground the LLM's response in verifiable therapeutic guidelines.

> This dynamic switching reduces average token usage by over 50%.

### State Management & Context Saving

To maintain natural conversational continuity without exceeding context windows, the backend implements:

- **Sliding Window Memory** – The orchestrator maintains a short-term rolling context window of the most recent user and assistant interactions.
- **Long-Term Personalized Embeddings** – Every significant interaction, along with its ML-generated telemetry (sentiment, risk level), is vectorized and persisted in PostgreSQL using `pgvector`. This allows the AI to recall previous coping strategies and triggers weeks after they were discussed, creating a genuinely personalized longitudinal memory.

---

## 🚀 How to Run the Project
 
### Prerequisites
 
- Docker & Docker Compose
- Node.js (v18+)
- Expo CLI
### Step 1: Clone the Repository
 
```bash
git clone https://github.com/Parth-justparth/ApnaBuddy.git
cd ApnaBuddy
```
 
### Step 2: Configure Environment Variables
 
Create a `.env` file in the root directory and add the following keys:
 
```env
DB_USER=your_db_user
DB_PASS=your_db_password
JWT_SECRET=your_super_secret_jwt_key_here
OPENAI_API_KEY=sk-your-openai-or-gemini-key
```
 
### Step 3: Start the ML Engine
 
Navigate to the `ml-engine` directory and start the FastAPI service:
 
```bash
cd E:\mh\ml-engine
python -m uvicorn main:app --reload --port 8000
```
 
The ML service will be available at: **http://localhost:8000**
 
### Step 4: Start the Java Backend
 
Navigate to the `ApnaBuddy` directory and run the Spring Boot application using the Maven wrapper:
 
```bash
cd ApnaBuddy
./mvnw spring-boot:run
```
 
> On Windows, use `mvnw.cmd` instead:
> ```cmd
> mvnw.cmd spring-boot:run
> ```
 
The Spring Boot API will be available at: **http://localhost:8084**
 
| Service | URL |
|---|---|
| Spring Boot API | http://localhost:8084 |
| FastAPI ML Service | http://localhost:8000 |
 
> **Note:** Make sure the ML Engine (Step 3) is running before starting the Java backend, as it performs a health check on startup.
 
### Step 4: Run the Mobile Frontend
 
Navigate to the frontend directory and start the Expo development server:
 
```bash
cd frontend
npm install
npx expo start
```
 
---
 
## 👨‍💻 Developer
 
**Parth Singh**
B.Tech in Computer Science and Engineering
National Institute of Technology (NIT) Delhi