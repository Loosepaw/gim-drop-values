# Submit GIM Drop Values

Version 1.0.2. Prepared for source submission; nothing has been published or submitted yet.

## Project details to finalize

In `runelite-plugin.properties`, replace the generic author label
`GIM Drop Values contributors` with your intended public author name or GitHub
handle. The plugin class, display name, tags, description, and `build=standard`
are already set. Retain the license. The source still displays High Alch values
as `(78,000 gp)`, without an HA label.

Put the contents of this extracted project at the root of your public
`gim-drop-values` GitHub repository. Keep the directory structure, including
`src/` and the genuine Gradle wrapper JAR. This package excludes the generated
client JAR, dependency bundle, and local build directories.

## Submission marker

Follow the [official submission instructions](https://github.com/runelite/plugin-hub#submitting-a-plugin).
In your fork of `runelite/plugin-hub`, create `plugins/gim-drop-values` with:

```properties
repository=https://github.com/YOUR_USERNAME/gim-drop-values.git
commit=YOUR_FULL_40_CHARACTER_COMMIT_HASH
```

Replace both placeholders with your real public repository and the exact
commit to review, then open a pull request to `runelite/plugin-hub`.
If code changes, update the marker to the new commit in the same pull request.

## Suggested pull-request title

Add GIM Drop Values

## Suggested pull-request description

Group Ironman drop broadcasts and personal valuable-drop reports in game
chat show a coin value that some ironman players prefer to hide or view as
High Alchemy value. GIM Drop Values provides both
options through plugin configuration.

- Hides the displayed price, or replaces it with the item's High Alchemy value
  multiplied by quantity. The value is formatted as `(78,000 gp)`, without an
  HA label.
- Defaults to Group Ironman system broadcasts and personal Game chat loot
  reports. Both use the same display mode and can be toggled independently;
  clan and guest clan broadcasts are optional.
- Changes chat rendering only, preserving the original message node and chat
  events. Disabling the plugin restores the original display.
- Adds no gameplay automation or plugin-specific network requests. Unknown or
  ambiguous item matches hide the value rather than substituting a GE price.
- Uses the standard build and no additional runtime dependencies.

Validation: 37 unit tests passed against genuine RuneLite 1.13.7 dependencies;
see VALIDATION.md for the exact build-environment qualification. Live Group
Ironman and personal game-chat drop reports have not yet been verified in-game.

## Review expectations

RuneLite's [review policy](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)
includes human and AI review. It describes automatic approval for plugin
updates and many simple plugins; it does not promise instant acceptance of a
new submission. A passing build does not establish publication or account
safety. Wait until the submission is accepted and the plugin is available
through the normal Plugin Hub.
