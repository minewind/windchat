# Windchat

A chat enhancement mod for Minecraft 1.21.1 (Fabric), built for the Minewind server. Provides a tabbed chat interface, DMs, notifications, message filtering, color rules, macros, and more.

---

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16+
- Fabric API 0.102.0+1.21.1
- [YetAnotherConfigLib](https://modrinth.com/mod/yacl) 3.5.0+1.21-fabric
- [ModMenu](https://modrinth.com/mod/modmenu) (optional)

---

## Features

### Chat Tabs

Windchat replaces the vanilla chatbox with a persistent box with tabs

Available tabs:

- **Main** — standard player chat
- **Global** — unfiltered view of everything
- **Clan** — messages from clan chat (e.g. `[TSC] username: message`). Can be set to your own clan tag.
- **Events** — Spawn events, castle, win/loss
- **Deaths** — killfeed and death messages
- **Mentions** — Whole messages where your username or specified trigger word appears (excludes filters i.e clans, key opens, etc)
- **DMs** — per-user direct message tabs (see below)
- **Addon tabs** — registered by addons such as Windfire

Tabs can be individually enabled or disabled. Each non-Deaths tab shows an unread badge when new messages arrive while you are on a different tab. Clicking a tab switches to it instantly.

### Direct Messages

Incoming and outgoing DMs automatically open a per-user tab, no need for `/r` anymore. DM tabs can be:

- Pinned so they never close
- Set to auto-close after 2.5 minutes of inactivity (optional, off by default)
- Manually opened or closed with `/wtab <username>` (only if you have existing chat history that session with a user, will open upon messaging them)

### Custom Chat Box

The chatbox is fully custom-rendered with a fixed background that persists even when empty. Options include:

- **Persistent Chat** — disables message fade, messages always display at full opacity
- **Chat Width** — expands or narrows the box width beyond the vanilla default
- **Chat Height** — expands or narrows the box height, with scroll support

### Notifications

- Ping on incoming DMs
- Ping on mentions (trigger words or your own username in a message body)
- Ping on event messages
- Configurable trigger word list
- XP-orb ping sound, toggleable

### Muting

- Mute specific players by username
- Mute specific clan tags
- Filter server-side muted player messages (heart replacement format)
- Hide welcome messages, vote notifications, and sharpen announcements

### Color Rules

Define rules to color chat messages by keyword. Three scopes:

- `LINE` — colors the entire message
- `WORD` — colors only the matched keyword
- `CLAN` — colors all messages from a given clan tag

Format: `keyword:::hexcolor:::SCOPE`

### Regex Filters

Hide any message matching a Java regex pattern. Each filter has an ID and a pattern. Matched messages are silently dropped before they reach any tab.

### Timestamps

Prepend a timestamp to every chat message. Modes: Off, 12-hour (AM/PM), 24-hour.

### Macros

Bind chat commands to keyboard keys. Press the bound key to send the command immediately. Macros are configured in the settings screen and support any key name (NUMPAD0-9, F1-F12, letter keys, SPACE, etc.).

### Auto-Post

Automatically send a message on a configurable interval (1-60 minutes). Useful for trade ads. Can be set with `/wpost set <interval> <message>` or through the settings screen.

### Addon API

Windchat also has an addon API that allows other mods to:

- Register custom chat tabs with a label and color
- Post messages to those tabs
- Inject a settings category into the WindChat config screen
- Receive a callback when settings are saved

Addons register via the `windchat` Fabric entrypoint. The Windfire loot tracker is the reference implementation.

---

## Commands

| Command | Description |
|---|---|
| `/wconfig` | Open the settings screen |
| `/wtab <username>` | Open or close a DM tab for a player |
| `/wtab pin <username>` | Pin a DM tab so it never closes |
| `/wtab unpin <username>` | Unpin a DM tab |
| `/wmute <username>` | Mute a player |
| `/wunmute <username>` | Unmute a player |
| `/wpost set <interval> <message>` | Set auto-post message and interval (e.g. `30m`) |
| `/wpost remove` | Remove the auto-post message |

---

## Configuration

Settings are stored in `config/windchat.json`. The settings screen is accessible via ModMenu or `/wconfig`. All options are live — changes take effect immediately after saving.

---

## Windfire Addon

Windfire is a separate companion mod that adds a loot tracker for Minewind key drops (Chaos, Jester, Tempest, Paradox, Dimensional, Inferno). It runs as a Windchat addon and requires Windchat to be installed.

Features:

- Tracks essence and gear drops per key type
- Notifies with a gold message and ping sound when a wanted drop lands
- Configurable per-essence tracking (specific tiers or wildcard)
- Configurable gear rules (item type, soul count range, soul type, required enchants)
- Drop history stored to JSON in `config/windfire/`
- `/wloot stats <key>` — shows drop rates for your tracked essences
- `/wloot import` — imports legacy drop data from text files

Windfire requires the same Fabric + Fabric API stack as Windchat, plus Windchat itself present in the mods folder.
(ask dove for the latest version of Windfire if you want it)
