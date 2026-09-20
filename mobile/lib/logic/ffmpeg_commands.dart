import 'subtitle_timing.dart';

/// Construction pure des commandes ffmpeg (listes d'arguments) utilisées par les
/// générateurs. Ces fonctions ne dépendent d'aucun binding Flutter/plateforme :
/// elles sont testables avec `flutter test` et peuvent aussi être exécutées via
/// n'importe quel ffmpeg de bureau pour validation croisée avant exécution sur
/// l'appareil (le moteur ffmpeg est le même, seuls le packaging et les codecs
/// disponibles diffèrent selon la variante).
const double kDefaultOverlayOpacity = 0.35;
const double kDefaultMusicVolume = 0.15;
const double kZoomSpeed = 0.0006;
const double kMaxZoom = 1.08;

String _fmt(double v) => v.toStringAsFixed(3);

String _zoompanFilter(double duration, int width, int height, int fps) {
  final frames = (duration * fps).round().clamp(1, 1 << 30);
  return "zoompan=z='min(zoom+$kZoomSpeed,$kMaxZoom)':d=$frames:s=${width}x$height:fps=$fps";
}

/// Normalise une vidéo source en fond 1080x1920 de durée exacte [duration] :
/// boucle si trop courte, coupe si trop longue (`-stream_loop -1` + `-t`),
/// recadrée en mode "cover" (remplit le cadre puis rogne l'excédent).
List<String> buildNormalizeVideoBackgroundArgs({
  required String inputPath,
  required String outputPath,
  required double duration,
  int width = 1080,
  int height = 1920,
  int fps = 30,
}) {
  return [
    '-y',
    '-stream_loop', '-1',
    '-i', inputPath,
    '-t', _fmt(duration),
    '-vf', 'scale=$width:$height:force_original_aspect_ratio=increase,crop=$width:$height,fps=$fps',
    '-pix_fmt', 'yuv420p',
    '-an',
    outputPath,
  ];
}

/// Normalise une image source en fond animé (effet "Ken Burns" doux via zoompan)
/// 1080x1920 de durée exacte [duration].
List<String> buildImageBackgroundArgs({
  required String inputPath,
  required String outputPath,
  required double duration,
  int width = 1080,
  int height = 1920,
  int fps = 30,
}) {
  final zoompan = _zoompanFilter(duration, width, height, fps);
  return [
    '-y',
    '-loop', '1',
    '-i', inputPath,
    '-t', _fmt(duration),
    '-vf', 'scale=$width:$height:force_original_aspect_ratio=increase,crop=$width:$height,$zoompan',
    '-pix_fmt', 'yuv420p',
    '-an',
    outputPath,
  ];
}

/// Fond généré par défaut : [gradientPngPath] est déjà rendu à la bonne taille
/// (voir `GradientRenderer`), seul un léger zoom animé est appliqué.
List<String> buildGeneratedBackgroundArgs({
  required String gradientPngPath,
  required String outputPath,
  required double duration,
  int width = 1080,
  int height = 1920,
  int fps = 30,
}) {
  final zoompan = _zoompanFilter(duration, width, height, fps);
  return [
    '-y',
    '-loop', '1',
    '-i', gradientPngPath,
    '-t', _fmt(duration),
    '-vf', zoompan,
    '-pix_fmt', 'yuv420p',
    '-an',
    outputPath,
  ];
}

/// Synthétise une nappe d'ambiance (accord détuné + tremolo + fondu) en AAC,
/// entièrement via les filtres lavfi d'ffmpeg (aucun asset audio nécessaire).
/// Portage de `defaults.py::generate_default_ambient_music`.
List<String> buildAmbientMusicArgs({
  required (double, double, double) chord,
  required List<double> detunes,
  required double duration,
  required String outputPath,
  int sampleRate = 44100,
}) {
  assert(detunes.length == 3);
  final f1 = chord.$1 + detunes[0];
  final f2 = chord.$2 + detunes[1];
  final f3 = chord.$3 + detunes[2];
  final expr = 'sin(2*PI*$f1*t)+sin(2*PI*$f2*t)+sin(2*PI*$f3*t)';
  final fadeDur = duration < 8 ? duration / 4 : 2.0;
  final fadeOutStart = (duration - fadeDur).clamp(0.0, duration);
  final af = 'volume=0.3,tremolo=f=0.15:d=0.08,'
      'afade=t=in:st=0:d=${_fmt(fadeDur)},'
      'afade=t=out:st=${_fmt(fadeOutStart)}:d=${_fmt(fadeDur)}';
  return [
    '-y',
    '-f', 'lavfi',
    '-i', "aevalsrc=exprs='$expr':s=$sampleRate:d=${_fmt(duration)}",
    '-af', af,
    '-c:a', 'aac', '-b:a', '128k',
    outputPath,
  ];
}

