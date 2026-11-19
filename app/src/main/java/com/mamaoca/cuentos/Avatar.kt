package com.mamaoca.cuentos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** El personaje que se crea el niño: aparece como primera calcomanía. */
const val AVATAR_ID = "st-mi-personaje"

enum class Sex(val key: String, val label: String) { Boy("boy", "Niño"), Girl("girl", "Niña") }

/** Una parte del personaje (cuerpo con ropa, cara, peinado o accesorio). [adjust]: corrección fina de posición y tamaño. */
data class AvatarPart(val id: String, val name: String, val image: String, val extra: String? = null,
                      val head: RectF? = null, val kind: String? = null, val girlsOnly: Boolean = false,
                      val dx: Float = 0f, val dy: Float = 0f, val scale: Float = 1f,
                      /** Peinados: centro del hueco de la cara y línea de la frente, en fracciones de la imagen. */
                      val anchorX: Float = 0.5f, val anchorY: Float = 0.3f,
                      /** Peinados: la misma imagen en cada color de pelo. */
                      val colors: List<String> = emptyList())

class AvatarCatalog(json: JSONObject) {
    private fun parts(obj: JSONObject?, key: String): List<AvatarPart> {
        val list = obj?.optJSONArray(key) ?: return emptyList()
        return List(list.length()) { i ->
            val p = list.getJSONObject(i)
            val head = p.optJSONArray("head")?.let { RectF(it.getDouble(0).toFloat(), it.getDouble(1).toFloat(),
                (it.getDouble(0) + it.getDouble(2)).toFloat(), (it.getDouble(1) + it.getDouble(3)).toFloat()) }
            val adj = p.optJSONArray("adjust")
            val anchor = p.optJSONArray("anchor")
            AvatarPart(p.getString("id"), p.getString("name"), p.getString("image"),
                p.optString("skin").ifEmpty { p.optString("iris").ifEmpty { null } }, head,
                p.optString("kind").ifEmpty { null }, p.optBoolean("girlsOnly", false),
                adj?.getDouble(0)?.toFloat() ?: 0f, adj?.getDouble(1)?.toFloat() ?: 0f, adj?.getDouble(2)?.toFloat() ?: 1f,
                anchor?.getDouble(0)?.toFloat() ?: 0.5f, anchor?.getDouble(1)?.toFloat() ?: 0.3f,
                p.optJSONArray("colors")?.let { c -> List(c.length()) { c.getString(it) } } ?: emptyList())
        }
    }
    val bodies = Sex.entries.associateWith { parts(json.optJSONObject("bodies"), it.key) }
    val faces = Sex.entries.associateWith { parts(json.optJSONObject("faces"), it.key) }
    val hair = Sex.entries.associateWith { parts(json.optJSONObject("hair"), it.key) }
    private val accessories = parts(json, "accessories")
    fun accessories(kind: String, sex: Sex) = accessories.filter { it.kind == kind && (!it.girlsOnly || sex == Sex.Girl) }

    companion object {
        fun load(context: Context) = AvatarCatalog(JSONObject(context.assets.open("avatar/avatar.json").bufferedReader().use { it.readText() }))
    }
}

/** Lo que eligió el niño. Los accesorios son el índice dentro de su tipo, o -1 si no lleva. */
data class AvatarSpec(val sex: Sex = Sex.Boy, val skin: Int = 1, val eyes: Int = 0, val face: Int = 0, val hair: Int = 0,
                      val outfit: Int = 0, val cap: Int = -1, val glasses: Int = -1, val earrings: Int = -1, val hairColor: Int = 0) {
    fun encode() = listOf(sex.key, skin, eyes, face, hair, outfit, cap, glasses, earrings, hairColor).joinToString(",")

    companion object {
        val SKINS = listOf(0xFFFBDCC6, 0xFFF0C09C, 0xFFD69C6E, 0xFFA96D45, 0xFF6E432A).map { it.toInt() }
        val SKIN_NAMES = listOf("Muy clara", "Clara", "Morena clara", "Morena", "Oscura")
        val EYES = listOf(0xFF6B3E1E, 0xFF2B1D14, 0xFF3C7DD9, 0xFF3E9B4F, 0xFFB07A2A, 0xFF8A97A3).map { it.toInt() }
        val EYE_NAMES = listOf("Café", "Negros", "Azules", "Verdes", "Miel", "Grises")
        /** Colores de pelo, en el orden de las imágenes de cada peinado (tools/crop-avatar.py). */
        val HAIR = listOf(0xFF6B3F22, 0xFF2A211C, 0xFFA27A52, 0xFFEBCB6B, 0xFFC8562A, 0xFFF07BBE).map { it.toInt() }
        val HAIR_NAMES = listOf("Castaño", "Negro", "Castaño claro", "Rubio", "Pelirrojo", "Rosa")
        fun decode(s: String?): AvatarSpec? = runCatching {
            val p = s!!.split(',')
            AvatarSpec(Sex.entries.first { it.key == p[0] }, p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toInt(),
                p[6].toInt(), p[7].toInt(), p[8].toInt(), p.getOrNull(9)?.toInt() ?: 0)
        }.getOrNull()
    }

    /** Ajusta las elecciones a lo que existe para ese sexo (al cambiar de niño a niña, por ejemplo). */
    fun fit(c: AvatarCatalog): AvatarSpec {
        fun idx(i: Int, n: Int) = if (n == 0) 0 else i.coerceIn(0, n - 1)
        fun opt(i: Int, n: Int) = if (i >= n) -1 else i
        return copy(skin = skin.coerceIn(0, SKINS.lastIndex), eyes = eyes.coerceIn(0, EYES.lastIndex), hairColor = hairColor.coerceIn(0, HAIR.lastIndex),
            face = idx(face, c.faces[sex]!!.size), hair = idx(hair, c.hair[sex]!!.size), outfit = idx(outfit, c.bodies[sex]!!.size),
            cap = opt(cap, c.accessories("cap", sex).size), glasses = opt(glasses, c.accessories("glasses", sex).size),
            earrings = opt(earrings, c.accessories("earrings", sex).size))
    }
}

