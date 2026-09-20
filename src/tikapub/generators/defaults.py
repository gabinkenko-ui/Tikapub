"""Génération procédurale de fonds animés et de musique d'ambiance par défaut.

Aucune dépendance externe ni accès réseau : tout est synthétisé localement, pour que
chaque vidéo ait un fond et une musique corrects même si l'utilisateur n'en fournit pas.
"""

from __future__ import annotations

import math
import random
import wave
from pathlib import Path

import numpy as np

from tikapub.generators.base import VIDEO_FPS, VIDEO_HEIGHT, VIDEO_WIDTH

# Chaque palette est un dégradé (haut -> bas) choisi pour rester lisible sous du texte blanc.
BACKGROUND_PALETTES: list[tuple[tuple[int, int, int], tuple[int, int, int]]] = [
    ((20, 20, 40), (70, 30, 90)),  # violet nuit
    ((10, 25, 40), (10, 90, 110)),  # bleu océan
    ((35, 12, 10), (120, 50, 20)),  # coucher de soleil
    ((10, 35, 20), (15, 95, 60)),  # vert forêt
    ((30, 10, 35), (130, 20, 90)),  # magenta néon
    ((15, 15, 15), (55, 55, 65)),  # gris anthracite
]

# Accords simples (fréquences en Hz) utilisés pour la nappe d'ambiance par défaut.
AMBIENT_CHORDS: list[tuple[float, float, float]] = [
    (130.81, 164.81, 196.00),  # C3 majeur
    (146.83, 185.00, 220.00),  # D3 majeur
    (164.81, 207.65, 246.94),  # E3 majeur
    (196.00, 246.94, 293.66),  # G3 majeur
    (110.00, 138.59, 164.81),  # A2 majeur
]


def make_default_background_clip(duration: float, seed: int | None = None):
    """Renvoie un moviepy VideoClip : dégradé vertical qui respire doucement dans le temps."""
    from moviepy.editor import VideoClip

    rng = random.Random(seed)
    top, bottom = rng.choice(BACKGROUND_PALETTES)
    top_arr = np.array(top, dtype=float)
    bottom_arr = np.array(bottom, dtype=float)
    phase_offset = rng.uniform(0, math.tau)

    y_ratios = np.linspace(0.0, 1.0, VIDEO_HEIGHT).reshape(VIDEO_HEIGHT, 1)

    def make_frame(t: float) -> np.ndarray:
        # Léger déplacement du point médian du dégradé au fil du temps (respiration lente).
        shift = 0.06 * math.sin(0.25 * t + phase_offset)
        ratios = np.clip(y_ratios + shift, 0.0, 1.0)
        colors = top_arr + (bottom_arr - top_arr) * ratios  # (H, 1, 3)
        frame = np.broadcast_to(colors[:, np.newaxis, :], (VIDEO_HEIGHT, VIDEO_WIDTH, 3))
        return frame.astype("uint8")

    return VideoClip(make_frame, duration=duration).set_fps(VIDEO_FPS)


def generate_default_ambient_music(
    output_path: Path, duration: float, seed: int | None = None, sample_rate: int = 44100
) -> Path:
    """Synthétise une nappe d'ambiance douce (accord détuné + tremolo + fade) en WAV mono."""
    rng = random.Random(seed)
    chord = rng.choice(AMBIENT_CHORDS)

    sample_count = max(int(sample_rate * duration), 1)
    t = np.linspace(0.0, duration, sample_count, endpoint=False)

    signal = np.zeros(sample_count)
    for freq in chord:
        detune = rng.uniform(-0.4, 0.4)
        signal += np.sin(2 * np.pi * (freq + detune) * t)

    fade_seconds = min(2.0, duration / 4)
    fade_samples = int(fade_seconds * sample_rate)
    envelope = np.ones(sample_count)
    if fade_samples > 0:
        envelope[:fade_samples] = np.linspace(0.0, 1.0, fade_samples)
        envelope[-fade_samples:] = np.linspace(1.0, 0.0, fade_samples)

    tremolo = 1.0 + 0.08 * np.sin(2 * np.pi * 0.15 * t)
    signal = signal * envelope * tremolo

    peak = np.max(np.abs(signal))
    if peak > 0:
        signal = signal / peak
    pcm = (signal * 0.6 * 32767).astype(np.int16)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(output_path), "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(pcm.tobytes())

    return output_path
