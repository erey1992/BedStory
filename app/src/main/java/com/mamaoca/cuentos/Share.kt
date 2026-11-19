package com.mamaoca.cuentos

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/** Guarda en la galería o envía por WhatsApp las creaciones de los juegos (calcomanías y dibujos). */
object ImageShare {
    private const val ALBUM = "Cuentos de Mamá Oca"

    /** Guarda la imagen en Imágenes/Cuentos de Mamá Oca. En Android 9 o anterior hace falta el permiso de almacenamiento. */
    fun save(context: Context, bitmap: Bitmap, name: String): Boolean = runCatching {
        val file = "$name-${System.currentTimeMillis() / 1000}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + ALBUM)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM).apply { mkdirs() }
            val out = File(dir, file)
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            android.media.MediaScannerConnection.scanFile(context, arrayOf(out.path), arrayOf("image/png"), null)
        }
    }.isSuccess.also { ok ->
        Toast.makeText(context, if (ok) "¡Guardado en tus fotos!" else "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show()
    }

    /** Abre WhatsApp con la imagen lista para enviar; si no está instalado, el selector de Android. */
    fun share(context: Context, bitmap: Bitmap, name: String) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val file = File(dir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val whatsapp = Intent(send).setPackage("com.whatsapp")
        runCatching { context.startActivity(whatsapp) }.onFailure {
            context.startActivity(Intent.createChooser(send, "Enviar imagen").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