/** Junta las capas del personaje y le pone el borde blanco de calcomanía. */
object AvatarRenderer {
    private val cache = HashMap<String, Bitmap>()

    @Synchronized fun asset(context: Context, path: String): Bitmap =
        cache.getOrPut(path) { context.assets.open(path).use { BitmapFactory.decodeStream(it) } }

    fun render(context: Context, c: AvatarCatalog, spec: AvatarSpec, maxSide: Int = 600): Bitmap? {
        val s = spec.fit(c)
        val body = c.bodies[s.sex]!!.getOrNull(s.outfit) ?: return null
        val bodyBmp = asset(context, body.image)
        val mx = bodyBmp.width * 0.3f; val my = bodyBmp.height * 0.15f
        val out = Bitmap.createBitmap((bodyBmp.width + 2 * mx).toInt(), (bodyBmp.height + my * 1.2f).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bodyBmp, mx, my, paint)
        fun tinted(color: Int) = Paint(paint).apply { colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY) }
        body.extra?.let { canvas.drawBitmap(asset(context, it), mx, my, tinted(AvatarSpec.SKINS[s.skin])) }
        val head = RectF(body.head ?: RectF(0f, 0f, bodyBmp.width.toFloat(), bodyBmp.width.toFloat())).apply { offset(mx, my) }
        val hw = head.width(); val hh = min(head.height(), hw * 1.1f)

