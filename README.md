# 📶 Data Usage Tracker

App Android que mide y diferencia el consumo de datos móviles entre **foreground** (app en uso activo) y **background** (app ejecutándose en segundo plano).

---

## ¿Para qué sirve?

Permite al usuario conocer con precisión cuántos datos consume cada aplicación instalada en su dispositivo, diferenciando si el consumo ocurrió mientras la app estaba en primer plano o en segundo plano. Esto ayuda a identificar apps que consumen datos de forma silenciosa e innecesaria.

---

## Funcionalidades principales

### Vista por aplicación
| | All | Mobile | Wi-Fi |
|---|---|---|---|
| Foreground | ✅ | ✅ | ✅ |
| Background | ✅ | ✅ | ✅ |

### Vista agregada (total del dispositivo)
| | All | Mobile | Wi-Fi |
|---|---|---|---|
| Foreground | ✅ | ✅ | ✅ |
| Background | ✅ | ✅ | ✅ |

- 📅 Filtro por periodo de tiempo (día, semana, mes)
- 🔒 Sin backend: todos los datos se procesan y almacenan localmente en el dispositivo

---

## Requisitos

| Requisito         | Detalle                          |
|-------------------|----------------------------------|
| Android mínimo    | Android 6.0 (API 23)             |
| Permiso necesario | `PACKAGE_USAGE_STATS`            |
| Conexión a red    | No requerida                     |
| Backend           | Ninguno — funciona offline       |

> ⚠️ El permiso `PACKAGE_USAGE_STATS` debe ser concedido manualmente por el usuario en **Ajustes > Apps > Acceso especial > Acceso a uso**.

---

## Cómo funciona

La app utiliza la API nativa de Android `NetworkStatsManager` para consultar las estadísticas de red del sistema operativo. Android registra de forma nativa el consumo separado por:

- **Foreground**: bytes transferidos mientras la app estaba visible para el usuario
- **Background**: bytes transferidos mientras la app estaba en segundo plano

No se captura ni transmite ningún dato fuera del dispositivo.

---

## Instalación y compilación

### Opción A — Instalar APK directamente
1. Descarga el archivo `.apk` desde la sección [Releases](./releases)
2. En tu dispositivo Android, activa *Instalar desde fuentes desconocidas*
3. Abre el `.apk` e instala

### Opción B — Compilar desde código fuente
```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/data-usage-tracker.git

# Abrir en Android Studio y ejecutar sobre dispositivo o emulador
```

Requisitos de entorno:
- Android Studio Hedgehog o superior
- JDK 17
- Gradle 8+

---

## Estructura del proyecto

```
app/
├── src/
│   ├── main/
│   │   ├── java/         # Lógica de la app (Kotlin/Java)
│   │   ├── res/          # Layouts, strings, iconos
│   │   └── AndroidManifest.xml
├── build.gradle
README.md
DATA_MODEL.md             # Esquema de datos recogidos
METRICS.md                # Definición de métricas calculadas
```

---

## Limitaciones conocidas

- Los datos históricos disponibles dependen del sistema operativo (Android guarda hasta 4 semanas por defecto)
- En algunos fabricantes (Xiaomi, Huawei) pueden existir restricciones adicionales de permisos
- El consumo en redes VPN puede no reflejarse con precisión

---

## Estado del proyecto

| Estado       | Versión |
|--------------|---------|
| 🟢 Activo    | v1.0.0  |

---

## Contacto y mantenimiento

Proyecto mantenido por el equipo de producto de datos.
Para dudas o mejoras, abrir un issue en este repositorio.
