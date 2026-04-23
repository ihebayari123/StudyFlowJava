import joblib, pandas as pd, numpy as np
model    = joblib.load("best_sleep_model.pkl")
scaler   = joblib.load("scaler.pkl")
features = joblib.load("features_list.pkl")
def predict(d):
    h = float(np.clip(model.predict(scaler.transform(pd.DataFrame([d])[features])),0,10)[0])
    h = round(h,1); missing = round(max(0.,8.-h),1)
    status = "OK" if h>=7 else ("insuffisant" if h>=5 else "CRITIQUE")
    return {"hours_slept":h,"missing_hours":missing,"status":status}
