from tikapub.generators.voiceover import (
    _align_script_words,
    _group_aligned_words,
    _resolve_subtitle_timings,
    _subtitle_chunks_with_timing,
)


def test_subtitle_chunks_cover_full_duration_without_gaps():
    script = "un deux trois quatre cinq six sept huit"
    timings = _subtitle_chunks_with_timing(script, total_duration=10.0, chunk_size=2)

    assert timings[0][1] == 0.0
    assert timings[-1][2] == 10.0
    for (_, _, end), (_, next_start, _) in zip(timings, timings[1:]):
        assert end == next_start


def test_subtitle_chunks_preserve_all_words_in_order():
    script = "a b c d e"
    timings = _subtitle_chunks_with_timing(script, total_duration=5.0, chunk_size=2)
    reconstructed = " ".join(text for text, _, _ in timings)
    assert reconstructed == "a b c d e"


def test_longer_chunk_gets_more_time_than_shorter_chunk():
    script = "court mot-beaucoup-plus-long-que-le-precedent"
    timings = _subtitle_chunks_with_timing(script, total_duration=10.0, chunk_size=1)
    (_, _, first_end), (_, second_start, second_end) = timings
    assert (second_end - second_start) > first_end


def test_align_script_words_matches_word_counts_one_to_one():
    script = "un deux trois"
    word_timings = [("un", 0.0, 0.4), ("deux", 0.4, 0.9), ("trois", 0.9, 1.5)]
    aligned = _align_script_words(script, word_timings)
    assert aligned == [("un", 0.0, 0.4), ("deux", 0.4, 0.9), ("trois", 0.9, 1.5)]


def test_align_script_words_keeps_original_text_when_whisper_word_differs():
    script = "Bonjour, ça va ?"
    word_timings = [("bonjour", 0.0, 0.5), ("ça", 0.5, 0.8), ("va", 0.8, 1.2)]
    aligned = _align_script_words(script, word_timings)
    assert [w for w, _, _ in aligned] == ["Bonjour,", "ça", "va", "?"]


def test_align_script_words_returns_none_when_word_counts_diverge_too_much():
    script = "un deux trois quatre cinq six"
    word_timings = [("un", 0.0, 0.4)]
    assert _align_script_words(script, word_timings) is None


def test_align_script_words_returns_none_without_transcription():
    assert _align_script_words("un deux", []) is None


def test_group_aligned_words_spans_first_to_last_word_of_each_chunk():
    aligned = [
        ("un", 0.0, 0.4),
        ("deux", 0.4, 0.9),
        ("trois", 0.9, 1.5),
        ("quatre", 1.5, 2.0),
    ]
    grouped = _group_aligned_words(aligned, chunk_size=2)
    assert grouped == [("un deux", 0.0, 0.9), ("trois quatre", 0.9, 2.0)]


def test_resolve_subtitle_timings_falls_back_when_whisper_unavailable(tmp_path):
    script = "un deux trois quatre"
    timings = _resolve_subtitle_timings(
        script,
        tmp_path / "missing.mp3",
        duration=4.0,
        chunk_size=2,
        align_subtitles=True,
        whisper_model="base",
        tts_lang="fr",
    )
    assert timings == _subtitle_chunks_with_timing(script, total_duration=4.0, chunk_size=2)


def test_resolve_subtitle_timings_uses_proportional_estimate_when_disabled(tmp_path):
    script = "un deux trois quatre"
    timings = _resolve_subtitle_timings(
        script,
        tmp_path / "missing.mp3",
        duration=4.0,
        chunk_size=2,
        align_subtitles=False,
        whisper_model="base",
        tts_lang="fr",
    )
    assert timings == _subtitle_chunks_with_timing(script, total_duration=4.0, chunk_size=2)
