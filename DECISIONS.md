# 🧭 Technical Decisions

Registro de decisiones técnicas relevantes del proyecto, con su contexto y razonamiento. El objetivo es que cualquier persona que llegue al proyecto entienda **por qué** se hizo así, no solo **qué** se hizo.

---

## [001] Arquitectura: Empty Activity sin librerías externas

**Fecha:** 2026-03  
**Estado:** ✅ Adoptada

### Contexto
La app tiene un único propósito: consultar y mostrar el consumo de datos foreground/background por app. No requiere persistencia local, ni llamadas a APIs externas, ni navegación compleja entre pantallas.

### Decisión
Usar **Empty Activity** como punto de partida, apoyándose exclusivamente en el SDK nativo de Android, sin incorporar librerías de terceros.

### Componentes utilizados (todos nativos)

| Componente        | Propósito                                           |
|-------------------|-----------------------------------------------------|
| `NetworkStatsManager` | Consultar consumo foreground/background al SO   |
| `PackageManager`  | Obtener nombre e icono de cada app instalada        |
| `RecyclerView`    | Listar las apps con su consumo (incluido en AndroidX)|
| `ViewBinding`     | Conectar vistas con código (activado en gradle)     |

### Razonamiento
- La lógica es una única consulta al sistema operativo — no justifica introducir capas de abstracción
- Menos dependencias externas = menos superficie de error, menos actualizaciones a mantener y menor tamaño del APK
- Facilita que cualquier persona del equipo pueda leer y entender el código sin conocer frameworks específicos

### Consecuencias
- Si en el futuro se añade persistencia de datos → evaluar **Room**
- Si se añaden múltiples pantallas con navegación → evaluar **Navigation Component**
- Si crece la complejidad de estado → evaluar **ViewModel + LiveData**

---

## [002] Permiso de acceso a estadísticas de uso

**Fecha:** 2026-03  
**Estado:** ✅ Adoptada

### Contexto
Para consultar el consumo de datos por app diferenciando foreground y background, Android requiere un permiso especial que el usuario debe conceder manualmente.

### Decisión
Usar el permiso **`PACKAGE_USAGE_STATS`**, declarado en `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions"/>
```

### Razonamiento
- Es el permiso estándar para acceder a `NetworkStatsManager` y `UsageStatsManager` desde apps de usuario
- El permiso alternativo (`READ_NETWORK_USAGE_HISTORY`) es de nivel sistema y solo está disponible para apps del fabricante — no válido para distribución en Play Store
- Al ser un permiso especial (`appOp`), no se solicita en tiempo de ejecución como los permisos normales: hay que redirigir al usuario a **Ajustes > Apps > Acceso especial > Acceso a uso**

### Consecuencias
- La app debe detectar si el permiso está concedido al arrancar y mostrar la pantalla de ajustes si no lo está
- En algunos fabricantes (Xiaomi, Huawei, Samsung) la ruta de ajustes puede variar — considerar guía visual en onboarding

---

## [003] Sin backend — procesamiento 100% en cliente

**Fecha:** 2026-03  
**Estado:** ✅ Adoptada

### Contexto
Los datos de consumo son locales al dispositivo y no necesitan sincronizarse con ningún servidor externo.

### Decisión
La app opera completamente **offline**, sin ningún tipo de backend, base de datos remota ni analítica externa.

### Razonamiento
- Los datos consultados ya residen en el propio dispositivo — no hay necesidad de extraerlos
- Elimina dependencias de conectividad, latencia y costes de infraestructura
- Simplifica el modelo de privacidad: ningún dato del usuario sale del dispositivo

### Consecuencias
- El histórico disponible está limitado a lo que Android retiene en el SO (~4 semanas)
- Si en el futuro se quiere histórico más largo o comparativas entre dispositivos → requeriría introducir backend y rediseñar el modelo de datos
