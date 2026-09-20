"""Interface en ligne de commande de Tikapub."""

from __future__ import annotations

from pathlib import Path

import click

from tikapub.config import load_settings


def _optional_path(value: str | None) -> Path | None:
    return Path(value) if value else None


@click.group()
def cli() -> None:
    """Tikapub : génération et publication automatique de vidéos TikTok."""


@cli.group()
def generate() -> None:
    """Génère une vidéo (sans la publier)."""


@generate.command("quote")
@click.option("--text", required=True, help="Texte de la citation.")
@click.option("--author", default=None, help="Auteur affiché sous la citation.")
@click.option("--background-image", default=None, type=click.Path(exists=True))
@click.option("--background-video", default=None, type=click.Path(exists=True))
@click.option("--music", default=None, type=click.Path(exists=True))
@click.option(
    "--no-default-music",
    is_flag=True,
    default=False,
    help="Ne pas générer de musique d'ambiance automatiquement si --music n'est pas fourni.",
)
@click.option("--seed", default=None, type=int, help="Graine aléatoire (fond et musique générés).")
@click.option("--duration", default=8.0, type=float, help="Durée en secondes.")
@click.option("--output", default=None, type=click.Path(), help="Chemin du fichier .mp4 de sortie.")
def generate_quote(
    text, author, background_image, background_video, music, no_default_music, seed, duration, output
):
    """Génère une vidéo « citation » : texte animé sur fond image/vidéo/généré par défaut."""
    from tikapub.generators.quote import QuoteVideoConfig, QuoteVideoGenerator

    settings = load_settings()
    config = QuoteVideoConfig(
        text=text,
        author=author,
        duration=duration,
        background_image=_optional_path(background_image),
        background_video=_optional_path(background_video),
        music=_optional_path(music),
        generate_default_music=not no_default_music,
        seed=seed,
        fonts_dir=settings.fonts_dir,
    )
    output_path = Path(output) if output else settings.output_dir / "quote.mp4"
    result = QuoteVideoGenerator(config).generate(output_path)
    click.echo(f"Vidéo générée : {result}")


@generate.command("voiceover")
@click.option("--script", default=None, help="Script à lire tel quel.")
@click.option(
    "--script-file", default=None, type=click.Path(exists=True), help="Fichier texte contenant le script."
)
@click.option("--topic", default=None, help="Sujet à partir duquel générer le script via IA (OPENAI_API_KEY requis).")
@click.option("--background-image", default=None, type=click.Path(exists=True))
@click.option("--background-video", default=None, type=click.Path(exists=True))
@click.option("--music", default=None, type=click.Path(exists=True))
@click.option(
    "--no-default-music",
    is_flag=True,
    default=False,
    help="Ne pas générer de musique d'ambiance automatiquement si --music n'est pas fourni.",
)
@click.option("--seed", default=None, type=int, help="Graine aléatoire (fond et musique générés).")
@click.option(
    "--no-whisper-align",
    is_flag=True,
    default=False,
    help="Désactive l'alignement précis des sous-titres via Whisper (utilise l'estimation proportionnelle).",
)
@click.option(
    "--whisper-model",
    default="base",
    help="Taille du modèle faster-whisper utilisé pour l'alignement des sous-titres.",
)
@click.option("--output", default=None, type=click.Path())
def generate_voiceover(
    script,
    script_file,
    topic,
    background_image,
    background_video,
    music,
    no_default_music,
    seed,
    no_whisper_align,
    whisper_model,
    output,
):
    """Génère une vidéo « résumé / voix off IA » avec sous-titres synchronisés."""
    from tikapub.generators.voiceover import VoiceoverVideoConfig, VoiceoverVideoGenerator

    settings = load_settings()
    if script_file:
        script = Path(script_file).read_text(encoding="utf-8")

    config = VoiceoverVideoConfig(
        script=script,
        topic=topic,
        openai_api_key=settings.openai_api_key,
        background_image=_optional_path(background_image),
        background_video=_optional_path(background_video),
        music=_optional_path(music),
        generate_default_music=not no_default_music,
        seed=seed,
        fonts_dir=settings.fonts_dir,
        align_subtitles=not no_whisper_align,
        whisper_model=whisper_model,
    )
    output_path = Path(output) if output else settings.output_dir / "voiceover.mp4"
    result = VoiceoverVideoGenerator(config).generate(output_path)
    click.echo(f"Vidéo générée : {result}")


