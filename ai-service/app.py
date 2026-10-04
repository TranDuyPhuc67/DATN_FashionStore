print(">>> REALISTIC TRY-ON UPGRADE RUNNING <<<")

from fastapi import FastAPI, UploadFile, File
from fastapi.responses import FileResponse
from ultralytics import YOLO
from PIL import Image
import numpy as np
import cv2
import io
import uuid

from rembg import remove

app = FastAPI()

pose_model = YOLO("yolov8n-pose.pt")


# ==========================
# POSE
# ==========================
def get_pose(image):
    r = pose_model(np.array(image.convert("RGB")))[0]

    if r.keypoints is None or len(r.keypoints.xy) == 0:
        return None

    p = r.keypoints.xy[0].cpu().numpy()

    return {
        "ls": p[5],
        "rs": p[6],
        "lh": p[11],
        "rh": p[12],
        "la": p[15],
        "ra": p[16]
    }


# ==========================
# BODY REGION (IMPORTANT)
# ==========================
def get_regions(pose):
    ls, rs = pose["ls"], pose["rs"]
    lh, rh = pose["lh"], pose["rh"]
    la, ra = pose["la"], pose["ra"]

    torso_top = min(ls[1], rs[1])
    torso_bottom = (lh[1] + rh[1]) / 2

    leg_bottom = max(la[1], ra[1])

    cx = (ls[0] + rs[0] + lh[0] + rh[0]) / 4

    return {
        "torso_top": torso_top,
        "torso_bottom": torso_bottom,
        "leg_bottom": leg_bottom,
        "cx": cx,
        "shoulder_w": abs(rs[0] - ls[0]),
        "hip_w": abs(rh[0] - lh[0])
    }


# ==========================
# SOFT EDGE (REALISM BOOST)
# ==========================
def soft_mask(img):
    img = img.convert("RGBA")
    arr = np.array(img)

    alpha = arr[:,:,3]

    alpha = cv2.GaussianBlur(alpha, (7,7), 0)

    arr[:,:,3] = alpha

    return Image.fromarray(arr)


# ==========================
# HOME
# ==========================
@app.get("/")
def home():
    return {"message": "REALISTIC TRY-ON UPGRADE RUNNING"}


# ==========================
# TRY ON
# ==========================
@app.post("/try-on")
async def try_on(
    user: UploadFile = File(...),
    shirt: UploadFile = File(None),
    pants: UploadFile = File(None)
):

    user_img = Image.open(io.BytesIO(await user.read())).convert("RGBA")
    result = user_img.copy()

    pose = get_pose(user_img)

    if pose is None:
        return FileResponse("no_pose.png")


    reg = get_regions(pose)


    # ==========================
    # SHIRT (REALISTIC FIT)
    # ==========================
    if shirt:

        shirt_img = Image.open(io.BytesIO(await shirt.read())).convert("RGBA")
        shirt_img = remove(shirt_img).convert("RGBA")
        shirt_img = soft_mask(shirt_img)

        # scale theo torso region
        torso_h = reg["torso_bottom"] - reg["torso_top"]

        scale = (torso_h / shirt_img.height) * 1.8
        scale = max(0.6, min(3.0, scale))

        new_w = int(shirt_img.width * scale)
        new_h = int(shirt_img.height * scale)

        shirt_img = shirt_img.resize((new_w, new_h))

        # warp nhẹ (affine realism)
        src = np.float32([[0,0],[new_w,0],[0,new_h]])
        dst = np.float32([
            [new_w*0.05,0],
            [new_w*0.95, new_h*0.02],
            [0,new_h]
        ])

        M = cv2.getAffineTransform(src, dst)
        shirt_np = cv2.warpAffine(np.array(shirt_img), M, (new_w, new_h))

        shirt_img = Image.fromarray(shirt_np)

        x = int(reg["cx"] - new_w/2)
        y = int(reg["torso_top"] - new_h*0.1)

        result.paste(shirt_img, (x,y), shirt_img)


    # ==========================
    # PANTS (REALISTIC FIT)
    # ==========================
    if pants:

        pants_img = Image.open(io.BytesIO(await pants.read())).convert("RGBA")
        pants_img = remove(pants_img).convert("RGBA")
        pants_img = soft_mask(pants_img)

        leg_h = reg["leg_bottom"] - reg["torso_bottom"]

        scale = (leg_h / pants_img.height) * 1.1
        scale = max(0.6, min(3.0, scale))

        new_w = int(pants_img.width * scale)
        new_h = int(pants_img.height * scale)

        pants_img = pants_img.resize((new_w, new_h))

        x = int(reg["cx"] - new_w/2)
        y = int(reg["torso_bottom"] - new_h*0.05)

        result.paste(pants_img, (x,y), pants_img)


    # ==========================
    # SAVE
    # ==========================
    out = f"result_{uuid.uuid4()}.png"
    result.save(out)

    return FileResponse(out, media_type="image/png")