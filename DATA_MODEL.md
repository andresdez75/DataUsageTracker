# 📦 Data Model

Descripción de los datos que la app consulta, estructura y presenta al usuario. Todos los datos provienen de la API nativa de Android `NetworkStatsManager` y se obtienen en tiempo real — la app no persiste ningún dato propio en base de datos local.

---

## Fuente de datos

| Elemento              | Detalle                                                  |
|-----------------------|----------------------------------------------------------|
| API Android           | `NetworkStatsManager` + `PackageManager`                 |
| Permiso requerido     | `PACKAGE_USAGE_STATS`                                    |
| Disponibilidad        | Android 6.0 (API 23) en adelante                         |
| Persistencia propia   | Ninguna — consulta directa al sistema operativo          |
| Retención del SO      | Android conserva histórico de hasta ~4 semanas           |

---

## Entidad principal: `AppUsageEntry`

Representa el consumo de datos de **una aplicación** en un **periodo y tipo de red** determinados.

| Campo           | Tipo     | Descripción                                                  | Ejemplo                        |
|-----------------|----------|--------------------------------------------------------------|--------------------------------|
| `packageName`   | `String` | Identificador único de la app en el sistema Android          | `com.instagram.android`        |
| `appName`       | `String` | Nombre visible de la app (obtenido via `PackageManager`)     | `Instagram`                    |
| `uid`           | `Int`    | User ID del proceso asignado por Android                     | `10085`                        |
| `networkType`   | `Enum`   | Tipo de red consultada: `ALL`, `MOBILE`, `WIFI`              | `MOBILE`                       |
| `startTime`     | `Long`   | Inicio del periodo consultado (timestamp epoch ms)           | `1741824000000`                |
| `endTime`       | `Long`   | Fin del periodo consultado (timestamp epoch ms)              | `1741910400000`                |
| `fgRxBytes`     | `Long`   | Bytes **recibidos** en **foreground**                        | `52428800` (50 MB)             |
| `fgTxBytes`     | `Long`   | Bytes **enviados** en **foreground**                         | `10485760` (10 MB)             |
| `bgRxBytes`     | `Long`   | Bytes **recibidos** en **background**                        | `20971520` (20 MB)             |
| `bgTxBytes`     | `Long`   | Bytes **enviados** en **background**                         | `5242880` (5 MB)               |

### Campos derivados (calculados en app, no vienen del SO)

| Campo             | Fórmula                                        | Descripción                        |
|-------------------|------------------------------------------------|------------------------------------|
| `fgTotalBytes`    | `fgRxBytes + fgTxBytes`                        | Total consumido en foreground      |
| `bgTotalBytes`    | `bgRxBytes + bgTxBytes`                        | Total consumido en background      |
| `totalBytes`      | `fgTotalBytes + bgTotalBytes`                  | Total consumido por la app         |
| `bgRatio`         | `bgTotalBytes / totalBytes`                    | % del consumo que es background    |

---

## Entidad agregada: `TotalUsageSummary`

Representa el consumo **total del dispositivo** (suma de todas las apps) para un periodo y tipo de red. Se calcula agregando todos los `AppUsageEntry`.

| Campo           | Tipo     | Descripción                                      |
|-----------------|----------|--------------------------------------------------|
| `networkType`   | `Enum`   | Tipo de red: `ALL`, `MOBILE`, `WIFI`             |
| `startTime`     | `Long`   | Inicio del periodo (timestamp epoch ms)          |
| `endTime`       | `Long`   | Fin del periodo (timestamp epoch ms)             |
| `fgTotalBytes`  | `Long`   | Total foreground de todas las apps               |
| `bgTotalBytes`  | `Long`   | Total background de todas las apps               |
| `totalBytes`    | `Long`   | Consumo total del dispositivo en el periodo      |
| `appCount`      | `Int`    | Número de apps con consumo registrado            |

---

## Enum: `NetworkType`

| Valor    | Constante Android                        | Descripción                        |
|----------|------------------------------------------|------------------------------------|
| `ALL`    | *(suma de MOBILE + WIFI)*                | Todo el consumo independiente de red|
| `MOBILE` | `ConnectivityManager.TYPE_MOBILE`        | Solo datos móviles                 |
| `WIFI`   | `ConnectivityManager.TYPE_WIFI`          | Solo Wi-Fi                         |

---

## Enum: `TimePeriod`

Periodos de consulta predefinidos que la app ofrece al usuario:

| Valor     | Descripción                                      |
|-----------|--------------------------------------------------|
| `TODAY`   | Desde las 00:00 del día actual hasta ahora       |
| `WEEK`    | Últimos 7 días                                   |
| `MONTH`   | Últimos 30 días                                  |

---

## Notas importantes

- **Rx = Received (descarga)** / **Tx = Transmitted (subida)** — terminología estándar de Android
- Los bytes vienen siempre en formato `Long` desde el SO; la conversión a KB/MB/GB se hace en la capa de presentación
- Las apps del sistema también tienen `uid` y aparecen en los resultados; la app puede optar por filtrarlas o mostrarlas
- Si una app no tiene consumo en el periodo, `NetworkStatsManager` no devuelve entrada para ella (no aparece con ceros)
- El tipo `ALL` no es una consulta directa al SO — se obtiene sumando `MOBILE` + `WIFI` en la propia app
