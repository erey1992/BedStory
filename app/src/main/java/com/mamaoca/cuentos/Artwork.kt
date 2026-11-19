package com.mamaoca.cuentos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object ArtLoader {
    private val cache = LruCache<String, ImageBitmap>(24)
    @Synchronized fun load(context: Context, asset: String): ImageBitmap =
        cache.get(asset) ?: context.assets.open(asset).use { BitmapFactory.decodeStream(it) }.asImageBitmap()
            .also { cache.put(asset, it) }
}

/** Grano de papel: ruido muy suave que se repite sobre las páginas. */
private val paperGrain: ImageBitmap by lazy {
    val size = 160
    val rnd = Random(11)
    val pixels = IntArray(size * size) {
        val v = rnd.nextInt(256)
        val alpha = 6 + rnd.nextInt(10)
        (alpha shl 24) or (if (v > 128) 0x8A6E4B else 0xFFFFFF)
    }
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}
private val paperColors = listOf(Color(0xFFFFFBF2), Color(0xFFF7EDD8))

/** Fondo de página impresa: papel crema con grano. */
fun Modifier.paper(): Modifier = this
    .background(Brush.verticalGradient(paperColors))
    .drawBehind { drawRect(ShaderBrush(ImageShader(paperGrain, TileMode.Repeated, TileMode.Repeated))) }

/** Ilustración de una página (0 = portada): la pintada si existe, si no la escena vectorial. */
@Composable
fun StoryArt(story: Story, index: Int, description: String, modifier: Modifier = Modifier,
             animated: Boolean = false, thumbnail: Boolean = false) {
    val image = if (index == 0) story.coverImage else story.pages[index - 1].image
    if (image == null) {
        val scene = if (index == 0) story.cover else story.pages[index - 1].scene
        if (scene != null) Illustration(scene, description, modifier, animated) else Ornament(story, index, description, modifier, thumbnail)
        return
    }
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, image) {
        value = withContext(Dispatchers.IO) { runCatching { ArtLoader.load(context.applicationContext, image) }.getOrNull() }
    }
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val bmp = bitmap ?: return@Box
        if (thumbnail) Image(bmp, description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        // impresa: la ilustración completa, con la tinta un poco absorbida por el papel
        else Image(bmp, description, Modifier.fillMaxSize().graphicsLayer { alpha = 0.97f }, contentScale = ContentScale.Fit)
    }
}

/** La ilustración impresa sobre la página, con margen de papel alrededor. */
@Composable
fun PrintedArt(story: Story, index: Int, description: String, modifier: Modifier = Modifier) {
    Box(modifier.paper(), contentAlignment = Alignment.Center) {
        StoryArt(story, index, description, Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp))
    }
}

/** Viñeta impresa para las páginas que aún no tienen ilustración: la inicial del cuento en un marco. */
@Composable
private fun Ornament(story: Story, index: Int, description: String, modifier: Modifier, thumbnail: Boolean) {
    val accent = storyColor(story)
    BoxWithConstraints(modifier.then(if (thumbnail) Modifier.paper().background(accent.copy(alpha = 0.12f)) else Modifier)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        val side = min(maxWidth, maxHeight) * (if (thumbnail) 0.8f else 0.62f)
        Box(Modifier.size(side).border(1.5.dp, accent.copy(alpha = 0.6f)).padding(5.dp).border(1.dp, accent.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center) {
            Text(story.title.first { it.isLetter() }.uppercase(), fontFamily = Andika, fontWeight = FontWeight.Bold,
                fontSize = (side.value * 0.55f).sp, color = accent)
            if (!thumbnail) Text("❦  ❦  ❦".takeLast(if (index == 0) 7 else 1), color = accent.copy(alpha = 0.7f), fontSize = (side.value * 0.08f).sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = side * 0.06f))
        }
    }
}
