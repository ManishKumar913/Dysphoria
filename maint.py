import asyncio
import websockets
import cv2
import base64
import numpy as np
import time
import json
import threading


net = cv2.dnn.readNetFromCaffe("MobileNetSSD_deploy.prototxt", "MobileNetSSD_deploy.caffemodel")
CLASSES = [
    "background", "aeroplane", "bicycle", "bird", "boat", "bottle", "bus", "car", "cat", "chair",
    "cow", "diningtable", "dog", "horse", "motorbike", "person", "pottedplant", "sheep",
    "sofa", "train", "tvmonitor"
]


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


def estimate_speed(prev, curr):
    if prev is None:
        return 0
    prev_gray = cv2.cvtColor(prev, cv2.COLOR_BGR2GRAY)
    curr_gray = cv2.cvtColor(curr, cv2.COLOR_BGR2GRAY)
    flow = cv2.calcOpticalFlowFarneback(prev_gray, curr_gray, None,
                                        0.5, 3, 15, 3, 5, 1.2, 0)
    mag, _ = cv2.cartToPolar(flow[..., 0], flow[..., 1])
    return np.mean(mag) * 10  


def recommend_safe_speed(current_speed, detected_objs):
    base = 60
    if "person" in detected_objs or "car" in detected_objs:
        return max(20, base - current_speed * 2)
    return max(30, base - current_speed)


latest_data = {"json": "{}"}


def vision_loop():
    cap = cv2.VideoCapture(0)
    prev_frame = None
    prev_left, prev_right = True, True

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        frame = cv2.resize(frame, (320, 240))  

        
        left_lane, right_lane = detect_lanes(frame)
        lane_changed = (prev_left and prev_right) and (not left_lane or not right_lane)
        prev_left, prev_right = left_lane, right_lane

        
        detected_objects = detect_objects(frame)

        
        speed = estimate_speed(prev_frame, frame)
        prev_frame = frame.copy()

        
        safe_speed = recommend_safe_speed(speed, detected_objects)

        data = {
            "detected_objects": detected_objects,
            "lane_change_detected": lane_changed,
            "estimated_movement": round(speed, 2),
            "recommended_safe_speed_kmh": round(safe_speed)
        }

        latest_data["json"] = json.dumps(data, indent=2)
        print(latest_data["json"])
        
        time.sleep(0.2) 
    cap.release()


async def video_stream(websocket, path):
    try:
        while True:
            await websocket.send(latest_data["json"])
            await asyncio.sleep(0.05)  
    except websockets.ConnectionClosed:
        print("Client disconnected")
    finally:
      cap.release()

async def main():
    server = await websockets.serve(video_stream, "192.168.56.92", 8765)
    print("WebSocket server started on ws://192.168.56.92:8765")
    await server.wait_closed()

if __name__ == "__main__":

    t = threading.Thread(target=vision_loop, daemon=True)
    t.start()

    asyncio.run(main())
            
