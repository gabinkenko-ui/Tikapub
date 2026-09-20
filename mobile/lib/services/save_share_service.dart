import 'package:gal/gal.dart';
import 'package:share_plus/share_plus.dart';

/// Enregistrement dans la galerie et partage natif — c'est par ce chemin que
/// l'utilisateur publie lui-même la vidéo sur TikTok, YouTube, etc.
/// Contrairement à la version CLI, cette application ne publie jamais rien
/// automatiquement : elle génère la vidéo puis laisse la main à l'utilisateur.
class SaveShareService {
  const SaveShareService();

  Future<void> saveToGallery(String videoPath) async {
    final hasAccess = await Gal.hasAccess();
    if (!hasAccess) {
      final granted = await Gal.requestAccess();
      if (!granted) {
        throw SaveShareException(
          "Accès à la galerie refusé. Autorise Tikapub dans les paramètres de l'application.",
        );
      }
    }
    try {
      await Gal.putVideo(videoPath, album: 'Tikapub');
    } on GalException catch (e) {
      throw SaveShareException("Échec de l'enregistrement dans la galerie : ${e.type.message}");
    }
  }

  /// Ouvre la feuille de partage native (TikTok, YouTube, etc. apparaissent
  /// automatiquement si ces applications sont installées sur l'appareil).
  Future<void> shareVideo(String videoPath, {String? text}) async {
    await SharePlus.instance.share(
      ShareParams(files: [XFile(videoPath)], text: text),
    );
  }
}

class SaveShareException implements Exception {
  final String message;
  SaveShareException(this.message);

  @override
  String toString() => message;
}
