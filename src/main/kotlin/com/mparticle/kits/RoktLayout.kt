package com.mparticle.kits

import android.graphics.Typeface
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mparticle.MpRoktEventCallback
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import com.rokt.roktsdk.Rokt
import java.lang.ref.WeakReference

@Composable
fun RoktLayout(
    sdkTriggered: Boolean,
    viewName: String,
    modifier: Modifier,
    attributes: Map<String, String>,
    location: String,
    mpRoktEventCallback: MpRoktEventCallback?
) {
    val instance = RoktKit.get()

    data class RoktResult(val attributes: Map<String, String>, val callback: Rokt.RoktCallback)

    val resultMapState = remember { mutableStateOf<RoktResult?>(null) }
    val trigger = remember { mutableStateOf(0) }
    LaunchedEffect(trigger.value) {
        instance?.ComposableTest(attributes, mpRoktEventCallback, { resultMap, callback ->
            resultMapState.value = RoktResult(resultMap, callback)
        })
    }

    resultMapState.value?.let { resultMap ->
        com.rokt.roktsdk.RoktLayout(sdkTriggered, viewName, modifier, resultMap.attributes, location)
    }
}