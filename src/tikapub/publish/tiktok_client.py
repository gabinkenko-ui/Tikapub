"""Client pour la TikTok Content Posting API v2 (publication directe, source FILE_UPLOAD).

Référence officielle (à revérifier avant mise en production, l'API évolue) :
https://developers.tiktok.com/doc/content-posting-api-get-started
https://developers.tiktok.com/doc/content-posting-api-reference-direct-post

Flux général :
1. `build_authorization_url` puis autorisation utilisateur -> code renvoyé sur `redirect_uri`.
2. `exchange_code_for_token(code)` -> access_token / refresh_token.
3. `publish_video(...)` : init -> upload par chunks -> polling du statut.
"""

from __future__ import annotations

import time
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urlencode

import requests

AUTH_BASE_URL = "https://www.tiktok.com/v2/auth/authorize/"
API_BASE_URL = "https://open.tiktokapis.com"
TOKEN_URL = f"{API_BASE_URL}/v2/oauth/token/"
CREATOR_INFO_URL = f"{API_BASE_URL}/v2/post/publish/creator_info/query/"
INIT_UPLOAD_URL = f"{API_BASE_URL}/v2/post/publish/video/init/"
STATUS_URL = f"{API_BASE_URL}/v2/post/publish/status/fetch/"

MIN_CHUNK_SIZE = 5 * 1024 * 1024  # 5 Mo
MAX_CHUNK_SIZE = 64 * 1024 * 1024  # 64 Mo

TERMINAL_STATUSES = {"PUBLISH_COMPLETE", "FAILED"}


class TikTokAPIError(RuntimeError):
    """Levée quand l'API TikTok renvoie une erreur ou un statut inattendu."""


@dataclass
class UploadPlan:
    video_size: int
    chunk_size: int
    total_chunk_count: int


