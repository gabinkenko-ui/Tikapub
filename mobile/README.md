# Tikapub Mobile

Application Android (Flutter) qui génère des vidéos verticales (citation,
voix off, compilation) **entièrement sur l'appareil**, sans réseau ni
publication automatique. Une fois la vidéo générée, tu l'enregistres dans ta
galerie ou tu la partages directement — à toi de la publier sur TikTok,
YouTube ou ailleurs.

## Fonctionnalités

- **Citation** : texte + auteur sur fond image/vidéo/généré automatiquement.
- **Voix off** : script → synthèse vocale **hors-ligne** (moteur TTS natif
  d'Android) → sous-titres synchronisés automatiquement.
- **Compilation** : assemble plusieurs clips de ta galerie en une seule vidéo,
  avec titre d'intro et musique optionnels.
- **Fonds et musique générés** : dégradé animé + nappe d'ambiance synthétisée
  (filtres ffmpeg), sans assets à télécharger, comme la version CLI Python du
  dépôt.
- **Aucune publication automatique** : la vidéo est enregistrée dans la
  galerie (`Enregistrer`) ou partagée via la feuille de partage native
  (`Partager`), qui propose TikTok/YouTube si ces applications sont
  installées.

Durée par défaut : 30 à 90 secondes (réglable), format vertical 1080x1920.

## Stack technique

- **Flutter** (Dart) — un seul code source, ciblage Android pour l'instant.
- **[ffmpeg_kit_flutter_new](https://pub.dev/packages/ffmpeg_kit_flutter_new)**
  (fork maintenu de ffmpeg-kit, variante `full-gpl`) pour l'encodage et la
  composition vidéo. Le paquet historique `ffmpeg_kit_flutter` d'Arthenica a
  été abandonné en 2025 — ne pas l'utiliser.
- **[flutter_tts](https://pub.dev/packages/flutter_tts)** pour la synthèse
  vocale hors-ligne (`TextToSpeech.synthesizeToFile` d'Android).
- **dart:ui** (Canvas) pour le rendu du texte et des dégradés — aucune police
  ni asset à fournir, Flutter retombe sur la police système de l'appareil.
- **gal** / **share_plus** pour l'enregistrement galerie et le partage natif.
- **file_picker** pour choisir fonds/musiques/clips depuis l'appareil.

## Architecture

```
lib/
  logic/
    subtitle_timing.dart    # découpage + minutage des sous-titres (pur, testé)
    ffmpeg_commands.dart    # construction des commandes ffmpeg (pur, testé)
  services/
    gradient_renderer.dart       # dégradé de fond (dart:ui)
    text_overlay_renderer.dart   # texte/sous-titres/intro en PNG transparent
    default_media_service.dart   # fond animé + musique générés par défaut
    background_resolver.dart     # normalise image/vidéo/défaut en un fond unique
    tts_service.dart             # synthèse vocale hors-ligne -> fichier WAV
    ffmpeg_runner.dart           # exécution ffmpeg + ffprobe, erreurs lisibles
    media_picker_service.dart    # sélection de fichiers
    save_share_service.dart      # galerie + partage natif
    temp_workspace.dart          # dossier de travail temporaire par génération
  generators/
    quote_generator.dart
    voiceover_generator.dart
    compilation_generator.dart
  screens/
    home_screen.dart, quote_screen.dart, voiceover_screen.dart,
    compilation_screen.dart, preview_screen.dart
```

## Développement

```bash
cd mobile
flutter pub get
flutter analyze
flutter test
```

`flutter test` couvre la logique pure (minutage des sous-titres, construction
des commandes ffmpeg) et un test de fumée de l'écran d'accueil — aucun de ces
tests ne nécessite d'émulateur.

## Compiler l'APK

Le SDK Android n'a pas pu être installé dans l'environnement où ce projet a
été développé (le domaine `dl.google.com` y est bloqué par la politique
réseau). La compilation réelle est donc déléguée à la CI GitHub Actions
(`.github/workflows/build-mobile-apk.yml`), qui construit un `.apk` de
release à chaque push touchant `mobile/` et le publie comme artefact
téléchargeable de l'exécution.

Pour compiler toi-même en local (sur une machine avec Flutter + Android SDK
installés) :

```bash
cd mobile
flutter pub get
flutter build apk --release
# APK généré dans build/app/outputs/flutter-apk/app-release.apk
```

L'APK est signé avec la clé de debug par défaut (comme `flutter build apk`
sans configuration de signature dédiée) : installable directement sur un
appareil en activant « Sources inconnues », mais pas destiné à une
publication sur le Play Store en l'état.

## Limites connues

- **Testé sans appareil physique** : la logique pure (minutage, commandes
  ffmpeg) est testée unitairement et les filtres ffmpeg ont été validés
  manuellement avec un ffmpeg de bureau (même moteur libavfilter), mais le
  pipeline complet sur un vrai téléphone Android n'a pas pu être exécuté dans
  cet environnement de développement.
- La génération de script par IA (présente côté CLI Python) n'est pas
  reprise ici : le script de la voix off doit être écrit manuellement.
- `minSdk` 24 (Android 7.0+), requis par `ffmpeg-kit-full-gpl`.
