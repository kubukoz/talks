# Instructions

1. Tab 1: `sbt docs/mdoc --watch`
2. Tab 2: `PORT=9090 marp -w -s --html=true myproject-docs/target/mdoc/`
3. Open `localhost:9090` in anything but Firefox (Chrome should do) - we need [WebSocketStream][wss] support.

[wss]: https://developer.mozilla.org/en-US/docs/Web/API/WebSocketStream
