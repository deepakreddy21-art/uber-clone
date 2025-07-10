from fastapi import FastAPI, Query, Request
from pydantic import BaseModel
import numpy as np
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

class ETAPredictRequest(BaseModel):
    pickup_lat: float
    pickup_lng: float
    dropoff_lat: float
    dropoff_lng: float
    hour_of_day: int

class ETAPredictResponse(BaseModel):
    eta_minutes: float

@app.post("/predict-eta", response_model=ETAPredictResponse)
def predict_eta(req: ETAPredictRequest, request: Request):
    logger.info(f"Received ETA prediction request: {req}")
    try:
        dist = np.sqrt((req.pickup_lat - req.dropoff_lat) ** 2 + (req.pickup_lng - req.dropoff_lng) ** 2)
        eta = dist * 60 + np.random.uniform(2, 5) + (0.5 if 7 <= req.hour_of_day <= 9 or 17 <= req.hour_of_day <= 19 else 0)
        logger.info(f"Predicted ETA: {eta}")
        return ETAPredictResponse(eta_minutes=round(eta, 2))
    except Exception as e:
        logger.error(f"Error in ETA prediction: {e}")
        raise 