def _plan_upload(video_size: int) -> UploadPlan:
    """Découpe le fichier en chunks conformes aux règles TikTok (5-64 Mo).

    Le dernier chunk peut être plus petit que `chunk_size` : c'est explicitement autorisé
    par l'API. `total_chunk_count` doit donc correspondre exactement au nombre d'itérations
    que fera `upload_video_file` (division entière arrondie au supérieur).
    """
    if video_size <= MIN_CHUNK_SIZE:
        return UploadPlan(video_size=video_size, chunk_size=video_size, total_chunk_count=1)

    chunk_size = MIN_CHUNK_SIZE
    total_chunk_count = -(-video_size // chunk_size)  # division entière arrondie au supérieur
    return UploadPlan(video_size=video_size, chunk_size=chunk_size, total_chunk_count=total_chunk_count)


class TikTokClient:
    def __init__(
        self,
        client_key: str,
        client_secret: str,
        access_token: str | None = None,
        refresh_token: str | None = None,
        redirect_uri: str | None = None,
    ):
        self.client_key = client_key
        self.client_secret = client_secret
        self.access_token = access_token
        self.refresh_token = refresh_token
        self.redirect_uri = redirect_uri

    # --- OAuth2 ---------------------------------------------------------

    def build_authorization_url(
        self, scope: str = "video.publish,video.upload", state: str = "tikapub"
    ) -> str:
        if not self.redirect_uri:
            raise ValueError("redirect_uri manquant pour construire l'URL d'autorisation.")
        params = {
            "client_key": self.client_key,
            "scope": scope,
            "response_type": "code",
            "redirect_uri": self.redirect_uri,
            "state": state,
        }
        return f"{AUTH_BASE_URL}?{urlencode(params)}"

    def exchange_code_for_token(self, code: str) -> dict:
        response = requests.post(
            TOKEN_URL,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            data={
                "client_key": self.client_key,
                "client_secret": self.client_secret,
                "code": code,
                "grant_type": "authorization_code",
                "redirect_uri": self.redirect_uri,
            },
            timeout=30,
        )
        data = _raise_for_error(response)
        self.access_token = data.get("access_token")
        self.refresh_token = data.get("refresh_token")
        return data

    def refresh_access_token(self) -> dict:
        if not self.refresh_token:
            raise ValueError("Aucun refresh_token disponible.")
        response = requests.post(
            TOKEN_URL,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            data={
                "client_key": self.client_key,
                "client_secret": self.client_secret,
                "grant_type": "refresh_token",
                "refresh_token": self.refresh_token,
            },
            timeout=30,
        )
        data = _raise_for_error(response)
        self.access_token = data.get("access_token")
        self.refresh_token = data.get("refresh_token", self.refresh_token)
        return data

    def _auth_headers(self) -> dict:
        if not self.access_token:
            raise ValueError("access_token manquant : authentifie-toi d'abord (voir `tikapub auth`).")
        return {
            "Authorization": f"Bearer {self.access_token}",
            "Content-Type": "application/json; charset=UTF-8",
        }

    # --- Publication ------------------------------------------------------

    def query_creator_info(self) -> dict:
        response = requests.post(CREATOR_INFO_URL, headers=self._auth_headers(), timeout=30)
        return _raise_for_error(response)

    def init_video_upload(
        self,
        video_path: Path,
        title: str,
        privacy_level: str = "SELF_ONLY",
        disable_duet: bool = False,
        disable_comment: bool = False,
        disable_stitch: bool = False,
        video_cover_timestamp_ms: int = 1000,
    ) -> dict:
        video_size = video_path.stat().st_size
        plan = _plan_upload(video_size)

        payload = {
            "post_info": {
                "title": title,
                "privacy_level": privacy_level,
                "disable_duet": disable_duet,
                "disable_comment": disable_comment,
                "disable_stitch": disable_stitch,
                "video_cover_timestamp_ms": video_cover_timestamp_ms,
            },
            "source_info": {
                "source": "FILE_UPLOAD",
                "video_size": plan.video_size,
                "chunk_size": plan.chunk_size,
                "total_chunk_count": plan.total_chunk_count,
            },
        }
        response = requests.post(
            INIT_UPLOAD_URL, headers=self._auth_headers(), json=payload, timeout=30
        )
        data = _raise_for_error(response)
        data["data"]["_plan"] = plan
        return data

    def upload_video_file(self, upload_url: str, video_path: Path, plan: UploadPlan) -> None:
        with open(video_path, "rb") as f:
            offset = 0
            while offset < plan.video_size:
                chunk_end = min(offset + plan.chunk_size, plan.video_size) - 1
                f.seek(offset)
                chunk = f.read(chunk_end - offset + 1)
                headers = {
                    "Content-Range": f"bytes {offset}-{chunk_end}/{plan.video_size}",
                    "Content-Type": "video/mp4",
                }
                response = requests.put(upload_url, headers=headers, data=chunk, timeout=120)
                if response.status_code not in (200, 201, 206):
                    raise TikTokAPIError(
                        f"Échec de l'upload du chunk {offset}-{chunk_end} : "
                        f"{response.status_code} {response.text}"
                    )
                offset = chunk_end + 1

    def get_publish_status(self, publish_id: str) -> dict:
        response = requests.post(
            STATUS_URL,
            headers=self._auth_headers(),
            json={"publish_id": publish_id},
            timeout=30,
        )
        return _raise_for_error(response)

    def publish_video(
        self,
        video_path: Path,
        title: str,
        privacy_level: str = "SELF_ONLY",
        poll_interval: float = 3.0,
        timeout: float = 180.0,
        **post_info_kwargs,
    ) -> dict:
        """Orchestre init -> upload -> polling jusqu'à un statut terminal."""
        init_data = self.init_video_upload(
            video_path, title=title, privacy_level=privacy_level, **post_info_kwargs
        )["data"]
        publish_id = init_data["publish_id"]
        upload_url = init_data["upload_url"]
        plan: UploadPlan = init_data["_plan"]

        self.upload_video_file(upload_url, video_path, plan)

        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            status_data = self.get_publish_status(publish_id)["data"]
            status = status_data.get("status")
            if status in TERMINAL_STATUSES:
                if status == "FAILED":
                    raise TikTokAPIError(f"Publication échouée : {status_data}")
                return status_data
            time.sleep(poll_interval)

        raise TikTokAPIError(
            f"Délai dépassé en attendant le statut final de la publication {publish_id}."
        )


def _raise_for_error(response: requests.Response) -> dict:
    try:
        data = response.json()
    except ValueError as exc:
        raise TikTokAPIError(f"Réponse non-JSON ({response.status_code}) : {response.text}") from exc

    error = data.get("error") or {}
    if response.status_code >= 400 or error.get("code") not in (None, "ok"):
        raise TikTokAPIError(f"Erreur API TikTok : {error or response.text}")
    return data
