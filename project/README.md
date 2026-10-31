# NoCapAI — Intelligent Library & Overdue Risk Prediction

**TY BSc Data Science Project**

NoCapAI is an end-to-end Data Science application that combines library operations with supervised machine learning. Its main ML task is to estimate whether a library loan is likely to become overdue.

## Tech Stack
- Python 3.12
- FastAPI REST backend
- scikit-learn ML pipeline
- Random Forest final model
- Pandas / NumPy
- HTML/CSS/JavaScript + Chart.js frontend
- Docker + Render deployment
- Original Java data-structure implementation retained under `legacy_java_copy/`

## Project Structure
```text
NoCapAI/
├── backend/                 # FastAPI API
├── frontend/                # Professional web UI
├── data/                    # Training dataset
├── ml/                      # Dataset generation + model training
├── models/                  # Trained .joblib model + metrics
├── reports/                 # Model comparison + plots
├── notebooks/               # Academic training notebook
├── docs/                    # Report and model card
└── legacy_java_copy/        # Original Java implementation
```

## ML Workflow
1. Load dataset
2. Validate data
3. Stratified 80/20 train-test split
4. Standardise numerical features
5. One-hot encode categorical features
6. Compare Logistic Regression, Decision Tree, Random Forest and Gradient Boosting
7. Evaluate accuracy, precision, recall, F1 and ROC-AUC
8. Run 5-fold stratified cross-validation
9. Save final Random Forest pipeline using joblib
10. Serve predictions through FastAPI

## Re-train
```bash
python ml/generate_dataset.py
python ml/train_model.py
```

The generated dataset is synthetic and intended for academic reproducibility. Do not claim its metrics represent real library performance.

## Run locally
```bash
pip install -r backend/requirements.txt
uvicorn backend.main:app --reload
```
Open `http://127.0.0.1:8000`.

## Demo login
- Librarian: `admin` / `admin123`
- Student: `M001` / `student123`

Change demo credentials before any public deployment.

## Academic Submission
The key files to demonstrate during viva are:
- `notebooks/NoCapAI_Model_Training.ipynb`
- `ml/train_model.py`
- `data/training_data.csv`
- `models/overdue_risk_model.joblib`
- `reports/model_comparison.csv`
- `reports/confusion_matrix.png`
- `reports/feature_importance.png`
- `docs/DATA_SCIENCE_REPORT.md`
- `docs/MODEL_CARD.md`

## Limitation
The current dataset is synthetic. For a research-grade submission, replace it with anonymised historical library transactions and repeat the analysis.