@generate.command("compilation")
@click.option("--source-dir", required=True, type=click.Path(exists=True, file_okay=False))
@click.option("--max-duration", default=58.0, type=float)
@click.option("--music", default=None, type=click.Path(exists=True))
@click.option(
    "--no-default-music",
    is_flag=True,
    default=False,
    help="Ne pas générer de musique d'ambiance automatiquement si --music n'est pas fourni.",
)
@click.option("--intro-text", default=None)
@click.option("--shuffle", is_flag=True, default=False)
@click.option("--seed", default=None, type=int)
@click.option("--output", default=None, type=click.Path())
def generate_compilation(source_dir, max_duration, music, no_default_music, intro_text, shuffle, seed, output):
    """Génère une vidéo « compilation / repost » à partir d'un dossier de clips."""
    from tikapub.generators.compilation import CompilationVideoConfig, CompilationVideoGenerator

    settings = load_settings()
    config = CompilationVideoConfig(
        source_dir=Path(source_dir),
        max_duration=max_duration,
        music=_optional_path(music),
        generate_default_music=not no_default_music,
        intro_text=intro_text,
        shuffle=shuffle,
        seed=seed,
        fonts_dir=settings.fonts_dir,
    )
    output_path = Path(output) if output else settings.output_dir / "compilation.mp4"
    result = CompilationVideoGenerator(config).generate(output_path)
    click.echo(f"Vidéo générée : {result}")


@cli.group()
def auth() -> None:
    """Flux d'authentification OAuth2 pour la TikTok Content Posting API."""


@auth.command("url")
def auth_url() -> None:
    """Affiche l'URL à ouvrir dans un navigateur pour autoriser l'application."""
    from tikapub.publish.tiktok_client import TikTokClient

    settings = load_settings()
    if not settings.tiktok_client_key:
        raise click.ClickException("TIKTOK_CLIENT_KEY manquant dans l'environnement (.env).")

    client = TikTokClient(
        client_key=settings.tiktok_client_key,
        client_secret=settings.tiktok_client_secret or "",
        redirect_uri=settings.tiktok_redirect_uri,
    )
    click.echo(client.build_authorization_url())


@auth.command("exchange")
@click.argument("code")
def auth_exchange(code: str) -> None:
    """Échange le code d'autorisation reçu contre un access_token / refresh_token."""
    from tikapub.publish.tiktok_client import TikTokAPIError, TikTokClient

    settings = load_settings()
    if not settings.tiktok_client_key or not settings.tiktok_client_secret:
        raise click.ClickException("TIKTOK_CLIENT_KEY / TIKTOK_CLIENT_SECRET manquants dans .env.")

    client = TikTokClient(
        client_key=settings.tiktok_client_key,
        client_secret=settings.tiktok_client_secret,
        redirect_uri=settings.tiktok_redirect_uri,
    )
    try:
        data = client.exchange_code_for_token(code)
    except TikTokAPIError as exc:
        raise click.ClickException(str(exc)) from exc
    click.echo("Ajoute ces valeurs à ton fichier .env :")
    click.echo(f"TIKTOK_ACCESS_TOKEN={data.get('access_token')}")
    click.echo(f"TIKTOK_REFRESH_TOKEN={data.get('refresh_token')}")


@cli.command("publish")
@click.option("--file", "video_file", required=True, type=click.Path(exists=True))
@click.option("--title", required=True, help="Titre / légende de la publication.")
@click.option(
    "--privacy-level",
    default="SELF_ONLY",
    type=click.Choice(
        ["SELF_ONLY", "PUBLIC_TO_EVERYONE", "MUTUAL_FOLLOW_FRIENDS", "FOLLOWER_OF_CREATOR"]
    ),
    help="SELF_ONLY par défaut par sécurité : passe explicitement PUBLIC_TO_EVERYONE pour publier réellement.",
)
def publish(video_file: str, title: str, privacy_level: str) -> None:
    """Publie une vidéo déjà générée sur TikTok via la Content Posting API."""
    from tikapub.publish.tiktok_client import TikTokAPIError, TikTokClient

    settings = load_settings()
    missing = [
        name
        for name, value in [
            ("TIKTOK_CLIENT_KEY", settings.tiktok_client_key),
            ("TIKTOK_CLIENT_SECRET", settings.tiktok_client_secret),
            ("TIKTOK_ACCESS_TOKEN", settings.tiktok_access_token),
        ]
        if not value
    ]
    if missing:
        raise click.ClickException(
            f"Configuration manquante : {', '.join(missing)}. Lance `tikapub auth url` d'abord."
        )

    client = TikTokClient(
        client_key=settings.tiktok_client_key,
        client_secret=settings.tiktok_client_secret,
        access_token=settings.tiktok_access_token,
        refresh_token=settings.tiktok_refresh_token,
        redirect_uri=settings.tiktok_redirect_uri,
    )
    try:
        result = client.publish_video(Path(video_file), title=title, privacy_level=privacy_level)
    except TikTokAPIError as exc:
        raise click.ClickException(str(exc)) from exc
    click.echo(f"Statut final : {result}")


if __name__ == "__main__":
    cli()
