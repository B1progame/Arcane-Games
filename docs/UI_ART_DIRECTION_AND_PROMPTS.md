# Arcane Games UI art direction and generation prompts

Use these as visual reference prompts for concept images or mockups. The actual interface must remain a responsive, interactive Minecraft screen: use the image as art direction only, then implement text, hit targets, focus, and state with Minecraft widgets/renderers. Do not bake generated words, a chess logo, or button labels into a background image.

## Shared art direction (apply to every screen)

A polished vanilla-Minecraft fantasy tabletop interface: crisp pixel art, block-built ornament, dark aubergine ink surfaces, warm parchment, oak/espresso wood, antique gold trim, tiny amethyst accents, restrained enchantment glow. Use the Minecraft pixel font and vanilla button texture, with custom gold framing and hover/focus states. Keep contrast strong, spacing consistent, corners square or stair-stepped, and decoration behind content. No glassmorphism, gradients that look like a web dashboard, photorealism, modern sans-serif UI, Chess.com board colors/assets, copied brand logos, emoji icons, or text baked into the art. Animation should be brief and purposeful: page unfold, rune shimmer, piece lift; respect reduced-motion/performance settings.

Palette reference from the existing screen: ink `#1C1724`, deep shadow `#09070D`, parchment `#D8D0E2`, warm gold `#E0B759`, quiet plum `#54412E`, wood squares `#D8C59F` and `#62513F`, legal move green `#73834C`, check/capture red `#B85C5C`. Keep colors as flat pixel clusters, not gradients.

## Prompt 1 — First-run account / guest choice

```text
Create a 16:9 Minecraft Java Edition pause-screen concept for a fantasy chess mod named Arcane Games. Show a dimmed blurred Minecraft world behind one centered compact dark aubergine panel with a thin warm-gold top rule and subtle blocky corner ornaments. Panel hierarchy: small gold ARCANE GAMES eyebrow, clear title “Chess.com profile” (leave text areas clean for game-rendered pixel font), two short reassurance lines, one horizontal pair of actions for “Chess.com sign in” and “Create account”, then a wider distinct “Play as Guest” action. Keep all text and controls inside the panel with generous separation; no overlap. Include a small independent-project note at the bottom. Use vanilla Minecraft pixel font/button styling, warm parchment body text, gold primary accent, sharp readable 1-pixel borders. No logos, no fake Chess.com branding, no extra controls, no baked text if image text fidelity is uncertain.
```

## Prompt 2 — Arcane Tome game selection

```text
Design a Minecraft fantasy game-selection screen as if an enchanted tome has opened on a wooden table. Center an open blocky book with parchment pages and gold corners; the left page has a short “Choose a game” heading and a clean vertical list. Chess is the single available, clearly focused card with a small pixel chessboard illustration and “Ready to play” state. Other future-game slots should appear as quiet sealed page tabs without names, fake buttons, or claims they are playable. Put Back/Close in predictable lower corners. Deep plum ink, warm parchment, oak, antique gold and a tiny amethyst accent. Keep content aligned to an 8-pixel rhythm and leave all labels as clean text areas for Minecraft's pixel font. No web UI, no photorealism, no modern cards floating outside the book.
```

## Prompt 3 — Match setup / local play

```text
Create a Minecraft-native Arcane Chess match setup screen with the open magical Tome motif. Show three large clear selectable modes: Local two-player, Play vs Computer, and Replay a saved/imported game. Below the selected mode, reserve a compact settings area for side/color, difficulty or time control only when that mode supports it. Provide one strong Start button and a Back button. Locked or unavailable features must look disabled and say so; do not invent a working Chess.com live mode. Use blocky parchment panels, ink-plum background, gold focus border, warm oak and simple pixel piece silhouettes. Keep labels readable and all controls widely spaced, with keyboard focus visible.
```

## Prompt 4 — Flat chess match screen

