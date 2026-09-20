"""Conversion PIL -> numpy pour les clips moviepy."""

from __future__ import annotations

from PIL import Image


def pil_to_array(image: "Image.Image"):
    import numpy as np

    mode = "RGBA" if image.mode == "RGBA" else "RGB"
    return np.array(image.convert(mode))
