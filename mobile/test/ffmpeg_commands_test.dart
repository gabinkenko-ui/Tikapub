import 'package:flutter_test/flutter_test.dart';
import 'package:tikapub_mobile/logic/ffmpeg_commands.dart';
import 'package:tikapub_mobile/logic/subtitle_timing.dart';

void main() {
  group('buildGeneratedBackgroundArgs', () {
    test('loops the image and applies a zoompan of the right frame count', () {
      final args = buildGeneratedBackgroundArgs(
        gradientPngPath: 'grad.png',
        outputPath: 'out.mp4',
        duration: 6.0,
        fps: 30,
      );
      expect(args, containsAllInOrder(['-loop', '1', '-i', 'grad.png']));
      expect(args, contains('-t'));
      expect(args[args.indexOf('-t') + 1], '6.000');
      final vf = args[args.indexOf('-vf') + 1];
      expect(vf, contains('zoompan='));
      expect(vf, contains('d=180')); // 6s * 30fps
      expect(vf, contains('s=1080x1920'));
      expect(args, contains('-an'));
      expect(args.last, 'out.mp4');
    });
  });

  group('buildNormalizeVideoBackgroundArgs', () {
    test('loops indefinitely then trims to the target duration', () {
      final args = buildNormalizeVideoBackgroundArgs(
        inputPath: 'in.mp4',
        outputPath: 'out.mp4',
        duration: 8.0,
      );
      expect(args, containsAllInOrder(['-stream_loop', '-1', '-i', 'in.mp4']));
      expect(args[args.indexOf('-t') + 1], '8.000');
      expect(args, contains('-an'));
    });
  });

  group('buildAmbientMusicArgs', () {
    test('bakes the detuned chord frequencies into the sine expression', () {
      final args = buildAmbientMusicArgs(
        chord: (130.81, 164.81, 196.00),
        detunes: [0.1, -0.2, 0.3],
        duration: 10.0,
        outputPath: 'music.m4a',
      );
      final input = args[args.indexOf('-i') + 1];
      expect(input, contains('sin(2*PI*130.91*t)'));
      expect(input, contains('sin(2*PI*164.61*t)'));
      expect(input, contains('sin(2*PI*196.3*t)'));
      final af = args[args.indexOf('-af') + 1];
      expect(af, contains('tremolo=f=0.15:d=0.08'));
      expect(af, contains('afade=t=in'));
      expect(af, contains('afade=t=out'));
    });

    test('uses a shorter fade for very short clips to avoid overlap', () {
      final args = buildAmbientMusicArgs(
        chord: (130.81, 164.81, 196.00),
        detunes: [0, 0, 0],
        duration: 4.0,
        outputPath: 'music.m4a',
      );
      final af = args[args.indexOf('-af') + 1];
      // fadeDur = 4/4 = 1.0s, fade-out should start at 3.0s.
      expect(af, contains('afade=t=in:st=0:d=1.000'));
      expect(af, contains('afade=t=out:st=3.000:d=1.000'));
    });
  });

  group('buildQuoteComposeArgs', () {
    test('dims the background and overlays the text layer when opacity > 0', () {
      final args = buildQuoteComposeArgs(
        backgroundPath: 'bg.mp4',
        textOverlayPath: 'text.png',
        audioPath: 'music.m4a',
        duration: 6.0,
        outputPath: 'out.mp4',
      );
      final filter = args[args.indexOf('-filter_complex') + 1];
      expect(filter, contains('color=black@0.350'));
      expect(filter, contains('[0:v][dim]overlay[bgdim]'));
      expect(filter, contains('[bgdim][1:v]overlay[outv]'));
      expect(args, containsAllInOrder(['-map', '[outv]']));
      expect(args, containsAllInOrder(['-map', '2:a']));
    });

    test('skips the dim layer entirely when opacity is 0', () {
      final args = buildQuoteComposeArgs(
        backgroundPath: 'bg.mp4',
        textOverlayPath: 'text.png',
        duration: 6.0,
        outputPath: 'out.mp4',
        overlayOpacity: 0,
      );
      final filter = args[args.indexOf('-filter_complex') + 1];
      expect(filter, isNot(contains('color=black')));
      expect(filter, '[0:v][1:v]overlay[outv]');
      expect(args, containsAllInOrder(['-map', '[outv]']));
      // No audio requested: no audio stream should be mapped or encoded.
      expect(args, isNot(contains('2:a')));
      expect(args, isNot(contains('-c:a')));
    });
  });

  group('buildVoiceoverComposeArgs', () {
    test('chains one overlay per subtitle, gated to its own time window', () {
      final timings = [
        const SubtitleChunk(text: 'a b', start: 0, end: 2),
        const SubtitleChunk(text: 'c d', start: 2, end: 4),
      ];
      final args = buildVoiceoverComposeArgs(
        backgroundPath: 'bg.mp4',
        subtitleOverlayPaths: ['s0.png', 's1.png'],
        timings: timings,
        narrationPath: 'voice.wav',
        musicPath: 'music.m4a',
        duration: 4.0,
        outputPath: 'out.mp4',
      );
      final filter = args[args.indexOf('-filter_complex') + 1];
      expect(filter, contains("between(t,0.000,2.000)"));
      expect(filter, contains("between(t,2.000,4.000)"));
      expect(filter, contains('[outv]'));
      expect(filter, contains('amix=inputs=2'));
      expect(args, containsAllInOrder(['-map', '[outv]']));
      expect(args, containsAllInOrder(['-map', '[outa]']));
    });

    test('maps the narration audio directly when no music is provided', () {
      final timings = [const SubtitleChunk(text: 'a', start: 0, end: 3)];
      final args = buildVoiceoverComposeArgs(
        backgroundPath: 'bg.mp4',
        subtitleOverlayPaths: ['s0.png'],
        timings: timings,
        narrationPath: 'voice.wav',
        duration: 3.0,
        outputPath: 'out.mp4',
      );
      // inputs: 0=bg, 1=s0.png, 2=narration -> narration index is 2.
      expect(args, containsAllInOrder(['-map', '2:a']));
      final filter = args[args.indexOf('-filter_complex') + 1];
      expect(filter, isNot(contains('amix')));
    });
  });

  group('buildConcatFileContent', () {
    test('quotes each path for the concat demuxer', () {
      final content = buildConcatFileContent(['/a/b.mp4', "/a/it's.mp4"]);
      expect(content, contains("file '/a/b.mp4'"));
      expect(content, contains("file '/a/it'\\''s.mp4'"));
    });
  });

  group('buildNormalizeClipSilentArgs', () {
    test('adds a silent audio track for clips without one', () {
      final args = buildNormalizeClipSilentArgs(
        inputPath: 'clip.mp4',
        outputPath: 'out.mp4',
        maxDuration: 5.0,
      );
      expect(args, containsAllInOrder(['-f', 'lavfi', '-i', 'anullsrc=r=44100:cl=stereo']));
      expect(args, contains('-shortest'));
    });
  });

  group('buildIntroOverlayArgs', () {
    test('gates the intro overlay to the requested duration and keeps original audio', () {
      final args = buildIntroOverlayArgs(
        inputPath: 'video.mp4',
        introPngPath: 'intro.png',
        introDuration: 3.0,
        outputPath: 'out.mp4',
      );
      final filter = args[args.indexOf('-filter_complex') + 1];
      expect(filter, contains("lt(t,3.000)"));
      expect(args, containsAllInOrder(['-map', '0:a?']));
    });
  });

  group('buildMixMusicUnderArgs', () {
    test('loops the music, scales its volume, and copies the video stream untouched', () {
      final args = buildMixMusicUnderArgs(
        inputVideoPath: 'video.mp4',
        musicPath: 'music.m4a',
        duration: 20.0,
        outputPath: 'out.mp4',
        musicVolume: 0.2,
      );
      expect(args, containsAllInOrder(['-stream_loop', '-1', '-t', '20.000', '-i', 'music.m4a']));
      final filter = args[args.indexOf('-filter_complex') + 1];
      expect(filter, contains('volume=0.200'));
      expect(filter, contains('amix=inputs=2'));
      expect(args, containsAllInOrder(['-c:v', 'copy']));
    });
  });
}
