# NoCapAI — TY BSc Data Science Project Report (Technical Notes)

## 1. Title
**NoCapAI: ML-Based Library Management and Overdue Risk Prediction System**

## 2. Problem Statement
Library staff need an early-warning indicator for loans that may become overdue. NoCapAI combines a library management application with a supervised machine-learning classifier that estimates overdue probability before the loan is completed.

## 3. Objectives
1. Manage books, members and circulation digitally.
2. Build a reproducible Data Science pipeline.
3. Compare multiple classification algorithms.
4. Train a final Random Forest model.
5. Expose the trained model through a REST API.
6. Present predictions and model analytics in a professional web interface.

## 4. Dataset
The repository contains `data/training_data.csv` with 1,600 synthetic records. The target is `overdue` (0 = on time, 1 = overdue). Synthetic data is included only for reproducibility and demonstration. It must not be described as historical real-library data.

### Features
- `member_tenure_months`
- `previous_loans`
- `previous_late_returns`
- `current_loans`
- `book_popularity`
- `loan_duration_days`
- `member_type`
- `genre`

## 5. Methodology
**Data → validation → train/test split → preprocessing → model comparison → evaluation → model serialization → API inference → web visualisation.**

Numeric variables are standardised with `StandardScaler`. Categorical variables are one-hot encoded using `OneHotEncoder(handle_unknown="ignore")`. Preprocessing is kept inside the scikit-learn pipeline to avoid leakage between training and testing data.

## 6. Algorithms Compared
- Logistic Regression
- Decision Tree
- Random Forest
- Gradient Boosting

The final application uses **Random Forest (300 estimators)** because it provides non-linear modelling capability, a useful accuracy/AUC balance on this demonstration dataset, and interpretable feature-importance outputs.

## 7. Evaluation
A stratified 80/20 train-test split with `random_state=42` is used. Metrics include accuracy, precision, recall, F1-score and ROC-AUC. Five-fold stratified cross-validation is also reported.

For the included synthetic dataset, the held-out test results for the final Random Forest are approximately:
- Accuracy: **66.3%**
- Precision (overdue): **42.9%**
- Recall (overdue): **27.3%**
- F1 (overdue): **33.3%**
- ROC-AUC: **0.612**

These numbers are academic demonstration metrics, not production performance claims.

## 8. System Architecture
```text
Browser Frontend
      ↓ HTTP/JSON
FastAPI Backend
      ↓
Saved scikit-learn Pipeline (.joblib)
      ↓
Prediction / Analytics
```

The original Java implementation and custom data structures are retained under `legacy_java_copy/` for reference and viva discussion.

## 9. Limitations & Future Work
- Replace synthetic records with anonymised real transactions.
- Perform stronger hyperparameter tuning and repeated cross-validation.
- Investigate threshold optimisation because overdue detection is a risk problem where recall can matter more than raw accuracy.
- Add PostgreSQL for persistent production data.
- Add secure authentication and password hashing.
- Add model monitoring, drift detection and fairness checks.

## 10. Ethical Use
Predictions should be used as decision support only. The model should not be used to penalise students automatically. Human review and transparent library policy should remain the final authority.
