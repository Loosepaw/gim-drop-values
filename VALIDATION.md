# Validation — version 1.0.2

This update applies the selected hide-value or High Alch display mode to both
Group Ironman broadcasts and supported personal loot reports in Game chat.
Both options are enabled by default and can be switched independently. High
Alch values continue to appear as `(78,000 gp)`, without an `HA:` prefix.

Prepared November 19, 2026. The plugin was checked against the genuine RuneLite
**1.13.7** client and API dependencies from the official RuneLite repository.
The existing dependency cache was reused; this verification did not download
or install anything.

## Results

| Check | Result |
| --- | --- |
| Compile six main Java files | Passed against the real RuneLite dependency classpath |
| Compile four test/launcher Java files | Passed |
| Broadcast and personal Game chat parsing and formatting | 19 tests passed |
| Channel filtering and render-only plugin behavior | 13 tests passed |
| Exact item matching and High Alch calculation | 5 tests passed |
| Overall automated tests | **37 passed, 0 failed, 0 errors, 0 skipped** |
| Plugin bytecode version | 55 (Java 11 target) |
| Gradle test, build, and shadowJar packaging | Passed using the compiled source sets |
| Prebuilt client launch or CLI smoke check | Not performed for this update |
| Live OSRS login, Group Ironman drop, or personal Game chat drop | Not performed |
| Windows batch execution | Not performed; scripts were reviewed in a Linux build environment |

The tests cover published Group Ironman broadcast and RuneLite valuable-drop
examples, coloured messages, leading Group Ironman markers, stack quantities,
item names with parentheses, source/kill-count text, and personal loot reports
in both RuneLite game-message types. They verify the two independent channel
switches, consistent whole-stack High Alch values, and preservation of original
messages. Player chat, malformed values, unrelated messages, PvP announcements,
and aggregate loot totals are left alone. Item-resolution tests cover duplicate
names, unknown HA values, currency items, and arithmetic overflow.

Plugin behavior checks use the genuine RuneLite API interfaces through Java
test doubles and verify that the original MessageNode is never changed. They
do not simulate a logged-in game server. The three JUnit result XML files are
included in **verification/**.

## Build environment qualification

The build environment provided Java 21 runtime modules, including the genuine
Java compiler, but lacked the normal `javac` launcher and `ct.sym` archive.
Verification used this sequence:

1. Reuse the genuine RuneLite, JUnit, and transitive dependency classpath that
   Gradle previously resolved.
2. Compile the main and test sources with
   `java com.sun.tools.javac.Main -source 11 -target 11` against that classpath.
3. Run Gradle offline with `test build shadowJar -x compileJava
   -x compileTestJava -PruneLiteVersion=1.13.7` using those compiled classes.

No replacement RuneLite API stubs were used. Exact Java 11 standard-library API
surface checking was unavailable in that environment. The delivered Gradle
project retains normal `options.release.set(11)` for use with a complete JDK.

## Prebuilt files

`gim-drop-values-1.0.2-all.jar` is the executable developer client, not a Plugin
Hub installer. It contains RuneLite 1.13.7 plus this plugin and its runtime
dependencies.

- Size: 32,649,647 bytes.
- SHA-256: `009c8b46f230561f62be6e8d74a2c0d854f54c6ba447b619e917f3b16c21b36c`.
- Launch flags: `-ea` before `-jar`, and `--developer-mode` after the JAR path.

The separate small development artifact, `gim-drop-values-1.0.2.jar`, is 13,903
bytes with SHA-256
`a3952cd540ebb12dfd90b8bfd0aeac54e261e55d2e123d029187d8b0426c1531`.

The mandatory assertion flag is included in **run-prebuilt.bat** and the source
launcher. The normal source launch task follows RuneLite's latest stable
release so that the project can be rebuilt after game updates.

## Live behavior still to verify

When live verification is undertaken, leave the existing OSRS Group Ironman
broadcast and personal loot-notification settings enabled. Check a qualifying
Group Ironman drop and a personal Game chat item report in both display modes,
including a stack of multiple items. Toggle the two plugin channel options
independently, and disable the plugin to check restoration of the original
lines. Existing OSRS thresholds continue to decide which messages are sent;
HA display does not change those thresholds.

Whole-loot summaries such as `Your loot is worth around ...` remain controlled
by RuneLite's Loot Tracker **Price type** setting and are outside this plugin's
individual-item formatting.

## Windows launcher status

The launcher checks candidate Java versions before starting the client. It
tries the normal RuneLite bundled runtime, then `JAVA_HOME`, then Java
executables on `PATH`. Old or broken candidates do not stop discovery. A client
failure after selecting Java is reported separately with its actual console
output and exit code. Its JAR references now point to version 1.0.2.

Windows batch execution remains unverified in this Linux environment.
