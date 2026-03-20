import torch.nn as nn

class VoiceModel(nn.Module):
    def __init__(self, input_size):
        super(VoiceModel, self).__init__()
        # First layer
        self.fc1 = nn.Linear(input_size, 64)
        self.relu = nn.ReLU()
        
        # Dropout layer (randomly drops 30% of neurons to prevent overfitting)
        self.dropout = nn.Dropout(p=0.3) 
        
        # Output layer (2 classes: Human or AI)
        self.fc2 = nn.Linear(64, 2)

    def forward(self, x):
        x = self.fc1(x)
        x = self.relu(x)
        x = self.dropout(x) # Apply dropout before the final decision
        x = self.fc2(x)
        return x