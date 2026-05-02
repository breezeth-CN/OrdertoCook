# Fabric Example Mod

## Setup

For setup instructions please see the [fabric documentation page](https://docs.fabricmc.net/develop/getting-started/setting-up) that relates to the IDE that you are using.

## Commands

- `/ordertocook dev <true|false>`
  - Requires permission level 4
  - Enables/disables dev mode (persisted to config and survives server restart)
  - When dev mode is `false`, the following are disabled immediately:
    - `/ordertocook neworder`
    - `[Order NPC]` spawn/despawn chat debug messages
- `/ordertocook neworder`
  - Requires permission level 4
  - Only available when dev mode is `true`
  - Refreshes the nearest Order Machine within 64 blocks

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
