# GIM Drop Values

A RuneLite plugin that changes the coin value shown in Group Ironman loot
announcements and your own valuable-drop reports in Game chat. Choose
**Hide coin value** or **Show High Alch value** in its settings. Hiding is the
default, and both Group Ironman and personal Game chat reports start enabled.

Install the published version through RuneLite's Plugin Hub. Source changes
take effect in the Hub after the corresponding update is reviewed and published.

## What it does

| Setting | Display |
| --- | --- |
| Hide coin value | Keeps the player, item, quantity, source, and any kill-count text; removes the price in parentheses. |
| Show High Alch value | Replaces that price with `(… gp)`, using the item's High Alchemy value multiplied by the quantity, without an HA label. |

An illustrative announcement changes from:

```text
Iron Timzor received a drop: 30 x Cosmic rune (3,540 coins) from Sergeant Strongstack.
```

to:

```text
Iron Timzor received a drop: 30 x Cosmic rune from Sergeant Strongstack.
```

The High Alch calculation uses `ItemComposition.getHaPrice()` from the game
definition, with the drop quantity applied using checked 64-bit arithmetic.
The displayed amount is the total HA value before rune costs, not profit. The
game's restrictions on casting High Alchemy still apply.

If the item name cannot be matched exactly, or different matching items have
conflicting HA values, the plugin hides the price. It does the same for currency
items such as Coins. It never substitutes the GE price for a missing HA value.

## Scope and settings

- **Group Ironman broadcasts:** enabled by default.
- **Game chat loot reports:** enabled by default; applies the same display mode
  to your own price-bearing `Valuable drop:` notifications and supported
  `You received a drop:` or named loot messages shown in Game chat.
- **Clan broadcasts:** optional; disabled by default.
- **Guest clan broadcasts:** optional; disabled by default.
- Supported price-bearing messages include ordinary drops, rare drops, clue
  items, and special loot from a raid, using OSRS's broadcast sentence formats.
- Player-written chat, private messages, other game messages, achievements,
  and announcements without a coin value keep their normal display.
- The original item name and quantity are retained, including names containing
  parentheses, such as `Dragon dagger(p++)`.

Whole-loot summaries such as `Your loot is worth around ...` are not individual
item reports. Their valuation remains controlled by RuneLite's Loot Tracker
**Price type** setting.

The change is local to your client. Each teammate who wants the same appearance
needs to run the plugin. OSRS's existing Group Ironman broadcast threshold and
personal loot-notification threshold still decide which drops are announced.
Selecting High Alch mode does not change either threshold to use HA values.

## Installation and development

Open the Plugin Hub in your usual RuneLite client and search for
**GIM Drop Values**.

Both **Group Ironman broadcasts** and **Game chat loot reports** start enabled.
Choose **Hide coin value** or **Show High Alch value** to apply the same display
mode to both. The channels can be toggled separately.

For local development, install a full JDK 11 and run `gradlew.bat run` on Windows
(or `./gradlew run` elsewhere). See **[SETUP.md](SETUP.md)** for the development
steps and **[SUBMITTING.md](SUBMITTING.md)** for this project's submission notes.

This repository uses RuneLite's `build=standard` submission mode. Its Java
source uses RuneLite and Java APIs without additional runtime dependencies.

## How it works

The plugin listens for RuneLite's `chatMessageBuilding` rendering callback after
the built-in chat formatter. It accepts supported loot messages in the enabled
broadcast and Game chat channels and edits only the displayed message string on
the script's object stack. The underlying `MessageNode` and `ChatMessage` event
are not modified, so other plugins can keep reading the original message.

Game and clan system messages can include hidden fields separated by `|`
before the visible sentence. The plugin follows the game's channel-specific
prefix handling, formats the visible drop sentence, and preserves those fields
unchanged. Guest-clan messages and filtered Game chat retain their separate
handling.

Changing the setting refreshes existing chat lines. Disabling the plugin
restores their normal display. If another plugin fully rewrites the same
broadcast sentence, an unrecognized message is left unchanged.

The HA name lookup uses RuneLite's existing item index and the local game
definitions. This plugin adds no network requests. An exact-name result is
cached briefly; after a login or setting change, the cache is cleared. Unknown
results expire too, allowing the item index to finish loading.

## Build and verification

The metadata uses `build=standard` for Plugin Hub submission. The project has
no additional runtime dependencies.

```powershell
.\gradlew.bat test build
```

To reproduce the version used for verification:

```powershell
.\gradlew.bat test build -PruneLiteVersion=1.13.7
```

The default dependency is `latest.release`, following RuneLite's plugin
template. The Gradle wrapper is included and verifies its official distribution
checksum. See **[VALIDATION.md](VALIDATION.md)** for the actual build and test
results, including what remains to be verified in a live game session.

## References

- [Official RuneLite plugin template](https://github.com/runelite/example-plugin)
- [Plugin Hub development guide](https://github.com/runelite/plugin-hub)
- [Jagex Account login for development clients](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)
- [RuneLite chat rendering implementation](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/chat/ChatMessageManager.java)
- [Item composition API and High Alchemy value](https://static.runelite.net/runelite-api/apidocs/net/runelite/api/ItemComposition.html)
- [Published GIM broadcast examples](https://github.com/DaRoms/group-event-notifications/blob/main/src/test/java/com/groupeventnotifications/GroupEventParserTest.java)

## License

Plugin source: BSD 2-Clause; see **LICENSE**. The Gradle wrapper scripts and
wrapper JAR retain their Apache 2.0 license; see **LICENSE-Gradle.txt**.

