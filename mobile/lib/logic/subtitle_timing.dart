/// Un paquet de mots à afficher comme sous-titre, avec son intervalle de temps.
class SubtitleChunk {
  final String text;
  final double start;
  final double end;

  const SubtitleChunk({required this.text, required this.start, required this.end});

  double get duration => end - start;

  @override
  String toString() => 'SubtitleChunk("$text", $start -> $end)';
}

/// Découpe une liste de mots en paquets de [chunkSize] mots.
List<List<String>> chunkWords(List<String> words, int chunkSize) {
  if (chunkSize <= 0) {
    throw ArgumentError.value(chunkSize, 'chunkSize', 'doit être positif');
  }
  final chunks = <List<String>>[];
  for (var i = 0; i < words.length; i += chunkSize) {
    chunks.add(words.sublist(i, i + chunkSize > words.length ? words.length : i + chunkSize));
  }
  return chunks;
}

/// Découpe [script] en paquets de mots et estime leur timing au prorata du
/// nombre de caractères de chaque paquet, pour couvrir exactement
/// [totalDuration] secondes sans trou ni chevauchement.
///
/// Portage direct de `voiceover.py::_subtitle_chunks_with_timing`.
List<SubtitleChunk> computeSubtitleTimings(
  String script,
  double totalDuration,
  int chunkSize,
) {
  final words = script.trim().split(RegExp(r'\s+')).where((w) => w.isNotEmpty).toList();
  if (words.isEmpty) return const [];

  final chunks = chunkWords(words, chunkSize);
  final texts = chunks.map((c) => c.join(' ')).toList();
  final weights = texts.map((t) => t.isNotEmpty ? t.length : 1).toList();
  final totalWeight = weights.fold<int>(0, (a, b) => a + b);

  final timings = <SubtitleChunk>[];
  var elapsed = 0.0;
  for (var i = 0; i < texts.length; i++) {
    final span = totalDuration * (weights[i] / totalWeight);
    timings.add(SubtitleChunk(text: texts[i], start: elapsed, end: elapsed + span));
    elapsed += span;
  }
  return timings;
}
