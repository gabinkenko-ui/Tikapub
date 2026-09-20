import math
from unittest.mock import MagicMock, patch

import pytest

from tikapub.publish.tiktok_client import (
    MAX_CHUNK_SIZE,
    MIN_CHUNK_SIZE,
    TikTokAPIError,
    TikTokClient,
    _plan_upload,
)


def test_plan_upload_single_chunk_for_small_video():
    plan = _plan_upload(1_000_000)
    assert plan.total_chunk_count == 1
    assert plan.chunk_size == 1_000_000


def test_plan_upload_exact_multiple_of_chunk_size():
    video_size = MIN_CHUNK_SIZE * 3
    plan = _plan_upload(video_size)
    assert plan.chunk_size == MIN_CHUNK_SIZE
    assert plan.total_chunk_count == 3


def test_plan_upload_total_chunk_count_matches_actual_iterations():
    """total_chunk_count doit être le nombre exact de PUT que fera upload_video_file."""
    video_size = MIN_CHUNK_SIZE * 2 + 123  # dernier chunk minuscule, autorisé par l'API
    plan = _plan_upload(video_size)

    offset = 0
    iterations = 0
    while offset < plan.video_size:
        chunk_end = min(offset + plan.chunk_size, plan.video_size) - 1
        offset = chunk_end + 1
        iterations += 1

    assert iterations == plan.total_chunk_count
    assert plan.chunk_size <= MAX_CHUNK_SIZE


@pytest.mark.parametrize("video_size", [1, MIN_CHUNK_SIZE - 1, MIN_CHUNK_SIZE, MIN_CHUNK_SIZE + 1, 200_000_000])
def test_plan_upload_matches_ceil_division(video_size):
    plan = _plan_upload(video_size)
    if video_size <= MIN_CHUNK_SIZE:
        assert plan.total_chunk_count == 1
    else:
        assert plan.total_chunk_count == math.ceil(video_size / MIN_CHUNK_SIZE)


def test_raise_for_error_raises_on_error_payload():
    from tikapub.publish.tiktok_client import _raise_for_error

    response = MagicMock(status_code=200)
    response.json.return_value = {"error": {"code": "invalid_param", "message": "bad"}}
    with pytest.raises(TikTokAPIError):
        _raise_for_error(response)


def test_raise_for_error_passes_through_ok_payload():
    from tikapub.publish.tiktok_client import _raise_for_error

    response = MagicMock(status_code=200)
    response.json.return_value = {"data": {"publish_id": "abc"}, "error": {"code": "ok"}}
    assert _raise_for_error(response) == {"data": {"publish_id": "abc"}, "error": {"code": "ok"}}


def test_exchange_code_for_token_stores_tokens():
    client = TikTokClient(client_key="k", client_secret="s", redirect_uri="http://localhost/cb")

    fake_response = MagicMock(status_code=200)
    fake_response.json.return_value = {
        "access_token": "AT",
        "refresh_token": "RT",
        "error": {"code": "ok"},
    }
    with patch("tikapub.publish.tiktok_client.requests.post", return_value=fake_response) as mock_post:
        data = client.exchange_code_for_token("some-code")

    assert data["access_token"] == "AT"
    assert client.access_token == "AT"
    assert client.refresh_token == "RT"
    mock_post.assert_called_once()


def test_publish_video_requires_access_token():
    client = TikTokClient(client_key="k", client_secret="s")
    with pytest.raises(ValueError):
        client._auth_headers()
