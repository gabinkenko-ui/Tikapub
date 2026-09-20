import 'package:file_picker/file_picker.dart';

/// Sélection de fichiers médias sur l'appareil (fond image/vidéo, musique,
/// clips sources pour la compilation). `file_picker` gère lui-même les
/// permissions via le sélecteur système (Storage Access Framework) sur les
/// versions récentes d'Android.
class MediaPickerService {
  const MediaPickerService();

  Future<String?> pickImage() async {
    final file = await FilePicker.pickFile(type: FileType.image);
    return file?.path;
  }

  Future<String?> pickVideo() async {
    final file = await FilePicker.pickFile(type: FileType.video);
    return file?.path;
  }

  Future<String?> pickAudio() async {
    final file = await FilePicker.pickFile(type: FileType.audio);
    return file?.path;
  }

  /// Sélection de plusieurs clips vidéo (générateur "compilation").
  Future<List<String>> pickMultipleVideos() async {
    final files = await FilePicker.pickFiles(type: FileType.video);
    return files.map((f) => f.path).whereType<String>().toList();
  }
}
