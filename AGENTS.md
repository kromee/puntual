# Puntual — guía para el agente

App Android personal de **check-in laboral** (alternativa a WhatsApp): un botón registra hora, calcula retardo, historial por bloques y evidencia local.

**Idioma:** responder al usuario en **español**. Textos de la app en `res/values/strings.xml` (español).

**Git:** no crear commits ni push salvo que el usuario lo pida explícitamente.

---

## Stack (no subir sin acuerdo)

| Componente | Versión / nota |
|------------|----------------|
| AGP | 7.4.2 |
| Gradle | 7.6.4 |
| Kotlin | 1.8.22 |
| Java (módulo app) | **11** — compatible con Android Studio con JBR 11 |
| compileSdk / targetSdk | 34 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| BD local | Room v2 |
| Preferencias | DataStore |
| Red | Retrofit 2.9 + Gson |
| Navegación | Navigation Compose (3 pestañas) |

Catálogo de versiones: `gradle/libs.versions.toml`. Módulo app: `app/build.gradle` (Groovy, no `.kts`).

---

## Arquitectura

```
ui/          → Compose, ViewModels, UiState
domain/      → modelos, reglas, interfaces de repositorio
data/        → Room, DataStore, Retrofit, implementaciones
di/          → DatabaseModule, NetworkModule, RepositoryModule
navigation/  → PuntualNavHost, rutas
```

Patrón: **MVVM** + repositorios + `Flow` reactivo desde DAOs.

---

## Web (`web/`)

Cliente Next.js para registrar asistencia desde el iPhone y el navegador. Usa el mismo Supabase, esquema `puntuall`, y las mismas reglas de la app Android. La hora de un registro nuevo la asigna Postgres (`America/Mexico_City`). Hasta el módulo Perfil, un registro web sin hora esperada guarda retardo 0. Esta entrega no usa biometría. La migración `supabase/migrations/20261104190000_register_check_in.sql` debe aplicarse en Supabase antes de registrar desde la web.

### Stack

| Componente | Nota |
|------------|------|
| Next.js | App Router, dentro de `web/` |
| UI | React, TypeScript y Tailwind |
| Acceso | Supabase Auth con sesión en cookie |
| Despliegue | Vercel, con directorio raíz `web` |

Variables locales en `web/.env.local` (no se versionan): `NEXT_PUBLIC_SUPABASE_URL` y `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`. En Vercel se cargan como variables de entorno.

### Patrones

La dependencia apunta al dominio. Las pantallas no llaman a Supabase.

- **Puerto y adaptador:** `domain/` define los contratos. `infrastructure/` los implementa.
- **Repositorio:** el acceso a datos pasa por una interfaz.
- **Caso de uso:** cada acción de negocio vive en `application/` y devuelve un resultado tipado.
- **Mapper:** las filas de Supabase se traducen a modelos de dominio en el borde.
- **Frontera de servidor:** las escrituras ocurren en Server Actions. El navegador no calcula la hora ni el retardo.
- **Composición:** las rutas de `app/` componen casos de uso y componentes.

```
web/app/              rutas y acciones de entrada
web/components/       interfaz
web/application/      casos de uso
web/domain/           modelos, puertos y resultados
web/infrastructure/   Supabase y mappers
```

Módulos de la primera entrega: Acceso, Entrada (`/`), Historial (`/historial`), Periodos (`/periodos`), Ausencias y Perfil.

---

## Pantallas (bottom nav)

1. **Entrada** (`ui/home/`) — registrar check-in lun–vie, corregir hora, **Ver frase**
2. **Historial** (`ui/history/`) — mes/año, totales, selector de **bloque/periodo**
3. **Config** (`ui/settings/`) — nombre, hora esperada, permisos, **cortes de periodo**

Marca fija **「Puntual」**; el nombre de persona es solo el campo en Config (`sanitizeDisplayName`, `headerUserLine()`).

---

## Reglas de negocio

### Check-in
- Un registro por **día laborable** (lun–vie) dentro del **periodo activo**
- Si hay **hora esperada** (DataStore): calcular retardo con `DelayCalculator`
- Sin hora configurada: solo evidencia, `delayMinutes = 0`
- PK Room: `(workDate, periodId)` en `check_ins`

