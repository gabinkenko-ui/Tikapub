"""Générateur de vidéos "résumé / voix off IA" : script -> TTS -> sous-titres synchronisés."""

from __future__ import annotations

import tempfile
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageDraw

from tikapub.generators.base import VIDEO_FPS, VIDEO_HEIGHT, VIDEO_WIDTH, VideoGenerator
from tikapub.generators.quote import _cover_resize, _gradient_background
from tikapub.utils.image import pil_to_array
from tikapub.utils.text import load_font, split_into_chunks, wrap_text


def generate_script_with_ai(topic: str, api_key: str, max_words: int = 120) -> str:
    """Génère un script court à partir d'un sujet, via l'API OpenAI (import différé)."""
    from openai import OpenAI

    client = OpenAI(api_key=api_key)
    prompt = (
        "Écris un script court et percutant pour une vidéo TikTok voix off, "
        f"en français, sur le sujet suivant : « {topic} ». "
        f"Maximum {max_words} mots, phrases courtes, pas de titre ni de mise en forme, "
        "juste le texte à lire tel quel."
    )
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": prompt}],
    )
    return response.choices[0].message.content.strip()


def _subtitle_chunks_with_timing(
    script: str, total_duration: float, chunk_size: int
) -> list[tuple[str, float, float]]:
    """Découpe le script en paquets de mots et estime leur timing au prorata du nombre de caractères."""
    words = script.split()
    chunks = split_into_chunks(words, chunk_size)
    texts = [" ".join(chunk) for chunk in chunks]
    weights = [max(len(t), 1) for t in texts]
    total_weight = sum(weights)

    timings: list[tuple[str, float, float]] = []
    elapsed = 0.0
    for text, weight in zip(texts, weights):
        span = total_duration * (weight / total_weight)
        timings.append((text, elapsed, elapsed + span))
        elapsed += span
    return timings


def _render_subtitle_rgba(text: str, fonts_dir: Path, font_name: str | None) -> "Image.Image":
    band_height = 320
    image = Image.new("RGBA", (VIDEO_WIDTH, band_height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    font = load_font(fonts_dir, font_name, 60)
    max_width = int(VIDEO_WIDTH * 0.88)
    lines = wrap_text(text, font, max_width)

    line_height = 72
    block_height = line_height * len(lines)
    start_y = (band_height - block_height) // 2

    for i, line in enumerate(lines):
        bbox = font.getbbox(line)
        line_width = bbox[2] - bbox[0]
        x = (VIDEO_WIDTH - line_width) // 2
        y = start_y + i * line_height
        # Contour noir pour la lisibilité, puis texte blanc par-dessus.
        for dx, dy in ((-2, 0), (2, 0), (0, -2), (0, 2)):
            draw.text((x + dx, y + dy), line, font=font, fill=(0, 0, 0, 255))
        draw.text((x, y), line, font=font, fill=(255, 255, 255, 255))

    return image


@dataclass
class VoiceoverVideoConfig:
    script: str | None = None
    topic: str | None = None
    openai_api_key: str | None = None
    tts_lang: str = "fr"
    background_image: Path | None = None
    background_video: Path | None = None
    music: Path | None = None
    music_volume: float = 0.15
    fonts_dir: Path = field(default_factory=lambda: Path("assets/fonts"))
    font_name: str | None = None
    subtitle_chunk_size: int = 4


class VoiceoverVideoGenerator(VideoGenerator):
    """Assemble script (fourni ou généré), voix off TTS, fond et sous-titres synchronisés."""

    def __init__(self, config: VoiceoverVideoConfig):
        self.config = config

    def _resolve_script(self) -> str:
        cfg = self.config
        if cfg.script:
            return cfg.script
        if cfg.topic and cfg.openai_api_key:
            return generate_script_with_ai(cfg.topic, cfg.openai_api_key)
        raise ValueError(
            "Fournis soit `script`, soit `topic` + `openai_api_key` pour générer le script."
        )

    def generate(self, output_path: Path) -> Path:
        from gtts import gTTS
        from moviepy.editor import (
            AudioFileClip,
            CompositeAudioClip,
            CompositeVideoClip,
            ImageClip,
            VideoFileClip,
            afx,
        )

        cfg = self.config
        output_path.parent.mkdir(parents=True, exist_ok=True)
        script = self._resolve_script()

        with tempfile.TemporaryDirectory() as tmp_dir:
            voice_path = Path(tmp_dir) / "voice.mp3"
            gTTS(text=script, lang=cfg.tts_lang).save(str(voice_path))
            voice_clip = AudioFileClip(str(voice_path))
            duration = voice_clip.duration

            if cfg.background_video and cfg.background_video.exists():
                bg_clip = VideoFileClip(str(cfg.background_video)).without_audio()
                bg_clip = (
                    bg_clip.loop(duration=duration)
                    if bg_clip.duration < duration
                    else bg_clip.subclip(0, duration)
                )
                background = bg_clip.resize(height=VIDEO_HEIGHT).crop(
                    x_center=bg_clip.w / 2, width=VIDEO_WIDTH, height=VIDEO_HEIGHT
                )
            else:
                if cfg.background_image and cfg.background_image.exists():
                    still = _cover_resize(
                        Image.open(cfg.background_image).convert("RGB"),
                        (VIDEO_WIDTH, VIDEO_HEIGHT),
                    )
                else:
                    still = _gradient_background((VIDEO_WIDTH, VIDEO_HEIGHT))
                background = ImageClip(pil_to_array(still)).set_duration(duration)

            subtitle_clips = []
            for text, start, end in _subtitle_chunks_with_timing(
                script, duration, cfg.subtitle_chunk_size
            ):
                rgba = _render_subtitle_rgba(text, cfg.fonts_dir, cfg.font_name)
                clip = (
                    ImageClip(pil_to_array(rgba), transparent=True)
                    .set_start(start)
                    .set_duration(max(end - start, 0.1))
                    .set_position(("center", int(VIDEO_HEIGHT * 0.68)))
                )
                subtitle_clips.append(clip)

            video = CompositeVideoClip(
                [background, *subtitle_clips], size=(VIDEO_WIDTH, VIDEO_HEIGHT)
            ).set_duration(duration)

            audio_tracks = [voice_clip]
            if cfg.music and cfg.music.exists():
                music_clip = AudioFileClip(str(cfg.music)).volumex(cfg.music_volume)
                music_clip = (
                    music_clip.fx(afx.audio_loop, duration=duration)
                    if music_clip.duration < duration
                    else music_clip.subclip(0, duration)
                )
                audio_tracks.append(music_clip)

            video = video.set_audio(CompositeAudioClip(audio_tracks))
            video.write_videofile(
                str(output_path), fps=VIDEO_FPS, codec="libx264", audio_codec="aac"
            )

        return output_path