/// Compose la vidéo "citation" finale : fond (déjà normalisé) + assombrissement
/// optionnel + calque de texte transparent + audio (musique fournie ou générée).
List<String> buildQuoteComposeArgs({
  required String backgroundPath,
  required String textOverlayPath,
  String? audioPath,
  required double duration,
  required String outputPath,
  double overlayOpacity = kDefaultOverlayOpacity,
  int width = 1080,
  int height = 1920,
  int fps = 30,
}) {
  final args = <String>['-y', '-i', backgroundPath, '-loop', '1', '-t', _fmt(duration), '-i', textOverlayPath];
  final filters = <String>[];
  String videoIn = '[0:v]';

  if (overlayOpacity > 0) {
    filters.add('color=black@${_fmt(overlayOpacity)}:size=${width}x$height:d=${_fmt(duration)}[dim]');
    filters.add('$videoIn[dim]overlay[bgdim]');
    videoIn = '[bgdim]';
  }
  filters.add('$videoIn[1:v]overlay[outv]');

  if (audioPath != null) {
    // Boucle indéfiniment : un fichier plus court que `duration` (ex. musique
    // fournie par l'utilisateur) est ainsi répété au lieu d'être tronqué par
    // `-shortest` en aval. Sans effet si le fichier est déjà assez long.
    args.addAll(['-stream_loop', '-1', '-i', audioPath]);
  }

  args.addAll(['-filter_complex', filters.join(';')]);
  args.addAll(['-map', '[outv]']);
  if (audioPath != null) {
    args.addAll(['-map', '2:a']);
  }
  args.addAll([
    '-c:v', 'libx264', '-pix_fmt', 'yuv420p',
    if (audioPath != null) ...['-c:a', 'aac'],
    '-r', '$fps',
    '-shortest', '-t', _fmt(duration),
    outputPath,
  ]);
  return args;
}

/// Compose la vidéo "voix off" finale : fond + sous-titres synchronisés
/// (une image transparente par paquet de mots, affichée uniquement sur sa
/// fenêtre de temps) + voix (TTS) + musique optionnelle mixée en dessous.
List<String> buildVoiceoverComposeArgs({
  required String backgroundPath,
  required List<String> subtitleOverlayPaths,
  required List<SubtitleChunk> timings,
  required String narrationPath,
  String? musicPath,
  required double duration,
  required String outputPath,
  double musicVolume = kDefaultMusicVolume,
  int width = 1080,
  int height = 1920,
  int fps = 30,
}) {
  assert(subtitleOverlayPaths.length == timings.length);

  final args = <String>['-y', '-i', backgroundPath];
  for (final path in subtitleOverlayPaths) {
    args.addAll(['-loop', '1', '-t', _fmt(duration), '-i', path]);
  }
  final narrationIndex = 1 + subtitleOverlayPaths.length;
  args.addAll(['-i', narrationPath]);
  int? musicIndex;
  if (musicPath != null) {
    musicIndex = narrationIndex + 1;
    // Boucle la musique si elle est plus courte que la narration (voir la
    // même correction sur buildQuoteComposeArgs).
    args.addAll(['-stream_loop', '-1', '-i', musicPath]);
  }

  final filters = <String>[];
  var videoIn = '[0:v]';
  for (var i = 0; i < subtitleOverlayPaths.length; i++) {
    final inputLabel = '[${i + 1}:v]';
    final outLabel = i == subtitleOverlayPaths.length - 1 ? '[outv]' : '[v${i + 1}]';
    final start = _fmt(timings[i].start);
    final end = _fmt(timings[i].end);
    filters.add("$videoIn$inputLabel overlay=enable='between(t,$start,$end)'$outLabel");
    videoIn = outLabel;
  }

  if (musicIndex != null) {
    filters.add('[$musicIndex:a]volume=${_fmt(musicVolume)}[musicvol]');
    filters.add('[$narrationIndex:a][musicvol]amix=inputs=2:duration=first:normalize=0[outa]');
  }

  args.addAll(['-filter_complex', filters.join(';')]);
  args.addAll(['-map', '[outv]']);
  args.addAll(['-map', musicIndex != null ? '[outa]' : '$narrationIndex:a']);
  args.addAll([
    '-c:v', 'libx264', '-pix_fmt', 'yuv420p',
    '-c:a', 'aac',
    '-r', '$fps',
    '-shortest', '-t', _fmt(duration),
    outputPath,
  ]);
  return args;
}

