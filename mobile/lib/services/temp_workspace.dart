import 'dart:io';

import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';

/// Dossier de travail temporaire dédié à une génération vidéo (fichiers
/// intermédiaires : fonds normalisés, calques de texte, audio synthétisé...).
/// Supprimé explicitement une fois la vidéo finale copiée ailleurs.
class TempWorkspace {
  final Directory dir;

  TempWorkspace._(this.dir);

  static Future<TempWorkspace> create() async {
    final base = await getTemporaryDirectory();
    final dir = Directory('${base.path}/tikapub_${const Uuid().v4()}');
    await dir.create(recursive: true);
    return TempWorkspace._(dir);
  }

  String path(String fileName) => '${dir.path}/$fileName';

  Future<void> dispose() async {
    if (await dir.exists()) {
      await dir.delete(recursive: true);
    }
  }
}
