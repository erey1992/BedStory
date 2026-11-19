/* Tercera colección: cuentos de «La Edad de Oro» de José Martí (1889), adaptados para niños menores de 10 años.
   «Los zapaticos de rosa» se conserva como poesía, con el texto original (dominio público). */

// Estrofas: cada una es un segmento; los versos van separados por saltos de línea.
const ZAPATICOS = `Hay sol bueno y mar de espuma,
Y arena fina, y Pilar
Quiere salir a estrenar
Su sombrerito de pluma.

–"¡Vaya la niña divina!"
Dice el padre, y le da un beso:
"Vaya mi pájaro preso
A buscarme arena fina."

–"Yo voy con mi niña hermosa",
Le dijo la madre buena:
"¡No te manches en la arena
Los zapaticos de rosa!"

Fueron las dos al jardín
Por la calle del laurel:
La madre cogió un clavel
Y Pilar cogió un jazmín.

Ella va de todo juego,
Con aro, y balde, y paleta:
El balde es color violeta:
El aro es color de fuego.

Vienen a verlas pasar:
Nadie quiere verlas ir:
La madre se echa a reír,
Y un viejo se echa a llorar.

El aire fresco despeina
A Pilar, que viene y va
Muy oronda: –"¡Di, mamá!
¿Tú sabes qué cosa es reina?"

Y por si vuelven de noche
De la orilla de la mar,
Para la madre y Pilar
Manda luego el padre el coche.

Está la playa muy linda:
Todo el mundo está en la playa:
Lleva espejuelos el aya
De la francesa Florinda.

Está Alberto, el militar
Que salió en la procesión
Con tricornio y con bastón,
Echando un bote a la mar.

¡Y qué mala, Magdalena
Con tantas cintas y lazos,
A la muñeca sin brazos
Enterrándola en la arena!

Conversan allá en las sillas,
Sentadas con los señores,
Las señoras, como flores,
Debajo de las sombrillas.

Pero está con estos modos
Tan serios, muy triste el mar:
¡Lo alegre es allá, al doblar,
En la barranca de todos!

Dicen que suenan las olas
Mejor allá en la barranca,
Y que la arena es muy blanca
Donde están las niñas solas.

Pilar corre a su mamá:
–"¡Mamá, yo voy a ser buena:
Déjame ir sola a la arena:
Allá, tú me ves, allá!"

–"¡Esta niña caprichosa!
No hay tarde que no me enojes:
Anda, pero no te mojes
Los zapaticos de rosa."

Le llega a los pies la espuma:
Gritan alegres las dos:
Y se va, diciendo adiós,
La del sombrero de pluma.

¡Se va allá, donde ¡muy lejos!
Las aguas son más salobres,
Donde se sientan los pobres,
Donde se sientan los viejos!

Se fue la niña a jugar,
La espuma blanca bajó,
Y pasó el tiempo, y pasó
Un águila por el mar.

Y cuando el sol se ponía
Detrás de un monte dorado,
Un sombrerito callado
Por las arenas venía.

Trabaja mucho, trabaja
Para andar: ¿qué es lo que tiene
Pilar que anda así, que viene
Con la cabecita baja?

Bien sabe la madre hermosa
Por qué le cuesta el andar:
–"¿Y los zapatos, Pilar,
Los zapaticos de rosa?

"¡Ah, loca! ¿en dónde estarán?
¡Di dónde, Pilar!" –"Señora",
Dice una mujer que llora:
"¡Están conmigo: aquí están!"

"Yo tengo una niña enferma
Que llora en el cuarto oscuro
Y la traigo al aire puro
A ver el sol, y a que duerma.

"Anoche soñó, soñó
Con el cielo, y oyó un canto:
Me dio miedo, me dio espanto,
Y la traje, y se durmió.

"Con sus dos brazos menudos
Estaba como abrazando:
Y yo mirando, mirando
Sus piececitos desnudos.

"Me llegó al cuerpo la espuma,
Alcé los ojos, y vi
Esta niña frente a mí
Con su sombrero de pluma.

"–¡Se parece a los retratos
Tu niña!" dijo: "¿Es de cera?
¿Quiere jugar? ¡si quisiera!...
¿Y por qué está sin zapatos?

"Mira: ¡la mano le abrasa,
Y tiene los pies tan fríos!
¡Oh, toma, toma los míos:
Yo tengo más en mi casa!"

"No sé bien, señora hermosa,
Lo que sucedió después:
¡Le vi a mi hijita en los pies
Los zapaticos de rosa!"

Se vio sacar los pañuelos
A una rusa y a una inglesa;
El aya de la francesa
Se quitó los espejuelos.

Abrió la madre los brazos:
Se echó Pilar en su pecho,
Y sacó el traje deshecho,
Sin adornos y sin lazos.

Todo lo quiere saber
De la enferma la señora:
¡No quiere saber que llora
De pobreza una mujer!

–"¡Sí, Pilar, dáselo! ¡y eso
También! ¡tu manta! ¡tu anillo!"
Y ella le dio su bolsillo,
Le dio el clavel, le dio beso.

Vuelven calladas de noche
A su casa del jardín:
Y Pilar va en el cojín
De la derecha del coche.

Y dice una mariposa
Que vio desde su rosal
Guardados en un cristal
Los zapaticos de rosa.`;

