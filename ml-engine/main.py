from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import pipeline
import logging
import warnings

# Suppress Hugging Face warnings for cleaner console output
warnings.filterwarnings("ignore")

# Configure basic logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger("MYBUDDY-ML-Engine")

app = FastAPI(title="MYBUDDY ML Inference Engine", version="2.0")

# ==========================================
# 1. DEFINE API DATA MODELS
# ==========================================
class ContextRequest(BaseModel):
    text: str

class AnalysisResponse(BaseModel):
    translated_text: str # Echoes input back to Java
    risk_level: str      # Low, Moderate, High, CRITICAL
    sentiment: str       # positive, neutral, negative
    emotion: str         # specific emotion (e.g., sadness, joy, anger)
    emotion_score: float # 0.0 to 1.0
    suicidal_score: float # ALWAYS 0.0 to 1.0 (Probability of Suicide)
    depression_score: float # ALWAYS 0.0 to 1.0 (Probability of Depression)

# ==========================================
# 2. LOAD MODELS ON STARTUP
# ==========================================
logger.info("⏳ Loading Hugging Face Models into Memory (This takes a moment)...")

try:
    sentiment_pipe = pipeline("sentiment-analysis", model="cardiffnlp/twitter-roberta-base-sentiment-latest")
    emotion_pipe = pipeline("text-classification", model="SamLowe/roberta-base-go_emotions", top_k=1)
    suicide_pipe = pipeline("text-classification", model="gooohjy/suicidal-electra")
    depression_pipe = pipeline("text-classification", model="rafalposwiata/deproberta-large-depression")
    logger.info("✅ All Models Loaded Successfully")
except Exception as e:
    logger.error(f"❌ Failed to load models. Error: {e}")

# ==========================================
# 3. GUARDRAIL: CONTEXT WINDOW LIMITER
# ==========================================
def enforce_token_limit(text: str, max_words: int = 350) -> str:
    """
    Ensures input fits within model token limits (512 tokens).
    Keeps the newest part of the conversation (the end of the string).
    """
    words = text.split()
    if len(words) > max_words:
        truncated = " ".join(words[-max_words:])
        logger.warning(f"⚠️ Truncated context from {len(words)} to {max_words} words.")
        return truncated
    return text

# ==========================================
# 4. THE MAIN API ENDPOINT
# ==========================================
@app.post("/analyze", response_model=AnalysisResponse)
async def analyze_context(request: ContextRequest):
    try:
        # Text from Spring Boot is already translated to English
        safe_text = enforce_token_limit(request.text)
        
        logger.info("=========================================")
        logger.info(f"📥 RECEIVED ENGLISH CONTEXT:\n{safe_text}")
        
        # 1. Run Inference
        sent_res = sentiment_pipe(safe_text)[0]
        emo_res = emotion_pipe(safe_text)[0]
        suicide_res = suicide_pipe(safe_text)[0]
        dep_res = depression_pipe(safe_text)[0]

        # 2. Extract Basic Features
        sentiment = sent_res['label'].lower()
        top_emotion = emo_res[0]['label']
        emotion_score = round(emo_res[0]['score'], 4)

        # 3. 🔥 SCORE NORMALIZATION (Crucial for Java Backend)
        # If the model says 'LABEL_0' (Safe), we must invert the score so it represents DANGER.
        # This means 0.99 safe becomes 0.01 risk. Java will always know higher = worse.
        suicide_prob = suicide_res['score'] if suicide_res['label'] == 'LABEL_1' else (1.0 - suicide_res['score'])
        dep_prob = dep_res['score'] if dep_res['label'] == 'LABEL_1' else (1.0 - dep_res['score'])
        
        suicide_prob = round(suicide_prob, 4)
        dep_prob = round(dep_prob, 4)

        # 4. 🔥 MULTI-FACTOR RISK LOGIC
        risk = "Low"
        
        # High Risk Emotions that flag a moderate warning even if suicide is low
        warning_emotions = ["sadness", "anger", "fear", "grief", "remorse", "nervousness", "disappointment"]

        if suicide_prob >= 0.85:
            risk = "CRITICAL"
        elif suicide_prob >= 0.60 or dep_prob >= 0.85:
            risk = "High"
        elif dep_prob >= 0.50 or (sentiment == "negative" and top_emotion in warning_emotions and emotion_score > 0.40):
            risk = "Moderate"

        # 5. Build Response
        response_payload = AnalysisResponse(
            translated_text=safe_text,
            risk_level=risk,
            sentiment=sentiment,
            emotion=top_emotion,
            emotion_score=emotion_score,
            suicidal_score=suicide_prob,
            depression_score=dep_prob
        )

        logger.info(f"🎯 ANALYSIS -> Risk: [{risk}] | Emotion: [{top_emotion}] | Sui_Prob: [{suicide_prob}] | Dep_Prob: [{dep_prob}]")
        logger.info("📤 SENDING JSON RESPONSE TO SPRING BOOT")
        logger.info("=========================================\n")

        return response_payload

    except Exception as e:
        logger.error(f"Error during ML analysis: {str(e)}")
        raise HTTPException(status_code=500, detail="ML processing failed internally")