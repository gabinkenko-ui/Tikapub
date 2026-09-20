"""Générateur de vidéos "citation" : texte animé sur fond (image/vidéo/généré par défaut)."""

from __future__ import annotations

import tempfile
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageDraw

from tikapub.generators.base import VIDEO_FPS, VIDEO_HEIGHT, VIDEO_WIDTH, VideoGenerator
from tikapub.utils.image import pil_to_array
from tikapub.utils.text import load_font, wrap_text


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
    generate_default_music: bool = True
    seed: int | None = None
    fonts_dir: Path = field(default_factory=lambda: Path("assets/fonts"))
    font_name: str | None = None
    text_color: tuple[int, int, int] = (255, 255, 255)
    overlay_opacity: int = 90  # 0-255, assombrit le fond pour la lisibilité du texte


class QuoteVideoGenerator(VideoGenerator):
    """Rend une citation (texte + auteur optionnel) sur un fond image/vidéo/généré par défaut."""

    def __init__(self, config: QuoteVideoConfig):
        self.config = config

    def _build_text_overlay(self) -> Image.Image:
        """Calque RGBA transparent contenant uniquement le texte (contour noir pour la lisibilité)."""
        cfg = self.config
        size = (VIDEO_WIDTH, VIDEO_HEIGHT)
        overlay = Image.new("RGBA", size, (0, 0, 0, 0))
        draw = ImageDraw.Draw(overlay)
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

        def _draw_outlined(x: int, y: int, text: str, font) -> None:
            for dx, dy in ((-2, 0), (2, 0), (0, -2), (0, 2)):
                draw.text((x + dx, y + dy), text, font=font, fill=(0, 0, 0, 255))
            draw.text((x, y), text, font=font, fill=(*cfg.text_color, 255))

        for i, line in enumerate(lines):
            bbox = font.getbbox(line)
            line_width = bbox[2] - bbox[0]
            x = (VIDEO_WIDTH - line_width) // 2
            y = start_y + i * line_height
            _draw_outlined(x, y, line, font)

        if cfg.author:
            author_font = load_font(cfg.fonts_dir, cfg.font_name, 44)
            author_text = f"— {cfg.author}"
            bbox = author_font.getbbox(author_text)
            author_width = bbox[2] - bbox[0]
            x = (VIDEO_WIDTH - author_width) // 2
            y = start_y + block_height + 40
            _draw_outlined(x, y, author_text, author_font)

        return overlay

    def _build_background_clip(self, duration: float):
        from moviepy.editor import ImageClip, VideoFileClip

        cfg = self.config
        if cfg.background_video and cfg.background_video.exists():
            bg_clip = VideoFileClip(str(cfg.background_video)).without_audio()
            bg_clip = (
                bg_clip.loop(duration=duration)
                if bg_clip.duration < duration
                else bg_clip.subclip(0, duration)
            )
            return bg_clip.resize(height=VIDEO_HEIGHT).crop(
                x_center=bg_clip.w / 2, width=VIDEO_WIDTH, height=VIDEO_HEIGHT
            )

        if cfg.background_image and cfg.background_image.exists():
            still = _cover_resize(
                Image.open(cfg.background_image).convert("RGB"), (VIDEO_WIDTH, VIDEO_HEIGHT)
            )
            return ImageClip(pil_to_array(still)).set_duration(duration)

        from tikapub.generators.defaults import make_default_background_clip

        return make_default_background_clip(duration, seed=cfg.seed)

    def generate(self, output_path: Path) -> Path:
        from moviepy.editor import AudioFileClip, ColorClip, CompositeVideoClip, ImageClip, afx

        cfg = self.config
        output_path.parent.mkdir(parents=True, exist_ok=True)

        layers = [self._build_background_clip(cfg.duration)]

        if cfg.overlay_opacity:
            dark_overlay = (
                ColorClip(size=(VIDEO_WIDTH, VIDEO_HEIGHT), color=(0, 0, 0))
                .set_opacity(cfg.overlay_opacity / 255)
                .set_duration(cfg.duration)
            )
            layers.append(dark_overlay)

        text_overlay = self._build_text_overlay()
        layers.append(ImageClip(pil_to_array(text_overlay), transparent=True).set_duration(cfg.duration))

        video = CompositeVideoClip(layers, size=(VIDEO_WIDTH, VIDEO_HEIGHT)).set_duration(cfg.duration)

        with tempfile.TemporaryDirectory() as tmp_dir:
            if cfg.music and cfg.music.exists():
                audio = AudioFileClip(str(cfg.music))
                audio = (
                    audio.fx(afx.audio_loop, duration=cfg.duration)
                    if audio.duration < cfg.duration
                    else audio.subclip(0, cfg.duration)
                )
                video = video.set_audio(audio.audio_fadeout(1.0))
            elif cfg.generate_default_music:
                from tikapub.generators.defaults import generate_default_ambient_music

                music_path = Path(tmp_dir) / "default_music.wav"
                generate_default_ambient_music(music_path, duration=cfg.duration, seed=cfg.seed)
                audio = AudioFileClip(str(music_path))
                video = video.set_audio(audio.audio_fadeout(1.0))

            video.write_videofile(
                str(output_path), fps=VIDEO_FPS, codec="libx264", audio_codec="aac"
            )

        return output_path
