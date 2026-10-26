from fastapi import FastAPI

app = FastAPI(title="API Lens AI Service")


@app.get("/internal/v1/health")
def health() -> dict:
    return {"status": "UP"}
