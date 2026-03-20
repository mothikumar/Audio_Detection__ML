from fastapi import FastAPI, UploadFile, File
import shutil
import os
import torch
import joblib # Added to load the scaler
import numpy as np
from pydub import AudioSegment
from model.feature_extraction import extract_features
from model.model_definition import VoiceModel

app = FastAPI()

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "model", "model.pt")
SCALER_PATH = os.path.join(BASE_DIR, "model", "scaler.pkl") # Path to scaler

device = torch.device("cpu")

# -------- Load Model & Scaler --------
checkpoint = torch.load(MODEL_PATH, map_location=device)
input_size = checkpoint["fc1.weight"].shape[1]

model = VoiceModel(input_size)
model.load_state_dict(checkpoint)
model.to(device)
model.eval()

# Load the scaler
scaler = joblib.load(SCALER_PATH)
# -----------------------------

@app.get("/")
def home():
    return {"message": "ListaSpam Backend Running"}

@app.post("/upload")
async def upload_audio(audio: UploadFile = File(...)):
    raw_path = f"temp_{audio.filename}"
    wav_path = "temp_audio.wav"

    try:
        # Save uploaded file
        with open(raw_path, "wb") as buffer:
            shutil.copyfileobj(audio.file, buffer)

        # Convert audio to WAV
        sound = AudioSegment.from_file(raw_path, format=None)
        sound.export(wav_path, format="wav")

        # Extract features
        features = extract_features(wav_path)

        if features is None:
            return {"prediction": "Error", "confidence": 0.0, "riskLevel": "Unknown"}

        # --- CRITICAL FIX: SCALE INCOMING DATA ---
        # We must reshape it to 2D array for the scaler, then back to 1D
        features_reshaped = np.array(features).reshape(1, -1)
        scaled_features = scaler.transform(features_reshaped)
        # -----------------------------------------

        # Convert features to tensor
        features_tensor = torch.tensor(scaled_features, dtype=torch.float32).to(device)

        # Model prediction
        with torch.no_grad():
            outputs = model(features_tensor)
            probabilities = torch.softmax(outputs, dim=1)
            confidence, predicted = torch.max(probabilities, 1)

        label = "Human" if predicted.item() == 0 else "AI"

        return {
            "prediction": label,
            "confidence": float(confidence.item()),
            "riskLevel": "High" if label == "AI" else "Low"
        }

    except Exception as e:
        print("Error:", e)
        return {"prediction": "Server Error", "confidence": 0.0, "riskLevel": "Unknown"}

    finally:
        # Remove temporary files
        if os.path.exists(raw_path):
            os.remove(raw_path)
        if os.path.exists(wav_path):
            os.remove(wav_path)

# -------- Run Server --------
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)