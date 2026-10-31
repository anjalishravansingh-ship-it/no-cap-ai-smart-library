from pathlib import Path
from typing import Optional
import json
import os
from datetime import datetime, timedelta

import pandas as pd
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field
from joblib import load

ROOT = Path(__file__).resolve().parents[1]
MODEL_PATH = ROOT / "models" / "overdue_risk_model.joblib"
COMPARISON_PATH = ROOT / "reports" / "model_comparison.csv"
METRICS_PATH = ROOT / "models" / "metrics.json"
DATA_PATH = ROOT / "data" / "training_data.csv"
FRONTEND = ROOT / "frontend"

app = FastAPI(title="NoCapAI Data Science API", version="2.0.0", description="ML-powered Smart Library Management System")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=False, allow_methods=["*"], allow_headers=["*"])

try:
    MODEL = load(MODEL_PATH)
except Exception:
    MODEL = None

books = [
    {"id":"B001","title":"Hands-On Machine Learning","author":"Aurélien Géron","genre":"Technology","copies":4,"available":3},
    {"id":"B002","title":"Python for Data Analysis","author":"Wes McKinney","genre":"Technology","copies":5,"available":5},
    {"id":"B003","title":"The Data Science Handbook","author":"Field Cady","genre":"Science","copies":3,"available":1},
    {"id":"B004","title":"Atomic Habits","author":"James Clear","genre":"Business","copies":3,"available":2},
    {"id":"B005","title":"Clean Code","author":"Robert C. Martin","genre":"Technology","copies":2,"available":0},
]
members = [
    {"id":"M001","name":"Aarav Sharma","type":"Student","email":"aarav@example.com","tenure":18,"previous_loans":12,"late_returns":2,"current_loans":1},
    {"id":"M002","name":"Meera Patel","type":"Student","email":"meera@example.com","tenure":9,"previous_loans":8,"late_returns":4,"current_loans":2},
    {"id":"M003","name":"Dr. Rohan Shah","type":"Faculty","email":"rohan@example.com","tenure":42,"previous_loans":31,"late_returns":1,"current_loans":1},
]
actions = [
    {"type":"ISSUE","book":"Python for Data Analysis","member":"Aarav Sharma","date":"2026-09-08","status":"Active"},
    {"type":"RETURN","book":"Atomic Habits","member":"Meera Patel","date":"2026-09-07","status":"Completed"},
    {"type":"QUEUE","book":"Clean Code","member":"Meera Patel","date":"2026-09-06","status":"Waiting"},
]

class PredictionInput(BaseModel):
    member_tenure_months: int = Field(ge=0, le=240)
    previous_loans: int = Field(ge=0, le=1000)
    previous_late_returns: int = Field(ge=0, le=1000)
    current_loans: int = Field(ge=0, le=20)
    book_popularity: int = Field(ge=1, le=100)
    loan_duration_days: int = Field(1, le=365)
    member_type: str
    genre: str

class Login(BaseModel):
    username: str
    password: str
    role: str

class IssueRequest(BaseModel):
    book_id: str
    member_id: str

@app.get("/api/health")
def health():
    return {"ok": True, "service": "NoCapAI Data Science Platform", "model_loaded": MODEL is not None}

@app.post("/api/login")
def login(body: Login):
    if body.role == "Librarian" and body.username == "admin" and body.password == "admin123":
        return {"ok": True, "name": "Administrator", "role": "Librarian"}
    if body.role == "Student":
        m = next((x for x in members if x["id"] == body.username), None)
        if m and body.password == "student123":
            return {"ok": True, "name": m["name"], "role": "Student", "memberId": m["id"]}
    raise HTTPException(401, "Invalid credentials")

@app.get("/api/dashboard")
def dashboard():
    return {
        "stats": {"books": len(books), "members": len(members), "available": sum(b["available"] for b in books), "issued": sum(b["copies"]-b["available"] for b in books)},
        "genres": {g: sum(1 for b in books if b["genre"] == g) for g in sorted(set(b["genre"] for b in books))},
        "activity": actions[:6],
        "model": model_info(),
    }

@app.get("/api/books")
def get_books(search: Optional[str] = None):
    data = books
    if search:
        q = search.lower()
        data = [b for b in books if q in (b["title"]+" "+b["author"]+" "+b["genre"]).lower()]
    return data

@app.get("/api/members")
def get_members(): return members

@app.get("/api/actions")
def get_actions(): return actions

@app.get("/api/model")
def model_info():
    metrics = json.loads(METRICS_PATH.read_text()) if METRICS_PATH.exists() else {}
    return {**metrics, "trained_on": int(len(pd.read_csv(DATA_PATH))) if DATA_PATH.exists() else 0, "status": "ready" if MODEL else "missing"}

@app.get("/api/model/comparison")
def model_comparison():
    path = ROOT / "reports" / "model_comparison.csv"
    if not path.exists():
        return []
    return json.loads(pd.read_csv(path).round(4).to_json(orient="records"))


@app.post("/api/predict")
def predict(body: PredictionInput):
    if MODEL is None: raise HTTPException(503, "Model is not loaded. Run the training script first.")
    x = pd.DataFrame([body.model_dump()])
    probability = float(MODEL.predict_proba(x)[0, 1])
    risk = "High" if probability >= .65 else "Medium" if probability >= .35 else "Low"
    return {"risk": risk, "probability": round(probability, 4), "probability_percent": round(probability*100, 1), "recommendation": {"High":"Send a reminder before the due date and consider a shorter loan period.","Medium":"Send a reminder 2–3 days before the due date.","Low":"Standard reminder policy is sufficient."}[risk]}

@app.post("/api/issue")
def issue_book(body: IssueRequest):
    book = next((b for b in books if b["id"] == body.book_id), None)
    member = next((m for m in members if m["id"] == body.member_id), None)
    if book is None:
        raise HTTPException(404, "Book not found")
    if member is None:
        raise HTTPException(404, "Member not found")
    if book["available"] <= 0:
        raise HTTPException(409, f"{book['title']} has no available copies right now")

    book["available"] -= 1
    member["current_loans"] += 1
    actions.insert(0, {
        "type": "ISSUE",
        "book": book["title"],
        "member": member["name"],
        "date": datetime.now().strftime("%Y-%m-%d"),
        "status": "Active",
    })
    return {"ok": True, "message": f"{book['title']} issued to {member['name']}", "book": book, "member": member}

@app.post("/api/train")
def train():
    # Training is intentionally kept as a separate reproducible CLI operation.
    raise HTTPException(501, "Training is run with: python ml/generate_dataset.py && python ml/train_model.py. This endpoint is disabled in production.")

app.mount("/", StaticFiles(directory=FRONTEND, html=True), name="frontend")
