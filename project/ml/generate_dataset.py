"""Generate a reproducible synthetic library dataset for model training."""
from pathlib import Path
import numpy as np
import pandas as pd

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "data" / "training_data.csv"
rng = np.random.default_rng(42)
n = 1600
member_type = rng.choice(["Student", "Faculty", "Researcher"], n, p=[.72, .18, .10])
genre = rng.choice(["Technology", "Science", "Business", "Literature", "History", "Arts"], n)
tenure = rng.integers(1, 72, n)
previous_loans = rng.poisson(7, n)
previous_late = np.minimum(rng.poisson(1.4, n), previous_loans)
current_loans = rng.integers(0, 4, n)
popularity = rng.integers(1, 101, n)
duration = rng.integers(7, 31, n)
logit = (
    -2.4 + .34 * previous_late + .22 * current_loans + .035 * (duration - 14)
    + .008 * popularity + .12 * (member_type == "Student")
    - .004 * tenure + rng.normal(0, .75, n)
)
p = 1 / (1 + np.exp(-logit))
overdue = rng.binomial(1, p)
df = pd.DataFrame({
    "member_tenure_months": tenure,
    "previous_loans": previous_loans,
    "previous_late_returns": previous_late,
    "current_loans": current_loans,
    "book_popularity": popularity,
    "loan_duration_days": duration,
    "member_type": member_type,
    "genre": genre,
    "overdue": overdue,
})
OUT.parent.mkdir(exist_ok=True)
df.to_csv(OUT, index=False)
print(f"Wrote {len(df)} rows to {OUT}")
print(df["overdue"].value_counts(normalize=True).rename("share"))
