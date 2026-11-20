# Developer setup

Install a full JDK 11, such as Eclipse Temurin, and open a terminal in this
folder. On Windows, enable the installer's PATH and JAVA_HOME options and
then open a new terminal.

The plugin applies one Hide/High Alch setting to both **Group Ironman
broadcasts** and **Game chat loot reports**, which start enabled.

Run the existing unit tests and build:

```powershell
.\gradlew.bat test build
```

For a local development client, use `run-plugin.bat` or:

```powershell
.\gradlew.bat run
```

On macOS/Linux, use `./gradlew` instead. The run task includes `-ea` and
`--developer-mode`. A development client is unreviewed; these technical
instructions are not an assurance of account safety.

See [VALIDATION.md](VALIDATION.md) for the actual checks already performed and
the remaining live-game verification. The source package intentionally does
not include the prebuilt client or its launch script.

Official references:

- [RuneLite developer guide](https://github.com/runelite/runelite/wiki/Developer-Guide)
- [Plugin Hub contribution instructions](https://github.com/runelite/plugin-hub)
- [Jagex Accounts and development clients](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)
- [Temurin Windows installation](https://adoptium.net/installation/windows)
