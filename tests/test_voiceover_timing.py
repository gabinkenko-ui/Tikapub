from tikapub.generators.voiceover import _subtitle_chunks_with_timing


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
