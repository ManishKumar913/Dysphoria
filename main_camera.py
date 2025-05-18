import cv2
import numpy as np

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
    blur = cv2.GaussianBlur(gray, (5, 5), 0)
    edges = cv2.Canny(blur, 50, 150)
    lines = cv2.HoughLinesP(edges, 1, np.pi/180, 50, minLineLength=100, maxLineGap=50)
    left, right = [], []
    if lines is not None:
        for x1, y1, x2, y2 in lines[:, 0]:
            cv2.line(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
            if x1 < frame.shape[1] // 2 and x2 < frame.shape[1] // 2:
                left.append((x1, y1, x2, y2))
            elif x1 > frame.shape[1] // 2 and x2 > frame.shape[1] // 2:
                right.append((x1, y1, x2, y2))
    return frame, len(left) > 0, len(right) > 0  # Return frame and left/right lane status

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
            box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
            (x1, y1, x2, y2) = box.astype("int")
            cv2.rectangle(frame, (x1, y1), (x2, y2), (255, 0, 0), 2)
            cv2.putText(frame, label, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 0, 0), 2)
            detected.append(label)
    return frame, detected

# === Speed Estimation ===
def estimate_speed(prev_frame, curr_frame):
    if prev_frame is None:
        return 0
    prev_gray = cv2.cvtColor(prev_frame, cv2.COLOR_BGR2GRAY)
    curr_gray = cv2.cvtColor(curr_frame, cv2.COLOR_BGR2GRAY)
    flow = cv2.calcOpticalFlowFarneback(prev_gray, curr_gray, None,
                                        0.5, 3, 15, 3, 5, 1.2, 0)
    mag, _ = cv2.cartToPolar(flow[..., 0], flow[..., 1])
    return np.mean(mag) * 10  # scale factor to simulate speed units

# === Safe Speed Recommendation ===
def get_safe_speed(speed, objects_present):
    base = 60  # base safe speed
    if 'person' in objects_present or 'car' in objects_present:
        return max(20, base - speed * 2)  # slow down near people/cars
    return max(30, base - speed)  # general slow-down rule

# === Main ===
cap = cv2.VideoCapture(0)
prev_frame = None
prev_left, prev_right = True, True  # assume car is in lane initially

while True:
    ret, frame = cap.read()
    if not ret:
        break

    # Resize and enhance
    frame = cv2.resize(frame, (320, 240))
    fr = cv2.convertScaleAbs(frame, alpha=1.5, beta=70)

    # Lane Detection
    lane_frame, left_lane, right_lane = detect_lanes(fr.copy())

    # Lane departure warning
    if not left_lane or not right_lane:
        if prev_left and prev_right:
            cv2.putText(lane_frame, "⚠️ Lane change detected!", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)
    prev_left, prev_right = left_lane, right_lane

    # Object Detection
    detected_frame, objects = detect_objects(lane_frame)

    # Speed Estimation
    speed = estimate_speed(prev_frame, fr)
    prev_frame = fr.copy()

    # Safe Speed Suggestion
    safe_speed = get_safe_speed(speed, objects)
    cv2.putText(detected_frame, f"Speed: {speed:.1f}  | Safe: {safe_speed:.0f}", (10, 60),
                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 0), 2)

    # Display
    cv2.imshow("Driver Assist", detected_frame)
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()