```text
Design a polished 2D chess game screen for Minecraft Java Edition. Use a large perfectly square 8x8 board at left, warm oak/parchment and dark walnut squares, coordinate labels on the frame, a thin gold selected-square outline, subtle green legal-destination dots, and a distinct last-move tint. At right, use a narrow parchment/ink side panel with player names and white/black markers, a strong current-turn strip, captured-piece rows, readable compact move history, optional clock area, and clearly separated Offer Draw and Resign controls. Add a small return-to-world action and a settings gear with a text label. Do not copy Chess.com layout, colors, icons, or piece art. Pixelated Minecraft sculpted-piece silhouettes, high contrast, square corners, no excess ornament. Leave exact text as clean spaces for native UI rendering.
```

## Prompt 5 — Immersive 3D board HUD

```text
Create an in-game screenshot concept for Arcane Games immersive chess: a Minecraft-style 3D board unfolded from an open enchanted tome, viewed from a low cinematic over-the-pieces angle. Pieces are charming stepped cuboid sculptures with clear pawn, rook, knight, bishop, queen and king silhouettes, not realistic chess figurines. A selected piece has a restrained gold outline; legal destinations are small green runes on squares; the last move has a translucent warm tint. Keep only a minimal HUD at the screen edges: turn/player names at top, camera preset and zoom hint at one corner, cancel/return hint at another. The world remains visible around the board. No giant reticle, no full-screen opaque panel, no custom shader look, no intrusive particles, no text baked into the image.
```

## Prompt 6 — Settings and appearance

```text
Design one reusable Minecraft settings screen for Arcane Games, using a parchment page mounted inside a dark plum frame. Sections: Board theme preview with Classic Wood, Royal Stone, Enchanted; Piece style preview with Minecraft Classic, Royal, Enchanted; Interface mode; Camera preset; animation speed; sound; particles; performance detail; coordinate/orientation toggle; profile image visibility toggle default off. Show one selected row clearly, use labeled sliders/toggles and pixel previews, and keep Apply/Done/Reset aligned at the bottom. All controls must be native-looking and keyboard-focusable. Use flat pixel art, oak/gold/amethyst accents, no glass cards, no emoji, no tiny low-contrast captions. Treat this as layout art direction; do not imply the settings are implemented unless enabled in the actual screen.
```

## Prompt 7 — Multiplayer lobby and spectator state

```text
Create an Arcane Games chess lobby screen styled like an enchanted Minecraft book. Show host and guest seats with white/black stone-piece silhouettes, player names, color assignment, optional clock selection, invite/join code with a copy affordance, and a clearly visible waiting/ready state. Add a spectator count and host-controlled allow-spectators toggle. The Start/Join action is prominent; Cancel is separate. Use short helper text and never show secrets, passwords, or external account tokens. Dark ink, parchment, gold dividers, small blocky runes, high contrast, no modern social-media layout. Do not show unsupported online services as available.
```

## Prompt 8 — Match result and stats

```text
Design a Minecraft fantasy chess result screen after a match. Let a small blocky king/queen piece pair and restrained confetti-like rune particles frame the result without obscuring it. Present Win, Draw, or Loss as the main heading, opponent and color beneath, then a compact stat row for Arcane Elo before/after and wins/draws/losses. Add Review Game, Rematch, Return to World actions only if the actual game state supports them. Use clear green/gold/red states plus text labels so color is not the only signal. Dark plum and parchment, pixel font, squared vanilla-style buttons, no casino imagery, no coins, wagers, or fake rewards.
```

## Prompt 9 — Live board / spectator presentation

```text
Create a world-rendering reference for a Minecraft chess table: an elegant oak-and-dark-wood 8x8 board built from square pixel geometry, subtle gold frame, small rank/file marks, and stylized ivory and charcoal cuboid chess sculptures. Leave clear room above the board for a very small optional hologram label showing player names and turn. Selected and last-move squares use subtle flat overlays; legal moves use small discrete runes. Keep the world lighting and shadows believable for vanilla Minecraft; no bloom-dependent detail, no custom-shader-only glow, no giant particles. The board remains legible from a few blocks away and does not replace surrounding blocks.
```

## Handoff process

Generate one concept at a time at 16:9, then review the text-free layout before implementing it. Send the image/reference and tell me which screen it represents. Keep generated art as a guide or decorative texture; labels, board squares, legal moves, player names, buttons, focus states, and dynamic content must be drawn by code so they scale, localize, animate, and remain interactive.
