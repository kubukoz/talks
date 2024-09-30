Feature status:

- [x] Playback
- [x] Play/pause
- [x] Hold
- [x] Transposing by semitone
- [x] Transposing by octave
- [x] Websocket connection (retriable)
  - [ ] More problem-immune retries (graceful startup in case of initial failure etc.)
- [ ] Better UI
- [ ] Maybe some state reconciliation beyond broadcasting from leader

# Instructions

1. Make sure server is up (see instructions in `../server`)
2. Tab 1: `scala-cli package . -f` (`-w` for watch mode)
3. Tab 2: `npm start`
4. Tab 3: `caddy run` (yeah I don't like this either)
5. Tab 4 (later): `ngrok http 2222` (exposes caddy)
