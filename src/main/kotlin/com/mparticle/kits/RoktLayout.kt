package com.mparticle.kits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mparticle.MpRoktEventCallback
import com.rokt.roktsdk.Rokt

@Composable
fun RoktLayout(
    sdkTriggered: Boolean,
    viewName: String,
    attributes: Map<String, String>,
    location: String,
    modifier: Modifier = Modifier,
    mpRoktEventCallback: MpRoktEventCallback? = null,
) {
    val instance = RoktKit.instance
    val resultMapState = remember { mutableStateOf<RoktResult?>(null) }
    LaunchedEffect(Unit) {
        instance?.runComposableWithCallback(attributes, mpRoktEventCallback, { resultMap, callback ->
            resultMapState.value = RoktResult(resultMap, callback)
        })
    }

    resultMapState.value?.let { resultMap ->
        com.rokt.roktsdk.RoktLayout(
            sdkTriggered, viewName, modifier, resultMap.attributes, location,
            onLoad = { resultMap.callback.onLoad() },
            onShouldShowLoadingIndicator = { resultMap.callback.onShouldShowLoadingIndicator() },
            onShouldHideLoadingIndicator = { resultMap.callback.onShouldHideLoadingIndicator() },
            onUnload = { reason -> resultMap.callback.onUnload(reason) })
    }
}

data class RoktResult(val attributes: Map<String, String>, val callback: Rokt.RoktCallback)
