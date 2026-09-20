"""Générateur de vidéos "citation" : texte animé sur fond (image/vidéo/dégradé)."""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageDraw

from tikapub.generators.base import VIDEO_FPS, VIDEO_HEIGHT, VIDEO_WIDTH, VideoGenerator
from tikapub.utils.image import pil_to_array
from tikapub.utils.text import load_font, wrap_text

_GRADIENT_TOP = (20, 20, 40)
_GRADIENT_BOTTOM = (70, 30, 90)


def _gradient_background(size: tuple[int, int]) -> Image.Image:
    width, height = size
    image = Image.new("RGB", size, _GRADIENT_TOP)
    draw = ImageDraw.Draw(image)
    for y in range(height):
        ratio = y / max(height - 1, 1)
        color = tuple(
            int(top + (bottom - top) * ratio)
            for top, bottom in zip(_GRADIENT_TOP, _GRADIENT_BOTTOM)
        )
        draw.line([(0, y), (width, y)], fill=color)
    return image


def _cover_resize(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    """Redimensionne + rogne `image` pour remplir exactement `size` (comme CSS object-fit: cover)."""
    target_w, target_h = size
    src_ratio = image.width / image.height
    target_ratio = target_w / target_h

    if src_ratio > target_ratio:
        new_height = target_h
        new_width = int(new_height * src_ratio)
    else:
        new_width = target_w
        new_height = int(new_width / src_ratio)

    resized = image.resize((new_width, new_height), Image.LANCZOS)
    left = (new_width - target_w) // 2
    top = (new_height - target_h) // 2
    return resized.crop((left, top, left + target_w, top + target_h))


@dataclass
class QuoteVideoConfig:
    text: str
    author: str | None = None
    duration: float = 8.0
    background_image: Path | None = None
    background_video: Path | None = None
    music: Path | None = None
    fonts_dir: Path = field(default_factory=lambda: Path("assets/fonts"))
    font_name: str | None = None
    text_color: tuple[int, int, int] = (255, 255, 255)
    overlay_opacity: int = 110  # 0-255, assombrit le fond pour la lisibilité du texte


class QuoteVideoGenerator(VideoGenerator):
    """Rend une citation (texte + auteur optionnel) sur un fond fixe ou vidéo."""

    def __init__(self, config: QuoteVideoConfig):
        self.config = config

    def _build_frame(self) -> Image.Image:
        size = (VIDEO_WIDTH, VIDEO_HEIGHT)
        cfg = self.config

        if cfg.background_image and cfg.background_image.exists():
            base = _cover_resize(Image.open(cfg.background_image).convert("RGB"), size)
        else:
            base = _gradient_background(size)

        if cfg.overlay_opacity:
            overlay = Image.new("RGBA", size, (0, 0, 0, cfg.overlay_opacity))
            base = Image.alpha_composite(base.convert("RGBA"), overlay).convert("RGB")

        draw = ImageDraw.Draw(base)
        max_text_width = int(VIDEO_WIDTH * 0.82)

        font_size = 76
        font = load_font(cfg.fonts_dir, cfg.font_name, font_size)
        lines = wrap_text(cfg.text, font, max_text_width)

        # Réduit la taille de police si le texte ne tient pas verticalement.
        while font_size > 32:
            line_height = int(font_size * 1.35)
            block_height = line_height * len(lines)
            if block_height <= VIDEO_HEIGHT * 0.55:
                break
            font_size -= 4
            font = load_font(cfg.fonts_dir, cfg.font_name, font_size)
            lines = wrap_text(cfg.text, font, max_text_width)

        line_height = int(font_size * 1.35)
        block_height = line_height * len(lines)
        start_y = (VIDEO_HEIGHT - block_height) // 2

        for i, line in enumerate(lines):
            bbox = font.getbbox(line)
            line_width = bbox[2] - bbox[0]
            x = (VIDEO_WIDTH - line_width) // 2
            y = start_y + i * line_height
            draw.text((x, y), line, font=font, fill=cfg.text_color)

        if cfg.author:
            author_font = load_font(cfg.fonts_dir, cfg.font_name, 44)
            author_text = f"— {cfg.author}"
            bbox = author_font.getbbox(author_text)
            author_width = bbox[2] - bbox[0]
            x = (VIDEO_WIDTH - author_width) // 2
            y = start_y + block_height + 40
            draw.text((x, y), author_text, font=author_font, fill=cfg.text_color)

        return base

    def generate(self, output_path: Path) -> Path:
        from moviepy.editor import (  # import différé : ffmpeg n'est requis qu'ici
            AudioFileClip,
            CompositeVideoClip,
            ImageClip,
            VideoFileClip,
            afx,
        )

        cfg = self.config
        output_path.parent.mkdir(parents=True, exist_ok=True)

        if cfg.background_video and cfg.background_video.exists():
            bg_clip = VideoFileClip(str(cfg.background_video)).without_audio()
            bg_clip = bg_clip.loop(duration=cfg.duration) if bg_clip.duration < cfg.duration else bg_clip.subclip(0, cfg.duration)
            bg_clip = bg_clip.resize(height=VIDEO_HEIGHT).crop(
                x_center=bg_clip.w / 2, width=VIDEO_WIDTH, height=VIDEO_HEIGHT
            )
            frame = self._build_frame()
            text_layer = ImageClip(pil_to_array(frame)).set_duration(cfg.duration)
            video = CompositeVideoClip([bg_clip, text_layer], size=(VIDEO_WIDTH, VIDEO_HEIGHT))
        else:
            frame = self._build_frame()
            video = ImageClip(pil_to_array(frame)).set_duration(cfg.duration)

        if cfg.music and cfg.music.exists():
            audio = AudioFileClip(str(cfg.music))
            if audio.duration < cfg.duration:
                audio = audio.fx(afx.audio_loop, duration=cfg.duration)
            else:
                audio = audio.subclip(0, cfg.duration)
            video = video.set_audio(audio.audio_fadeout(1.0))

        video.write_videofile(
            str(output_path), fps=VIDEO_FPS, codec="libx264", audio_codec="aac"
        )
        return output_path
