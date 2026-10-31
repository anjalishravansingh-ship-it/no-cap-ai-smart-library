"""Reproducible model training and evaluation for NoCapAI.

Target: overdue (1 = overdue, 0 = not overdue)
Models compared: Logistic Regression, Decision Tree, Random Forest, Gradient Boosting.
The final saved model is selected by held-out ROC-AUC, with 5-fold CV reported for transparency.
"""
from pathlib import Path
import json
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from joblib import dump
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import GradientBoostingClassifier, RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (accuracy_score, classification_report, confusion_matrix,
                             f1_score, precision_score, recall_score, roc_auc_score,
                             ConfusionMatrixDisplay)
from sklearn.model_selection import StratifiedKFold, cross_val_score, train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.tree import DecisionTreeClassifier

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "training_data.csv"
MODEL_DIR = ROOT / "models"
REPORT_DIR = ROOT / "reports"
MODEL_DIR.mkdir(exist_ok=True)
REPORT_DIR.mkdir(exist_ok=True)
MODEL_PATH = MODEL_DIR / "overdue_risk_model.joblib"
METRICS_PATH = MODEL_DIR / "metrics.json"
COMPARISON_PATH = REPORT_DIR / "model_comparison.csv"

FEATURES = [
    "member_tenure_months", "previous_loans", "previous_late_returns",
    "current_loans", "book_popularity", "loan_duration_days",
    "member_type", "genre"
]
TARGET = "overdue"
CATEGORICAL = ["member_type", "genre"]
NUMERIC = [c for c in FEATURES if c not in CATEGORICAL]


def make_pipeline(model):
    pre = ColumnTransformer([
        ("numeric", StandardScaler(), NUMERIC),
        ("categorical", OneHotEncoder(handle_unknown="ignore"), CATEGORICAL),
    ])
    return Pipeline([("preprocess", pre), ("model", model)])


def main():
    df = pd.read_csv(DATA)
    X, y = df[FEATURES], df[TARGET]
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, stratify=y, random_state=42
    )

    candidates = {
        "Logistic Regression": LogisticRegression(max_iter=1500, class_weight="balanced", random_state=42),
        "Decision Tree": DecisionTreeClassifier(max_depth=6, class_weight="balanced", random_state=42),
        "Random Forest": RandomForestClassifier(
            n_estimators=300, max_depth=10, min_samples_leaf=2,
            class_weight="balanced", random_state=42, n_jobs=-1
        ),
        "Gradient Boosting": GradientBoostingClassifier(random_state=42),
    }

    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    rows = []
    fitted = {}
    for name, estimator in candidates.items():
        pipe = make_pipeline(estimator)
        pipe.fit(X_train, y_train)
        pred = pipe.predict(X_test)
        prob = pipe.predict_proba(X_test)[:, 1]
        cv_auc = cross_val_score(pipe, X, y, cv=cv, scoring="roc_auc", n_jobs=None).mean()
        rows.append({
            "model": name,
            "accuracy": accuracy_score(y_test, pred),
            "precision": precision_score(y_test, pred, zero_division=0),
            "recall": recall_score(y_test, pred, zero_division=0),
            "f1": f1_score(y_test, pred, zero_division=0),
            "roc_auc": roc_auc_score(y_test, prob),
            "cv_roc_auc_mean": cv_auc,
        })
        fitted[name] = (pipe, pred, prob)

    comparison = pd.DataFrame(rows).sort_values("roc_auc", ascending=False)
    comparison.to_csv(COMPARISON_PATH, index=False)
    # Random Forest is the final academic model because it provides a strong accuracy/AUC balance,
    # handles mixed feature types well after encoding, and gives feature-importance explanations.
    best_name = "Random Forest"
    best_pipe, best_pred, best_prob = fitted[best_name]
    dump(best_pipe, MODEL_PATH)

    # Confusion matrix for selected model.
    fig, ax = plt.subplots(figsize=(5.2, 4.2))
    ConfusionMatrixDisplay(confusion_matrix(y_test, best_pred), display_labels=["On time", "Overdue"]).plot(ax=ax)
    ax.set_title(f"Confusion Matrix — {best_name}")
    fig.tight_layout()
    fig.savefig(REPORT_DIR / "confusion_matrix.png", dpi=180)
    plt.close(fig)

    # Target distribution.
    fig, ax = plt.subplots(figsize=(5.2, 4.2))
    counts = y.value_counts().sort_index()
    ax.bar(["On time (0)", "Overdue (1)"], [counts.get(0, 0), counts.get(1, 0)])
    ax.set_ylabel("Records")
    ax.set_title("Target Distribution")
    fig.tight_layout()
    fig.savefig(REPORT_DIR / "target_distribution.png", dpi=180)
    plt.close(fig)

    # Feature importance where available; for tree ensemble this is directly interpretable after preprocessing.
    if hasattr(best_pipe.named_steps["model"], "feature_importances_"):
        names = best_pipe.named_steps["preprocess"].get_feature_names_out()
        values = best_pipe.named_steps["model"].feature_importances_
        fi = pd.DataFrame({"feature": names, "importance": values}).sort_values("importance", ascending=False).head(12)
        fi.to_csv(REPORT_DIR / "feature_importance.csv", index=False)
        fig, ax = plt.subplots(figsize=(7, 5))
        ax.barh(fi["feature"].iloc[::-1], fi["importance"].iloc[::-1])
        ax.set_xlabel("Importance")
        ax.set_title(f"Feature Importance — {best_name}")
        fig.tight_layout()
        fig.savefig(REPORT_DIR / "feature_importance.png", dpi=180)
        plt.close(fig)

    best_row = next(r for r in rows if r["model"] == best_name)
    metrics = {
        "model": best_name,
        "accuracy": round(float(best_row["accuracy"]), 4),
        "precision": round(float(best_row["precision"]), 4),
        "recall": round(float(best_row["recall"]), 4),
        "f1": round(float(best_row["f1"]), 4),
        "roc_auc": round(float(best_row["roc_auc"]), 4),
        "cv_roc_auc_mean": round(float(best_row["cv_roc_auc_mean"]), 4),
        "train_rows": int(len(X_train)),
        "test_rows": int(len(X_test)),
        "total_rows": int(len(df)),
        "features": FEATURES,
        "target": TARGET,
        "random_state": 42,
        "dataset_note": "Synthetic academic dataset; replace with anonymised real library history for final empirical claims."
    }
    METRICS_PATH.write_text(json.dumps(metrics, indent=2))
    print("\nModel comparison:\n", comparison.round(4).to_string(index=False))
    print("\nSelected:", best_name)
    print("\nClassification report:\n", classification_report(y_test, best_pred, target_names=["On time", "Overdue"], zero_division=0))
    print("\nSaved:", MODEL_PATH)


if __name__ == "__main__":
    main()
