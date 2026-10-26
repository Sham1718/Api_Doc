from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_returns_up():
    response = client.get("/internal/v1/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}
