# Chess.com account and live-play status

Arcane's first-run client flow offers guest play, public-profile connection by Chess.com username, and an opt-in toggle for links to the real Chess.com sign-in and registration pages in the user's default browser. The profile screen requests `https://api.chess.com/pub/player/{username}` asynchronously, confirms that the returned public username matches, then stores the username and returned display name in local client preferences. It shows the saved public profile name and offers a confirmed link to that profile. If Fabric Mod Menu is installed, its Arcane config screen opens the same profile connection flow. Arcane never embeds the site or asks for/stores the Chess.com password or tokens.

The profile connection screen also offers **Import public game replays**. It requests the profile's public archive index, downloads only its latest monthly PGN endpoint, validates each standard-chess move locally, and presents the imported games in a read-only Minecraft replay screen. Requests run on a daemon worker and are bounded to 128 KiB for the index and 8 MB for PGN data. Multi-game archive parsing is covered by automated tests. This is completed-game replay only; it does not provide Chess.com bot matches, invitations, live moves, or an authenticated account link.

The username connection is a **read-only public profile association**, not account authentication: anyone can look up public usernames, so Arcane does not verify ownership. The separate browser sign-in redirect is also not authentication for Arcane; Arcane receives no login callback and cannot detect whether sign-in succeeded. Chess.com says its Published-Data API is read-only and cannot submit moves. Chess.com says account authentication and connected-board integration require applying for OAuth access. Until Chess.com grants and documents access for this project, the mod must not claim authenticated/verified accounts, Chess.com bot games, or live Chess.com opponent play.

The documented route is to apply through [Chess.com's OAuth access form](https://docs.google.com/forms/d/e/1FAIpQLSds2AeKLj9xqgu96Pu-rEAS0ItyqDbZbSgUFer0Mo6qMRx4Jg/viewform?usp=send_form), linked from its [official OAuth and connected-board announcement](https://www.chess.com/blog/CHESScom/chess-com-oauth-login-connected-board-application), then receive approval and integration instructions. The Arcane settings page now opens that application form directly. The project cannot treat an external browser sign-in or a public username lookup as an authenticated Arcane connection.

No public endpoint for joining a Chess.com bot game or submitting bot-game moves is documented. Chess.com's current User Agreement prohibits unauthorized automated access and third-party software designed to modify its services. A hidden or embedded browser that reads the website board or submits moves would be an unapproved automation workaround and is not part of Arcane. Arcane's offline practice opponent is separate and is named the Arcane opponent; it is not a Chess.com bot. Live Chess.com play must wait until Chess.com approves an application and provides the supported authentication and connected-board contract.

The guest option uses Arcane's own offline/server chess implementation. Local matches can be watched in the chess screen with `/chess spectate <match-id>` or by interacting with an active Tome. These are Arcane matches, not Chess.com matches.

The documented public player-profile response includes an optional avatar URL. Arcane currently does not fetch or render that image. A future avatar display should be opt-in per viewer, default off, local-hideable, fetched only from the documented profile field and trusted host, bounded and cached temporarily, and replaced with a generic hologram when unavailable. See [the cross-world matchmaking and profile-display concept](FUTURE_CROSS_WORLD_MATCHMAKING.md).

Sources:

- [Chess.com PubAPI guide](https://support.chess.com/en/articles/9650547-what-is-the-pubapi-and-how-do-i-use-it)
- [Chess.com Published-Data API profile endpoint](https://www.chess.com/news/view/published-data-api#pubapi-endpoint-player)
- [Chess.com OAuth / login / connected-board access announcement](https://www.chess.com/blog/CHESScom/chess-com-oauth-login-connected-board-application)
- [Chess.com bot gameplay help page](https://support.chess.com/en/articles/8614091-how-can-i-play-against-the-chess-com-bots)
- [Chess.com Fair Play Policy](https://www.chess.com/legal/fair-play)
- [Chess.com User Agreement, AI/automated access restrictions](https://www.chess.com/legal/user-agreement)
