# Novedades de Repisa

Cada versión se publica en la pestaña **Releases** con su APK listo para instalar.

## [0.11.4]

- **Corregir el tiempo jugado**: Ajustes → Tiempo de juego → «Al escribir el tiempo de un juego a mano»:
  *Sumarlo al tiempo registrado* (como hasta ahora) o *Usarlo como tiempo total del juego* (escribes 17 h 52 min y
  el juego queda con 17 h 52 min, sea más o menos de lo que tenía).
- En modo «sumar», la casilla muestra lo que pusiste a mano: borrarla o ponerla en 0 quita lo añadido.
- Botón **⟲ Reiniciar tiempo** en la ficha (con confirmación): deja el juego en 0; lo que juegues después se vuelve a
  sumar.

## [0.11.3]

- **Tiempo a mano en horas y minutos**: Editar datos → «Tiempo jugado fuera de Repisa» con dos casillas (17 h 52 min),
  en lugar de horas con decimales. Se guarda aparte de lo que mide Repisa y se suma al resto.
- Cambiar solo el tiempo ya no «congela» el resto de datos del juego (título, género, carátula siguen
  actualizándose).
- **Quitar de la repisa**: botón en la ficha de cada juego para quitar duplicados o lo que no quieras ver; no vuelve
  al sincronizar. Ajustes → Pantalla → «Volver a mostrarlos».
- La ficha de los juegos de emulador dice su **origen**: tu carpeta de ROMs, una playlist de RetroArch o
  ES-DE/Cocoon.

## [0.11.2]

- **Fuera los juegos de Steam/PC que salían como emulados** (Bravely Default Flying Fairy, Persona 5 Royal,
  Xenonia…): Cocoon exporta también tus juegos de PC, y Repisa los añadía como juegos de emulador. Ahora, si tienes
  carpetas de ROMs, solo aparecen los juegos de esas carpetas; Cocoon/ES-DE solo aportan datos (género, estudio,
  última partida…). Los juegos de PC nunca se añaden desde Cocoon: vienen de Steam y Epic.
- Con eso desaparece también el Pokémon Esmeralda repetido (el que venía solo de Cocoon); el de tu carpeta se
  queda y busca su género, estudio y año en Wikidata.

## [0.11.1]

- **Fuera los juegos fantasma de Cocoon**: lo que Cocoon conoce pero no está en tus carpetas (DLC, packs, entradas
  viejas, colecciones) ya no se añade como juego si tienes la carpeta de ese sistema. Solo se añaden juegos de
  Cocoon/ES-DE para sistemas de los que no hay ninguna carpeta en Repisa, y nunca DLC ni actualizaciones.
- Las ediciones «+ … Set / Pack / Pass» se emparejan con el juego base: «Dragon Ball Z Kakarot + A New Power
  Awakens Set» de Cocoon da sus datos a tu archivo «Dragon Ball Z Kakarot».
- El diagnóstico lista lo que el frontend tiene pero no se añadió.

## [0.11.0]

- **Género, estudio y año en los juegos de emulador**: se buscan solos en segundo plano en **GameTDB** (Switch, 3DS,
  DS, Wii, GameCube y Wii U, con sinopsis) y en **Wikidata** (cualquier consola, también juegos que no están en
  Steam). Ya funcionan las repisas por género, por estudio y por década con los juegos de emulador.
- **Cocoon**: lectura tolerante de los `gamelist.xml` (BOM, «&» sin escapar, datos como atributos, CDATA).
- **Diagnóstico** en Ajustes → Emuladores: botones *Copiar* y *Compartir* con qué archivos encontró Repisa en la
  carpeta de Cocoon/ES-DE y qué leyó de cada uno.
- **Mando**: el icono seleccionado de la barra superior (buscar, calificaciones, sincronizar, ajustes) se ve con un
  círculo dorado; **B en la repisa principal ya no cierra la app**.

## [0.10.0]

- **Jugar directo, sin preguntar**: Repisa detecta los emuladores instalados y abre cada juego con el adecuado
  (Eden, Citron o Sudachi para Switch; Azahar, Lime3DS o Citra para 3DS; melonDS o DraStic para DS; Dolphin,
  DuckStation, NetherSX2, PPSSPP, M64Plus FZ, Flycast, Yaba Sanshiro, los .emu de Robert Broglia…) y RetroArch
  con su core como último recurso. Ya no aparece el selector de Android con apps que no son emuladores.
- Ajustes → Emuladores muestra el emulador detectado para cada sistema («Automático: Eden») y deja elegir otro.
- Al pulsar «Escanear emuladores» se indica con qué emulador se abrirá cada sistema.

## [0.9.1]

- **Cocoon funciona con cualquier exportación**: Repisa busca los `gamelist.xml` en todas las subcarpetas y reconoce el
  sistema por la carpeta, el nombre del archivo, la etiqueta `<system>` o las extensiones de los juegos.
- Los juegos se emparejan por nombre de archivo **y por título** (también parecido: «Atelier Ryza: Ever Darkness &
  the Secret Hideout» ↔ `Atelier Ryza Ever Darkness and the Secret Hideout [0100…].nsp`), así llegan el género y el
  tiempo de juego aunque Cocoon y tu carpeta de ROMs llamen distinto al juego.
- Lee también las **copias de seguridad de Cocoon** (JSON, SQLite o .zip) para sacar el tiempo de juego registrado.
- **Basta con elegir la carpeta principal** («Cocoon», «RetroArch», «ROMs»…): Repisa busca en las subcarpetas
  las listas, las playlists, los registros de tiempo y las carpetas de cada sistema.
