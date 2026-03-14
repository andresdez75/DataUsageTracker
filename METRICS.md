# 📊 Metrics

Definición de las métricas que la app calcula y presenta al usuario a partir de los datos obtenidos de `NetworkStatsManager`. Todas las métricas son calculadas en cliente, en tiempo real, sin persistencia.

---

## Dimensiones de análisis

Todas las métricas se pueden consultar combinando estas dos dimensiones:

| Dimensión     | Valores posibles              |
|---------------|-------------------------------|
| `NetworkType` | `ALL`, `MOBILE`, `WIFI`       |
| `TimePeriod`  | `TODAY`, `WEEK`, `MONTH`      |

---

## Vista por aplicación

Métricas calculadas para **cada app individualmente**.

| Métrica               | Descripción                                                  | Fuente                          |
|-----------------------|--------------------------------------------------------------|---------------------------------|
| Consumo foreground    | Total de bytes (rx + tx) mientras la app estaba en pantalla  | `fgRxBytes + fgTxBytes`         |
| Consumo background    | Total de bytes (rx + tx) mientras la app estaba en segundo plano | `bgRxBytes + bgTxBytes`     |
| Consumo total         | Suma de foreground + background                              | `fgTotalBytes + bgTotalBytes`   |
| % background          | Proporción del consumo que ocurre en background              | `bgTotalBytes / totalBytes`     |
| Ranking por consumo   | Ordenación de apps de mayor a menor consumo total            | `totalBytes` desc               |

---

## Vista agregada (total dispositivo)

Métricas calculadas sumando **todas las apps** del dispositivo.

| Métrica                    | Descripción                                                       | Fuente                              |
|----------------------------|-------------------------------------------------------------------|-------------------------------------|
| Total foreground dispositivo | Suma del foreground de todas las apps                           | `Σ fgTotalBytes`                    |
| Total background dispositivo | Suma del background de todas las apps                           | `Σ bgTotalBytes`                    |
| Total consumo dispositivo  | Suma del consumo total de todas las apps                          | `Σ totalBytes`                      |
| % background global        | Qué proporción del consumo total del dispositivo es background    | `Σ bgTotalBytes / Σ totalBytes`     |
| Número de apps con consumo | Cuántas apps han generado tráfico en el periodo                   | `appCount`                          |

---

## Presentación de valores

Los bytes del SO se convierten en la capa de presentación según estas reglas:

| Rango              | Unidad mostrada | Ejemplo        |
|--------------------|-----------------|----------------|
| < 1.024 bytes      | B               | `512 B`        |
| 1.024 – 1.048.575  | KB              | `768 KB`       |
| 1.048.576 – 1 GB   | MB              | `45,3 MB`      |
| > 1 GB             | GB              | `1,2 GB`       |

> Los valores se redondean a **1 decimal** para la visualización.

---

## Notas

- El **% background** es la métrica clave del producto: permite detectar apps que consumen datos de forma silenciosa
- Una app con `bgRatio > 0.5` consume más en background que en foreground — candidata a revisar o restringir
- Apps sin consumo en el periodo no aparecen en ninguna vista
- El agregado `ALL` en `NetworkType` puede diferir ligeramente de `MOBILE + WIFI` si hay otros tipos de red activos (VPN, Ethernet por USB)
