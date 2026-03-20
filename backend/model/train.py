import os
import torch
import torch.nn as nn
import torch.optim as optim
import numpy as np
import pandas as pd
import joblib
from sklearn.preprocessing import StandardScaler
from tqdm import tqdm

# Import your custom modules
from feature_extraction import extract_features
from model_definition import VoiceModel

# --- 1. Setup Exact Paths based on your VS Code workspace ---
# BASE_DIR is 'backend/model'
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# ROOT_DIR is 'backend'
ROOT_DIR = os.path.dirname(BASE_DIR)

# Point to the audio folder and the CSV inside it
AUDIO_FOLDER = os.path.join(ROOT_DIR, "audio")
CSV_FILE = os.path.join(AUDIO_FOLDER, "meta.csv") # UPDATE THIS if your excel is named something else!

MODEL_PATH = os.path.join(BASE_DIR, "model.pt")
SCALER_PATH = os.path.join(BASE_DIR, "scaler.pkl")


print(f"Reading CSV map from: {CSV_FILE}")
try:
   
    df = pd.read_csv(CSV_FILE).head(100) 
except FileNotFoundError:
    print(f"Error: Could not find {CSV_FILE}. Please check the file name.")
    exit()

X = []
y = []

print("Extracting features from audio files...")
# Loop through the CSV
for index, row in tqdm(df.iterrows(), total=df.shape[0]):
    file_name = row['file']    # Assuming column is named 'file'
    label_text = row['label']  # Assuming column is named 'label'
    
    file_path = os.path.join(AUDIO_FOLDER, file_name)
    
    if os.path.exists(file_path):
        features = extract_features(file_path)
        
        if features is not None:
            X.append(features)
            # Map labels: 0 for Human (bona-fide), 1 for AI (spoof)
            if str(label_text).strip().lower() == 'bona-fide':
                y.append(0)
            else:
                y.append(1)

if len(X) == 0:
    print("No valid audio features extracted. Exiting.")
    exit()

X = np.array(X)
y = np.array(y)

print(f"Dataset Loaded: {X.shape[0]} samples, {X.shape[1]} features each.")

# --- 3. CRITICAL FIX: Scale the Features ---
print("Scaling features...")
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)
joblib.dump(scaler, SCALER_PATH)
print("Scaler saved successfully to scaler.pkl!")

# Convert to PyTorch tensors
X_tensor = torch.tensor(X_scaled, dtype=torch.float32)
y_tensor = torch.tensor(y, dtype=torch.long)

# --- 4. Initialize and Train the Model ---
model = VoiceModel(X_tensor.shape[1])
criterion = nn.CrossEntropyLoss()
optimizer = optim.Adam(model.parameters(), lr=0.001)

print("Training started...")
epochs = 50
for epoch in range(epochs):
    outputs = model(X_tensor)
    loss = criterion(outputs, y_tensor)

    optimizer.zero_grad()
    loss.backward()
    optimizer.step()

    if (epoch + 1) % 10 == 0:
        print(f"Epoch {epoch+1}/{epochs}, Loss: {loss.item():.4f}")

# --- 5. Evaluate and Save ---
with torch.no_grad():
    model.eval() # Set to evaluation mode
    outputs = model(X_tensor)
    _, predicted = torch.max(outputs, 1)
    accuracy = (predicted == y_tensor).sum().item() / len(y_tensor)

print(f"Final Training Accuracy: {round(accuracy * 100, 2)}%")

# Save the brain
torch.save(model.state_dict(), MODEL_PATH)
print(f"Model saved successfully to {MODEL_PATH}!")