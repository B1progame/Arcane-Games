# Development roadmap

## Completed and verified in source

- Fabric 26.3 / Java 25 project and Gradle build.
- Chess rules core with FEN, move history, legal move validation, special moves, check/mate/stalemate, repetition and draw conditions.
- Server-authoritative match controller, bounded networking payloads, command-based join/resume/spectate, world SavedData persistence.
- Flat GUI prototype with legal destination highlights, a four-choice promotion picker, draw offers, resignation, and synchronized state updates for players and active spectators.
- Flat chess rendering uses distinct blocky silhouettes instead of ambiguous one-letter glyphs, with coordinates, last-move tint, destination/capture indicators, responsive board/panel sizing, click and drag input, tested square mapping in both orientations, and a rotate-board control.
- Placeable Tome board block with block-entity match association and crafting recipe.
- First-run Chess.com/guest welcome screen; external browser pages are opened without capturing credentials, and status text clearly says account linking is unavailable pending approval.
- Loaded Tome block entities keep a synchronized FEN and render an animated board projection to nearby clients; live match spectators receive authoritative screen updates.
- Board parsing and move-delta detection are cached when synchronized FEN changes instead of being repeated every render frame. Renderer uses Minecraft's normal submit path and does not add a custom shader dependency.
- Server-local Elo and W/D/L statistics are persisted per UUID; `/chess stats` reports the caller's record. Every completed two-player match currently counts as rated; a casual/rated toggle is still needed.
- A local-only `/chess practice` flow opens an offline computer opponent with side/difficulty selection. Search is bounded and runs on a client worker; this is not Chess.com integration.
- `gradlew test build` succeeds with 26 JUnit tests and no failures, including chess rules/perft, match behavior, Elo, offline AI legality/mate, SavedData round trips, minigame registration, and board coordinate mapping. Dev client logged Arcane initialization. Manual gameplay, drag interaction, and multiplayer remain unverified.

## Remaining implementation

1. Improve match UX: invites, names, move list, clocks, spectators, disconnect/reconnect policies and end-to-end multiplayer verification.
2. Replace letter glyphs with actual 3D board-piece rendering and move/capture animation; produce verified Blockbench source and exported models.
3. Add the magical tome unfolding animation, 3D camera controls, and theme/config screens.
4. Improve the initial offline opponent, add achievements, rating modes, and progression beyond Elo/W-D-L.
5. Add permitted read-only Chess.com data/PGN import where API terms allow. Live bot play requires documented Chess.com OAuth/connected-board approval; no move-submission endpoint is public.
6. Establish a separate Forge/NeoForge implementation after verifying current 26.3 mappings and loader APIs; do not advertise it until it builds.
7. Test dedicated-server startup, save/reload, resource reload, and two-client multiplayer in game.
8. Verify vanilla, Sodium, and Iris shader-pack rendering in a launched client; performance and shader compatibility are targets, not yet verified claims.

Chess registers as `HYBRID` through the game registry. Future minigames can choose screen-only, world-board, or hybrid presentation. UNO is a candidate for a future hybrid card table, with strict per-player hand privacy; it is not currently registered or playable. See [Adding minigames](ADDING_MINIGAMES.md).

The possible optional cross-world queue and skin display are recorded as an unimplemented concept in [FUTURE_CROSS_WORLD_MATCHMAKING.md](FUTURE_CROSS_WORLD_MATCHMAKING.md).
