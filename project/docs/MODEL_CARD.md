# NoCapAI Overdue Risk Model Card

**Model:** Random Forest Classifier  
**Version:** 1.0 academic prototype  
**Task:** Binary classification of overdue vs on-time library loans  
**Framework:** scikit-learn  
**Training records:** 1,280  
**Test records:** 320  

## Intended use
Academic demonstration of an end-to-end Data Science project and library decision-support prototype.

## Not intended for
Automatic disciplinary action, credit-like scoring, or high-stakes decisions about students.

## Inputs
Borrower history, current borrowing load, book popularity, loan duration, member type and genre.

## Output
Probability of overdue risk plus Low/Medium/High presentation bands.

## Limitations
The bundled data is synthetic. Performance is not evidence of generalisation to a real library population. Class imbalance and threshold selection should be investigated with real anonymised data.