        /** Dibuja una parte de ancho [w] (en anchos de cabeza), centrada en x, con su borde de arriba en [top] (o su centro en [cy]). */
        fun place(part: AvatarPart, path: String, w: Float, top: Float? = null, cy: Float? = null, bottom: Float? = null, p: Paint = paint) {
            val bmp = asset(context, path)
            val width = w * hw * part.scale
            val height = width * bmp.height / bmp.width
            val y = when { top != null -> top; cy != null -> cy - height / 2; else -> bottom!! - height } + part.dy * hw
            val x = head.centerX() - width / 2 + part.dx * hw
            canvas.drawBitmap(bmp, null, RectF(x, y, x + width, y + height), p)
        }
        c.faces[s.sex]!!.getOrNull(s.face)?.let { f ->
            place(f, f.image, 0.62f, cy = head.top + 0.58f * hh)
            f.extra?.let { place(f, it, 0.62f, cy = head.top + 0.58f * hh, p = tinted(AvatarSpec.EYES[s.eyes])) }
        }
        // aretes: la imagen trae el par; cada mitad va en su oreja
        c.accessories("earrings", s.sex).getOrNull(s.earrings)?.let { e ->
            val bmp = asset(context, e.image)
            val half = bmp.width / 2
            val w = 0.13f * hw
            val h = w * bmp.height / half
            val cy = head.top + 0.68f * hh
            for ((side, cx) in listOf(0 to head.left + 0.12f * hw, 1 to head.right - 0.12f * hw)) {
                canvas.drawBitmap(bmp, android.graphics.Rect(side * half, 0, (side + 1) * half, bmp.height),
                    RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), paint)
            }
        }
        // peinado: el hueco de la cara va sobre la cara y la línea de la frente a un tercio de la cabeza
        c.hair[s.sex]!!.getOrNull(s.hair)?.let { hr ->
            val bmp = asset(context, hr.colors.getOrNull(s.hairColor) ?: hr.image)
            val width = 1.14f * hw * hr.scale
            val height = width * bmp.height / bmp.width
            val x = head.centerX() - hr.anchorX * width + hr.dx * hw
            val y = head.top + 0.30f * hh - hr.anchorY * height + hr.dy * hw
            canvas.drawBitmap(bmp, null, RectF(x, y, x + width, y + height), paint)
        }
        c.accessories("glasses", s.sex).getOrNull(s.glasses)?.let { place(it, it.image, 0.8f, cy = head.top + 0.5f * hh) }
        c.accessories("cap", s.sex).getOrNull(s.cap)?.let { place(it, it.image, 1.0f, bottom = head.top + 0.36f * hh) }
        return sticker(out, maxSide)
    }

    /** Recorta lo transparente, reduce y le pone el borde blanco grueso, como a las otras calcomanías. */
    private fun sticker(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width; val h = src.height
        val px = IntArray(w * h).also { src.getPixels(it, 0, w, 0, 0, w, h) }
        var l = w; var t = h; var r = -1; var b = -1
        for (y in 0 until h) for (x in 0 until w) if ((px[y * w + x] ushr 24) > 16) {
            if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > b) b = y
        }
        if (r < 0) return src
        val k = min(1f, (maxSide * 0.92f) / maxOf(r - l + 1, b - t + 1))
        val cw = ((r - l + 1) * k); val ch = ((b - t + 1) * k)
        val border = maxSide * 0.025f
        val out = Bitmap.createBitmap((cw + 2 * border + 4).toInt(), (ch + 2 * border + 4).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val dst = RectF(border + 2, border + 2, border + 2 + cw, border + 2 + ch)
        val srcRect = android.graphics.Rect(l, t, r + 1, b + 1)
        val white = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN) }
        for (i in 0 until 24) {
            val a = i * Math.PI * 2 / 24
            val o = RectF(dst).apply { offset((cos(a) * border).toFloat(), (sin(a) * border).toFloat()) }
            canvas.drawBitmap(src, srcRect, o, white)
        }
        canvas.drawBitmap(src, srcRect, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return out
    }

    fun file(context: Context) = File(context.filesDir, "avatar.png")
}

// ---------------- Pantalla «Mi personaje» ----------------

private enum class AvatarTab(val label: String) { Skin("Piel"), Eyes("Ojos"), Face("Cara"), Hair("Peinado"), Outfit("Ropa"), Extras("Accesorios") }

