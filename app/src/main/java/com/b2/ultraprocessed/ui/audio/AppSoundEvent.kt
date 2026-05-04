package com.b2.ultraprocessed.ui.audio

import androidx.annotation.RawRes
import com.b2.ultraprocessed.R

enum class AppSoundEvent(@param:RawRes val resId: Int) {
    Startup(R.raw.zest_app_open),
    Click(R.raw.zest_ui_click),
    Success(R.raw.zest_ui_success),
    Error(R.raw.zest_ui_error),
}
