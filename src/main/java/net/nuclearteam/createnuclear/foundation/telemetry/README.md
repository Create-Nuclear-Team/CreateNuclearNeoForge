# Create Nuclear telemetry

On-demand, local diagnostics for Create Nuclear: a timeline of structured events, metrics and health invariants, recorded during a time-limited **session** and written to files in the game directory.

1. [Why this system exists](#1-why-this-system-exists)
2. [Vocabulary](#2-vocabulary)
3. [Architecture](#3-architecture)
4. [Diagnosing a problem](#4-diagnosing-a-problem)
5. [Extending the system](#5-extending-the-system)
6. [Conventions and good practice](#6-conventions-and-good-practice)
7. [Design decisions](#7-design-decisions)

---

## 1. Why this system exists

Some bugs of the mod are hard to reproduce. The one that started this work:

> A running reactor's controller is broken. The player gets the "reactor disassembled" message, but **the output keeps spinning**.

The logs could not explain it. There was a single `CreateNuclear.LOGGER`, a few scattered `warn` calls, and no trace of what happened before the bug. Telemetry first recorded the bug, then confirmed its fix: the output is now stopped when the controller is removed.

Telemetry answers three needs:

| Need | Answer |
|---|---|
| Know **what happened** during a given period | A timeline of structured events, recorded during a **session** |
| Know **what state** the reactor was in | **Metrics**: counters, gauges and histograms |
| Be told when something **should not happen**, even without reproducing the bug | **Invariants**, with an SLI over a sliding window and an error budget |

The system follows three principles:
- **On demand.** Nothing is collected outside a session. A telemetry call then costs one static field read.
- **Local.** No data leaves the machine. Files stay in the game directory.
- **No personal data** (see [Privacy](#privacy)).

## 2. Vocabulary

| Term | Meaning |
|---|---|
| **Session** | A time-limited recording period, started with `/cn telemetry start` |
| **Module** | An observed area (`reactor`, `environment`…). Each module has its own config file. |
| **Category** | A kind of data inside a module (`reactor/output`, `reactor/fuel`…). Each one has its own level. |
| **Level** | `OFF`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`. An event is recorded only if its level is at most the level of its category. |
| **Subject** (`Subject`) | What the event is about: a reactor, an output, the server. **Never a player.** |
| **Event** | A timestamped fact: module, category, level, subject, message and attributes |
| **Metric** | A *counter* (only increases), a *gauge* (current value) or a *histogram* (distribution, with p50, p95 and p99) |
| **Invariant** | A rule that must always hold, checked at a regular interval |
| **SLI / SLO** | The share of healthy checks over the sliding window (SLI), compared with the target (SLO, 99% by default) |
| **Burn rate** | How fast the error budget is being used. Above 1, an alert is emitted. |
| **Sink** | Where events go: `log` (the game log) and `text` (the session file) |

## 3. Architecture

```
   Instrumented code (controller, assembler, outputs, fuel cycle…)
          │  Telemetry.info(ReactorCategory.STATE).atBlock(...).attr(...).emit()
          │  Telemetry.metrics("reactor", subject).gauge("heat", 412)
          │  Telemetry.health("reactor", subject).check("invariant", cond, detail).commit()
          ▼
   Telemetry (facade) ── filter: session running? module recorded? level high enough?
          │
          ▼
   TelemetrySession ── duration, ticks, metrics, HealthTracker (SLI, burn rate), PrivacyFilter
          │                         ▲
          │                         │ modules, categories, sinks, configs
          │                  TelemetryRegistry ── discovered automatically by annotation
          ▼
   Sinks: log (SLF4J → latest.log)   text (debug/createnuclear/telemetry/<session>/telemetry.log)
```

### Where the code lives

| Location | Content |
|---|---|
| `foundation/telemetry/` | The core: facade (`Telemetry`, `EventBuilder`), session, registry, `TelemetryRecorder`, server events. These classes share one package because they share package-private members. |
| `foundation/telemetry/api/` | Public contracts for modules and addons: annotations, `ITelemetryModule`, `ITelemetrySink`, `TelemetryCategory`, `BuiltinCategory`, `TelemetryLevel`, `TelemetryEvent`, `TelemetryContext`, `SessionInfo`, `Subject`, `TelemetryIds`. This package **does not depend on the core**. |
| `foundation/telemetry/config/` | TOML files: `TelemetryConfigs`, `TelemetryConfigBase`, `TelemetryCoreConfig`, `TelemetryModuleConfig` |
| `foundation/telemetry/health/` | Invariants: `HealthCheck`, `HealthTracker`, `HealthListener`, `HealthEventEmitter` |
| `foundation/telemetry/metrics/` | Metrics: `MetricSet`, `Histogram` |
| `foundation/telemetry/privacy/` | The privacy filter: `PrivacyFilter` |
| `foundation/telemetry/sink/` | The sinks, and `TelemetryFormat`, the line format they share |
| `foundation/telemetry/module/` | Cross-cutting modules: `EnvironmentTelemetryModule` |
| `content/<area>/telemetry/` | Domain modules, **next to the code they observe**, for example `content/multiblock/telemetry/` for the reactor |
| `infrastructure/command/telemetry/` | The `/cn telemetry` command: `TelemetryCommand` and one class per subcommand |

GameTests live in a `gametest/` folder next to the code they test.

### Lifecycle

1. **Mod construction.** `CNConfigs.register()` calls `TelemetryRegistry.bootstrap(container)`. The registry:
   - scans the classes annotated `@RegisterTelemetrySink` and `@RegisterTelemetryModule` in every loaded mod, addons included;
   - generates `createnuclear-telemetry.toml` (shared settings, one `sink_<id>` line per sink);
   - generates one `createnuclear-telemetry-<module>.toml` per module (one `level_<category>` line per category).
2. **`/cn telemetry start`.** The session opens the enabled sinks, writes `session started` and the `config` line, then calls `onSessionStart` on each module.
3. **During the session:**
   - instrumented code emits events and updates metrics;
   - `onServerTick` is called on each module at every server tick;
   - metrics are written every `metrics_interval_ticks`;
   - invariants are checked every `health_check_interval_ticks`;
   - every 20 ticks, sinks are flushed and config changes are written (`config changed`).
4. **End of the session**, when the duration is over, on `/cn telemetry stop`, or when the server stops:
   - last metrics (only for subjects updated since the previous dump);
   - health summary per subject;
   - `session stopped`, then the sinks are closed.

### Robustness

- A **sink** that throws is removed from the session. The game goes on. With no sink left, the session stops.
- A **module** that throws is removed from the session. The game goes on.
- A session cannot exceed `max_session_seconds`.

## 4. Diagnosing a problem

### Commands (permission level 2)

```
/cn telemetry modules                       modules, levels per category, sinks
/cn telemetry start [seconds] [modules]     modules: "all" or ids joined with '+', e.g. reactor+environment
/cn telemetry status                        running session, elapsed time, number of events
/cn telemetry stats [module]                SLI, burn rate, remaining budget, open invariants
/cn telemetry level <module> <category> <level>   changes a level live (saved in the config)
/cn telemetry stop
```

### Typical workflow

1. `/cn telemetry start 300 reactor`: record 5 minutes, reactor only.
2. To focus on one subsystem, raise its level, for example `/cn telemetry level reactor output TRACE`.
3. Reproduce the problem, or play normally until it happens.
4. Watch `/cn telemetry stats reactor` to see which invariants are violated.
5. `/cn telemetry stop`, then open the session file.

### Where to read the results

| File | Content |
|---|---|
| `debug/createnuclear/telemetry/<date>/telemetry.log` | The whole timeline of the session |
| `logs/latest.log` | Only `WARN` and `ERROR` by default (`latest_log_level`), under the `CreateNuclear/Telemetry/<module>` loggers |

Line format:
```
[+tick][LEVEL][module/category][subject] message {attributes}
```
Example: an output left spinning after its controller was removed (the original bug, now fixed):
```
[+37][INFO][reactor/assembly][reactor@minecraft:overworld[120,64,-30]] controller removed {assembled=true, outputs=2}
[+40][WARN][reactor/health][reactor_output@minecraft:overworld[122,64,-32]] violation started: orphan_output_spinning {invariant=orphan_output_spinning, detail="controller block entity missing: speed=12.0 controller=120,64,-30"}
```

How to read a session file:
- **Read the `config` line first.** Right after `session started`, it lists the settings that **differ from their default**. For example, `config {reactor.level_health=error}` explains why no violation shows up. `config {all_default=true}` means nothing was changed. A `config changed {setting=…, from=…, to=…}` line marks each setting changed during the session.
- `+tick` counts ticks since the session started (20 ticks = 1 second).
- `violation started` opens an incident, and `violation resolved` closes it.
- `reactor/metrics` lines give the reactor state at a regular interval: look at what changed just before the incident.
- The end-of-session summary gives, per subject, the SLI, the number of violations per invariant and the last violated invariant.

### Configuration

| File | Content |
|---|---|
| `config/createnuclear-telemetry.toml` | `default_session_seconds`, `max_session_seconds`, `latest_log_level`, `sink_<id>` |
| `config/createnuclear-telemetry-<module>.toml` | `enabled`, `metrics_interval_ticks`, `health_check_interval_ticks`, `slo_window_checks`, `slo_target`, `level_<category>`, plus the module's own options |

Levels apply at once, even during a session. The other settings are read when a session starts.

Each module automatically gets two categories on top of its own:
- `health`: invariants, SLI and alerts;
- `metrics`: periodic metric dumps.

### Built-in modules

| Module | Categories | Records |
|---|---|---|
| `environment` | `server`, `players` | Mod, Minecraft and NeoForge versions, mod list, dedicated or integrated server, **number** of players. Metrics: `mspt`, `tps`, `memory_used_mb`, `players`, `loaded_chunks`. Invariant: `tick_over_budget`. |
| `reactor` | `assembly`, `state`, `output`, `fuel`, `heat` | Assembly, controller and I/O removal, `canRun`, `ACTIVE`, danger, fuel cycle, output speeds, heat. Metrics per controller (`heat`, `running`, `output_speed_max`…). Invariants: `output_spinning_while_stopped`, `output_spinning_while_disassembled`, `io_missing_output`, `orphan_output_spinning`. |

## 5. Extending the system

Adding a module, a category, a sink or a checkpoint **does not require changing the core**.

### Add a checkpoint (event)

This is the most common case. Add a line where the fact happens:

```java
Telemetry.info(ReactorCategory.STATE)
    .atBlock("reactor", level, worldPosition)
    .msg("canRun changed")
    .attr("from", lastCanRun)
    .attr("to", canRun)
    .attr("rod_inputs", inputManager.size())
    .emit();
```

- Pick the level from how often the event happens (see [Choosing an event level](#choosing-an-event-level)).
- `atBlock(...)` only builds the subject if the event is actually recorded.
- Put numbers in `attr(...)`, not in `msg("{} -> {}", a, b)`: Java boxes the arguments of `msg` before the call, even outside a session. The primitive `attr` overloads allocate nothing.
- For an attribute that is costly to compute, use `.attrLazy("key", () -> compute())`. It is only evaluated if the event is recorded.
- If several attributes are costly, wrap the block in `if (Telemetry.isEnabled(category, level)) { ... }`.

### Add a metric

```java
MetricSet m = Telemetry.metrics(ReactorTelemetryModule.ID, ReactorTelemetry.subject(level, pos));
m.inc("explosions");                 // counter
m.gauge("heat", heat);               // gauge
m.observe("tick_us", micros);        // histogram (p50 / p95 / p99 / max)
```
If a gauge is costly to compute, only compute it when `Telemetry.isMetricsDue(moduleId)` is true.

A subject is written at each dump only if it was updated since the previous one. A broken reactor therefore stops appearing instead of repeating its last values.

### Add an invariant

In the module's health check, for example `ReactorTelemetry.afterControllerTick`, which only runs when `Telemetry.isHealthCheckDue(moduleId)` is true:

```java
Telemetry.health(ReactorTelemetryModule.ID, subject)
    .check("output_spinning_while_stopped", !running && maxSpeed != 0, () -> "maxSpeed=" + maxSpeed)
    .check("my_new_invariant", condition, () -> "useful detail")
    .commit();
```
The SLI, the `violation started` / `violation resolved` events, the burn rate and the summary work without anything else.

- In `check`, **`true` means violated**. Name the invariant after the broken rule.
- **Call `commit()` at every check, even when everything is fine.** A healthy check counts in the SLI and is the only way to close a violation.
- An invariant left out of a `commit` keeps its state. This lets you check some invariants only under some conditions.

### Add a category to an existing module

Add a value to the module's category enum:
```java
COOLANT(TelemetryLevel.DEBUG, "Coolant flow and temperature."),
```
On the next launch, the `level_coolant` line appears in `createnuclear-telemetry-reactor.toml`.

### Add a module

One file, anywhere in the mod (or in an addon):

```java
@RegisterTelemetryModule
public final class RadiationTelemetryModule implements ITelemetryModule {
    public static final String ID = "radiation";

    public enum Cat implements TelemetryCategory {
        EXPOSURE(TelemetryLevel.INFO, "Radiation dose changes on entities."),
        BIOME(TelemetryLevel.DEBUG, "Irradiated biome spread and restore.");

        private final TelemetryLevel defaultLevel;
        private final String description;
        Cat(TelemetryLevel l, String d) { defaultLevel = l; description = d; }
        @Override public String id() { return name().toLowerCase(Locale.ROOT); }
        @Override public TelemetryLevel defaultLevel() { return defaultLevel; }
        @Override public String description() { return description; }
    }

    @Override public String id() { return ID; }
    @Override public String description() { return "Radiation exposure and irradiated biomes."; }
    @Override public List<? extends TelemetryCategory> categories() { return List.of(Cat.values()); }

    // Optional:
    // defineOptions(Options)  : the module's own options (bool, integer, decimal), added to its config file
    // onSessionStart / onServerTick / onSessionStop : periodic collection
}
```
This file is enough to get, automatically:
- `config/createnuclear-telemetry-radiation.toml`;
- an entry in `/cn telemetry modules`, `start`, `stats` and `level`;
- the `health` and `metrics` categories;
- the sinks.

The module needs a **public no-argument constructor**. A single instance is used for the whole game: reset its state in `onSessionStart`.

### Add a sink

```java
@RegisterTelemetrySink
public final class CsvSink implements ITelemetrySink {
    @Override public String id() { return "csv"; }
    @Override public String description() { return "CSV export of metrics."; }
    @Override public boolean enabledByDefault() { return false; }

    @Override public void open(SessionInfo info) throws IOException { /* open the file in info.directory() */ }
    @Override public void accept(TelemetryEvent event) { /* write to a buffer */ }
    @Override public void flush() throws IOException { /* optional: flush the buffer */ }
    @Override public void close(SessionInfo info) { /* flush and close */ }
}
```
- The `sink_csv` line appears in `createnuclear-telemetry.toml`.
- `flush()` is optional. The session calls it every 20 ticks, even with no new event: a buffering sink writes its lines there, so a crash loses as few as possible.
- `accept`, `flush` and `close` are never called at the same time. If `open` throws, `close` is not called: close what was opened before rethrowing.
- A new instance is created for each session, so open files can be kept in fields.
- The sink must write its files in `info.directory()` and **nowhere else**.
- Received events have already been cleaned by the `PrivacyFilter`.
- A sink **must never emit telemetry** itself, so that an event cannot trigger another one in a loop.

Only `log` and `text` exist today. JSON Lines, chat and CSV sinks may come later.

## 6. Conventions and good practice

### Identifiers
- Module, sink, category and invariant ids are `snake_case`: lowercase letters, digits and `_` only. They are used as file names, command arguments and config keys.
- An id **never changes** once published, otherwise the players' configs no longer match.
- Invariants are named after the broken rule: `output_spinning_while_stopped`, `io_missing_output`.
- Subject types are plain nouns: `reactor`, `reactor_output`, `server`.
- The module id `telemetry` and the category ids `health` and `metrics` are reserved.

### Choosing an event level

| Level | When to use it | Example |
|---|---|---|
| `ERROR` | Unexpected error in the code | A caught exception |
| `WARN` | Abnormal situation, incident | Invariant violation, explosion |
| `INFO` | State change, rare | Assembly, `canRun` changing, danger transition |
| `DEBUG` | Useful detail, not frequent | Output speed changing, fuel rod consumed |
| `TRACE` | Very frequent, up to every tick | Every heat change |

**Log transitions, not states.** Keep the previous value in a field and emit only when it changes. Periodic states go through **metrics**, not events.

### Performance
- Outside a session, a telemetry call must cost no more than one check.
- Compute nothing costly before knowing it will be recorded: `attrLazy`, `isEnabled`, `isMetricsDue` or `isHealthCheckDue`.
- Never scan the world (chunks, entities) from a checkpoint.

### Privacy

The rule: **never pass personal data to telemetry.** The `PrivacyFilter` is only a safety net.

| Allowed | Forbidden |
|---|---|
| State of reactors and mod blocks, coordinates and dimension | A player's name, UUID, `GameProfile` |
| Versions of Create Nuclear, Minecraft, NeoForge, mod list | IP address, chat, a player's inventory |
| Dedicated or integrated server, **number** of connected players | World name, seed |
| MSPT, memory used by the game | OS, architecture, CPU, the machine's memory |
| Java version, **only** if `include_java_version = true` (off by default) | Absolute paths, OS user or machine name |

On top of these rules, the `PrivacyFilter` cleans every event:
- a `Player` becomes `<player>`;
- any other entity is reduced to its type;
- UUIDs and IPv4 addresses are masked;
- the home directory and the OS user name are removed from text.

Known limits of the filter, hence the rule above:
- IPv6 addresses are not masked;
- an IPv4 address stuck to a letter, a digit, `@` or `-` is not masked, because it reads as a version (`jei@19.21.0.247`);
- only **connected** players are recognised by name;
- a player name that is also a common word is masked everywhere.

The config state written to the file (`config`, `config changed`) only holds **typed values**: boolean, number or level. A text option, which could hold a path or an address, is never written. The module `Options` API only offers `bool`, `integer` and `decimal`.

Paths shown to the player are relative to the game directory. Who started the session is kept **in memory only**, to send them messages. The file only says `player` or `console`.

### Compatibility
- Do not change `TelemetryEvent`, `ITelemetryModule` or `ITelemetrySink` without saying so in the commit message: addons depend on them.
- A new interface method has a **default implementation**.
- What belongs to one area stays in its module (`content/<area>/telemetry/`), never in `foundation/telemetry/`.
- Telemetry **observes**. It never fixes gameplay, and no telemetry option changes it. A real fix goes in the game code.

## 7. Design decisions

The main choices behind the system, and why they were made.

### Scope

1. **Time-limited sessions, one at a time.** A session always stops by itself after `max_session_seconds`, and longer requests are shortened. *Why:* no endless recording, zero cost outside a session, and a forgotten session ends on its own.
2. **One command (`/cn telemetry`), local only, no JFR.** *Why:* one thing to learn, nothing leaves the machine so no consent is needed, and JFR was not needed for the use case.
3. **Structured events** (key/value attributes) rather than free text. *Why:* filtering, future sinks (JSON, CSV) and privacy all need fields, not sentences.

### Extensibility

4. **Automatic discovery** through `@RegisterTelemetryModule` and `@RegisterTelemetrySink`. An invalid module or sink (bad id, duplicate, no public constructor, exception) is skipped with an `ERROR` in the log. *Why:* adding a feature means adding a file, addons can use it too, and a broken addon must not stop the game from starting.
5. **One TOML per module** (COMMON), with one generated line per category. Levels are read at every call; the SLO window, the target and the intervals are read when a session starts. *Why:* configurable without code, global rather than per world, `/cn telemetry level` applies at once, and a window that changed size mid-session would distort the SLI.
6. **A category does not carry its module id.** The registry finds the owner of a category by instance identity. A module can only emit its own categories, plus `health` and `metrics`. *Why:* the category enum stays as simple as in the example above, two modules can both have a `state` category, and one module cannot write into another's.
7. **`api/` does not depend on the core.** Members that must cross sub-packages are public and marked `@ApiStatus.Internal`; installing a recorder stays package-private. *Why:* an addon implementing a sink must not see `TelemetrySession.start()`, and only harmless members are opened.

### Cost and safety

8. **The facade holds a single `volatile` recorder reference**, `null` outside a session, and returns `EventBuilder.NOOP` or `MetricSet.NOOP` when nothing is recorded. Nothing is allocated or formatted in the facade. *Why:* outside a session a call costs one field read.
9. **No public facade method ever throws**, and neither do the module hooks called from game code. Failures are logged (the first 10 per session) and never as telemetry. *Why:* observing must never break the game, and a failure must not trigger another in a loop.
10. **Calls from the client side are ignored.** *Why:* in single player the client and the integrated server share the process, so everything would be counted twice and the SLI distorted.

### Health and metrics

11. **`check(..., true)` means violated; transitions only.** A `commit` with no violation is healthy, an invariant left out of a `commit` keeps its state, and `violation started` / `violation resolved` are emitted only on change. *Why:* checks can be conditional without closing open violations, and logs show transitions, not states.
12. **Standard SLO definitions.** The window SLI is the share of healthy checks among the last `slo_window_checks`. The burn rate is the window failure rate divided by `1 − slo_target`. An alert needs at least 10 checks in the window. The summary also gives the session SLI. *Why:* without the minimum, one failure at start-up would give a burn rate of 100.
13. **Metric semantics.** Counters are cumulative over the session, gauges keep their last value, histograms are reset after each dump. A subject is written only if it was updated since the previous dump. *Why:* the p95 describes the last interval, which shows what changed just before an incident, and a broken reactor no longer appears with stale values such as `running=1`.

### Output

14. **One event per line**: `[+tick][LEVEL][module/category][subject] message {k=v, …}`. A value containing a space, `,`, `=`, `{`, `}` or `"` is quoted; backslashes, new lines and tabs are escaped. *Why:* the file can be read and filtered with `grep`, and you can always tell where a value ends.
15. **Two sinks with different roles.** `log` copies `WARN` and above to the game log (`latest_log_level`); `text` writes every recorded event to the session file. The text file is flushed at once on `WARN` or `ERROR`, otherwise every 20 ticks, and stops at 256 MiB with a `# truncated` line. A failing sink is removed; with no sink left the session stops. *Why:* `latest.log` stays readable, a crash loses at most 20 ticks of minor events, and a long `TRACE` session cannot fill the disk.
16. **Session events are never filtered.** `session started`, `config`, `config changed`, `module removed` and `session stopped` use the reserved module `telemetry`. The `config` line lists the settings that differ from their default. *Why:* a file must say by itself which settings produced it. A log once looked clean only because `level_health` was set to `ERROR`.

### Privacy

17. **No personal data, and the filter is a safety net.** Machine data (OS, CPU, memory) is excluded, and the Java version is off by default. No player id reaches the files, paths are relative, and the session launcher is kept in memory only. Objects are reduced before `toString()` can leak anything. If the filter throws, the event is dropped. Only typed config values are written. *Why:* an absolute path can hold the OS user name, and an unfiltered event must never be written.

### Reactor

18. **Telemetry observes, it never fixes.** The orphan output bug is fixed in `ReactorControllerBlock.onRemove`, which stops the outputs (`rotateOutputs(..., 0)`) before removing the block entity, for every player and with or without a session. *Why:* gameplay must not depend on a telemetry config, and auto-healing would hide the bug being observed.
19. **Each output remembers its controller**, as an offset relative to itself saved in NBT, set by `rotateOutputs`. The output checks `orphan_output_spinning` itself, after a grace delay following a chunk load (`orphan_grace_ticks`), and tells apart no controller, a missing controller, an untracked output and an unloaded controller. *Why:* the controller may be gone, so the subject is the output. Relative offsets keep moved reactors (contraptions) working, and the grace delay covers outputs that tick before their controller after loading.
20. **Each GameTest that starts a session has its own batch.** *Why:* only one session can run at a time, and tests in the same batch run together.