@Composable
fun ProfileScreen(vm: StoryViewModel, play: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val catalog = remember { runCatching { AvatarCatalog.load(context) }.getOrNull() }
    var spec by remember { mutableStateOf(vm.avatarSpec ?: AvatarSpec()) }
    var tab by remember { mutableStateOf(AvatarTab.Skin) }
    val preview by produceState<ImageBitmap?>(null, spec, catalog) {
        value = withContext(Dispatchers.Default) { catalog?.let { AvatarRenderer.render(context, it, spec)?.asImageBitmap() } }
    }
    fun done() { if (catalog != null) vm.saveAvatar(spec); play(); vm.back() }
    Column(Modifier.fillMaxSize().velvet(PlumLight)) {
        GameHeader("Mi personaje", Plum, onBack = ::done, icon = Icons.Default.Face) {
            GoldCircleButton(if (vm.sound) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                if (vm.sound) "Desactivar sonidos" else "Activar sonidos") { vm.toggleSound(); play() }
            PillAction(Icons.Default.Check, "¡Listo!", gold = true, onClick = ::done)
        }
        if (catalog == null) return@Column
        Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // el personaje, en grande
            Box(Modifier.fillMaxHeight().weight(0.36f).shadow(10.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                .paper().goldFrame(20.dp, 2.dp).padding(12.dp), contentAlignment = Alignment.Center) {
                preview?.let { Image(it, "Tu personaje", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                    ?: CircularProgressIndicator(color = GoldLeaf)
            }
            Column(Modifier.weight(0.64f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Sex.entries.forEach { s ->
                        val on = spec.sex == s
                        Box(Modifier.clip(RoundedCornerShape(12.dp)).then(if (on) Modifier.background(GoldBrush) else Modifier.background(Color(0x55000000)))
                            .goldBorder(RoundedCornerShape(12.dp), 1.dp).clickable(role = Role.Tab) {
                                play(); spec = spec.copy(sex = s, earrings = if (s == Sex.Boy) -1 else spec.earrings).fit(catalog)
                            }.padding(horizontal = 22.dp, vertical = 8.dp)) {
                            Text(s.label, color = if (on) Plum else GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
                GoldTabs(AvatarTab.entries.map { it.label }, tab.ordinal, Modifier.fillMaxWidth(), fill = true) { play(); tab = AvatarTab.entries[it] }
                val choose: (AvatarSpec) -> Unit = { play(); spec = it.fit(catalog) }
                LazyVerticalGrid(GridCells.Adaptive(if (tab == AvatarTab.Skin || tab == AvatarTab.Eyes) 74.dp else 104.dp),
                    Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (tab) {
                        AvatarTab.Skin -> items(AvatarSpec.SKINS.indices.toList()) { i ->
                            Swatch(AvatarSpec.SKINS[i], AvatarSpec.SKIN_NAMES[i], spec.skin == i) { choose(spec.copy(skin = i)) }
                        }
                        AvatarTab.Eyes -> items(AvatarSpec.EYES.indices.toList()) { i ->
                            Swatch(AvatarSpec.EYES[i], "Ojos ${AvatarSpec.EYE_NAMES[i].lowercase()}", spec.eyes == i) { choose(spec.copy(eyes = i)) }
                        }
                        AvatarTab.Face -> items(catalog.faces[spec.sex]!!.indices.toList()) { i ->
                            PartTile(catalog.faces[spec.sex]!![i], spec.face == i) { choose(spec.copy(face = i)) }
                        }
                        AvatarTab.Hair -> {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AvatarSpec.HAIR.forEachIndexed { i, color ->
                                        SmallSwatch(color, "Pelo ${AvatarSpec.HAIR_NAMES[i].lowercase()}", spec.hairColor == i) { choose(spec.copy(hairColor = i)) }
                                    }
                                }
                            }
                            items(catalog.hair[spec.sex]!!.indices.toList()) { i ->
                                val part = catalog.hair[spec.sex]!![i]
                                PartTile(part, spec.hair == i, part.colors.getOrNull(spec.hairColor) ?: part.image) { choose(spec.copy(hair = i)) }
                            }
                        }
                        AvatarTab.Outfit -> items(catalog.bodies[spec.sex]!!.indices.toList()) { i ->
                            PartTile(catalog.bodies[spec.sex]!![i], spec.outfit == i) { choose(spec.copy(outfit = i)) }
                        }
                        AvatarTab.Extras -> {
                            listOf(Triple("cap", "Gorra", spec.cap), Triple("glasses", "Lentes", spec.glasses), Triple("earrings", "Aretes", spec.earrings))
                                .forEach { (kind, title, current) ->
                                    val list = catalog.accessories(kind, spec.sex)
                                    if (list.isEmpty()) return@forEach
                                    fun set(i: Int) = when (kind) { "cap" -> spec.copy(cap = i); "glasses" -> spec.copy(glasses = i); else -> spec.copy(earrings = i) }
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Text(title, color = GoldPale, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    item { NoneTile(current == -1) { choose(set(-1)) } }
                                    items(list.indices.toList()) { i -> PartTile(list[i], current == i) { choose(set(i)) } }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Int, name: String, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(70.dp).clip(CircleShape).background(Color(color))
        .border(if (on) 5.dp else 2.dp, if (on) GoldLeaf else Color(0x88FFFFFF), CircleShape)
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = name }, contentAlignment = Alignment.Center) {
        if (on) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun SmallSwatch(color: Int, name: String, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(46.dp).clip(CircleShape).background(Color(color))
        .border(if (on) 4.dp else 2.dp, if (on) GoldLeaf else Color(0x88FFFFFF), CircleShape)
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = name }, contentAlignment = Alignment.Center) {
        if (on) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PartTile(part: AvatarPart, on: Boolean, image: String = part.image, onClick: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val img by produceState<ImageBitmap?>(null, image) {
        value = withContext(Dispatchers.IO) { runCatching { AvatarRenderer.asset(context, image).asImageBitmap() }.getOrNull() }
    }
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.clip(shape).background(if (on) Color(0xFFFFF4D6) else Parchment)
        .border(if (on) 4.dp else 1.5.dp, if (on) GoldLeaf else Color(0xFFC9A44C), shape)
        .clickable(role = Role.Button, onClickLabel = part.name, onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(84.dp), contentAlignment = Alignment.Center) {
            img?.let { Image(it, part.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        }
        Text(part.name, color = Plum, fontFamily = Andika, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun NoneTile(on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.clip(shape).background(if (on) Color(0xFFFFF4D6) else Parchment)
        .border(if (on) 4.dp else 1.5.dp, if (on) GoldLeaf else Color(0xFFC9A44C), shape)
        .clickable(role = Role.Button, onClickLabel = "Sin nada", onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(84.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Block, null, tint = Color(0xFFB0A090), modifier = Modifier.size(40.dp))
        }
        Text("Nada", color = Plum, fontFamily = Andika, fontSize = 12.sp)
    }
}
