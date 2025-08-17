#!/usr/bin/env python3
"""
Uber Clone ML Service
Advanced AI-powered features for ride-sharing platform
"""

import os
import json
import logging
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import Dict, List, Tuple, Optional
import asyncio
import aiohttp
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import redis
from kafka import KafkaProducer, KafkaConsumer
import joblib
import pickle

# ML Libraries
try:
    import tensorflow as tf
    from tensorflow import keras
    TENSORFLOW_AVAILABLE = True
except ImportError:
    TENSORFLOW_AVAILABLE = False
    print("TensorFlow not available, using fallback models")

try:
    import torch
    import torch.nn as nn
    PYTORCH_AVAILABLE = True
except ImportError:
    PYTORCH_AVAILABLE = False
    print("PyTorch not available, using fallback models")

from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import StandardScaler, MinMaxScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import xgboost as xgb
import lightgbm as lgb

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# FastAPI app
app = FastAPI(title="Uber Clone ML Service", version="2.0.0")

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global variables
redis_client = None
kafka_producer = None
models = {}
scalers = {}

# Pydantic models
class LocationData(BaseModel):
    latitude: float
    longitude: float
    timestamp: str
    driver_id: Optional[int] = None
    user_id: Optional[int] = None

class ETAPredictionRequest(BaseModel):
    pickup_lat: float
    pickup_lon: float
    dropoff_lat: float
    dropoff_lon: float
    vehicle_type: str = "STANDARD"
    hour_of_day: int
    day_of_week: int
    weather_condition: str = "CLEAR"
    traffic_level: str = "NORMAL"

class DemandForecastRequest(BaseModel):
    location_lat: float
    location_lon: float
    time_range: str  # "1h", "6h", "24h"
    date: str

class DriverMatchingRequest(BaseModel):
    pickup_lat: float
    pickup_lon: float
    vehicle_type: str
    passenger_count: int = 1
    special_requirements: List[str] = []

class SurgePricingRequest(BaseModel):
    location_lat: float
    location_lon: float
    current_demand: int
    available_drivers: int
    time_of_day: str
    day_type: str  # "weekday", "weekend", "holiday"