### Periodos (cortes / bloques)
- Tabla `attendance_periods`: `title`, `startDate`, `endDate`, `isActive`
- Solo un periodo activo; al cerrar: `PeriodRepository.closeActivePeriod(end, newStart)`
- Validación: `ClosePeriodDateDefaults` — fin ≥ inicio del bloque, nuevo inicio **después** del fin, nuevo inicio **≤ hoy**
- Defaults al abrir diálogo: fin = ayer, inicio = hoy (si aplica)
- Historial filtrado por `periodId`; años desde `HistoryPeriodRules.FIRST_HISTORY_YEAR` (2025)
- Tras un corte, Historial puede cambiar al periodo activo nuevo (`HistoryViewModel`)

### Frase motivacional (opcional)
- API: `GET https://zenquotes.io/api/random` → JSON `[{ "q": "...", "a": "..." }]`
- `ZenQuotesApi`, `QuoteRepository`, `NetworkModule`
- Botón **Ver frase** solo en **Entrada**, debajo de **Corregir hora real** (tras check-in del día)
- Modal: `MotivationalQuoteDialog` — Copiar al portapapeles + atribución ZenQuotes
- **No** se mostró al usuario: regla “8 h retardo = 1 día vacaciones” (eliminada a petición)

---

## Base de datos

- **Versión Room: 3** — migraciones `PuntualDatabase.MIGRATION_1_2` (v1 → periodos + `periodId` en check-ins) y `MIGRATION_2_3` (ausencias justificadas)
- `DatabaseModule` debe registrar `.addMigrations(MIGRATION_1_2)` y `.addMigrations(MIGRATION_2_3)`
- Periodo inicial en migración: id 1, desde 2025-01-01
- `PeriodInitializer` + `SampleDataSeeder` en `PuntualApplication.onCreate`

---

## Archivos clave

| Área | Archivos |
|------|----------|
| Check-in | `CheckInRepositoryImpl`, `CheckInDao`, `HomeViewModel` |
| Periodos | `PeriodRepositoryImpl`, `AttendancePeriodDao`, `ClosePeriodDialog` |
| Historial | `HistoryViewModel` (combinar flows: máx. 5 en un `combine`, anidar si hace falta) |
| Fechas periodo | `PeriodDateRules`, `ClosePeriodDateDefaults` |
| Formato UI | `PuntualFormatters`, `HistoryPeriodRules` |
| Frases | `QuoteRepositoryImpl`, `MotivationalQuoteDialog`, `HomeScreen` |
| Tema verde | `ui/theme/Color.kt`, `PuntualScreenShell`, `CheckInCard` |

---

## Convenciones de código

- Cambios **mínimos** y alineados al estilo existente
- No sobre-ingeniería ni tests triviales salvo que se pidan
- `CheckIn.toEntity(periodId)` — siempre pasar `periodId` del periodo activo
- Estados UI: `sealed interface` / `data class` en `*UiState.kt`
- Errores de registro: `RegisterCheckInResult` / `ClosePeriodResult`
- Strings nuevos en `strings.xml`, no hardcodear en Compose salvo excepción

---

## Permisos (`AndroidManifest.xml`)

- `INTERNET` — Zen Quotes
- `POST_NOTIFICATIONS` — opcional en Config (Android 13+)

---

## Build / entorno

- Compilar con **JDK 11** si falla `Unsupported class file major version` (JDK 24 en PATH)
- En PowerShell usar `;` en lugar de `&&` entre comandos
- Warning AGP 7.4 + compileSdk 34: esperado; opcional `android.suppressUnsupportedCompileSdk=34` en `gradle.properties`

---

## Ideas / pendientes (no implementado salvo petición)

- Frases en **español** (Zen Quotes devuelve inglés)
- **Ver frase** también antes del primer check-in del día
- Atribución visible con enlace a zenquotes.io si se publica la app
- Notificaciones recordatorio antes de la hora esperada

---

## Historial de decisiones (resumen)

1. Migración de plantilla a Compose + Hilt por stack acordado con el usuario
2. AGP bajado a 7.4.2 por Studio antiguo (Java 11)
3. Cortes de periodo para conteos independientes por tramo laboral
4. Zen Quotes en Entrada como acción opcional (no automática al cerrar bloque)
5. Eliminada UI de “días de vacaciones” / 8 horas
6. Cliente web en `web/` con Next.js, misma lógica de Supabase y sin biometría
7. La web registra la entrada con la hora de Postgres, en `America/Mexico_City`

Actualizar este archivo cuando cambien reglas de negocio, versión de Room o APIs externas.
