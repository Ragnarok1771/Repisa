# Firma de las APK

`repisa-debug.keystore` es la clave con la que se han firmado todas las versiones publicadas hasta ahora
(es una clave de depuración de Android: alias `androiddebugkey`, contraseña `android`). Se guarda aquí para que
cada versión nueva tenga **la misma firma** y se pueda instalar encima de la anterior sin perder datos.

Antes de publicar Repisa en una tienda conviene pasar a una clave privada guardada como secreto de GitHub.
Cambiar de clave obliga a desinstalar la versión anterior una vez, así que hacerlo cuando exista la copia de
seguridad de la biblioteca.
