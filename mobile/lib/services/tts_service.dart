import 'package:flutter_tts/flutter_tts.dart';

/// Synthèse vocale hors-ligne via le moteur TTS natif d'Android
/// (`TextToSpeech.synthesizeToFile`, exposé par flutter_tts). Contrairement à
/// la version CLI Python (gTTS, qui appelle l'API Google Translate en ligne),
/// cette synthèse fonctionne entièrement sur l'appareil, sans réseau.
class TtsService {
  final FlutterTts _tts = FlutterTts();
  bool _initialized = false;

  Future<void> _ensureInitialized(String language) async {
    if (_initialized) return;
    await _tts.awaitSynthCompletion(true);
    await _tts.setLanguage(language);
    _initialized = true;
  }

  /// Synthétise [text] et écrit le résultat (WAV) à l'emplacement [outputPath]
  /// (chemin absolu complet). Lève une [TtsException] en cas d'échec.
  Future<void> synthesizeToFile(
    String text, {
    required String outputPath,
    String language = 'fr-FR',
  }) async {
    await _ensureInitialized(language);
    final result = await _tts.synthesizeToFile(text, outputPath, true);
    // Sur Android, flutter_tts renvoie 1 pour succès (SUCCESS), tout autre
    // code (ou une exception de canal) indique un échec de la synthèse.
    if (result != 1) {
      throw TtsException(
        'La synthèse vocale a échoué (code $result). '
        "Vérifie qu'un moteur TTS et une voix française sont installés sur l'appareil "
        '(Paramètres > Accessibilité > Synthèse vocale).',
      );
    }
  }
}

class TtsException implements Exception {
  final String message;
  TtsException(this.message);

  @override
  String toString() => message;
}
