import 'dart:io';

import '../logic/ffmpeg_commands.dart';
import '../logic/subtitle_timing.dart';
import '../services/background_resolver.dart';
import '../services/default_media_service.dart';
import '../services/ffmpeg_runner.dart';
import '../services/temp_workspace.dart';
import '../services/text_overlay_renderer.dart';
import '../services/tts_service.dart';

class VoiceoverGenerationRequest {
  final String script;
  final String ttsLanguage;
  final String? backgroundImagePath;
  final String? backgroundVideoPath;
  final String? musicPath;
  final bool generateDefaultMusic;
  final double musicVolume;
  final int subtitleChunkSize;
  final int? seed;

  const VoiceoverGenerationRequest({
    required this.script,
    this.ttsLanguage = 'fr-FR',
    this.backgroundImagePath,
    this.backgroundVideoPath,
    this.musicPath,
    this.generateDefaultMusic = true,
    this.musicVolume = kDefaultMusicVolume,
    this.subtitleChunkSize = 4,
    this.seed,
  });
}

/// Génère une vidéo "résumé / voix off" : script -> TTS hors-ligne ->
/// sous-titres synchronisés -> fond + musique.
class VoiceoverGenerator {
  final BackgroundResolver _background;
  final DefaultMediaService _defaults;
  final FfmpegRunner _ffmpeg;
  final TextOverlayRenderer _textRenderer;
  final TtsService _tts;

  VoiceoverGenerator({
    BackgroundResolver background = const BackgroundResolver(),
    DefaultMediaService defaults = const DefaultMediaService(),
    FfmpegRunner ffmpeg = const FfmpegRunner(),
    TextOverlayRenderer textRenderer = const TextOverlayRenderer(),
    TtsService? tts,
  })  : _background = background,
        _defaults = defaults,
        _ffmpeg = ffmpeg,
        _textRenderer = textRenderer,
        _tts = tts ?? TtsService();

  Future<String> generate(
    VoiceoverGenerationRequest req,
    String outputPath, {
    void Function(String step)? onProgress,
  }) async {
    final workspace = await TempWorkspace.create();
    try {
      onProgress?.call('Synthèse de la voix off…');
      final narrationPath = workspace.path('narration.wav');
      await _tts.synthesizeToFile(
        req.script,
        outputPath: narrationPath,
        language: req.ttsLanguage,
      );
      final duration = await _ffmpeg.getDurationSeconds(narrationPath);

      onProgress?.call('Préparation du fond…');
      final bgPath = workspace.path('background.mp4');
      await _background.resolve(
        outputPath: bgPath,
        duration: duration,
        backgroundImagePath: req.backgroundImagePath,
        backgroundVideoPath: req.backgroundVideoPath,
        seed: req.seed,
      );

      onProgress?.call('Sous-titres…');
      final timings = computeSubtitleTimings(req.script, duration, req.subtitleChunkSize);
      final overlayPaths = <String>[];
      for (var i = 0; i < timings.length; i++) {
        final bytes = await _textRenderer.renderSubtitle(text: timings[i].text);
        final path = workspace.path('sub_$i.png');
        await File(path).writeAsBytes(bytes);
        overlayPaths.add(path);
      }

      String? musicPath = req.musicPath;
      if (musicPath == null && req.generateDefaultMusic) {
        onProgress?.call('Génération de la musique…');
        musicPath = workspace.path('music.m4a');
        await _defaults.generateAmbientMusic(outputPath: musicPath, duration: duration, seed: req.seed);
      }

      onProgress?.call('Assemblage final…');
      await _ffmpeg.run(
        buildVoiceoverComposeArgs(
          backgroundPath: bgPath,
          subtitleOverlayPaths: overlayPaths,
          timings: timings,
          narrationPath: narrationPath,
          musicPath: musicPath,
          duration: duration,
          outputPath: outputPath,
          musicVolume: req.musicVolume,
        ),
        step: 'composition voix off',
      );

      return outputPath;
    } finally {
      await workspace.dispose();
    }
  }
}
