# Arcane Games: MineCraft chess foundation

Arcane Games is an in-progress Minecraft minigames mod targeting Java Edition 26.3. Fabric is the current game platform; the plain Java rules and minigame registry now live in the `common` Gradle module so another loader can reuse them. It is a working development prototype, not a release build. The public repository is [github.com/B1progame/Arcane-Games](https://github.com/B1progame/Arcane-Games).

## What works in source

- Fabric 26.3 / Java 25 project with a registered, craftable Arcane Games Tome block item in Creative Mode.
- Place the Tome; right-clicking checks for a clear 2x2 footprint and two blocks of headroom, opens the deployed board, creates a server-owned chess match, and opens the flat chess screen. It never clears terrain to make room. The block entity saves the match UUID and deployed presentation state with the world.
- Multiplayer can also start from chat with `/chess create`; it opens a waiting match and prints the full ID plus a ready-to-share `/chess join <ID>` command. `/chess list` shows up to ten waiting matches. A second player can also join by right-clicking the Tome.
- Optional server-owned clocks are available with `/chess create <minutes>` (1–60). The active side loses time, timeout results are recorded once, client screens receive clock updates once a second, and saved time restores paused after a server restart. Tome-created matches remain untimed.
- A second player can join a waiting match with `/chess join <match-id>`. Participants can reopen it with `/chess resume <match-id>`. Other players can watch through `/chess spectate <match-id>` or by clicking an active Tome.
- The generic game-board block entity retains a namespaced game ID and bounded current/previous state. It now saves an open/closed Tome presentation state and implements GeckoLib's block-entity animation contract. Chess registers a client scene adapter that turns FEN into a low-cost 3D checkered board and cuboid chess pieces; the composed GeckoLib/block renderer animates the cover and changed chess pieces using Minecraft's normal render path.
- Server validates participant identity, turn, move legality, expected ply, payload bounds, and per-tick action rate. Match state and move history persist in world SavedData.
- Flat chess screen routes board presses directly against the custom-rendered square geometry after native buttons handle their own input. It supports click-to-select/click-to-move and cursor-tracked drag/drop, and shows legal destinations and controls for promotion/draw/resignation/rotation/return. Board and panel geometry is recalculated from the current GUI size before rendering and input hit tests.
- Chess core supports legal movement, captures, king safety, castling, en passant, promotion choices, checkmate, stalemate, insufficient material, threefold repetition, fifty-move condition, FEN, and UCI-like move history.
- Server operators can export any saved match as standard SAN PGN with `/chess pgn <match-id>`; the `.pgn` file is written beneath the world save's `arcane-games/replays` folder. The exporter includes a result marker and handles ambiguity, castling, en passant, checks, and promotion.
- A bounded standard-chess PGN parser validates SAN by replaying every move through the legal rules engine and produces immutable FEN snapshots for replay. The client can import the latest public Chess.com monthly archive and replay completed games in Minecraft. Chess.com live matches are not connected.
- Completed two-player matches update persistent server-local Elo and W/D/L totals. Run `/chess stats` to see your rating and record; live match panels also show both players' Arcane Elo. All completed matches currently count as rated; there is no casual toggle yet.
- In a singleplayer world, run `/chess practice` to open an offline computer-opponent setup with side and difficulty choices. The AI searches legal chess moves on a background worker and is not a Chess.com bot.
- When Mod Menu is installed, its Arcane Games config button opens a small settings screen where Chess.com browser links can be enabled. Without Mod Menu, the first-run welcome screen has the same opt-in toggle. Those buttons open only Chess.com sign-in/registration in the external browser; they do not sign you into Arcane or enable external games.
- First-run and Mod Menu settings now offer a public Chess.com profile connection: enter a username, fetch its published profile in the background, and save that profile name locally. This is read-only and does not authenticate or verify ownership. It stores no password or token; OAuth and Chess.com live games remain unavailable without Chess.com approval.
- The public profile page can fetch the latest published monthly Chess.com PGN archive and opens imported standard games in a read-only replay screen with previous/next ply controls. Requests are asynchronous and response-size bounded; the importer never uses browser sessions or sends moves to Chess.com.
- The loader-independent `common` module contains the tested chess rules and reusable minigame registry. Its standalone suite currently passes 44 tests covering opening move counts (including initial-position perft depth 3, Kiwipete depth 3, and a special endgame depth 4), illegal king exposure, castling, en passant, promotion, checkmate, draw detection, timeout adjudication and insufficient material, SAN/PGN import and output, clock increments and restoration, match authorization/state, Elo updates, AI move legality, minigame registry metadata, Tome deployment checks, and every square hit coordinate in both board orientations. Three SavedData codec tests remain in the Fabric project and passed in the previous full-suite run; they need rerunning together with the refactored Fabric build.

## Current limitations

The world board renders generic 3D cuboid geometry and Chess uses this path for its board and pieces. A first-pass GeckoLib opening-cover model and animation now composes with the board renderer; the full unfolding book/page animation and immersive camera remain unfinished. The flat screen uses client-drawn blocky silhouettes, file/rank coordinates, last-move tinting, destination dots/capture borders, responsive board/panel sizing, and a rotate-board control; rendering and input still need manual review in a world. Clock setup currently uses a server command, has no increment option, and still needs multiplayer runtime review. Player-profile display, approved Chess.com live integration, and Forge/NeoForge are also unfinished. Two-client move flow, in-world placement, and restart recovery still need manual verification.

The Tome's board-block art and icon are hand-authored pixel textures. No Blockbench project has been produced or visually checked yet. On first launch, a welcome screen offers guest play or opens Chess.com sign-in/registration in the default browser. That external sign-in is not linked to the mod; OAuth requires Chess.com approval. See [the integration status](docs/CHESSCOM_INTEGRATION.md). Do not treat the current jar as a public release.

The project declares GeckoLib 5.5.7 for Minecraft 26.3. The initial Tome cover model is under `assets/blockbench/` with runtime exports under `src/main/resources/assets/arcane/geckolib/`; its renderer integration compiles, but the source project and animation have not been visually reviewed in Blockbench or Minecraft. Remaining custom pieces, full page deployment, and runtime visual review are tracked in [the Blockbench handoff](docs/BLOCKBENCH_ASSET_BRIEF.md).

The reusable game-definition, board-state, and 3D scene adapter workflow is documented in [Adding minigames](docs/ADDING_MINIGAMES.md). The game menu/network/match controller still has chess-specific behavior, so additional games should remain non-playable until their own server-authoritative rules and UI are connected.

Rendering uses Minecraft's normal renderer without a required custom shader. The board FEN is parsed only when its synchronized position changes. Shader-pack and low-end performance testing remain outstanding; see [rendering and performance policy](docs/RENDERING_PERFORMANCE.md).

The first-run account panel has an opt-in toggle for external Chess.com browser links. Mod Menu integration is optional at runtime and provides the same setting from its config button. Settings link to Chess.com's OAuth application; live account login and connected-board play still require Chess.com approval. Public-profile lookup is read-only and is not account authentication. See [Chess.com integration status](docs/CHESSCOM_INTEGRATION.md) and the [emblem and account-screen design notes](docs/BRAND_CONCEPT.md). The copy-ready visual prompt pack for onboarding, game selection, match setup, flat chess, immersive chess, settings, lobby, results, and the world board is in [UI art direction and prompts](docs/UI_ART_DIRECTION_AND_PROMPTS.md). These are art direction; generated images do not replace interactive Minecraft UI code.

Chess.com profile avatars and optional cross-world matchmaking/skin display are not implemented. Their constraints and privacy-safe design are recorded in [the external integration notes](docs/CHESSCOM_INTEGRATION.md) and [the saved matchmaking concept](docs/FUTURE_CROSS_WORLD_MATCHMAKING.md).

## Build and tests

Install JDK 25. From the project root:

```powershell
./gradlew build
./gradlew test
# Run the loader-independent core tests without configuring Fabric Loom:
./gradlew -p common test --offline
```

The current target is Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.166.2+26.3, Fabric Loom 1.19, GeckoLib 5.5.7, and Gradle wrapper 9.8.2. Loom outputs development jars under `build/libs/`. The common module was compiled and its 44 tests passed on 2026-11-21. A full Fabric `test build` previously passed 47 tests before the source split, but has not yet been rerun after that refactor because open Minecraft processes hold the Loom cache. The build uses an isolated temporary Gradle home; a OneDrive path warning remains.

## Crafting

```text
Gold Ingot | Amethyst Shard | Gold Ingot
Ender Pearl | Book         | Ender Pearl
Gold Ingot | Amethyst Shard | Gold Ingot
```

## Troubleshooting

- Use Java 25.
- Run `./gradlew test` to verify the chess core.
- Use a local `GRADLE_USER_HOME` if OneDrive file locking or sync permissions interfere.
- For multiplayer, run `/chess create`, share the full match ID or printed `/chess join <ID>` command, and have the other player join on the same multiplayer server. `/chess list` lists available waiting matches. Or place/right-click the Tome and let the second player right-click it.
- To use a clock, create the match with `/chess create 10` for ten minutes per player; `/chess create` and Tome matches are untimed.
- In singleplayer, use `/chess practice` for the local offline opponent. This command is unavailable on dedicated/multiplayer servers.
- The current test suite does not launch Minecraft, verify rendering, or exercise live multiplayer.
- A development client previously reached the title screen and logged `Arcane Games initialized`; that confirms startup only. The last full Fabric build and its 47-test suite predate the common-module split. The current common module compiles and passes its 44 tests; rerun the full Fabric build after closing Minecraft clients, which currently hold a Gradle Loom cache file. Interacting with the book/board, public archive downloading in a client, playing two clients, clock display and flag-fall over multiplayer, and shader-pack visuals also remain manual checks.

## License

See [LICENSE](LICENSE).
