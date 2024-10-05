# Instructions

1. Make sure `server` is running (see its readme)
2. Tab 1: `sbt docs/mdoc --watch`
3. Tab 2: `./serve.sh`
4. Open `localhost:9090`, ideally in anything but Firefox (Chrome is fine) - we need [WebSocketStream][wss] support (there's a fallback to plain `WebSocket`).

[wss]: https://developer.mozilla.org/en-US/docs/Web/API/WebSocketStream
