# Blockbench handoff: animated Arcane Tome and chess set

The runtime target is **GeckoLib 5.5.7 for Minecraft 26.3**. Keep the style blocky and readable at Minecraft distance. Use cuboids and a 16/32/64 pixel texture atlas; avoid high-poly meshes, smooth shading, and tiny details that disappear in-game.

An initial editable source project is now present at `assets/blockbench/arcane_tome.bbmodel`, with its texture and GeckoLib export in the paths below. It has one opening-cover clip and is a technical starting point only: it has not been reopened in Blockbench or visually checked in Minecraft, and it does not yet include the deployed page geometry or piece models listed below. Continue the project by expanding the same source file; preserve `root`, `cover`, and the existing `animation.arcane.chessboard.open` runtime clip or update the Java controller and export together.

The Tome is anchored to one placed block entity. Its deployed board can extend to about a 2×2-block visual footprint around that anchor. The model must not imply terrain is removed. The code will check the space before deployment. Leave the underside at model Y=0, centered on the anchor; X/Z are centered, and -Z is the front. Minecraft uses 16 model pixels per block.

## Make these Blockbench projects

```text
assets/blockbench/arcane_tome.bbmodel
assets/blockbench/pieces/pawn.bbmodel
assets/blockbench/pieces/rook.bbmodel
assets/blockbench/pieces/knight.bbmodel
assets/blockbench/pieces/bishop.bbmodel
assets/blockbench/pieces/queen.bbmodel
assets/blockbench/pieces/king.bbmodel
```

Use the GeckoLib Animated Model project format for `arcane_tome`. Use Generic Model for the six piece projects if they are static reusable geometry. Reopen every `.bbmodel` before handoff and ensure all textures remain linked. Include a front and three-quarter textured screenshot for each project. These screenshots are for visual review; they do not replace the editable projects or exports.

## Tome and unfolded board model

`arcane_tome.bbmodel` is one model for the placed closed book, open selection book, and deployed chess scene. Keep the persistent pieces and moving parts in separate bones, with these exact names:

```text
root
book_base
book_spine
cover_left
cover_right
pages_left
pages_right
board_deck
board_frame
tiles
piece_anchor_00 ... piece_anchor_31
```

The 32 piece anchors are empty bones positioned over the starting squares; the Java renderer will place each live piece from authoritative chess state. Do not bake the starting position into the book texture. The chessboard has 64 equal squares, a one-pixel trim, oak and espresso wood, restrained gold corner details, and no painted highlights. Make the deployed squares large enough that the tallest king stays visually distinct. The 2×2-block board surface corresponds to 32×32 model pixels. The book before deployment should fit within the anchor block's 16×16 footprint.

Create these named GeckoLib clips. Keep transitions deterministic and short; the server selects the scene state, while clients play the corresponding clip locally. No animation may change the chess rules or position.

| Clip name | Intended motion |
|---|---|
| `animation.arcane_tome.idle_closed` | Subtle, near-static closed-book settle; no constant bobbing |
| `animation.arcane_tome.open_menu` | Covers hinge open, pages fan to menu pose |
| `animation.arcane_tome.deploy_board` | Pages spread, board deck unfolds from the book, gold trim settles |
| `animation.arcane_tome.close_board` | Board folds safely back to open book |
| `animation.arcane_tome.piece_appear` | Piece anchors lift pieces into their start squares with a small stagger |
| `animation.arcane_tome.move_piece` | One piece rises, travels, and settles; code supplies source/destination |
| `animation.arcane_tome.capture_piece` | Captured piece shrinks/fades or lifts away, then moving piece settles |
| `animation.arcane_tome.castle` | King and rook move together |
| `animation.arcane_tome.victory` | Brief restrained gold/amethyst flourish; no continuous particles |

For dynamic moves, keep the geometric rest pose at its anchor. The animation/controller must allow source and target coordinates to be supplied by code; do not create one hard-coded animation per chess square. Use separate left/right side piece textures or leave material regions addressable for Java-applied side themes. Avoid transparent full-board overlays so shader packs and depth sorting remain predictable.

## Piece projects

Each piece project should have a centered underside pivot, footprint no larger than 3×3 model pixels (one chess square is 4×4 pixels), and a base at Y=0. Suggested maximum heights: pawn 7 px, rook 8 px, knight 9 px, bishop 10 px, queen 11 px, king 12 px. Build recognizable silhouettes only from stacked/rotated cuboids: battlement rook; angular horse-head knight; marked split-top bishop; layered queen crown; cross-crown king; stepped, cube-rounded pawn head. Use a shared geometry language so classic, royal, and enchanted palettes can be applied without tripling runtime geometry.

## Export into the mod

Keep the `.bbmodel` sources under `assets/blockbench/`. Also export the GeckoLib 5 runtime model, animation JSON, and PNG using the GeckoLib plugin. The target runtime paths are:

```text
src/main/resources/assets/arcane/geckolib/models/block/chessboard.geo.json
src/main/resources/assets/arcane/geckolib/animations/block/chessboard.animation.json
src/main/resources/assets/arcane/textures/block/chessboard.png
src/main/resources/assets/arcane/geckolib/models/pieces/<piece>.geo.json
src/main/resources/assets/arcane/textures/pieces/<piece>.png
```

Use the GeckoLib 5 export format, not Bedrock entity exports or Minecraft Java Block/Item JSON. Preserve bone names, UVs, pivots, and animation names exactly. Do not add particle or sound keyframes until the Java events are wired; send those ideas as notes so they cannot silently fail in-game.

The build declares GeckoLib 5.5.7. `GameBoardBlockEntity` now implements `GeoBlockEntity`; `BoardSceneRenderer` extends `GeoBlockRenderer` and submits the existing dynamic chess scene after the GeckoLib model. The opened/closed presentation state is synchronized and persisted. The initial model and animation compile as packaged resources, but runtime loading, animated-world visual review, and performance/shader checks remain pending. The separate chess-piece cuboids are still code-built and do not yet use GeckoLib bones.

## Send these first

Start with `arcane_tome.bbmodel`, its PNG, the deployed board geometry, and pawn/rook/knight/bishop/queen/king projects. Include exported runtime files and screenshots. I will validate file paths, bone names, JSON structure, texture dimensions, and the game build before those assets are treated as integrated. If you cannot create empty anchor bones in the GeckoLib project, say so; the runtime can draw the pieces separately while GeckoLib animates the Tome and board.
