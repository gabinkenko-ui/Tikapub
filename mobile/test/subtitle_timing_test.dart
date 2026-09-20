import 'package:flutter_test/flutter_test.dart';
import 'package:tikapub_mobile/logic/subtitle_timing.dart';

void main() {
  group('chunkWords', () {
    test('groups words into fixed-size chunks with a smaller last chunk', () {
      final words = ['un', 'deux', 'trois', 'quatre', 'cinq'];
      expect(chunkWords(words, 2), [
        ['un', 'deux'],
        ['trois', 'quatre'],
        ['cinq'],
      ]);
    });

    test('throws for non-positive chunk size', () {
      expect(() => chunkWords(['a'], 0), throwsArgumentError);
    });
  });

  group('computeSubtitleTimings', () {
    test('covers the full duration without gaps or overlaps', () {
      final timings = computeSubtitleTimings(
        'un deux trois quatre cinq six sept huit',
        10.0,
        2,
      );

      expect(timings.first.start, 0.0);
      expect(timings.last.end, closeTo(10.0, 1e-9));
      for (var i = 0; i < timings.length - 1; i++) {
        expect(timings[i].end, closeTo(timings[i + 1].start, 1e-9));
      }
    });

    test('preserves all words in order', () {
      final timings = computeSubtitleTimings('a b c d e', 5.0, 2);
      final reconstructed = timings.map((c) => c.text).join(' ');
      expect(reconstructed, 'a b c d e');
    });

    test('a longer chunk gets more time than a shorter one', () {
      final timings = computeSubtitleTimings(
        'court mot-beaucoup-plus-long-que-le-precedent',
        10.0,
        1,
      );
      expect(timings.length, 2);
      expect(timings[1].duration, greaterThan(timings[0].duration));
    });

    test('returns an empty list for blank input', () {
      expect(computeSubtitleTimings('   ', 5.0, 3), isEmpty);
    });
  });
}
