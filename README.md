# Los Cuentos de Mamá Oca

Aplicación nativa en **Kotlin y Jetpack Compose**, para Android 8.0 (API 26) o posterior. Abre la raíz de este repositorio como proyecto en Android Studio.

## Contenido del repositorio

- `app/`: el código y los recursos de la app.
- `tools/content/`: los textos de los cuentos, los quizzes, las etiquetas y las calcomanías.
- `tools/*.py` y `tools/export-android.cjs`: recortan las láminas pintadas y generan los recursos en `app/src/main/assets/`.
- Las imágenes originales (`icon.png`, `portada.png` y las láminas de ilustraciones, calcomanías, fondos, dibujos para colorear y partes del personaje) no forman parte de la app: están fuera del proyecto, en la carpeta hermana `../BedStory-laminas/`, que es donde las buscan las herramientas de recorte.

## Compilar e instalar

Requisitos: JDK 17, SDK Android 35 y conexión para descargar las dependencias de la primera compilación. El proyecto incluye Gradle Wrapper.

En Windows:

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

En macOS/Linux: `sh gradlew :app:assembleDebug`. Si no tienes `ANDROID_HOME` configurado, establece la ruta del SDK en Android Studio o crea un `local.properties` con `sdk.dir=...`.

Para verificar:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

La segunda orden necesita un dispositivo o emulador conectado. Las pruebas comprueban el renderizado de las 164 escenas, integridad de los 20 cuentos y 144 páginas, búsqueda, favoritos, lectura y finalización.

## Funciones

- Portada ilustrada con partículas y biblioteca con miniaturas.
- Búsqueda sin distinción de mayúsculas ni acentos y filtro de favoritos.
- Lector horizontal con ilustraciones vectoriales por capas, animaciones nativas, transición de páginas, botones y gestos laterales.
- Texto desplazable, diálogos y seguimiento del texto durante la narración.
- Narración con `TextToSpeech`, voces de personajes y avance automático.
- Favoritos, cuentos leídos y preferencia de sonidos guardados localmente mediante `SharedPreferences`.
- Navegación y página actual restauradas por `SavedStateHandle` si Android recrea el proceso.
- Pantalla final con partículas, relectura y acceso a más cuentos.
- Sin servidor, WebView, JavaScript en ejecución, permiso de Internet ni fuentes remotas.

Para escuchar sin conexión, instala primero una voz en español desde los ajustes de síntesis de voz del dispositivo. La variedad de voces depende del motor de Android; si solo existe una voz, los personajes se diferencian por tono. La narración se detiene al ir al fondo o cambiar de pantalla.

## Código y contenido

`app/src/main/java/com/mamaoca/cuentos/` contiene pantallas, estado, narrador y dibujado nativo. `assets/stories.json` contiene el catálogo; `assets/scenes/` contiene las escenas vectoriales por capas. AndroidSVG interpreta los vectores y Android Canvas dibuja y anima sus capas.

Los textos y las ilustraciones se escriben en `tools/content/stories.js` y `tools/content/illustrations.js`. Para agregar o cambiar un cuento, edita esos archivos y regenera los recursos:

```powershell
node tools/export-android.cjs
```

Node solo interviene en esta conversión de contenido; no se necesita para compilar ni ejecutar la app. El exportador valida IDs y referencias de ilustraciones antes de escribir el catálogo.

### Ilustraciones pintadas

Los tres cerditos, Caperucita Roja, Ricitos de Oro y La liebre y la tortuga usan ilustraciones pintadas en lugar de las vectoriales. Cada cuento parte de una lámina con viñetas sobre fondo blanco, en orden de lectura (la primera es la portada). Para añadir otro cuento, coloca su lámina en `../BedStory-laminas/`, agrégala a `SHEETS` en `tools/crop-illustrations.py` y ejecuta:

```powershell
python tools/crop-illustrations.py
node tools/export-android.cjs
```

El primero recorta cada viñeta en `assets/art/<id>/<n>.webp` y comprueba que haya una por página; el segundo las enlaza en el catálogo. Las páginas sin imagen pintada siguen usando la escena vectorial.

Otros archivos de la interfaz: `Artwork.kt` (muestra las ilustraciones pintadas), `Effects.kt` (brillos de la portada y confeti del final), `Sfx.kt` (sonidos sintetizados: pasar página, toque y fanfarria) y `res/font/` (Fredoka y Nunito, incluidas en la app).

El APK de debug sirve para pruebas. Para distribuir en Google Play, configura una clave de firma propia y genera un Android App Bundle de release; las claves no se incluyen en el proyecto.
