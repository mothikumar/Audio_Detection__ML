from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import librosa
import numpy as np
import tempfile
import os
import joblib
from firebase_config import db

app = FastAPI(title="ListaSpam AI Voice Detection")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------- RESPONSE MODEL ----------
class AnalysisResponse(BaseModel):
    prediction: str
    confidence: float
    riskLevel: str

# ---------- LOAD ML MODEL ----------
MODEL_PATH = "model.pkl"

if not os.path.exists(MODEL_PATH):
    raise RuntimeError("model.pkl not found")

model = joblib.load(MODEL_PATH)

# ---------- FEATURE EXTRACTION ----------
def extract_features(file_path: str):
    y, sr = librosa.load(file_path, sr=22050)
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=20)
    return np.mean(mfcc.T, axis=0)

# ---------- API ----------
@app.post("/analyze", response_model=AnalysisResponse)
async def analyze_audio(file: UploadFile = File(...)):

    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in [".wav", ".mp3", ".m4a", ".flac"]:
        raise HTTPException(400, "Unsupported file type")

    content = await file.read()
    if not content:
        raise HTTPException(400, "Empty file")

    with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp:
        tmp.write(content)
        tmp_path = tmp.name

    try:
        features = extract_features(tmp_path).reshape(1, -1)

        probs = model.predict_proba(features)[0]
        human_prob, ai_prob = probs

        if ai_prob > human_prob:
            result = AnalysisResponse(
                prediction="AI Generated",
                confidence=float(ai_prob),
                riskLevel="High" if ai_prob > 0.8 else "Medium"
            )
        else:
            result = AnalysisResponse(
                prediction="Human",
                confidence=float(human_prob),
                riskLevel="Low"
            )

        # ---------- STORE IN FIREBASE ----------
        db.collection("voice_analysis").add({
            "filename": file.filename,
            "prediction": result.prediction,
            "confidence": result.confidence,
            "riskLevel": result.riskLevel,
            "source": "Android",
        })

        return result

    finally:
        os.remove(tmp_path)
