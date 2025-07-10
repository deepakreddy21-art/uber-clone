from fastapi.testclient import TestClient
from main import app

def test_predict_eta():
    client = TestClient(app)
    response = client.post('/predict-eta', json={
        'pickup_lat': 12.9,
        'pickup_lng': 77.6,
        'dropoff_lat': 12.95,
        'dropoff_lng': 77.65,
        'hour_of_day': 10
    })
    assert response.status_code == 200
    data = response.json()
    assert 'eta_minutes' in data
    assert isinstance(data['eta_minutes'], float) 