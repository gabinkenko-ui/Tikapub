"""Générateur de vidéos "compilation / repost" : assemble des clips existants."""

from __future__ import annotations

import tempfile
from dataclasses import dataclass, field
from pathlib import Path

from tikapub.generators.base import VIDEO_FPS, VIDEO_HEIGHT, VIDEO_WIDTH, VideoGenerator
from tikapub.utils.image import pil_to_array

SUPPORTED_EXTENSIONS = {".mp4", ".mov", ".m4v", ".webm", ".avi"}


@dataclass
class CompilationVideoConfig:
    source_dir: Path
    max_duration: float = 58.0
    music: Path | None = None
    music_volume: float = 0.2
    generate_default_music: bool = True
    shuffle: bool = False
    seed: int | None = None
    fonts_dir: Path = field(default_factory=lambda: Path("assets/fonts"))
    intro_text: str | None = None


class CompilationVideoGenerator(VideoGenerator):
    """Concatène les clips d'un dossier en une vidéo verticale sous la limite de durée."""

    def __init__(self, config: CompilationVideoConfig):
        self.config = config

    def _list_source_clips(self) -> list[Path]:
        cfg = self.config
        if not cfg.source_dir.is_dir():
            raise FileNotFoundError(f"Dossier source introuvable : {cfg.source_dir}")

        clips = sorted(
            p for p in cfg.source_dir.iterdir() if p.suffix.lower() in SUPPORTED_EXTENSIONS
        )
        if not clips:
            raise FileNotFoundError(f"Aucun clip vidéo trouvé dans {cfg.source_dir}")

        if cfg.shuffle:
            import random

            rng = random.Random(cfg.seed)
            rng.shuffle(clips)

        return clips

    def generate(self, output_path: Path) -> Path:
        from moviepy.editor import (
            AudioFileClip,
            CompositeAudioClip,
            CompositeVideoClip,
            ImageClip,
            VideoFileClip,
            afx,
            concatenate_videoclips,
        )

        cfg = self.config
        output_path.parent.mkdir(parents=True, exist_ok=True)

        selected = []
        total = 0.0
        for path in self._list_source_clips():
            if total >= cfg.max_duration:
                break
            clip = VideoFileClip(str(path))
            clip = clip.resize(height=VIDEO_HEIGHT).crop(
                x_center=clip.w / 2, width=VIDEO_WIDTH, height=VIDEO_HEIGHT
            )
            remaining = cfg.max_duration - total
            if clip.duration > remaining:
                clip = clip.subclip(0, remaining)
            selected.append(clip)
            total += clip.duration

        video = concatenate_videoclips(selected, method="compose")

        if cfg.intro_text:
            from tikapub.utils.text import load_font, wrap_text
            from PIL import Image, ImageDraw

            overlay = Image.new("RGBA", (VIDEO_WIDTH, VIDEO_HEIGHT), (0, 0, 0, 0))
            draw = ImageDraw.Draw(overlay)
            font = load_font(cfg.fonts_dir, None, 68)
            lines = wrap_text(cfg.intro_text, font, int(VIDEO_WIDTH * 0.85))
            y = int(VIDEO_HEIGHT * 0.08)
            for line in lines:
                bbox = font.getbbox(line)
                x = (VIDEO_WIDTH - (bbox[2] - bbox[0])) // 2
                for dx, dy in ((-2, 0), (2, 0), (0, -2), (0, 2)):
                    draw.text((x + dx, y + dy), line, font=font, fill=(0, 0, 0, 255))
                draw.text((x, y), line, font=font, fill=(255, 255, 255, 255))
                y += 80

            intro_clip = ImageClip(pil_to_array(overlay), transparent=True).set_duration(3.0)
            video = CompositeVideoClip([video, intro_clip], size=(VIDEO_WIDTH, VIDEO_HEIGHT)).set_duration(
                video.duration
            )

        with tempfile.TemporaryDirectory() as tmp_dir:
            music_path = cfg.music if (cfg.music and cfg.music.exists()) else None
            if music_path is None and cfg.generate_default_music:
                from tikapub.generators.defaults import generate_default_ambient_music

                music_path = Path(tmp_dir) / "default_music.wav"
                generate_default_ambient_music(music_path, duration=video.duration, seed=cfg.seed)

            if music_path is not None:
                music_clip = AudioFileClip(str(music_path)).volumex(cfg.music_volume)
                music_clip = (
                    music_clip.fx(afx.audio_loop, duration=video.duration)
                    if music_clip.duration < video.duration
                    else music_clip.subclip(0, video.duration)
                )
                tracks = [music_clip]
                if video.audio is not None:
                    tracks.insert(0, video.audio)
                video = video.set_audio(CompositeAudioClip(tracks))

            video.write_videofile(
                str(output_path), fps=VIDEO_FPS, codec="libx264", audio_codec="aac"
            )

        return output_path