const stanzas = ZAPATICOS.split('\n\n');
// tres estrofas por página: doce páginas
const versePages = [];
for (let i = 0; i < stanzas.length; i += 3) versePages.push({ tx: stanzas.slice(i, i + 3) });

STORIES.push(
  {
    id: 'camaron-encantado', title: 'El camarón encantado', color: '#e76f51',
    desc: 'De José Martí: un camarón mágico y una mujer que nunca se conformaba con nada.',
    pages: [
      P('Junto al mar vivía Loppi, un leñador muy pobre, con su esposa Masicas. Loppi era bueno y trabajador, pero Masicas siempre estaba de mal humor y nunca se conformaba con nada.'),
      P('Una mañana, junto a un charco, Loppi encontró un camarón enorme. Lo iba a guardar en su saco para la cena, cuando el camarón habló.',
        D('Camarón', '¡Déjame ir, amigo! No soy un camarón cualquiera: soy una maga. Si me ayudas, yo te ayudaré.'),
        D('Loppi', 'Está bien, camaroncito. No le hago daño a nadie por gusto.')),
      P('El camarón le enseñó unas palabras mágicas. Cada vez que necesitara algo, solo tenía que llamarlo:',
        D('Camarón', 'Di: «Camaroncito duro, sácame del apuro». Pero ten cuidado: si tu mujer empieza a pedir, no va a acabar nunca.')),
      P('Desde ese día, a Loppi nunca le faltó comida: peces de colores, sopa caliente y pasteles en la mesa. Pero Masicas pronto se cansó.',
        D('Masicas', '¿Para qué quiero tanta comida si visto con trapos? ¡Pídele a la maga un vestido de señora!')),
      P('La maga le dio el vestido. Luego Masicas quiso un castillo, y la maga le dio un castillo con jardines, caballos y vacas. Después quiso ser reina... ¡y también la hicieron reina!',
        D('Loppi', 'Ahora sí que tendrás todo lo que deseas, Masicas.')),
      P('Pero un día, sentada en su trono de oro, Masicas frunció el ceño.',
        D('Masicas', 'Estoy cansada de ser reina. ¡Dile a la maga que quiero ser dueña del cielo, del sol y de las estrellas!'),
        D('Loppi', '¡Masicas, eso es demasiado!')),
      P('Loppi fue al charco temblando y llamó al camarón. Esta vez el agua se agitó y se levantó una ola enorme.',
        D('Camarón', '¡Quien todo lo quiere, todo lo pierde!'),
        'Y en un instante desaparecieron el palacio, la corona y los vestidos.'),
      P('Loppi y Masicas volvieron a su casita pobre. Pero esa noche, Masicas se sentó junto a Loppi y le habló bajito.',
        D('Masicas', 'Perdóname, Loppi. Lo que de verdad necesitaba ya lo tenía: un buen compañero.'),
        'Y desde entonces aprendió a dar las gracias por lo que tenía.'),
    ],
  },
  {
    id: 'menique', title: 'Meñique', color: '#2a9d8f',
    desc: 'De José Martí: el más pequeño de tres hermanos demuestra que saber vale más que la fuerza.',
    pages: [
      P('Un campesino muy pobre tenía tres hijos: Pedro, grande y fuerte; Pablo, flaco y gruñón; y Juancito, tan chiquito que todos lo llamaban Meñique.',
        D('Papá', 'Hijos, vayan a buscar fortuna por el mundo. Y cuídense unos a otros.')),
      P('En el reino había un problema: un roble gigante tapaba el sol del palacio, y en el patio no había agua. El rey prometió la mitad de su reino a quien cortara el roble y abriera un pozo.'),
      P('Por el camino, Meñique oyó un ruido en lo alto del monte.',
        D('Meñique', 'Quiero saber quién corta leña allá arriba.'),
        D('Pablo', '¡Todo lo quiere saber el que no sabe nada!'),
        'Meñique subió y encontró un hacha mágica que cortaba sola. La guardó en su saco.'),
      P('Más adelante encontró un pico mágico que rompía las rocas como si fueran mantequilla, y luego una cáscara de nuez de la que brotaba agua fresca sin parar.',
        D('Las cosas mágicas', 'Hace muchos años que te estábamos esperando.'),
        D('Meñique', '¡Pues aquí me tienen!')),
      P('En el palacio, Pedro y Pablo intentaron cortar el roble. Pero cada vez que cortaban una rama, ¡salían dos nuevas!',
        D('Rey', '¡Otro que no puede! ¿Quién será capaz?'),
        D('Meñique', 'Yo quiero probar, señor rey.')),
      P('Meñique sacó su hacha mágica.',
        D('Meñique', '¡Corta, hachita!'),
        'Y el hacha cortó el roble en un momento. Luego el pico abrió un pozo en la roca, y la nuez lo llenó de agua clara.'),
      P('Todos aplaudieron, y la princesa, que era muy lista, se acercó a Meñique.',
        D('Princesa', 'Tú no eres el más grande, pero eres el más curioso y el más listo.'),
        D('Meñique', 'Es que siempre quiero saber cómo funcionan las cosas.')),
      P('Meñique llevó a su papá a vivir al palacio, perdonó a sus hermanos y les dio trabajo. Y en el reino todos aprendieron que el saber vale más que la fuerza.'),
    ],
  },
  {
    id: 'bebe-pomposo', title: 'Bebé y el señor Don Pomposo', color: '#e9c46a',
    desc: 'De José Martí: un niño de buen corazón sabe con quién compartir un regalo.',
    pages: [
      P('Bebé era un niño de cinco años, con el pelo rubio en rizos. Le encantaba preguntar cómo crecen las flores y de dónde sale la luz del sol.',
        'Como lo querían mucho, él quería mucho a los demás.'),
      P('Bebé iba a viajar a París con su mamá, y no quería ir solo. Así que lo acompañaba su primito Raúl, que vivía con ellos y no tenía tantos juguetes ni ropa elegante.',
        D('Bebé', '¡Raúl, en París veremos el teatro de muñecos!')),
      P('Antes del viaje fueron a visitar a un señor muy alto y muy flaco, el tío de mamá: el señor Don Pomposo. Llevaba una cadena de oro larguísima y una piedra enorme en la corbata.'),
      P('Don Pomposo saludó a Bebé con muchos besos, pero a Raúl ni siquiera lo miró. Raúl se quedó quietecito en un sillón, con el sombrero en la mano.'),
      P('Entonces Don Pomposo sacó un regalo.',
        D('Don Pomposo', 'Mira, Bebé: un sable dorado de juguete. Cuesta mucho dinero. Es solo para ti.'),
        'Bebé se puso muy contento... hasta que vio la cara triste de Raúl.'),
      P('Esa noche, Bebé no podía dormir. Bebé estaba pensando.',
        D('Bebé', 'Yo tengo muchos juguetes. Raúl casi no tiene ninguno. ¿Y a él quién le regala cosas?')),
      P('Cuando todos dormían, Bebé se levantó de puntillas, tomó el sable dorado y lo dejó con cuidado sobre la almohada de Raúl. Y volvió a su cama riéndose bajito.',
        'A la mañana siguiente, Raúl despertó con la sonrisa más grande del mundo.'),
    ],
  },
  {
    id: 'nene-traviesa', title: 'Nené traviesa', color: '#f78fb3',
    desc: 'De José Martí: una niña muy curiosa, un libro de cien años y un papá que perdona.',
    pages: [
      P('Nené era una niña de casi seis años, con el pelo rubio. Le gustaba jugar a las tiendas y a hacer dulces con sus muñecas. ¡Sobre todo a hacer dulces, porque le daban azúcar de verdad!'),
      P('Nené vivía con su papá, que la quería muchísimo. Cada tarde, cuando él volvía del trabajo, ella corría a recibirlo con los brazos abiertos.',
        D('Papá', '¡Aquí está mi hijita!'),
        'Y entraban juntos en casa, cantando.'),
      P('Su papá siempre traía libros nuevos, y le enseñaba los dibujos de las estrellas: la roja, la amarilla, la azul.',
        D('Nené', 'Papá, cuando sea grande quiero ir a la estrella azul.'),
        D('Papá', '¿Y me llevarás contigo?')),
      P('Una noche, su papá trajo un libro enorme y muy pesado.',
        D('Papá', 'Este libro tiene cien años y no es mío, hijita. No lo toques, por favor.'),
        'Nené se fue a dormir pensando en aquel libro misterioso.'),
      P('A la mañana siguiente, mientras su papá trabajaba, Nené entró de puntillas en el cuarto de los libros. El libro viejo estaba abierto sobre la silla.',
        D('Nené', 'Solo voy a mirarlo un poquito. No lo voy a tocar.')),
      P('Pero los dibujos eran tan bonitos: un gigante con una barba larguísima, una jirafa que se comía la luna y un elefante lleno de niños. Nené bajó el libro al suelo y pasó las hojas tan rápido que... ¡RAS! Se rompieron dos páginas.'),
      P('Justo entonces apareció su papá en la puerta.',
        D('Papá', 'Nené, ¿no te dije que no tocaras ese libro? Ese libro vale mucho y no es nuestro.'),
        'Nené se puso blanca como el papel y bajó la cabeza.'),
      P('Nené corrió a abrazar a su papá.',
        D('Nené', '¡Perdóname, papá de mi corazón! No volveré a desobedecerte.'),
        D('Papá', 'Te perdono, hijita. Ahora lo arreglaremos juntos, con mucho cuidado.'),
        'Y esa tarde pegaron las hojas, una a una, mientras Nené aprendía a cuidar los libros.'),
    ],
  },
  {
    id: 'zapaticos-rosa', title: 'Los zapaticos de rosa', color: '#ff8fab', poem: true,
    desc: 'Poema de José Martí: Pilar, la playa y un regalo que nace del corazón.',
    pages: versePages,
  },
);