- Al elegir una carpeta de emuladores, ES-DE o Cocoon, **se escanea al momento** (antes había que pulsar
  «Escanear emuladores»).
- Ajustes → Emuladores muestra **qué encontró** (listas, juegos, con género, con tiempo, emparejados) y explica
  paso a paso cómo exportar desde Cocoon.

## [0.9.0]

- **Ficha del juego adaptable a todas las carátulas.** Mi nota, Metacritic y HowLongToBeat tienen altura fija y texto
  en una línea; si no caben en una fila (carátulas anchas de DS, 3DS, Nintendo 64 o Game Boy Advance),
  HowLongToBeat pasa a la línea de abajo. La caja de la ficha ocupa como mucho ~40 % del ancho.
- **Metacritic y HowLongToBeat siempre visibles**, con «No disponible» cuando el juego no tiene datos
  (y «…» mientras se consultan).
- **Búsqueda más flexible en Metacritic y HowLongToBeat**: si no encuentra el título completo, prueba el subtítulo,
  el nombre principal y el nombre sin la saga delante («Shin Megami Tensei: Persona 2…» → «Persona 2…»).
- Si faltaban datos, se vuelven a buscar a los 2 días (antes, a las 2 semanas).

## [0.8.0]

- **Mucho más fluida**: las proporciones de las carátulas se aplican por lotes (antes cada carátula reconstruía toda
  la biblioteca y daba tirones); unir copias y construir repisas se hace en segundo plano.
- Caché de imágenes en memoria y en disco (hasta 768 MB): las carátulas no se vuelven a descargar.
- Carátulas de Steam a 600×900 (la cuarta parte de peso, igual de nítidas en la caja).
- **Steam es la copia preferente por defecto**; si no hay copia de Steam, la más jugada.
- Fuera paquetes de contenido que no son juegos («Death Stranding Content»).

## [0.7.0]

- **Copias del mismo juego unidas en una caja** (ediciones Legacy/Enhanced, el mismo juego en Steam y Epic), con el
  tiempo sumado y el nombre común. Botón **Copias** para elegir la copia preferente o separar una.
- **Fuera herramientas**: editores, kits de mods, servidores dedicados, SDK… (tipo de la tienda de Steam y categoría
  de Epic).
- Pantalla de abajo: globo con mi nota y el tiempo jugado encima de la carátula.
- El idioma se cambia **sin parpadeos** en la pantalla de abajo.
- Mis calificaciones: **ordenar de mayor a menor o al revés y filtrar por estrellas**.

## [0.6.0]

- **Epic Games funciona**: corregida la clave del lanzador.
- Ajustes en **submenús** (Emuladores, Carátulas y datos, Tiempo de juego, Pantalla, Idioma).
- Ajustes de calificación dentro de Mis calificaciones.
- Ficha: Mi nota, Metacritic y HowLongToBeat en una fila compacta.
- Barra superior equilibrada: juegos y horas en el centro; ordenar y repisas a la derecha.
- **9 idiomas** con pantalla de bienvenida y banderas: español (España y Latinoamérica), inglés (EE. UU. y Reino
  Unido), francés, alemán, italiano y portugués (Brasil y Portugal).
- Fuera las portadas genéricas de «Nintendo eShop»; IGDB eliminado.

## [0.5.0]

- **Calificaciones propias**: aspectos libres con peso en % y nota, escala 0-5 / 0-10 / 0-100, estrellas que se
  llenan en proporción y pantalla Mis calificaciones.
- **Metacritic** (prensa y usuarios) y **HowLongToBeat** (historia, extras y completista) en la ficha.
- **GameTDB**: carátulas sin clave para Switch, 3DS, DS, Wii, GameCube y Wii U.
- Búsqueda de carátulas sin acentos y por palabras clave.
- Tipografías Fredoka y Nunito; versión junto al nombre.

## [0.4.0]

- **Grupo familiar de Steam**: juegos compartidos y sus horas.
- **Buscador de carátulas** en la ficha (libretro, SteamGridDB, tienda de Steam) y «Buscar carátulas que faltan».
- Limpieza de nombres de archivo y búsqueda por palabras clave («4262 - Kingdom Hearts 358-2 Days» →
  «Kingdom Hearts 358/2 Days»).
- **Cajas a escala real**: Switch más baja que PS2, DS/3DS pequeñas, N64/SNES apaisadas.
- Icono nuevo y fondo de vitrina de nogal con luz bajo las baldas.

## [0.3.0]

- **Tiempo de juego siempre al día**: Steam, Epic y emuladores se sincronizan solos al abrir la app.
- Medición precisa con el permiso **Acceso a datos de uso** (solo el tiempo con el emulador en pantalla).
- Lectura de `gamelist.xml` de ES-DE, Cocoon y Batocera, con tiempo de juego.
- Las partidas no se pierden si Android cierra Repisa mientras juegas.

## [0.2.0]

- **Vincular Steam iniciando sesión** en la web oficial (sin clave de API).
- La **carátula real** ocupa todo el frontal de la caja y la caja toma su forma exacta.

## [0.1.0]

- Primera versión: repisas de madera con cajas de juegos de Steam, Epic Games y emuladores (carpetas de ROMs, ES-DE,
  RetroArch), ordenar y agrupar, ficha del juego, segunda pantalla de la AYN Thor y control con mando.
