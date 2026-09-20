from tikapub.config import load_settings


def test_load_settings_creates_output_dir(tmp_path, monkeypatch):
    monkeypatch.setenv("TIKAPUB_OUTPUT_DIR", str(tmp_path / "out"))
    monkeypatch.setenv("TIKAPUB_ASSETS_DIR", str(tmp_path / "assets"))
    settings = load_settings()

    assert settings.output_dir == tmp_path / "out"
    assert settings.output_dir.is_dir()
    assert settings.backgrounds_dir == settings.assets_dir / "backgrounds"


def test_load_settings_reads_tiktok_credentials(monkeypatch):
    monkeypatch.setenv("TIKTOK_CLIENT_KEY", "abc")
    monkeypatch.setenv("TIKTOK_CLIENT_SECRET", "xyz")
    settings = load_settings()

    assert settings.tiktok_client_key == "abc"
    assert settings.tiktok_client_secret == "xyz"
