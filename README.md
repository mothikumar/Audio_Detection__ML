# 🛡️ Intelligent Spam Call Classification (Deepfake Detection)

An end-to-end, dual-layer telecommunication security system designed to intercept spoofed numbers and detect AI-generated synthetic voices (deepfakes) in real-time.

## 📖 Overview
Traditional caller ID applications rely on static blacklists and fail against zero-hour spoofing and AI voice cloning. This project introduces a hybrid architecture:
1. **The Network Layer (Android/Kotlin):** A highly concurrent mobile client that races multiple APIs (Truecaller, Tellows, ListaSpam) alongside local STIR/SHAKEN protocols to block known threats instantly.
2. **The Acoustic Layer (Python/FastAPI):** A deep learning backend that extracts Mel-Frequency Cepstral Coefficients (MFCCs) and utilizes a custom PyTorch Neural Network to classify the audio payload as either human or synthetic.

## 🚀 Architecture Highlights
* **Kotlin Coroutines:** Implemented an asynchronous API "Race" architecture, reducing network querying latency by 60%.
* **PyTorch Inference Engine:** Trained on 31,000+ audio samples, achieving **94.2% accuracy** and **95.1% precision** in identifying high-frequency synthetic artifacts.
* **RESTful Microservice:** A high-performance FastAPI backend designed for rapid audio ingestion, stateless processing, and low-latency JSON responses.

## 🛠️ Technology Stack
* **Frontend/Mobile:** Android SDK, Kotlin, Coroutines
* **Backend API:** Python 3.9+, FastAPI, Uvicorn
* **Machine Learning:** PyTorch, Scikit-Learn (Joblib)
* **Audio Processing:** Librosa, Soundfile, NumPy

---

## ⚙️ Getting Started (Backend Setup)

### Prerequisites
* Python 3.9 or higher
* Minimum 4GB RAM (for PyTorch CPU inference)

### 1. Clone the Repository
```bash
git clone [https://github.com/mothikumar/spam-call-classification.git](https://github.com/mothikumar/spam-call-classification.git)
cd spam-call-classification

2. Environment Setup
Create and activate a virtual environment to manage dependencies:

Bash
# Windows
python -m venv venv
venv\Scripts\activate

# Linux/Mac
python3 -m venv venv
source venv/bin/activate
3. Install Dependencies
Bash
pip install -r requirements.txt
4. Run the FastAPI Server
Bash
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
The backend API will now be running at http://localhost:8000. You can access the interactive Swagger UI documentation at http://localhost:8000/docs.

📱 Android Client Setup
(Note: Ensure the Python backend is running and accessible on your local network before testing the mobile client.)

Open the /android-client directory in Android Studio.

Sync the Gradle files.

In your local.properties file, add your local backend IP address:

Properties
BACKEND_BASE_URL="http://YOUR_LOCAL_IP:8000/"
Build and deploy the .apk to a physical Android device (Emulators do not support active call state interception effectively).

🧠 Machine Learning Pipeline
If you wish to retrain the PyTorch model:

Ensure your audio dataset is placed in the /data directory (structured as /data/human and /data/ai).

Run the feature extraction and training script:

Bash
python train.py
The script will output a new model.pt and scaler.pkl in the /model directory.

🔒 Security & Privacy
Ephemeral Processing: All audio payloads sent to the backend are temporarily stored in memory/disk and are strictly deleted within milliseconds of the inference returning a result. No user call data is persisted.

Environment Variables: Sensitive API keys (e.g., Truecaller API) are managed via .env files and are excluded from version control.

👨‍💻 Author
Mothikumar R * LinkedIn

GitHub


### **Why this README works:**
1. **The Emojis & Headers:** It makes the document highly scannable. Hiring managers spend about 10 seconds looking at a repository; make sure they see the architecture immediately.
2. **The "Architecture Highlights" Section:** This is the most critical part for a DevOps application. It highlights your focus on *latency*, *microservices*, and *concurrent processing*, rather than just writing code.
3. **The Security Section:** Pointing out "Ephemeral Processing" shows that you understand
