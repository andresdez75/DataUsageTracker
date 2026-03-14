# CLAUDE.md — Data Usage Tracker

## Qué es este proyecto
App Android que mide el consumo de datos móviles diferenciando **foreground** (app en uso activo) y **background** (app en segundo plano) para cada app instalada por el usuario.

## Stack técnico
- **Lenguaje**: Kotlin
- **Min SDK**: 23 (Android 6.0)
- **Arquitectura**: Empty Views Activity, sin librerías externas salvo AndroidX + Material
- **Sin backend**: todo se procesa localmente en el dispositivo
- **Build**: Gradle con `.kts`, version catalog (`libs.versions.toml`)

## Estructura del proyecto
```
app/src/main/java/com/datausage/tracker/
├── data/
│   └── NetworkStatsHelper.kt   # Toda la lógica de consulta al SO
├── model/
│   └── Models.kt               # AppUsageEntry, TotalUsageSummary, enums
├── ui/
│   ├── MainActivity.kt         # Pantalla principal
│   └── AppUsageAdapter.kt      # RecyclerView adapter
└── util/
    └── ByteFormatter.kt        # Conversión bytes → B/KB/MB/GB

app/src/main/res/
├── layout/
│   ├── activity_main.xml       # Layout principal
│   └── item_app_usage.xml      # Fila de la lista de apps
└── values/
    ├── colors.xml
    ├── strings.xml
    └── themes.xml
```

## Permisos requeridos (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
```
El permiso PACKAGE_USAGE_STATS es especial — el usuario lo concede manualmente en Ajustes → Apps → Acceso especial → Acceso a datos de uso.

## Modelo de datos clave

### AppUsageEntry
Representa el consumo de UNA app en un periodo y tipo de red:
- `fgRxBytes`, `fgTxBytes` — bytes foreground (recibidos/enviados)
- `bgRxBytes`, `bgTxBytes` — bytes background
- `fgTotalBytes`, `bgTotalBytes`, `totalBytes` — campos derivados
- `bgRatio` — proporción background (0.0–1.0). Si > 0.5 se marca en naranja

### TotalUsageSummary
Agregado de todas las apps para resumen del dispositivo.

### Enums
- `NetworkType`: ALL, MOBILE, WIFI
- `TimePeriod`: TODAY, WEEK (7 días), MONTH (30 días)

## Lógica principal — NetworkStatsHelper.kt
- Usa `NetworkStatsManager.querySummary()` para obtener datos del SO
- Separa foreground/background según `bucket.state == STATE_FOREGROUND`
- Para `NetworkType.ALL` suma MOBILE + WIFI (el SO no tiene constante ALL)
- En datos móviles puede necesitar `subscriberId` de `TelephonyManager`
- El filtro de apps usa `Intent.ACTION_MAIN + CATEGORY_LAUNCHER` para obtener solo apps con icono en el launcher (apps de usuario)

## Problema conocido activo
El filtro de apps de usuario no funciona correctamente — solo aparece Google Play en la lista. WhatsApp, Instagram y otras apps instaladas por el usuario no aparecen. El problema está en cómo se cruzan los UIDs del launcher con los UIDs de los buckets de NetworkStatsManager.

## Métricas que muestra la app
- Por app: consumo FG, consumo BG, total, % background
- Resumen dispositivo: total FG, total BG, total, % BG global, nº apps
- Ranking de apps por consumo total (mayor a menor)
- Unidades: B / KB / MB / GB con 1 decimal

## Decisiones técnicas importantes
1. Sin librerías externas — solo SDK nativo + AndroidX + Material
2. Sin backend — todo offline, datos del propio SO
3. Si crece la complejidad → evaluar ViewModel + LiveData
4. Android guarda histórico de ~4 semanas en NetworkStatsManager

## Cómo compilar
Abrir en Android Studio, sincronizar Gradle y ejecutar sobre dispositivo Android 6.0+.
El usuario debe conceder el permiso de acceso a datos de uso antes de ver cualquier dato.
