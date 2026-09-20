package com.gabinkenko.tikapub.tiktok

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Bridges the OAuth redirect intent (received in MainActivity) to whichever screen is listening. */
object AuthEventBus {
    private val _events = MutableSharedFlow<TikTokAuthManager.CallbackResult>(extraBufferCapacity = 1)
    val events: SharedFlow<TikTokAuthManager.CallbackResult> = _events

    suspend fun emit(result: TikTokAuthManager.CallbackResult) {
        _events.emit(result)
    }
}
