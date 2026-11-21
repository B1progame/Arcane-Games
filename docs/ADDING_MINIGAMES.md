# Adding another minigame

Arcane Games keeps game identity and presentation behind small adapters so a new tabletop game does not need to be forced into chess's board renderer. Chess is the first registered game and the reference implementation.

## Current extension points

1. Add a `MinigameDefinition` in the common source set. Give it a namespaced ID, display text, player limits, `playable()` status, and a `MinigamePresentation` (`SCREEN_ONLY`, `WORLD_BOARD`, or `HYBRID`). Register it during common initialization with `MinigameRegistry.register(...)`.
2. Store the game's bounded serialized state in `GameBoardBlockEntity`. It records the game ID, current state, previous state, and transition timestamp. The synchronized state limit is 8192 characters.
3. In client initialization, register a `BoardSceneSource` for the same ID with `BoardSceneRegistry.register(...)`.
4. Have the source convert only the two supplied state snapshots into an immutable `BoardScene`: grid dimensions, board colors, cuboid token models, and at most one movement animation. The shared `BoardSceneRenderer` handles the Minecraft world geometry and animation.
5. Add game-specific rules, server actions, validation, screen, persistence, and interactions as their own adapters. The common registry and shared renderer do not make those game systems automatic.

## UNO and card games (future idea)

UNO is a good future Tome game, but it needs a card-table view rather than the chess grid. A hybrid UNO view could show the shared draw pile and discard pile on a small world table, while the player's hand appears only in that player's screen. The server must own the shuffled deck and private hands; synchronized block state and spectator updates must never reveal opponents' hidden cards. The rules controller should validate turn order, color choice, draw/skip/reverse effects, and any server-selected house-rule set. This is a design note only; UNO is not registered or implemented yet.

Other candidates include Checkers, Connect Four, and Tic-Tac-Toe. Each should select the presentation that fits its interaction instead of inheriting Chess's assumptions.

## Scene model

`BoardScene` uses cell coordinates and ARGB colors. Each token is a list of normalized cuboids, allowing simple block-built models to share one renderer. A movement names the moving token and its source/destination cells. The renderer submits solid Minecraft geometry, so it uses the active Minecraft lighting and shader pipeline; it does not require a separate shader or texture atlas.

Keep game logic out of `src/client`. Client scene sources must be safe for malformed or unknown serialized state and should return no scene rather than throw. Parse state only when the block entity's state changes; never calculate legal moves per render frame.

## Known boundary

This is a renderer/state extension foundation, not yet a complete third-party game plugin system. The Tome interaction currently creates chess matches, and the screens/network payloads still implement chess. Future work should introduce game-specific match controllers and payload dispatch before adding another playable entry to the Tome menu. Until then, definitions that have no complete controller should report `playable() == false` and must not be presented as launchable.