/// Contenu du fichier liste pour le concat demuxer d'ffmpeg.
String buildConcatFileContent(List<String> normalizedClipPaths) {
  return normalizedClipPaths.map((p) => "file '${p.replaceAll("'", "'\\''")}'").join('\n');
}

/// Normalise un clip source (avec audio) en 1080x1920, tronqué à [maxDuration].
List<String> buildNormalizeClipWithAudioArgs({
  required String inputPath,
  required String outputPath,
  required double maxDuration,
  int width = 1080,
  int height = 1920,
  int fps = 30,
}) {
  return [
    '-y',
    '-i', inputPath,
    '-t', _fmt(maxDuration),
    '-vf', 'scale=$width:$height:force_original_aspect_ratio=increase,crop=$width:$height,fps=$fps',
    '-c:v', 'libx264', '-pix_fmt', 'yuv420p',
    '-c:a', 'aac', '-ar', '44100', '-ac', '2',
    outputPath,
  ];
}

/// Normalise un clip source SANS piste audio : une piste silencieuse est
/// ajoutée pour que la concaténation ultérieure reste homogène.
List<String> buildNormalizeClipSilentArgs({
  required String inputPath,
  required String outputPath,
  required double maxDuration,
  int width = 1080,
  int height = 1920,
  int fps = 30,
}) {
  return [
    '-y',
    '-i', inputPath,
    '-f', 'lavfi', '-i', 'anullsrc=r=44100:cl=stereo',
    '-t', _fmt(maxDuration),
    '-vf', 'scale=$width:$height:force_original_aspect_ratio=increase,crop=$width:$height,fps=$fps',
    '-c:v', 'libx264', '-pix_fmt', 'yuv420p',
    '-c:a', 'aac', '-shortest',
    outputPath,
  ];
}

/// Concatène des clips déjà normalisés (même codec/résolution) sans ré-encodage.
List<String> buildConcatArgs({
  required String listFilePath,
  required String outputPath,
}) {
  return ['-y', '-f', 'concat', '-safe', '0', '-i', listFilePath, '-c', 'copy', outputPath];
}

/// Superpose un titre d'intro (image transparente) sur les premières
/// [introDuration] secondes d'une vidéo déjà assemblée.
List<String> buildIntroOverlayArgs({
  required String inputPath,
  required String introPngPath,
  required double introDuration,
  required String outputPath,
}) {
  return [
    '-y',
    '-i', inputPath,
    '-i', introPngPath,
    '-filter_complex', "[0:v][1:v]overlay=enable='lt(t,${_fmt(introDuration)})'[outv]",
    '-map', '[outv]', '-map', '0:a?',
    '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-c:a', 'copy',
    outputPath,
  ];
}

/// Mixe une musique (fournie ou générée) sous la piste audio déjà présente
/// dans [inputVideoPath], sans ré-encoder la vidéo (`-c:v copy`).
List<String> buildMixMusicUnderArgs({
  required String inputVideoPath,
  required String musicPath,
  required double duration,
  required String outputPath,
  double musicVolume = 0.2,
}) {
  return [
    '-y',
    '-i', inputVideoPath,
    '-stream_loop', '-1', '-t', _fmt(duration), '-i', musicPath,
    '-filter_complex',
    '[1:a]volume=${_fmt(musicVolume)}[musicvol];[0:a][musicvol]amix=inputs=2:duration=first:normalize=0[outa]',
    '-map', '0:v', '-map', '[outa]',
    '-c:v', 'copy', '-c:a', 'aac',
    '-shortest',
    outputPath,
  ];
}
