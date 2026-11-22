# Validation — proposed version 0.1.3

Prepared November 21, 2026 (Pacific time), from source commit
`f937e127b9bcd8c9dc1a9419e5b24eda971ea3d9`.

## Issue and scope of the correction

A live screenshot showed GIM broadcasts still displaying coin values with the
plugin enabled, Hide coin value selected, and all channel options enabled.
The visible sentences themselves are accepted by the existing formatter.

The investigation identified an unsupported input format: the game can place
hidden fields before the visible sentence, separated by `|`. RuneLite's
`chatMessageBuilding` callback runs before the game removes those fields.
The published plugin tried to match the entire encoded string and rejected
messages containing the additional fields.

The correction is confined to `GimDropValuesPlugin.java`. It follows the
channel-specific game-script handling, formats the visible body, and reattaches
the original prefix unchanged. It does not change the event hook, original
MessageNode, other plugins' events, item lookup, or High Alch calculation.

The screenshot's raw message payload has not been captured. This is a verified
compatibility defect and a plausible explanation for the report, not proof that
hidden metadata caused that particular live failure.

## Reproduction and results

The actual published 0.1.2 JAR was downloaded from RuneLite's official plugin
manifest and its content hash verified. It retains the expected plugin metadata
and event annotations.

The updated behavior tests were first run against that published JAR: **19
behavior tests ran and 4 failed**, all because coin values remained unchanged
in messages with modeled hidden metadata. After applying the correction, all
six production and four test/launcher Java files compiled successfully.

| Suite | Result |
| --- | --- |
| Broadcast and personal Game chat formatting | 20 passed |
| Event dispatch, chat formatting, and plugin behavior | 19 passed |
| Item-name resolution and High Alch arithmetic | 5 passed |
| Total | **44 passed, 0 failures** |

Behavior tests now use RuneLite's genuine EventBus. The GIM hide/High Alch and
Game-chat High Alch regressions also run RuneLite's genuine ChatMessageManager
first, including chat recoloring. The Client and MessageNode interfaces use
test doubles; this is not a logged-in client simulation.

The two visible screenshot sentences are included in regression tests. Hidden
prefixes such as `||` and `|metadata|` are modeled from the game-script contract,
not represented as captured user packets. Coverage checks preservation of
metadata and color markup, whole-stack High Alch values without an HA label,
independent channel options, player-chat exclusions, and unchanged original
messages. Guest-clan/SPAM messages do not receive metadata handling that their
rendering paths lack. Additional unsupported separators remain unchanged.

## Build environment

Verification used genuine RuneLite 1.13.7 client/API dependencies and JUnit
4.13.2 recovered from the previously prepared development archive. Freshly
compiled project classes were placed first on the test classpath.

The environment provides the Java 21 compiler module but not the `javac`
launcher or `ct.sym`. Compilation therefore used
`java com.sun.tools.javac.Main -source 11 -target 11`. Exact Java 11
standard-library API checking was unavailable. The project keeps its normal
Gradle `options.release.set(11)` configuration for full JDK builds.

A fresh Gradle build, Windows execution, live OSRS login, and live drop test
were not performed for this patch. No custom developer-client JAR is included
in the source update package.

## Live verification still needed

After the updated commit has been reviewed and published through the Plugin
Hub, check the same GIM drop reports in Hide mode, then High Alch mode. Check a
personal valuable-drop report as well. Change the settings and disable the
plugin to confirm that existing lines refresh and restore correctly. If the
reported failure persists, capture the actual callback payload and installed
version before making another behavior change.

## Primary implementation references

- [RuneLite ChatBuilder script](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/scripts/ChatBuilder.rs2asm)
- [RuneLite ChatMessageManager](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/chat/ChatMessageManager.java)
- [Official 1.13.7 Plugin Hub manifest](https://repo.runelite.net/plugins/manifest/1.13.7_full.js)

The released RuneLite ChatBuilder and current Jagex game script were inspected.
The relevant game helper splits at the first pipe; GIM messages consume the
routing marker and then an optional metadata field. Regular clan and Game
messages have their own optional metadata handling.
