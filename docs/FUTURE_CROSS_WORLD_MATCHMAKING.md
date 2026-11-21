# Saved concept: optional Arcane cross-world chess queue

**Status: idea only; not implemented or promised.** This records the requested possibility for a later design review.

## Player experience

- An opt-in setting lets a player enter or leave an Arcane chess queue. It is off by default and available only when the future service is operational.
- A player can queue from a singleplayer world or a participating multiplayer server, choose casual/rated and a time control, and cancel at any time.
- On a match, both clients are invited to a private Arcane match session. Players explicitly accept before leaving their current world. The game can also remain local to a server when both players are already there.
- Spectators can join only if both players/server rules allow it. The physical Tome shows the live board and a clear spectator opt-out.
- Results update the appropriate Arcane rating only after a server-authoritative finished match. Chess.com ratings or games are not mixed into Arcane Elo.

## Possible skin and profile display

- Displaying an opponent's Minecraft skin on the board is optional and client-side. Do not upload skin textures as match state or replace the actual player skin. Prefer resolving the player's public Minecraft profile texture from the signed session profile, with an explicit consent/visibility policy and a generic piece silhouette fallback.
- Chess.com public profile avatars are a separate opt-in display choice. The viewer's local setting defaults off, hides the image from that viewer without affecting other viewers, and always has a neutral fallback. Do not proxy arbitrary URLs or permit user-provided image URLs. Restrict any future fetch to the official documented profile field and trusted image host, bound download size and time, cache briefly, and do not keep a permanent copy.
- Since profile images and skins are user-generated content, the safe default is not to display them. A viewer who enables them accepts that content may be unsuitable; server packs may disable them entirely.

## Service architecture required before implementation

Cross-singleplayer matching cannot be achieved by a client-only mod or by opening LAN. It requires an independently operated relay/matchmaking service, an authenticated session protocol, operational hosting, abuse reporting, privacy/retention policy, service security review, and ongoing cost/support. The service must never request Minecraft or Chess.com passwords. Queue tickets should contain only a short-lived opaque account/session identifier, region/ping preference, time control, and rating band; avoid sending world save data or unrelated profile fields.

The service should allocate/authorize a dedicated match session or relay moves between two connected authoritative hosts. It must enforce bounded packets, replay protection, rate limits, disconnect/reconnect policy, and no client-authoritative result claims. Offline singleplayer must remain available if the service is unreachable. Queue and account linking must be independently optional.

## Go/no-go checklist

Before implementation, decide who operates and pays for the service, whether it is necessary for the project's privacy/support burden, supported regions and versions, consent and moderation policy for profile content, and how users can delete service data. Build only after a threat model and a no-service fallback are agreed. Until then, local-server chess remains the implemented multiplayer model.
