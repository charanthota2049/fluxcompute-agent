# FluxCompute Iris ML Workload

Trains a Random Forest classifier on scikit-learn's built-in Iris dataset.

## Required mounts
- Extracted workload directory -> `/workspace` (read-only).
- Job output directory -> `/output` (read-write).

The script saves artifacts to `/output`, never `/workspace/output`.
The container image must include `scikit-learn` and `joblib`.

Run with: `python /workspace/train.py`

Expected artifacts:
- `/output/iris_model.joblib`
- `/output/metrics.txt`

The ZIP root directly contains `train.py`, `requirements.txt`, and `README.md`.
