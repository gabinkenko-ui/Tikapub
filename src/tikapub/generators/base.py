"""Interface commune à tous les générateurs de vidéos."""

from __future__ import annotations

from abc import ABC, abstractmethod
from pathlib import Path

# moviepy 1.x appelle PIL.Image.ANTIALIAS, retiré dans Pillow >= 10 (remplacé par LANCZOS).
import PIL.Image

if not hasattr(PIL.Image, "ANTIALIAS"):
    PIL.Image.ANTIALIAS = PIL.Image.LANCZOS

# Format vertical standard TikTok.
VIDEO_WIDTH = 1080
VIDEO_HEIGHT = 1920
VIDEO_FPS = 30


class VideoGenerator(ABC):
    """Toute stratégie de génération produit un fichier .mp4 vertical."""

    @abstractmethod
    def generate(self, output_path: Path) -> Path:
        """Génère la vidéo et retourne le chemin du fichier produit."""
        raise NotImplementedError
