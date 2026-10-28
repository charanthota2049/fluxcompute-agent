from pathlib import Path
import joblib
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score

def main():
    # /workspace is read-only; generated files belong in the /output mount.
    output_dir = Path("/output")
    output_dir.mkdir(parents=True, exist_ok=True)

    iris = load_iris()
    X_train, X_test, y_train, y_test = train_test_split(
        iris.data, iris.target, test_size=0.2, random_state=42, stratify=iris.target
    )
    model = RandomForestClassifier(n_estimators=100, random_state=42)
    model.fit(X_train, y_train)

    accuracy = accuracy_score(y_test, model.predict(X_test))
    model_path = output_dir / "iris_model.joblib"
    joblib.dump(model, model_path)
    (output_dir / "metrics.txt").write_text(
        f"dataset=Iris\n test_accuracy={accuracy:.4f}\n test_samples={len(y_test)}\n",
        encoding="utf-8",
    )
    print("Training completed successfully.")
    print(f"Test accuracy: {accuracy:.4f}")
    print(f"Model saved to: {model_path}")

if __name__ == "__main__":
    main()
