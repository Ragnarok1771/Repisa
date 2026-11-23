# Repisa

Tu colección de juegos como cajas físicas sobre repisas de madera, al estilo de *Box Room*,
pensada para la **AYN Thor** (Android, doble pantalla) y cualquier consola o móvil Android.

## Qué hace

- **Cajas con su forma real**: cada sistema tiene su caja (DVD negra con franja de PS2, estuche rojo de Switch,
  caja de cartón apaisada de SNES/N64, estuche blanco de DS/Wii, jewel case de PS1/Dreamcast…).
- **Ordenar**: A–Z, tiempo de juego, jugado recientemente, año, consola.
- **Repisas por**: género, consola, estudio, plataforma (Steam/Epic/Emuladores), década o favoritos.
- **Búsqueda** por título, género, estudio o consola.
- **Ficha del juego**: la caja sale de la repisa y gira; muestra tiempo de juego, última partida, géneros,
  estudio, año y descripción. Se pueden editar los datos a mano.
- **Segunda pantalla**: en la AYN Thor, la pantalla de abajo muestra la ficha del juego seleccionado.
- **Mando**: cruceta para moverte, A = ficha, X = jugar, Y = favorito, B = atrás,
  L1/R1 = filtrar por plataforma, Select = cambiar tipo de repisa, Start = ajustes.

## Fuentes

| Fuente | Qué lee | Qué necesitas |
|---|---|---|
| **Steam** | Juegos, horas jugadas, última partida; géneros/estudio de la tienda; portada vertical | Clave de la [Steam Web API](https://steamcommunity.com/dev/apikey) y tu SteamID64 o URL de perfil. "Detalles de juegos" en Público |
| **Epic Games** | Juegos, horas jugadas, portada vertical, estudio | Iniciar sesión (mismo método que Heroic/Legendary) |
| **Carpetas de ROMs** | Un juego por ROM, agrupando discos (.m3u/.cue/.chd) | Elegir la carpeta con subcarpetas por sistema (`snes`, `psx`, `ps2`, `gba`, `switch`…) |
| **ES-DE** | Nombre, género, estudio, año, descripción, última partida y portadas descargadas | Elegir la carpeta `ES-DE` |
| **RetroArch** | Playlists (.lpl) y tiempo de juego (.lrtl) | Elegir la carpeta `RetroArch` y activar *Guardar registros de tiempo de juego* |
| **libretro-thumbnails** | Portadas de juegos de emulador | Nada (activado por defecto) |
| **IGDB** (opcional) | Géneros, estudio, año y portada para cualquier juego | Client ID y Secret de Twitch (gratis) |

Además, Repisa **mide el tiempo** de los juegos de emulador que abres desde la app
(eliges el emulador de cada sistema en Ajustes; Dolphin, DuckStation, NetherSX2/AetherSX2, PPSSPP,
Flycast y RetroArch tienen arranque directo, el resto se abre con "Abrir con…").

## Compilar

Cada push compila el APK en GitHub Actions (pestaña *Actions* → artefacto `Repisa-apk`).
En local:

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

Requisitos: JDK 17+ y Android SDK 35. El APK release se firma con la clave debug para poder instalarlo directamente.
