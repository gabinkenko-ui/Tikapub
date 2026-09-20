import wave

from tikapub.generators.defaults import generate_default_ambient_music


def test_generate_default_ambient_music_is_deterministic_with_seed(tmp_path):
    path_a = generate_default_ambient_music(tmp_path / "a.wav", duration=1.0, seed=123)
    path_b = generate_default_ambient_music(tmp_path / "b.wav", duration=1.0, seed=123)
    assert path_a.read_bytes() == path_b.read_bytes()


def test_generate_default_ambient_music_varies_with_seed(tmp_path):
    path_a = generate_default_ambient_music(tmp_path / "a.wav", duration=1.0, seed=1)
    path_b = generate_default_ambient_music(tmp_path / "b.wav", duration=1.0, seed=2)
    assert path_a.read_bytes() != path_b.read_bytes()


def test_generate_default_ambient_music_produces_valid_wav(tmp_path):
    sample_rate = 44100
    duration = 2.0
    path = generate_default_ambient_music(tmp_path / "music.wav", duration=duration, seed=0, sample_rate=sample_rate)

    with wave.open(str(path), "rb") as wav_file:
        assert wav_file.getnchannels() == 1
        assert wav_file.getsampwidth() == 2
        assert wav_file.getframerate() == sample_rate
        expected_frames = int(sample_rate * duration)
        assert wav_file.getnframes() == expected_frames


def test_generate_default_ambient_music_no_clipping(tmp_path):
    import numpy as np

    path = generate_default_ambient_music(tmp_path / "music.wav", duration=1.0, seed=5)
    with wave.open(str(path), "rb") as wav_file:
        frames = wav_file.readframes(wav_file.getnframes())
    samples = np.frombuffer(frames, dtype=np.int16)
    assert np.max(np.abs(samples)) < 32767
