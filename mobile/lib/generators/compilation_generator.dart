import 'dart:io';
import 'dart:math';

import '../logic/ffmpeg_commands.dart';
import '../services/default_media_service.dart';
import '../services/ffmpeg_runner.dart';
import '../services/temp_workspace.dart';
import '../services/text_overlay_renderer.dart';

class CompilationGenerationRequest {
  final List<String> clipPaths;
  final double maxDuration;
  final String? introText;
  final String? musicPath;
  final bool generateDefaultMusic;
  final double musicVolume;
  final bool shuffle;
  final int? seed;

  const CompilationGenerationRequest({
    required this.clipPaths,
    this.maxDuration = 58.0,
    this.introText,
    this.musicPath,
    this.generateDefaultMusic = true,
    this.musicVolume = 0.2,
    this.shuffle = false,
    this.seed,
  });
}

/// Génère une vidéo "compilation / repost" à partir de clips existants.
class CompilationGenerator {
  final DefaultMediaService _defaults;
  final FfmpegRunner _ffmpeg;
  final TextOverlayRenderer _textRenderer;

  const CompilationGenerator({
    DefaultMediaService defaults = const DefaultMediaService(),
    FfmpegRunner ffmpeg = const FfmpegRunner(),
    TextOverlayRenderer textRenderer = const TextOverlayRenderer(),
  })  : _defaults = defaults,
        _ffmpeg = ffmpeg,
        _textRenderer = textRenderer;

  Future<String> generate(
    CompilationGenerationRequest req,
    String outputPath, {
    void Function(String step)? onProgress,
  }) async {
    if (req.clipPaths.isEmpty) {
      throw ArgumentError('Sélectionne au moins un clip.');
    }

    final workspace = await TempWorkspace.create();
    try {
      final clips = List<String>.from(req.clipPaths);
      if (req.shuffle) {
        clips.shuffle(req.seed != null ? Random(req.seed) : Random());
      }

      onProgress?.call('Analyse des clips…');
      final normalizedPaths = <String>[];
      var remaining = req.maxDuration;
      for (var i = 0; i < clips.length && remaining > 0.2; i++) {
        final srcDuration = await _ffmpeg.getDurationSeconds(clips[i]);
        final allotted = min(srcDuration, remaining);
        final hasAudio = await _ffmpeg.hasAudioStream(clips[i]);
        final normalizedPath = workspace.path('clip_$i.mp4');

        onProgress?.call('Traitement du clip ${i + 1}/${clips.length}…');
        await _ffmpeg.run(
          hasAudio
              ? buildNormalizeClipWithAudioArgs(
                  inputPath: clips[i],
                  outputPath: normalizedPath,
                  maxDuration: allotted,
                )
              : buildNormalizeClipSilentArgs(
                  inputPath: clips[i],
                  outputPath: normalizedPath,
                  maxDuration: allotted,
                ),
          step: 'normalisation du clip ${i + 1}',
        );
        normalizedPaths.add(normalizedPath);
        remaining -= allotted;
      }

      onProgress?.call('Assemblage des clips…');
      final listFile = File(workspace.path('concat_list.txt'));
      await listFile.writeAsString(buildConcatFileContent(normalizedPaths));
      var currentPath = workspace.path('concatenated.mp4');
      await _ffmpeg.run(
        buildConcatArgs(listFilePath: listFile.path, outputPath: currentPath),
        step: 'concaténation',
      );

      if (req.introText != null && req.introText!.isNotEmpty) {
        onProgress?.call("Ajout de l'intro…");
        final introBytes = await _textRenderer.renderIntroTitle(text: req.introText!);
        final introPngPath = workspace.path('intro.png');
        await File(introPngPath).writeAsBytes(introBytes);
        final withIntroPath = workspace.path('with_intro.mp4');
        final totalDuration = await _ffmpeg.getDurationSeconds(currentPath);
        await _ffmpeg.run(
          buildIntroOverlayArgs(
            inputPath: currentPath,
            introPngPath: introPngPath,
            introDuration: min(3.0, totalDuration),
            outputPath: withIntroPath,
          ),
          step: "intro",
        );
        currentPath = withIntroPath;
      }

      String? musicPath = req.musicPath;
      if (musicPath == null && req.generateDefaultMusic) {
        onProgress?.call('Génération de la musique…');
        final totalDuration = await _ffmpeg.getDurationSeconds(currentPath);
        musicPath = workspace.path('music.m4a');
        await _defaults.generateAmbientMusic(
          outputPath: musicPath,
          duration: totalDuration,
          seed: req.seed,
        );
      }

      if (musicPath != null) {
        onProgress?.call('Mixage de la musique…');
        final totalDuration = await _ffmpeg.getDurationSeconds(currentPath);
        final mixedPath = workspace.path('mixed.mp4');
        await _ffmpeg.run(
          buildMixMusicUnderArgs(
            inputVideoPath: currentPath,
            musicPath: musicPath,
            duration: totalDuration,
            outputPath: mixedPath,
            musicVolume: req.musicVolume,
          ),
          step: 'mixage musique',
        );
        currentPath = mixedPath;
      }

      await File(currentPath).copy(outputPath);
      return outputPath;
    } finally {
      await workspace.dispose();
    }
  }
}
