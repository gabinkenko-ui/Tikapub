"""Utilitaires de mise en page de texte (indépendants de moviepy/ffmpeg)."""

from __future__ import annotations

from pathlib import Path

from PIL import ImageFont

_DEFAULT_FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
]


def load_font(fonts_dir: Path, preferred_name: str | None, size: int) -> ImageFont.FreeTypeFont:
    """Charge une police depuis assets/fonts, ou retombe sur une police système."""
    candidates: list[Path | str] = []
    if preferred_name:
        candidates.append(fonts_dir / preferred_name)
    if fonts_dir.is_dir():
        candidates.extend(sorted(fonts_dir.glob("*.ttf")))
    candidates.extend(_DEFAULT_FONT_CANDIDATES)

    for candidate in candidates:
        try:
            return ImageFont.truetype(str(candidate), size=size)
        except OSError:
            continue
    return ImageFont.load_default()


def wrap_text(text: str, font: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    """Découpe `text` en lignes qui tiennent dans `max_width` pixels pour `font`."""
    words = text.split()
    if not words:
        return []

    lines: list[str] = []
    current = words[0]
    for word in words[1:]:
        candidate = f"{current} {word}"
        width = font.getbbox(candidate)[2]
        if width <= max_width:
            current = candidate
        else:
            lines.append(current)
            current = word
    lines.append(current)
    return lines


def split_into_chunks(words: list[str], chunk_size: int) -> list[list[str]]:
    """Regroupe une liste de mots en paquets de `chunk_size` (pour des sous-titres courts)."""
    if chunk_size <= 0:
        raise ValueError("chunk_size doit être positif")
    return [words[i : i + chunk_size] for i in range(0, len(words), chunk_size)]
