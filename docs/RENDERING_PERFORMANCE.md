# Rendering and performance policy

## Shader-pack compatibility

Arcane Games will use Minecraft's standard block model, texture, lighting, and block-entity submission paths. The chess renderer must not require a custom GLSL/core shader, private framebuffer, or direct OpenGL state changes. This lets shader packs handle the Tome and pieces through their normal terrain/block-entity passes. It is a compatibility target, not a claim that every shader pack has already been tested.

Iris documents that custom shaders supplied by mods can conflict with shader-pack programs and recommends avoiding them where possible: [Iris core shader compatibility](https://github.com/IrisShaders/Iris/blob/26.1/docs/development/compatibility/core-shaders.md). We will use no custom shader in the baseline. If a future effect needs one, it must have a normal Minecraft-rendered fallback and remain optional.

## Runtime budgets and rules

- Chess rules are computed only on match actions, never in a render callback.
- Network updates carry bounded match state; animation is interpolated locally between updates.
- Parse the board position only when its FEN changes. Render state caches the 64-square arrays; it must not allocate arrays or scan move history every frame.
- Keep common geometry and textures shared. Do not create one texture/material per piece or square.
- Use Minecraft's normal frustum/distance culling and lighting. Avoid always-on particles, per-frame world scans, and global render hooks.
- Any animation must be time-based, bounded, and gracefully become static after its duration.
- Client rendering code stays in `src/client`; dedicated servers must not load client or graphics classes.

## Graphics options planned

The game should remain readable at low settings. A compact client preference set is planned: animation (off/normal/smooth), particles (off/low/normal), board detail (low/normal), and immersive camera effects (off/on). Low mode should use the same chess state and legal-move display, with simpler highlights and no decorative particles. Settings must not change server rules or match synchronization.

## Verification matrix

Shader compatibility cannot be established from source alone. Before release, launch on the target Minecraft version with:

| Renderer configuration | Required check |
| --- | --- |
| Vanilla renderer, no shader pack | Board/pieces render, are lit, animate, and disappear when culled/unloaded |
| Sodium without shaders | Same visible board and interactions |
| Iris with at least one supported shader pack | No missing board/piece geometry, broken transparency, or render crash |
| Iris with shader pack disabled | Runtime falls back to standard Minecraft rendering |

Record the actual game, loader, Sodium/Iris and shader-pack versions when these checks are run. Until then, compatibility is designed for but unverified.
