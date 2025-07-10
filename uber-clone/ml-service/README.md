# ML Microservice (ETA Prediction)

This is a FastAPI-based microservice for ETA prediction.

## Setup

1. Install Python 3.9+
2. Install dependencies:
   ```sh
   pip install -r requirements.txt
   ```
3. Run the service:
   ```sh
   uvicorn main:app --reload --port 8001
   ```

## API
- `POST /predict-eta` — Predicts ETA in minutes given pickup/dropoff coordinates and hour of day. 