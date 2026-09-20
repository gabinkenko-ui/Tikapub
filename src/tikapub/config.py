"""Chargement de la configuration depuis l'environnement (.env)."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()

PROJECT_ROOT = Path(__file__).resolve().parents[2]


def _path_from_env(var: str, default: str) -> Path:
    value = os.getenv(var, default)
    path = Path(value)
    if not path.is_absolute():
        path = PROJECT_ROOT / path
    return path


@dataclass(frozen=True)
class Settings:
    assets_dir: Path
    output_dir: Path

    tiktok_client_key: str | None
    tiktok_client_secret: str | None
    tiktok_access_token: str | None
    tiktok_refresh_token: str | None
    tiktok_redirect_uri: str

    openai_api_key: str | None

    @property
    def backgrounds_dir(self) -> Path:
        return self.assets_dir / "backgrounds"

    @property
    def music_dir(self) -> Path:
        return self.assets_dir / "music"

    @property
    def fonts_dir(self) -> Path:
        return self.assets_dir / "fonts"


def load_settings() -> Settings:
    settings = Settings(
        assets_dir=_path_from_env("TIKAPUB_ASSETS_DIR", "assets"),
        output_dir=_path_from_env("TIKAPUB_OUTPUT_DIR", "output"),
        tiktok_client_key=os.getenv("TIKTOK_CLIENT_KEY") or None,
        tiktok_client_secret=os.getenv("TIKTOK_CLIENT_SECRET") or None,
        tiktok_access_token=os.getenv("TIKTOK_ACCESS_TOKEN") or None,
        tiktok_refresh_token=os.getenv("TIKTOK_REFRESH_TOKEN") or None,
        tiktok_redirect_uri=os.getenv("TIKTOK_REDIRECT_URI", "http://localhost:8787/callback"),
        openai_api_key=os.getenv("OPENAI_API_KEY") or None,
    )
    settings.output_dir.mkdir(parents=True, exist_ok=True)
    return settings
