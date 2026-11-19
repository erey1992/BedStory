package com.mamaoca.cuentos

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import android.content.Context
import android.graphics.Picture
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class SceneLayer(val picture: Picture, val animation: String, val phase: Float)
data class NativeScene(val background: Picture, val layers: List<SceneLayer>, val tint: Picture?)

object SceneLoader {
    private val cache = LruCache<String, NativeScene>(48)
    @Synchronized fun load(context: Context, asset: String): NativeScene {
        cache.get(asset)?.let { return it }
        val json = JSONObject(context.assets.open(asset).bufferedReader().use { it.readText() })
        fun picture(xml: String) = SVG.getFromString(xml).renderToPicture(600, 500)
        val layers = json.getJSONArray("layers")
        return NativeScene(picture(json.getString("background")), List(layers.length()) { i ->
            val layer = layers.getJSONObject(i)
            SceneLayer(picture(layer.getString("svg")), layer.getString("animation"), layer.getDouble("phase").toFloat())
        }, if (json.isNull("tint")) null else picture(json.getString("tint"))).also { cache.put(asset, it) }
    }
}

/** Encuadre de la escena (600x500): en páginas anchas recorta cielo, en páginas altas muestra más. */
private fun frame(ratio: Float): FloatArray {
    val w: Float; val h: Float
    if (ratio >= 1.2f) { h = 430f; w = min(600f, h * ratio) } else { w = 560f; h = min(500f, w / ratio) }
    return floatArrayOf(300f - w / 2, 500f - h, w, h)
}

@Composable
fun Illustration(asset: String, description: String, modifier: Modifier = Modifier, animated: Boolean = false) {
    val context = LocalContext.current
    val scene by produceState<NativeScene?>(null, asset) {
        value = withContext(Dispatchers.IO) { SceneLoader.load(context.applicationContext, asset) }
    }
    var time by remember { mutableFloatStateOf(0f) }
    if (animated) LaunchedEffect(Unit) {
        val start = withInfiniteAnimationFrameNanos { it }
        while (isActive) withInfiniteAnimationFrameNanos { time = (it - start) / 1e9f }
    }
    Canvas(modifier.semantics { contentDescription = description }) {
        val current = scene ?: return@Canvas
        val (vx, vy, vw, vh) = frame(size.width / size.height).toList()
        val k = max(size.width / vw, size.height / vh)
        clipRect {
            drawIntoCanvas { c ->
                val nc = c.nativeCanvas
                nc.save()
                nc.translate((size.width - vw * k) / 2 - vx * k, size.height - vh * k - vy * k)
                nc.scale(k, k)
                nc.drawPicture(current.background)
                current.layers.forEach { layer ->
                    nc.save()
                    if (animated) {
                        val t = time + layer.phase
                        fun wave(period: Float) = sin(2 * PI * t / period).toFloat()
                        when (layer.animation) {
                            "idle" -> nc.translate(0f, wave(3.2f) * 1.6f)
                            "bob" -> nc.translate(0f, -4.5f + wave(1.4f) * 4.5f)
                            "float" -> nc.translate(0f, -6f + wave(3.6f) * 6f)
                            "swim" -> nc.translate(7f + wave(4f) * 7f, 0f)
                            "sway" -> nc.rotate(wave(3f) * 2f, 300f, 450f)
                        }
                    }
                    nc.drawPicture(layer.picture)
                    nc.restore()
                }
                current.tint?.let { nc.drawPicture(it) }
                nc.restore()
            }
        }
    }
}
