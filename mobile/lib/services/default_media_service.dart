import 'dart:io';
import 'dart:math';

import '../logic/ffmpeg_commands.dart';
import '../utils/palettes.dart';
import 'ffmpeg_runner.dart';
import 'gradient_renderer.dart';

/// Génère un fond animé et une musique d'ambiance quand l'utilisateur n'en
/// fournit pas — portage de `defaults.py` côté CLI Python. Tout est produit
/// localement (rendu Dart + filtres lavfi ffmpeg), sans réseau ni asset.
class DefaultMediaService {
  final FfmpegRunner _ffmpeg;
  final GradientRenderer _gradient;

  const DefaultMediaService({
    FfmpegRunner ffmpeg = const FfmpegRunner(),
    GradientRenderer gradient = const GradientRenderer(),
  })  : _ffmpeg = ffmpeg,
        _gradient = gradient;

  /// Produit un fond dégradé animé (zoom doux) de durée [duration] à
  /// [outputPath].
  Future<void> generateBackground({
    required String outputPath,
    required double duration,
    int? seed,
  }) async {
    final gradientBytes = await _gradient.renderRandomGradient(seed: seed);
    final gradientPath = '$outputPath.gradient.png';
    await File(gradientPath).writeAsBytes(gradientBytes);
    try {
      await _ffmpeg.run(
        buildGeneratedBackgroundArgs(
          gradientPngPath: gradientPath,
          outputPath: outputPath,
          duration: duration,
        ),
        step: 'fond généré',
      );
    } finally {
      final f = File(gradientPath);
      if (await f.exists()) await f.delete();
    }
  }

  /// Produit une nappe d'ambiance synthétisée de durée [duration] à
  /// [outputPath] (AAC).
  Future<void> generateAmbientMusic({
    required String outputPath,
    required double duration,
    int? seed,
  }) async {
    final rng = seed != null ? Random(seed) : Random();
    final chord = kAmbientChords[rng.nextInt(kAmbientChords.length)];
    final detunes = List.generate(3, (_) => rng.nextDouble() * 0.8 - 0.4);

    await _ffmpeg.run(
      buildAmbientMusicArgs(
        chord: chord,
        detunes: detunes,
        duration: duration,
        outputPath: outputPath,
      ),
      step: 'musique générée',
    );
  }
}
