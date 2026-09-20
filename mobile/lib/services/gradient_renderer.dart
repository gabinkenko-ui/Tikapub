import 'dart:math';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import '../utils/palettes.dart';
import '../utils/video_format.dart';

/// Rendu du fond dégradé par défaut (voir `defaults.py::BACKGROUND_PALETTES`).
/// L'animation ("respiration" douce) est ensuite appliquée côté ffmpeg via
/// `zoompan` (voir `ffmpeg_commands.dart`) : ce rendu ne produit qu'une image
/// statique de base.
class GradientRenderer {
  const GradientRenderer();

  /// Choisit une palette (aléatoire ou reproductible via [seed]) et rend un
  /// dégradé vertical plein cadre en PNG.
  Future<Uint8List> renderRandomGradient({
    int? seed,
    int width = kVideoWidth,
    int height = kVideoHeight,
  }) async {
    final rng = seed != null ? Random(seed) : Random();
    final palette = kBackgroundPalettes[rng.nextInt(kBackgroundPalettes.length)];

    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder, Rect.fromLTWH(0, 0, width.toDouble(), height.toDouble()));
    final rect = Rect.fromLTWH(0, 0, width.toDouble(), height.toDouble());
    final paint = Paint()
      ..shader = ui.Gradient.linear(
        Offset(width / 2, 0),
        Offset(width / 2, height.toDouble()),
        [palette.$1, palette.$2],
      );
    canvas.drawRect(rect, paint);

    final picture = recorder.endRecording();
    final image = await picture.toImage(width, height);
    final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
    return byteData!.buffer.asUint8List();
  }
}
