# Tikapub

Création et publication automatique de vidéos de différentes natures sur TikTok.

Ce dépôt contient deux projets :

- **`/` (ce dossier)** : outil en ligne de commande Python (voir plus bas) —
  génère des vidéos et peut les publier directement sur TikTok via son API
  officielle.
- **[`mobile/`](mobile/README.md)** : application Android (Flutter) qui génère
  des vidéos directement sur le téléphone, sans publication automatique —
  c'est toi qui enregistres/partages la vidéo ensuite sur TikTok ou YouTube.

## Fonctionnalités (CLI Python)

- **Citations / texte animé** : texte + auteur sur fond image, vidéo ou généré par défaut.
- **Résumé / voix off IA** : script (fourni ou généré via OpenAI), voix off TTS,
  sous-titres synchronisés automatiquement.
- **Compilation / repost** : assemble des clips d'un dossier en une vidéo verticale
  sous la limite de durée, avec titre d'intro optionnel.
- **Fonds et musique générés par défaut** : si tu ne fournis ni image/vidéo de fond
  ni musique, Tikapub en génère automatiquement (dégradé animé + nappe d'ambiance
  synthétisée), 100% local, sans réseau ni assets à télécharger.
- **Publication TikTok** : intégration de la Content Posting API officielle
  (OAuth2 + upload par chunks + suivi de statut).

Toutes les vidéos sont générées au format vertical standard TikTok (1080x1920).

## Installation

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -e .
```

`ffmpeg` est nécessaire à l'export vidéo. Le paquet `imageio-ffmpeg` (inclus dans
les dépendances) fournit un binaire portable si `ffmpeg` n'est pas installé sur le
système.

Copie `.env.example` vers `.env` et renseigne au minimum les identifiants TikTok si
tu comptes publier automatiquement :

```bash
cp .env.example .env
```

## Utilisation

### Générer une vidéo « citation »

```bash
tikapub generate quote \
  --text "Le succès n'est pas final, l'échec n'est pas fatal." \
  --author "Winston Churchill" \
  --background-image assets/backgrounds/mon_fond.jpg \
  --music assets/music/ma_musique.mp3 \
  --output output/citation.mp4
```

Sans image/vidéo de fond, un dégradé animé généré procéduralement est utilisé ; sans
`--music`, une nappe d'ambiance est synthétisée automatiquement (désactivable avec
`--no-default-music`). Utilise `--seed` pour reproduire le même fond/musique généré
d'une exécution à l'autre.

### Générer une vidéo « voix off IA »

```bash
# Script fourni directement
tikapub generate voiceover --script "Voici un résumé rapide sur..." --output output/voiceover.mp4

# Ou script généré à partir d'un sujet (nécessite OPENAI_API_KEY dans .env)
tikapub generate voiceover --topic "3 faits surprenants sur l'espace" --output output/voiceover.mp4
```

La voix off utilise [gTTS](https://github.com/pndurette/gTTS) par défaut (nécessite
un accès réseau sortant vers `translate.google.com`). Les sous-titres sont
synchronisés automatiquement au prorata de la durée de l'audio généré. Comme pour
le générateur « citation », le fond et la musique sont générés automatiquement en
l'absence de `--background-image`/`--background-video`/`--music` (`--no-default-music`
pour désactiver la musique générée).

### Générer une vidéo « compilation »

```bash
tikapub generate compilation \
  --source-dir clips_source \
  --max-duration 58 \
  --intro-text "Top moments de la semaine" \
  --output output/compilation.mp4
```

Place tes clips sources (`.mp4`, `.mov`, `.m4v`, `.webm`, `.avi`) dans
`clips_source/` avant de lancer la commande. Sans `--music`, une musique
d'ambiance générée est mixée sous l'audio original des clips (désactivable avec
`--no-default-music`).

### Publier sur TikTok

1. Crée une application sur le [TikTok for Developers portal](https://developers.tiktok.com/)
   et active la Content Posting API.
2. Renseigne `TIKTOK_CLIENT_KEY`, `TIKTOK_CLIENT_SECRET` et `TIKTOK_REDIRECT_URI`
   dans `.env`.
3. Récupère l'URL d'autorisation, ouvre-la dans un navigateur, autorise l'app :

   ```bash
   tikapub auth url
   ```

4. Échange le code reçu sur `redirect_uri` contre des tokens, puis ajoute-les à `.env` :

   ```bash
   tikapub auth exchange <code>
   ```

5. Publie une vidéo générée :

   ```bash
   tikapub publish --file output/citation.mp4 --title "Ma légende TikTok" --privacy-level SELF_ONLY
   ```

   `--privacy-level` vaut `SELF_ONLY` par défaut (brouillon privé, visible seulement
   par toi) pour éviter une publication publique accidentelle. Passe explicitement
   `PUBLIC_TO_EVERYONE` pour publier réellement.

## Architecture

```
src/tikapub/
  config.py           # chargement de la configuration (.env)
  cli.py               # commandes `tikapub generate ...` / `tikapub auth ...` / `tikapub publish`
  generators/
    base.py            # interface commune (VideoGenerator), format vidéo, correctif Pillow/moviepy
    defaults.py          # fond animé + musique d'ambiance générés procéduralement (sans réseau)
    quote.py            # texte + auteur sur fond image/vidéo/généré par défaut
    voiceover.py         # script -> TTS -> sous-titres synchronisés
    compilation.py       # concaténation de clips + intro optionnelle
  publish/
    tiktok_client.py    # OAuth2 + Content Posting API (init/upload/status)
  utils/
    text.py             # chargement de police, retour à la ligne, découpage en chunks
    image.py             # conversion PIL -> tableau numpy pour moviepy

assets/        # fonds, musiques, polices (non versionnés, sauf .gitkeep)
clips_source/  # clips à assembler pour le générateur "compilation"
output/        # vidéos générées (non versionnées)
tests/         # tests unitaires (logique pure, sans dépendance à ffmpeg/réseau)
```

## Tests

```bash
pip install -e ".[dev]"
pytest
```

Les tests couvrent la logique indépendante de ffmpeg et du réseau (mise en page du
texte, timing des sous-titres, découpage des chunks d'upload TikTok, configuration).
La génération vidéo elle-même a été validée manuellement de bout en bout (rendu,
composition, export ffmpeg).

## Limites connues / à faire

- La génération de script IA (`--topic`) nécessite `OPENAI_API_KEY` et le package
  optionnel `openai` (`pip install -e ".[ai-script]"`).
- Le timing des sous-titres du générateur voix off est une approximation
  proportionnelle au nombre de caractères, pas un alignement forcé (type Whisper).
- Le client TikTok cible le flux `FILE_UPLOAD` de la Content Posting API ; vérifie
  la documentation officielle avant mise en production, l'API évolue régulièrement.
