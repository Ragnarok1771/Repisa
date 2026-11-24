# Firma de las APK

**Desde la 0.13.0**, Repisa se firma con una **clave privada** que no está en este repositorio. GitHub Actions la
recibe de dos secretos del repositorio (*Settings → Secrets and variables → Actions*):

| Secreto | Contenido |
|---|---|
| `REPISA_KEYSTORE_BASE64` | El archivo `repisa-release.jks` codificado en base64 |
| `REPISA_KEYSTORE_PASSWORD` | Su contraseña (la misma para el almacén y la clave; alias `repisa`) |

El workflow *Publicar versiones* comprueba que cada APK publicado lleve la firma `CN=Repisa`; si faltan los secretos,
no publica nada.

Para compilar una versión firmada en local:

```bash
REPISA_KEYSTORE=/ruta/repisa-release.jks REPISA_KEYSTORE_PASSWORD=... ./gradlew assembleRelease
```

Sin esas variables, la versión release se firma con la clave debug de quien compila (solo sirve para pruebas).

**Guarda una copia del `.jks` y de la contraseña fuera de GitHub**: si se pierden, las versiones nuevas no se
podrán instalar encima y todos tendrían que desinstalar.

Las versiones 0.1.0–0.12.0 se firmaron con una clave de depuración (alias `androiddebugkey`) que se quitó del
repositorio. Por eso, pasar de la 0.12.0 a la 0.13.0 requiere desinstalar una vez (con la copia de seguridad hecha).
