# ThinWS-Android
This is a client library for use with [ThinWS](https://github.com/cloverstudio/ThinWS) websocket server.

Instantiating the client with a websocket url and a unique user connection ID will automatically attempt to establish a connection. There is also a listener that will be called on socket events (new message, open, close etc.).

```
private const val SOCKET_URL = "ws://192.168.1.119:8000"

private val socketManager = ThinWS(SOCKET_URL, connectionID, listener)
```

Calling the `close()` method on the client instance will close the connections
```
socketManager.close()
```

There are two methods for sending socket messages to the server:
- Asyncshronous, using observables
```
socketManager.request(
                Message(
                    type = MESSAGE_GENERAL,
                    roomID = "roomID",
                    messageID = "messageID"
                )
            ).subscribe()
```

- Synchronous (needs to be called off the main thread)
```
socketManager.syncRequest(
                Message(
                    type = MESSAGE_GENERAL,
                    roomID = "roomID",
                    messageID = "messageID"
                )
            )
```
