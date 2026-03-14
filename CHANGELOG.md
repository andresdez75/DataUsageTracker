# Changelog

Todas las versiones notables de este proyecto se documentan aquí.
Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.0.0/).

---

## [1.0.0] — 2026-03

### Primera versión funcional

#### Funcionalidades
- Lista de apps instaladas por el usuario con su icono y nombre
- Consumo de datos diferenciado por **foreground** y **background** para cada app
- Resumen agregado del dispositivo (total FG, total BG, total, % BG global, nº apps)
- Ranking de apps ordenado de mayor a menor consumo total
- Filtro por tipo de red: **Todo**, **Móvil**, **Wi-Fi**
- Filtro por periodo: **Hoy**, **7 días**, **30 días**
- Indicador visual (naranja) para apps con más del 50% de consumo en background
- Pantalla de solicitud de permiso con redirección a Ajustes

#### Técnico
- Consulta nativa a `NetworkStatsManager` del SO Android
- Filtro de apps de usuario via `Intent.ACTION_MAIN + CATEGORY_LAUNCHER`
- Soporte de `subscriberId` para datos móviles en dispositivos que lo requieren
- Sin backend — 100% offline, datos del propio dispositivo
- Sin librerías externas — solo AndroidX + Material
- Soporte modo oscuro via `values-night/colors.xml`
- Min SDK 23 (Android 6.0)

#### Limitaciones conocidas
- El histórico disponible depende del SO (Android guarda hasta ~4 semanas)
- En algunos fabricantes (Xiaomi, Huawei) pueden existir restricciones adicionales
- El consumo en redes VPN puede no reflejarse con precisión

#### Permisos requeridos
- `PACKAGE_USAGE_STATS` — concedido manualmente por el usuario en Ajustes
- `READ_PHONE_STATE` — para obtener `subscriberId` en datos móviles
