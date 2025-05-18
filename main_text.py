import cv2
import numpy as np
import time

# === Load Pretrained MobileNet-SSD ===
net = cv2.dnn.readNetFromCaffe("MobileNetSSD_deploy.prototxt", "MobileNetSSD_deploy.caffemodel")
CLASSES = [
    "background", "aeroplane", "bicycle", "bird", "boat", "bottle", "bus", "car", "cat", "chair",
    "cow", "diningtable", "dog", "horse", "motorbike", "person", "pottedplant", "sheep",
    "sofa", "train", "tvmonitor"
]

# === Lane Detection ===
def detect_lanes(frame):
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    edges = cv2.Canny(gray, 50, 150)
    lines = cv2.HoughLinesP(edges, 1, np.pi/180, 50, minLineLength=100, maxLineGap=50)
    left, right = 0, 0
    if lines is not None:
        for x1, _, x2, _ in lines[:, 0]:
            if x1 < frame.shape[1] // 2 and x2 < frame.shape[1] // 2:
                left += 1
            elif x1 > frame.shape[1] // 2 and x2 > frame.shape[1] // 2:
                right += 1
    return left > 0, right > 0

# === Object Detection ===
def detect_objects(frame):
    h, w = frame.shape[:2]
    blob = cv2.dnn.blobFromImage(cv2.resize(frame, (300, 300)), 0.007843, (300, 300), 127.5)
    net.setInput(blob)
    detections = net.forward()
    detected = []
    for i in range(detections.shape[2]):
        confidence = detections[0, 0, i, 2]
        if confidence > 0.5:
            idx = int(detections[0, 0, i, 1])
            label = CLASSES[idx]
            if label not in detected:
                detected.append(label)
    return detected

# === Speed Estimation ===
def estimate_speed(prev, curr):
    if prev is None:
        return 0
    prev_gray = cv2.cvtColor(prev, cv2.COLOR_BGR2GRAY)
    curr_gray = cv2.cvtColor(curr, cv2.COLOR_BGR2GRAY)
    flow = cv2.calcOpticalFlowFarneback(prev_gray, curr_gray, None,
                                        0.5, 3, 15, 3, 5, 1.2, 0)
    mag, _ = cv2.cartToPolar(flow[..., 0], flow[..., 1])
    return np.mean(mag) * 10  # Scaling for interpretability

# === Safe Speed Recommendation ===
def recommend_safe_speed(current_speed, detected_objs):
    base = 60
    if "person" in detected_objs or "car" in detected_objs:
        return max(20, base - current_speed * 2)
    return max(30, base - current_speed)

# === Main Loop ===
cap = cv2.VideoCapture(0)
prev_frame = None
prev_left, prev_right = True, True
frame_id = 0

try:
    while True:
        ret, frame = cap.read()
        if not ret:
            break

        frame = cv2.resize(frame, (320, 240))  # Reduce resolution to lower load

        # Lane detection
        left_lane, right_lane = detect_lanes(frame)
        lane_changed = (prev_left and prev_right) and (not left_lane or not right_lane)
        prev_left, prev_right = left_lane, right_lane

        # Object detection
        detected_objects = detect_objects(frame)

        # Speed estimation
        speed = estimate_speed(prev_frame, frame)
        prev_frame = frame.copy()

        # Safe speed logic
        safe_speed = recommend_safe_speed(speed, detected_objects)

        # === Console Output ===
        print(f"\nFrame {frame_id}")
        print(f"  Detected Objects       : {detected_objects}")
        print(f"  Lane Change Detected   : {'Yes' if lane_changed else 'No'}")
        print(f"  Estimated Movement     : {speed:.2f}")
        print(f"  Recommended Safe Speed : {safe_speed:.0f} km/h")

        frame_id += 1
        time.sleep(0.2)  # slight delay to reduce CPU load

except KeyboardInterrupt:
    print("\n🛑 Stopped by user.")

cap.release()
