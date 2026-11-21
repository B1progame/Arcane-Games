# Chess.com OAuth / Connected Board application draft

Use this as a truthful draft for the Chess.com application. Arcane's current build supports public-profile lookup and completed-game PGN replay only. It does not yet sign in to Chess.com or read or send live moves. Describe live support as a proposed feature that will be implemented only if Chess.com approves access and supplies its integration requirements.

## Application Name

Arcane Games

## Briefly describe your application and why it requires Chess.com OAuth or Connected Board access

> Arcane Games is an independent Minecraft Java Edition mod by B1progame. It provides a Minecraft chessboard, local multiplayer chess, an offline practice opponent, and read-only replay of publicly available Chess.com games. The current prototype does not connect to Chess.com accounts or live games.
>
> We are applying for OAuth / Connected Board access to investigate an approved way for a player to view and play their own Chess.com game through the Arcane chessboard. The public Published-Data API is read-only, so it cannot support a live connected board. We will implement live functionality only using the authentication, permissions, and move protocol Chess.com approves. We will not collect Chess.com passwords, scrape logged-in pages, automate the website, or provide engine assistance during Chess.com games. If access is not granted, the mod will continue to offer only its independent local chess features and public-game replays.

## Link to project in progress

https://github.com/B1progame/Arcane-Games

This is the public in-progress repository. The README describes the current prototype and its limitations.

## Public Logo URL (optional)

Leave blank until the Arcane Games logo is hosted at a stable public HTTPS URL. The repository's local icon cannot be used as a public URL.

## Terms of Service URL (optional)

Leave blank only if you are comfortable accepting Chess.com's terms for yourself and your users, as the form states. Otherwise, publish a project-specific Terms page first and provide its public HTTPS URL.

## Privacy Policy URL (optional)

The form says this is not required, but a public policy is advisable before requesting OAuth. Do not link to a local file. Publish a policy that accurately explains what account data is requested, whether tokens are stored, retention/deletion, server versus client processing, and contact details. Those implementation choices are not finalized in the current prototype, so do not claim that Arcane currently stores or protects OAuth tokens.

## Are you creating a Connected Board app?

Select **Yes** if requesting Chess.com games to be displayed or played through the Minecraft board. That is the intended connected-board use. If you are applying only for account sign-in without live board functionality, answer according to the actual scope of the application instead.

## Before submitting

- Replace the project-link placeholder with a real public URL you control.
- Review every statement and make sure it still describes the build you intend to ship.
- Do not include Chess.com credentials, access tokens, or private account information in the form.
- Do not promise OAuth behavior until Chess.com supplies and approves the needed protocol.
