import 'dart:io';

import '../logic/ffmpeg_commands.dart';
import '../services/background_resolver.dart';
import '../services/default_media_service.dart';
import '../services/ffmpeg_runner.dart';
import '../services/temp_workspace.dart';
import '../services/text_overlay_renderer.dart';

class QuoteGenerationRequest {
  final String text;
  final String? author;
  final double duration;
  final String? backgroundImagePath;
  final String? backgroundVideoPath;
  final String? musicPath;
  final bool generateDefaultMusic;
  final int? seed;

  const QuoteGenerationRequest({
    required this.text,
    this.author,
    required this.duration,
    this.backgroundImagePath,
    this.backgroundVideoPath,
    this.musicPath,
    this.generateDefaultMusic = true,
    this.seed,
  });
}

/// Génère une vidéo "citation" : texte + auteur sur fond image/vidéo/généré.
class QuoteGenerator {
  final BackgroundResolver _background;
  final DefaultMediaService _defaults;
  final FfmpegRunner _ffmpeg;
  final TextOverlayRenderer _textRenderer;

  const QuoteGenerator({
    BackgroundResolver background = const BackgroundResolver(),
    DefaultMediaService defaults = const DefaultMediaService(),
    FfmpegRunner ffmpeg = const FfmpegRunner(),
    TextOverlayRenderer textRenderer = const TextOverlayRenderer(),
  })  : _background = background,
        _defaults = defaults,
        _ffmpeg = ffmpeg,
        _textRenderer = textRenderer;

  /// Génère la vidéo et la copie vers [outputPath]. Retourne [outputPath].
  Future<String> generate(
    QuoteGenerationRequest req,
    String outputPath, {
    void Function(String step)? onProgress,
  }) async {
    final workspace = await TempWorkspace.create();
    try {
      onProgress?.call('Préparation du fond…');
      final bgPath = workspace.path('background.mp4');
      await _background.resolve(
        outputPath: bgPath,
        duration: req.duration,
        backgroundImagePath: req.backgroundImagePath,
        backgroundVideoPath: req.backgroundVideoPath,
        seed: req.seed,
      );

      onProgress?.call('Mise en page du texte…');
      final overlayBytes = await _textRenderer.renderQuote(text: req.text, author: req.author);
      final overlayPath = workspace.path('text.png');
      await File(overlayPath).writeAsBytes(overlayBytes);

      String? audioPath = req.musicPath;
      if (audioPath == null && req.generateDefaultMusic) {
        onProgress?.call('Génération de la musique…');
        audioPath = workspace.path('music.m4a');
        await _defaults.generateAmbientMusic(
          outputPath: audioPath,
          duration: req.duration,
          seed: req.seed,
        );
      }

      onProgress?.call('Assemblage final…');
      await _ffmpeg.run(
        buildQuoteComposeArgs(
          backgroundPath: bgPath,
          textOverlayPath: overlayPath,
          audioPath: audioPath,
          duration: req.duration,
          outputPath: outputPath,
        ),
        step: 'composition citation',
      );

      return outputPath;
    } finally {
      await workspace.dispose();
    }
  }
}
