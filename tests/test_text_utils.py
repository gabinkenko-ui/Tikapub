from pathlib import Path

from tikapub.utils.text import load_font, split_into_chunks, wrap_text


def test_wrap_text_respects_max_width():
    font = load_font(Path("/nonexistent"), None, 40)
    text = "Le succès n'est pas final, l'échec n'est pas fatal, c'est le courage de continuer."
    lines = wrap_text(text, font, max_width=300)

    assert len(lines) > 1
    for line in lines:
        width = font.getbbox(line)[2]
        assert width <= 300
    # Aucun mot ne doit être perdu lors du découpage.
    assert " ".join(lines).split() == text.split()


def test_wrap_text_single_line_when_width_is_large():
    font = load_font(Path("/nonexistent"), None, 40)
    lines = wrap_text("Petit texte", font, max_width=5000)
    assert lines == ["Petit texte"]


def test_wrap_text_empty_string_returns_no_lines():
    font = load_font(Path("/nonexistent"), None, 40)
    assert wrap_text("", font, max_width=300) == []


def test_split_into_chunks_groups_words():
    words = ["un", "deux", "trois", "quatre", "cinq"]
    assert split_into_chunks(words, 2) == [["un", "deux"], ["trois", "quatre"], ["cinq"]]


def test_split_into_chunks_rejects_non_positive_size():
    import pytest

    with pytest.raises(ValueError):
        split_into_chunks(["a"], 0)