# ML Models
class ETAPredictionModel:
    """Advanced ETA prediction using multiple ML algorithms"""
    
    def __init__(self):
        self.models = {}
        self.scalers = {}
        self.feature_columns = [
            'distance_km', 'hour_of_day', 'day_of_week', 'is_peak_hour',
            'is_weekend', 'vehicle_type_encoded', 'weather_encoded',
            'traffic_level_encoded', 'historical_avg_eta'
        ]
        self.initialize_models()
    
    def initialize_models(self):
        """Initialize multiple ML models for ensemble prediction"""
        # Random Forest
        self.models['random_forest'] = RandomForestRegressor(
            n_estimators=100, max_depth=10, random_state=42
        )
        
        # XGBoost
        self.models['xgboost'] = xgb.XGBRegressor(
            n_estimators=100, max_depth=6, learning_rate=0.1, random_state=42
        )
        
        # LightGBM
        self.models['lightgbm'] = lgb.LGBMRegressor(
            n_estimators=100, max_depth=6, learning_rate=0.1, random_state=42
        )
        
        # Neural Network (if TensorFlow available)
        if TENSORFLOW_AVAILABLE:
            self.models['neural_network'] = self.create_neural_network()
        
        # Scaler for features
        self.scalers['standard'] = StandardScaler()
        self.scalers['minmax'] = MinMaxScaler()
    
    def create_neural_network(self):
        """Create TensorFlow neural network for ETA prediction"""
        model = keras.Sequential([
            keras.layers.Dense(128, activation='relu', input_shape=(len(self.feature_columns),)),
            keras.layers.Dropout(0.3),
            keras.layers.Dense(64, activation='relu'),
            keras.layers.Dropout(0.2),
            keras.layers.Dense(32, activation='relu'),
            keras.layers.Dense(1, activation='linear')
        ])
        
        model.compile(
            optimizer='adam',
            loss='mse',
            metrics=['mae']
        )
        
        return model
    
    def prepare_features(self, request: ETAPredictionRequest) -> np.ndarray:
        """Prepare features for ML models"""
        # Calculate distance
        distance = self.haversine_distance(
            request.pickup_lat, request.pickup_lon,
            request.dropoff_lat, request.dropoff_lon
        )
        
        # Encode categorical variables
        vehicle_encoded = self.encode_vehicle_type(request.vehicle_type)
        weather_encoded = self.encode_weather(request.weather_condition)
        traffic_encoded = self.encode_traffic(request.traffic_level)
        
        # Time-based features
        is_peak_hour = 1 if request.hour_of_day in [7, 8, 9, 17, 18, 19] else 0
        is_weekend = 1 if request.day_of_week in [5, 6] else 0
        
        # Historical average (placeholder - would come from database)
        historical_avg = 15.0
        
        features = np.array([
            distance, request.hour_of_day, request.day_of_week,
            is_peak_hour, is_weekend, vehicle_encoded,
            weather_encoded, traffic_encoded, historical_avg
        ]).reshape(1, -1)
        
        return features
    
    def predict(self, request: ETAPredictionRequest) -> Dict:
        """Make ensemble prediction using multiple models"""
        features = self.prepare_features(request)
        
        predictions = {}
        for name, model in self.models.items():
            try:
                if name == 'neural_network':
                    # Scale features for neural network
                    scaled_features = self.scalers['minmax'].fit_transform(features)
                    pred = model.predict(scaled_features)[0][0]
                else:
                    pred = model.predict(features)[0]
                predictions[name] = max(1, int(pred))  # Ensure positive ETA
            except Exception as e:
                logger.error(f"Error in {name} model: {e}")
                predictions[name] = 15  # Fallback prediction
        
        # Ensemble prediction (weighted average)
        weights = {
            'random_forest': 0.3,
            'xgboost': 0.3,
            'lightgbm': 0.2,
            'neural_network': 0.2
        }
        
        ensemble_pred = 0
        total_weight = 0
        
        for name, pred in predictions.items():
            if name in weights:
                ensemble_pred += pred * weights[name]
                total_weight += weights[name]
        
        final_eta = int(ensemble_pred / total_weight) if total_weight > 0 else 15
        
        return {
            "eta_minutes": final_eta,
            "individual_predictions": predictions,
            "confidence": 0.85,
            "model_used": "ensemble"
        }
    
    def haversine_distance(self, lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """Calculate Haversine distance between two points"""
        R = 6371  # Earth's radius in kilometers
        
        lat1_rad = np.radians(lat1)
        lat2_rad = np.radians(lat2)
        delta_lat = np.radians(lat2 - lat1)
        delta_lon = np.radians(lon2 - lon1)
        
        a = (np.sin(delta_lat/2)**2 + 
             np.cos(lat1_rad) * np.cos(lat2_rad) * np.sin(delta_lon/2)**2)
        c = 2 * np.arctan2(np.sqrt(a), np.sqrt(1-a))
        
        return R * c
    
    def encode_vehicle_type(self, vehicle_type: str) -> int:
        """Encode vehicle type to numeric"""
        encoding = {
            "STANDARD": 0, "COMFORT": 1, "PREMIUM": 2, "POOL": 3
        }
        return encoding.get(vehicle_type.upper(), 0)
    
    def encode_weather(self, weather: str) -> int:
        """Encode weather condition to numeric"""
        encoding = {
            "CLEAR": 0, "CLOUDY": 1, "RAIN": 2, "SNOW": 3
        }
        return encoding.get(weather.upper(), 0)
    
    def encode_traffic(self, traffic: str) -> int:
        """Encode traffic level to numeric"""
        encoding = {
            "LIGHT": 0, "NORMAL": 1, "HEAVY": 2, "CONGESTED": 3
        }
        return encoding.get(traffic.upper(), 1)

class DemandForecastingModel:
    """ML-based demand forecasting for different locations and times"""
    
    def __init__(self):
        self.model = RandomForestRegressor(n_estimators=200, random_state=42)
        self.scaler = StandardScaler()
        self.is_trained = False
    
    def train(self, historical_data: pd.DataFrame):
        """Train the demand forecasting model"""
        # Feature engineering
        features = self.engineer_features(historical_data)
        target = historical_data['demand_count'].values
        
        # Scale features
        features_scaled = self.scaler.fit_transform(features)
        
        # Train model
        self.model.fit(features_scaled, target)
        self.is_trained = True
        
        logger.info("Demand forecasting model trained successfully")
    
    def engineer_features(self, data: pd.DataFrame) -> np.ndarray:
        """Engineer features for demand forecasting"""
        features = []
        
        for _, row in data.iterrows():
            # Time-based features
            hour = row['hour']
            day_of_week = row['day_of_week']
            month = row['month']
            
            # Cyclical encoding for time
            hour_sin = np.sin(2 * np.pi * hour / 24)
            hour_cos = np.cos(2 * np.pi * hour / 24)
            day_sin = np.sin(2 * np.pi * day_of_week / 7)
            day_cos = np.cos(2 * np.pi * day_of_week / 7)
            month_sin = np.sin(2 * np.pi * month / 12)
            month_cos = np.cos(2 * np.pi * month / 12)
            
            # Location features (normalized)
            lat_norm = (row['latitude'] + 90) / 180
            lon_norm = (row['longitude'] + 180) / 360
            
            # Weather and events
            weather_encoded = self.encode_weather(row.get('weather', 'CLEAR'))
            is_holiday = row.get('is_holiday', 0)
            is_event = row.get('is_event', 0)
            
            feature_vector = [
                hour_sin, hour_cos, day_sin, day_cos, month_sin, month_cos,
                lat_norm, lon_norm, weather_encoded, is_holiday, is_event
            ]
            
            features.append(feature_vector)
        
        return np.array(features)
    
    def encode_weather(self, weather: str) -> int:
        """Encode weather condition"""
        encoding = {"CLEAR": 0, "CLOUDY": 1, "RAIN": 2, "SNOW": 3}
        return encoding.get(weather.upper(), 0)
    
    def predict(self, request: DemandForecastRequest) -> Dict:
        """Predict demand for given location and time"""
        if not self.is_trained:
            # Return default prediction if model not trained
            return {
                "predicted_demand": 50,
                "confidence": 0.5,
                "model_status": "not_trained"
            }
        
        # Prepare features for prediction
        features = self.prepare_prediction_features(request)
        features_scaled = self.scaler.transform(features.reshape(1, -1))
        
        # Make prediction
        prediction = self.model.predict(features_scaled)[0]
        
        return {
            "predicted_demand": max(0, int(prediction)),
            "confidence": 0.8,
            "model_status": "trained"
        }
    
    def prepare_prediction_features(self, request: DemandForecastRequest) -> np.ndarray:
        """Prepare features for demand prediction"""
        # Parse date
        date_obj = datetime.strptime(request.date, "%Y-%m-%d")
        hour = 12  # Default to noon
        day_of_week = date_obj.weekday()
        month = date_obj.month
        
        # Cyclical encoding
        hour_sin = np.sin(2 * np.pi * hour / 24)
        hour_cos = np.cos(2 * np.pi * hour / 24)
        day_sin = np.sin(2 * np.pi * day_of_week / 7)
        day_cos = np.cos(2 * np.pi * day_of_week / 7)
        month_sin = np.sin(2 * np.pi * month / 12)
        month_cos = np.cos(2 * np.pi * month / 12)
        
        # Location features
        lat_norm = (request.location_lat + 90) / 180
        lon_norm = (request.location_lon + 180) / 360
        
        # Default values
        weather_encoded = 0  # CLEAR
        is_holiday = 0
        is_event = 0
        
        features = np.array([
            hour_sin, hour_cos, day_sin, day_cos, month_sin, month_cos,
            lat_norm, lon_norm, weather_encoded, is_holiday, is_event
        ])
        
        return features

class DriverMatchingModel:
    """AI-powered driver matching using multiple criteria"""
    
    def __init__(self):
        self.matching_weights = {
            'distance': 0.4,
            'rating': 0.25,
            'availability': 0.15,
            'vehicle_match': 0.1,
            'historical_performance': 0.1
        }
    
    def find_best_drivers(self, request: DriverMatchingRequest, available_drivers: List[Dict]) -> List[Dict]:
        """Find best drivers using AI scoring"""
        scored_drivers = []
        
        for driver in available_drivers:
            score = self.calculate_driver_score(request, driver)
            driver['ai_score'] = score
            scored_drivers.append(driver)
        
        # Sort by AI score (highest first)
        scored_drivers.sort(key=lambda x: x['ai_score'], reverse=True)
        
        return scored_drivers[:10]  # Return top 10 drivers
    
    def calculate_driver_score(self, request: DriverMatchingRequest, driver: Dict) -> float:
        """Calculate AI score for driver matching"""
        # Distance score (closer is better)
        distance = driver.get('distance_km', 10)
        distance_score = max(0, 10 - distance) / 10
        
        # Rating score (0-5 scale)
        rating = driver.get('rating', 3.0)
        rating_score = rating / 5
        
        # Availability score
        availability_score = 1.0 if driver.get('is_available', False) else 0.0
        
        # Vehicle match score
        vehicle_match = 1.0 if driver.get('vehicle_type') == request.vehicle_type else 0.5
        
        # Historical performance (placeholder)
        historical_score = driver.get('completion_rate', 0.8)
        
        # Calculate weighted score
        total_score = (
            distance_score * self.matching_weights['distance'] +
            rating_score * self.matching_weights['rating'] +
            availability_score * self.matching_weights['availability'] +
            vehicle_match * self.matching_weights['vehicle_match'] +
            historical_score * self.matching_weights['historical_performance']
        )
        
        return total_score

class SurgePricingModel:
    """ML-based dynamic surge pricing"""
    
    def __init__(self):
        self.model = GradientBoostingRegressor(n_estimators=100, random_state=42)
        self.scaler = StandardScaler()
        self.is_trained = False
    
    def train(self, historical_data: pd.DataFrame):
        """Train surge pricing model"""
        features = self.engineer_surge_features(historical_data)
        target = historical_data['surge_multiplier'].values
        
        features_scaled = self.scaler.fit_transform(features)
        self.model.fit(features_scaled, target)
        self.is_trained = True
        
        logger.info("Surge pricing model trained successfully")
    
    def engineer_surge_features(self, data: pd.DataFrame) -> np.ndarray:
        """Engineer features for surge pricing"""
        features = []
        
        for _, row in data.iterrows():
            # Demand-supply ratio
            demand_supply_ratio = row['current_demand'] / max(1, row['available_drivers'])
            
            # Time features
            hour = row['hour_of_day']
            is_peak_hour = 1 if hour in [7, 8, 9, 17, 18, 19] else 0
            is_weekend = 1 if row['day_type'] == 'weekend' else 0
            is_holiday = 1 if row['day_type'] == 'holiday' else 0
            
            # Weather impact
            weather_impact = self.get_weather_impact(row.get('weather', 'CLEAR'))
            
            # Historical patterns
            historical_surge = row.get('historical_avg_surge', 1.0)
            
            feature_vector = [
                demand_supply_ratio, is_peak_hour, is_weekend, is_holiday,
                weather_impact, historical_surge
            ]
            
            features.append(feature_vector)
        
        return np.array(features)
    
    def get_weather_impact(self, weather: str) -> float:
        """Get weather impact on surge pricing"""
        impacts = {
            "CLEAR": 1.0, "CLOUDY": 1.1, "RAIN": 1.3, "SNOW": 1.5
        }
        return impacts.get(weather.upper(), 1.0)
    
    def predict_surge(self, request: SurgePricingRequest) -> Dict:
        """Predict surge pricing multiplier"""
        if not self.is_trained:
            # Return default surge calculation
            return self.calculate_default_surge(request)
        
        # Prepare features
        features = self.prepare_surge_features(request)
        features_scaled = self.scaler.transform(features.reshape(1, -1))
        
        # Predict surge multiplier
        surge_multiplier = self.model.predict(features_scaled)[0]
        
        # Ensure reasonable bounds
        surge_multiplier = max(1.0, min(3.0, surge_multiplier))
        
        return {
            "surge_multiplier": round(surge_multiplier, 2),
            "confidence": 0.8,
            "factors": self.analyze_surge_factors(request)
        }
    
    def prepare_surge_features(self, request: SurgePricingRequest) -> np.ndarray:
        """Prepare features for surge prediction"""
        # Parse time
        time_parts = request.time_of_day.split(':')
        hour = int(time_parts[0]) if len(time_parts) > 0 else 12
        
        # Calculate features
        demand_supply_ratio = request.current_demand / max(1, request.available_drivers)
        is_peak_hour = 1 if hour in [7, 8, 9, 17, 18, 19] else 0
        is_weekend = 1 if request.day_type == 'weekend' else 0
        is_holiday = 1 if request.day_type == 'holiday' else 0
        weather_impact = 1.0  # Default
        historical_surge = 1.2  # Default
        
        features = np.array([
            demand_supply_ratio, is_peak_hour, is_weekend, is_holiday,
            weather_impact, historical_surge
        ])
        
        return features
    
    def calculate_default_surge(self, request: SurgePricingRequest) -> Dict:
        """Calculate default surge pricing when ML model not available"""
        base_multiplier = 1.0
        
        # Demand-supply adjustment
        if request.available_drivers > 0:
            demand_ratio = request.current_demand / request.available_drivers
            if demand_ratio > 2.0:
                base_multiplier *= 1.5
            elif demand_ratio > 1.5:
                base_multiplier *= 1.3
            elif demand_ratio > 1.2:
                base_multiplier *= 1.1
        
        # Time-based adjustments
        time_parts = request.time_of_day.split(':')
        hour = int(time_parts[0]) if len(time_parts) > 0 else 12
        
        if hour in [7, 8, 9, 17, 18, 19]:
            base_multiplier *= 1.2  # Peak hours
        
        if request.day_type == 'weekend':
            base_multiplier *= 1.1  # Weekend premium
        
        # Ensure bounds
        base_multiplier = max(1.0, min(2.5, base_multiplier))
        
        return {
            "surge_multiplier": round(base_multiplier, 2),
            "confidence": 0.6,
            "factors": ["demand_supply", "time_of_day", "day_type"],
            "model": "rule_based"
        }
    
    def analyze_surge_factors(self, request: SurgePricingRequest) -> List[str]:
        """Analyze factors contributing to surge pricing"""
        factors = []
        
        if request.current_demand > request.available_drivers * 1.5:
            factors.append("high_demand")
        
        time_parts = request.time_of_day.split(':')
        hour = int(time_parts[0]) if len(time_parts) > 0 else 12
        
        if hour in [7, 8, 9, 17, 18, 19]:
            factors.append("peak_hours")
        
        if request.day_type in ['weekend', 'holiday']:
            factors.append("special_day")
        
        return factors

# Initialize models
eta_model = ETAPredictionModel()
demand_model = DemandForecastingModel()
driver_matching_model = DriverMatchingModel()
surge_pricing_model = SurgePricingModel()

# API Endpoints
@app.post("/predict/eta")
async def predict_eta(request: ETAPredictionRequest):
    """Predict ETA for a ride"""
    try:
        prediction = eta_model.predict(request)
        return prediction
    except Exception as e:
        logger.error(f"Error predicting ETA: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/forecast/demand")
async def forecast_demand(request: DemandForecastRequest):
    """Forecast demand for a location and time"""
    try:
        forecast = demand_model.predict(request)
        return forecast
    except Exception as e:
        logger.error(f"Error forecasting demand: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/match/drivers")
async def match_drivers(request: DriverMatchingRequest):
    """Find best driver matches using AI"""
    try:
        # Mock available drivers (in production, fetch from database)
        available_drivers = [
            {
                "driver_id": 1,
                "distance_km": 2.5,
                "rating": 4.8,
                "is_available": True,
                "vehicle_type": "STANDARD",
                "completion_rate": 0.95
            },
            {
                "driver_id": 2,
                "distance_km": 1.8,
                "rating": 4.6,
                "is_available": True,
                "vehicle_type": "COMFORT",
                "completion_rate": 0.92
            }
        ]
        
        matches = driver_matching_model.find_best_drivers(request, available_drivers)
        return {"matches": matches, "total_found": len(matches)}
    except Exception as e:
        logger.error(f"Error matching drivers: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/predict/surge")
async def predict_surge(request: SurgePricingRequest):
    """Predict surge pricing multiplier"""
    try:
        surge_prediction = surge_pricing_model.predict_surge(request)
        return surge_prediction
    except Exception as e:
        logger.error(f"Error predicting surge pricing: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/train/eta-model")
async def train_eta_model(background_tasks: BackgroundTasks):
    """Train ETA prediction model with new data"""
    background_tasks.add_task(train_eta_model_background)
    return {"message": "ETA model training started", "status": "training"}

@app.post("/train/demand-model")
async def train_demand_model(background_tasks: BackgroundTasks):
    """Train demand forecasting model with new data"""
    background_tasks.add_task(train_demand_model_background)
    return {"message": "Demand model training started", "status": "training"}

@app.post("/train/surge-model")
async def train_surge_model(background_tasks: BackgroundTasks):
    """Train surge pricing model with new data"""
    background_tasks.add_task(train_surge_model_background)
    return {"message": "Surge pricing model training started", "status": "training"}

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "uber-ml-service",
        "models": {
            "eta_prediction": "ready",
            "demand_forecasting": "ready",
            "driver_matching": "ready",
            "surge_pricing": "ready"
        },
        "ml_frameworks": {
            "tensorflow": TENSORFLOW_AVAILABLE,
            "pytorch": PYTORCH_AVAILABLE,
            "scikit_learn": True,
            "xgboost": True,
            "lightgbm": True
        }
    }

