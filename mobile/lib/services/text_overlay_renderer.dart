import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import '../utils/video_format.dart';

/// Rendu de texte en PNG transparent (calque à superposer sur une vidéo via
/// ffmpeg), en utilisant directement `dart:ui` (aucun asset de police requis :
/// Flutter retombe sur la police système par défaut de l'appareil).
class TextOverlayRenderer {
  const TextOverlayRenderer();

  /// Citation centrée + auteur optionnel, avec redimensionnement automatique
  /// de la police pour tenir dans la zone verticale disponible.
  /// Portage de `quote.py::_build_text_overlay`.
  Future<Uint8List> renderQuote({
    required String text,
    String? author,
    Color textColor = Colors.white,
    int width = kVideoWidth,
    int height = kVideoHeight,
  }) async {
    final maxTextWidth = width * 0.82;
    var fontSize = 76.0;
    late _LaidOutText body;

    while (fontSize > 32) {
      body = _layoutOutlinedText(
        text,
        TextStyle(fontSize: fontSize, fontWeight: FontWeight.bold, color: textColor),
        maxTextWidth,
      );
      if (body.painter.height <= height * 0.55) break;
      fontSize -= 4;
    }

    _LaidOutText? authorText;
    if (author != null && author.isNotEmpty) {
      authorText = _layoutOutlinedText(
        '— $author',
        TextStyle(fontSize: 44, fontWeight: FontWeight.bold, color: textColor),
        maxTextWidth,
      );
    }

    final blockHeight = body.painter.height + (authorText != null ? authorText.painter.height + 40 : 0);
    final startY = (height - blockHeight) / 2;

    return _renderToPng(width, height, (canvas) {
      body.paintCentered(canvas, width, startY);
      if (authorText != null) {
        authorText.paintCentered(canvas, width, startY + body.painter.height + 40);
      }
    });
  }

  /// Un paquet de sous-titre, positionné dans le tiers inférieur de l'écran.
  /// Portage de `voiceover.py::_render_subtitle_rgba`.
  Future<Uint8List> renderSubtitle({
    required String text,
    int width = kVideoWidth,
    int height = kVideoHeight,
  }) async {
    final maxTextWidth = width * 0.88;
    final laidOut = _layoutOutlinedText(
      text,
      const TextStyle(fontSize: 60, fontWeight: FontWeight.bold, color: Colors.white),
      maxTextWidth,
    );
    final y = height * 0.68;

    return _renderToPng(width, height, (canvas) {
      laidOut.paintCentered(canvas, width, y);
    });
  }

  /// Titre d'intro affiché en haut de l'écran (générateur "compilation").
  Future<Uint8List> renderIntroTitle({
    required String text,
    int width = kVideoWidth,
    int height = kVideoHeight,
  }) async {
    final maxTextWidth = width * 0.85;
    final laidOut = _layoutOutlinedText(
      text,
      const TextStyle(fontSize: 68, fontWeight: FontWeight.bold, color: Colors.white),
      maxTextWidth,
    );
    final y = height * 0.08;

    return _renderToPng(width, height, (canvas) {
      laidOut.paintCentered(canvas, width, y);
    });
  }

  Future<Uint8List> _renderToPng(
    int width,
    int height,
    void Function(Canvas canvas) paint,
  ) async {
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder, Rect.fromLTWH(0, 0, width.toDouble(), height.toDouble()));
    paint(canvas);
    final picture = recorder.endRecording();
    final image = await picture.toImage(width, height);
    final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
    return byteData!.buffer.asUint8List();
  }

  _LaidOutText _layoutOutlinedText(String text, TextStyle style, double maxWidth) {
    final strokePainter = TextPainter(
      text: TextSpan(
        text: text,
        style: style.copyWith(
          foreground: Paint()
            ..style = PaintingStyle.stroke
            ..strokeWidth = style.fontSize! * 0.08
            ..color = Colors.black,
        ),
      ),
      textAlign: TextAlign.center,
      textDirection: TextDirection.ltr,
    )..layout(maxWidth: maxWidth);

    final fillPainter = TextPainter(
      text: TextSpan(text: text, style: style),
      textAlign: TextAlign.center,
      textDirection: TextDirection.ltr,
    )..layout(maxWidth: maxWidth);

    return _LaidOutText(stroke: strokePainter, painter: fillPainter);
  }
}

class _LaidOutText {
  final TextPainter stroke;
  final TextPainter painter;

  _LaidOutText({required this.stroke, required this.painter});

  void paintCentered(Canvas canvas, int frameWidth, double y) {
    final x = (frameWidth - painter.width) / 2;
    stroke.paint(canvas, Offset(x, y));
    painter.paint(canvas, Offset(x, y));
  }
}
