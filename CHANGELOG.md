# Novedades de Repisa

Cada versión se publica en la pestaña **Releases** con su APK listo para instalar.

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
