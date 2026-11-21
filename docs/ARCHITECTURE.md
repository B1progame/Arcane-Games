# Architecture decisions

## Initial target

The project targets Minecraft Java Edition 26.3 and Java 25. The Fabric setup uses the official Fabric Loom workflow. The `common` Java library module contains the chess rules, AI, PGN parser, match and clock models, board-coordinate mapping, and minigame registry. It has no Minecraft or loader imports, so platform adapters can depend on the same tested logic. Fabric remains the only implemented Minecraft platform.

## Chess state

`ChessPosition` owns a compact 64-square position, side to move, castling rights, en-passant square, half-move clock, full-move number, move history, and repetition counts. Legal move generation filters pseudo-legal moves by simulating them and rejecting any that leave the moving side's king attacked. `fromFen` and `toFen` cover position interchange. Tests include start-position perft through depth three and explicit promotion choice validation.

The rules core is synchronous and independent of rendering. `ChessMatch` and `ChessNetworking` put move acceptance on the logical server; client UI sends intents, never a claimed board state. World `SavedData` stores match metadata, FEN, and move history, and reload reconstructs the position by replaying and validating the history. `/chess create`, `/chess list`, `/chess join <id>`, `/chess resume <id>`, and `/chess spectate <id>` form the current server-hosted play and recovery path. Dedicated invitation notifications and replay exports remain future work.

## Loader scope

Only Fabric is implemented. Forge and NeoForge 26.3 have published builds, but a second loader requires its own verified Gradle plugin, entry point, registration, networking, and client setup. This repository does not label Forge or NeoForge as supported. `common/settings.gradle` lets the plain Java rules module run its own tests without configuring Fabric Loom or locking the game's development cache.

## Rendering and assets

The Tome is a placeable one-block anchor for a chess surface and is associated with a saved match through a generic game-board block entity. A common minigame definition registry identifies games; a client scene-source registry translates per-game snapshots into immutable `BoardScene` data. The shared renderer submits batched solid cuboids for tiles, frames, and tokens and interpolates a moving token. Chess currently supplies block-built chess-piece geometry. The entity now implements GeckoLib's `GeoBlockEntity`; its renderer composes the GeckoLib Tome model with the dynamic chess scene. The Tome's open state is separate from chess state, synchronized, and saved with the block entity.

The block-entity renderer composes GeckoLib 5.5.7's `GeoBlockRenderer` with Minecraft's submit-node path for the board tiles and pieces. Its render state caches parsed 64-square arrays by FEN and derives the moving piece once when a new position arrives. An initial GeckoLib project, model, texture, and open clip are now in the repository. This is a first-pass closed-cover animation, not yet the complete unfolding book or modelled pieces. It has compiled but has not been visually checked in a launched game. Shader-pack compatibility remains unverified; see [rendering and performance policy](RENDERING_PERFORMANCE.md).
