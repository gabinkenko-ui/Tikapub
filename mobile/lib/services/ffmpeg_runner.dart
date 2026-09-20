import 'package:ffmpeg_kit_flutter_new/ffmpeg_kit.dart';
import 'package:ffmpeg_kit_flutter_new/ffprobe_kit.dart';
import 'package:ffmpeg_kit_flutter_new/return_code.dart';

/// Exécute des commandes ffmpeg et transforme un échec en message clair
/// (logs ffmpeg inclus), plutôt que de laisser fuiter une exception de bas
/// niveau jusqu'à l'écran — même principe que la correction apportée côté
/// client TikTok Python après le test en conditions réelles.
class FfmpegRunner {
  const FfmpegRunner();

  Future<void> run(List<String> arguments, {String? step}) async {
    final session = await FFmpegKit.executeWithArguments(arguments);
    final returnCode = await session.getReturnCode();
    if (!ReturnCode.isSuccess(returnCode)) {
      final logs = await session.getLogsAsString();
      final label = step != null ? ' ($step)' : '';
      throw FfmpegException(
        "Échec du traitement vidéo$label (code $returnCode).\n"
        '${_lastLines(logs, 20)}',
      );
    }
  }

  /// Durée d'un fichier média (en secondes), via ffprobe.
  Future<double> getDurationSeconds(String path) async {
    final session = await FFprobeKit.getMediaInformation(path);
    final info = session.getMediaInformation();
    final raw = info?.getDuration();
    if (raw == null) {
      throw FfmpegException('Impossible de lire la durée de $path (ffprobe).');
    }
    return double.parse(raw);
  }

  /// Indique si le fichier vidéo contient au moins une piste audio.
  Future<bool> hasAudioStream(String path) async {
    final session = await FFprobeKit.getMediaInformation(path);
    final streams = session.getMediaInformation()?.getStreams() ?? [];
    return streams.any((s) => s.getType() == 'audio');
  }

  String _lastLines(String logs, int n) {
    final lines = logs.split('\n');
    if (lines.length <= n) return logs;
    return lines.sublist(lines.length - n).join('\n');
  }
}

class FfmpegException implements Exception {
  final String message;
  FfmpegException(this.message);

  @override
  String toString() => message;
}
