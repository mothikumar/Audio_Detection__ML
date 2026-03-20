import librosa
import numpy as np

def extract_features(audio_path):
    try:
        y, sr = librosa.load(audio_path, sr=16000, duration=5)

        if len(y) == 0:
            raise ValueError("Audio file is empty")

        # MFCC
        mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13)
        mfcc_mean = np.mean(mfcc, axis=1)
        mfcc_var = np.var(mfcc, axis=1)

        # Zero Crossing Rate
        zcr = librosa.feature.zero_crossing_rate(y)
        zcr_mean = np.mean(zcr)

        # Spectral Centroid
        spectral_centroid = librosa.feature.spectral_centroid(y=y, sr=sr)
        spectral_centroid_mean = np.mean(spectral_centroid)

        # RMS Energy
        rms = librosa.feature.rms(y=y)
        rms_mean = np.mean(rms)

        features = np.hstack([
            mfcc_mean,
            mfcc_var,
            zcr_mean,
            spectral_centroid_mean,
            rms_mean
        ])

        return features

    except Exception as e:
        print("Feature extraction failed:", audio_path)
        print("Error:", e)
        return None