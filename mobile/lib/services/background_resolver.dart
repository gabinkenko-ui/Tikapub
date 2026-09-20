import '../logic/ffmpeg_commands.dart';
import 'default_media_service.dart';
import 'ffmpeg_runner.dart';

/// Résout la source de fond (image, vidéo ou génération par défaut) en un
/// unique fichier vidéo normalisé (1080x1920, durée exacte, sans audio),
/// pour que la suite de la composition n'ait à traiter qu'un seul cas.
class BackgroundResolver {
  final FfmpegRunner _ffmpeg;
  final DefaultMediaService _defaults;

  const BackgroundResolver({
    FfmpegRunner ffmpeg = const FfmpegRunner(),
    DefaultMediaService defaults = const DefaultMediaService(),
  })  : _ffmpeg = ffmpeg,
        _defaults = defaults;

  Future<void> resolve({
    required String outputPath,
    required double duration,
    String? backgroundImagePath,
    String? backgroundVideoPath,
    int? seed,
  }) async {
    if (backgroundVideoPath != null) {
      await _ffmpeg.run(
        buildNormalizeVideoBackgroundArgs(
          inputPath: backgroundVideoPath,
          outputPath: outputPath,
          duration: duration,
        ),
        step: 'normalisation du fond vidéo',
      );
      return;
    }
    if (backgroundImagePath != null) {
      await _ffmpeg.run(
        buildImageBackgroundArgs(
          inputPath: backgroundImagePath,
          outputPath: outputPath,
          duration: duration,
        ),
        step: 'normalisation du fond image',
      );
      return;
    }
    await _defaults.generateBackground(outputPath: outputPath, duration: duration, seed: seed);
  }
}