# Background training tasks
async def train_eta_model_background():
    """Background task to train ETA model"""
    logger.info("Starting ETA model training...")
    
    # Simulate training with mock data
    await asyncio.sleep(10)
    
    # In production, this would:
    # 1. Fetch new training data from database
    # 2. Preprocess and engineer features
    # 3. Retrain the model
    # 4. Evaluate performance
    # 5. Update model weights
    
    logger.info("ETA model training completed")

async def train_demand_model_background():
    """Background task to train demand forecasting model"""
    logger.info("Starting demand model training...")
    
    # Mock training data
    mock_data = pd.DataFrame({
        'hour': np.random.randint(0, 24, 1000),
        'day_of_week': np.random.randint(0, 7, 1000),
        'month': np.random.randint(1, 13, 1000),
        'latitude': np.random.uniform(-90, 90, 1000),
        'longitude': np.random.uniform(-180, 180, 1000),
        'weather': np.random.choice(['CLEAR', 'CLOUDY', 'RAIN'], 1000),
        'is_holiday': np.random.choice([0, 1], 1000),
        'is_event': np.random.choice([0, 1], 1000),
        'demand_count': np.random.poisson(50, 1000)
    })
    
    demand_model.train(mock_data)
    logger.info("Demand model training completed")

async def train_surge_model_background():
    """Background task to train surge pricing model"""
    logger.info("Starting surge pricing model training...")
    
    # Mock training data
    mock_data = pd.DataFrame({
        'current_demand': np.random.randint(10, 200, 1000),
        'available_drivers': np.random.randint(5, 100, 1000),
        'hour_of_day': np.random.randint(0, 24, 1000),
        'day_type': np.random.choice(['weekday', 'weekend', 'holiday'], 1000),
        'weather': np.random.choice(['CLEAR', 'CLOUDY', 'RAIN', 'SNOW'], 1000),
        'historical_avg_surge': np.random.uniform(1.0, 2.0, 1000),
        'surge_multiplier': np.random.uniform(1.0, 3.0, 1000)
    })
    
    surge_pricing_model.train(mock_data)
    logger.info("Surge pricing model training completed")

# Startup event
@app.on_event("startup")
async def startup_event():
    """Initialize services on startup"""
    global redis_client, kafka_producer
    
    # Initialize Redis
    try:
        redis_client = redis.Redis(host='localhost', port=6379, db=0)
        redis_client.ping()
        logger.info("Redis connected successfully")
    except Exception as e:
        logger.warning(f"Redis connection failed: {e}")
        redis_client = None
    
    # Initialize Kafka
    try:
        kafka_producer = KafkaProducer(
            bootstrap_servers=['localhost:9092'],
            value_serializer=lambda x: json.dumps(x).encode('utf-8')
        )
        logger.info("Kafka producer connected successfully")
    except Exception as e:
        logger.warning(f"Kafka connection failed: {e}")
        kafka_producer = None
    
    # Train models with initial data
    logger.info("Initializing ML models...")
    await train_demand_model_background()
    await train_surge_model_background()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000) 