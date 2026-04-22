# Protocol description

This domain.client-domain.server protocol describes the following scenarios:
- Setting up a connection between domain.client and domain.server.
- Broadcasting a message to all connected clients.
- Periodically sending heartbeat to connected clients.
- Disconnection from the domain.server.
- Handling invalid domain.client.messages.

In the description below, `C -> S` represents a message from the domain.client `C` is sent to domain.server `S`. When applicable, `C` is extended with a number to indicate a specific domain.client, e.g., `C1`, `C2`, etc. The keyword `others` is used to indicate all other clients except for the domain.client who made the request. Messages can contain a JSON body. Text shown between `<` and `>` are placeholders.

The protocol follows the formal JSON specification, RFC 8259, available on https://www.rfc-editor.org/rfc/rfc8259.html

All domain.client.messages may end using Linux line endings (\n) or windows line endings (\r\n) and domain.client and domain.server should interpret both cases as valid domain.client.messages.

# 1. Establishing a connection

The domain.client first sets up a socket connection to which the domain.server responds with a welcome message. The domain.client supplies a username on which the domain.server responds with an OK if the username is accepted or an ERROR with a number in case of an error.
_Note:_ A username may only consist of characters, numbers, and underscores ('_') and has a length between 3 and 14 characters.

## 1.1 Happy flow

Client sets up the connection with domain.server.
```
S -> C: HI {"version": "<domain.server version number>"}
```
- `<domain.server version number>`: the semantic version number of the domain.server.

After a while when the domain.client logs the user in:
```
C -> S: LOGON {"username":"<username>"}
S -> C: LOGON_RESP {"status":"OK"}
```

- `<username>`: the username of the user that needs to be logged in.

To other clients (Only applicable when working on Level 2):
```
S -> others: JOINED {"username":"<username>"}
```

## 1.2 Unhappy flow
```
S -> C: LOGON_RESP {"status":"ERROR", "code":<error code>}
```      
Possible `<error code>`:

| Error code | Description                              |
|------------|------------------------------------------|
| 5000       | User with this name already exists       |
| 5001       | Username has an invalid format or length |      
| 5002       | Already logged in                        |

# 2. Broadcast message

Sends a message from a domain.client to all other clients. The sending domain.client does not receive the message itself but gets a confirmation that the message has been sent.

## 2.1 Happy flow

```
C -> S: BROADCAST_REQ {"message":"<message>"}
S -> C: BROADCAST_RESP {"status":"OK"}
```
- `<message>`: the message that must be sent.

Other clients receive the message as follows:
```
S -> others: BROADCAST {"username":"<username>","message":"<message>"}   
```   
- `<username>`: the username of the user that is sending the message.

## 2.2 Unhappy flow

```
S -> C: BROADCAST_RESP {"status": "ERROR", "code": <error code>}
```
Possible `<error code>`:

| Error code | Description            |
|------------|------------------------|
| 6000       | User is not logged in  |

# 3. Private message

## 3.1 Happy flow

```
C -> S: PRIVATE_REQ {"message": "<message>", "receiverUsername": "<username>"}
S -> C: PRIVATE_RESP {"status": "OK", "code": "0"}
```

- `<message>`: the message that must be sent to a specific user 
- `<username>`: the username of the specific user to whom the message must be sent

The domain.client with the specified username receives the message as follows:
```
S -> C: PRIVATE  {"username": "<username>", "message": "<message>"}
```

- `<message>`: the message that must be sent to a specific user
- `<username>`: the username of the user who sent the message

## 3.2 Unhappy flow

```
S -> C: PRIVATE_RESP {"status": "ERROR", "code": <error code>}
```
Possible `<error code>`:

| Error code | Description                       |
|------------|-----------------------------------|
| 6000       | User is not logged in             |
| 6001       | Username specified does not exist |

# 4. Heartbeat message

Sends a ping message to the domain.client to check whether the domain.client is still active. The receiving domain.client should respond with a pong message to confirm it is still active. If after 3 seconds no pong message has been received by the domain.server, the connection to the domain.client is closed. Before closing, the domain.client is notified with a HANGUP message, with reason code 7000.

The domain.server sends a ping message to a domain.client every 10 seconds. The first ping message is send to the domain.client 10 seconds after the domain.client is logged in.

When the domain.server receives a PONG message while it is not expecting one, a PONG_ERROR message will be returned.

## 4.1 Happy flow

```
S -> C: PING
C -> S: PONG
```     

## 4.2 Unhappy flow

```
S -> C: HANGUP {"reason": <reason code>}
[Server disconnects the domain.client]
```      
Possible `<reason code>`:

| Reason code | Description      |
|-------------|------------------|
| 7000        | No pong received |    

```
S -> C: PONG_ERROR {"code": <error code>}
```
Possible `<error code>`:

| Error code | Description         |
|------------|---------------------|
| 8000       | Pong without ping   |    

# 5. Termination of the connection

When the connection needs to be terminated, the domain.client sends a bye message. This will be answered (with a BYE_RESP message) after which the domain.server will close the socket connection.

## 5.1 Happy flow
```
C -> S: BYE
S -> C: BYE_RESP {"status":"OK"}
[Server closes the socket connection]
```

Other, still connected clients, clients receive:
```
S -> others: LEFT {"username":"<username>"}
```

## 5.2 Unhappy flow

- None

# 6. List users

## 6.1 Happy flow

```
C -> S: LIST
S -> C: LIST_RESP {"status": "OK", "users": "<listOfUsers>"}
```

`<listOfUsers>`: A List<String> of usernames

## 6.2 Unhappy flow

- None

# 7. Coin toss game

## 7.1 Start game

### 7.1.1 Happy flow

```
C -> S: COIN_TOSS_REQ {"username": <username>}
```

`<username>`: a username of an existing user

The user who sent the request receives:
```
S -> C: COIN_TOSS_RESP {"status": "OK", "code": "0"}
```

Both users receive:
```
S -> C: COIN_TOSS_START {"<username>", "gameId": "<id>"}
```

`<username>`: The username of the opponent in the coin game that the message recipient is playing against

### 7.1.2 Unhappy flow

```
S -> C: COIN_TOSS_RESP {"status": "ERROR", "code": <error code>}
```
Possible `<error code>`:

| Error code | Description                       |
|------------|-----------------------------------|
| 6000       | User is not logged in             |
| 6001       | Username specified does not exist |

## 7.2 During the game

### 7.2.1 Happy flow

C1: Player 1
C2: Player 2

```
S -> C1, C2: COIN_TOSS_CHOICE
C1, C2 -> S: HEADS {"gameId": "<id>"} [OR] TAILS {"gameId": "<id>"}
```

`<id>`: id of the current game

Both players choose the same option:
```
S -> C1, C2: COIN_TOSS_RESULT {"<username1>": "<points>", "<username2>": "<points>"}
S -> COIN_TOSS_CHOICE
```
[No points changed.
So if this is a fresh game then points stay 0-0]

`<username1>`: Player 1 username
`<username2>`: Player 2 username
`<points>`: points of a player


Players choose different options:
[Server performs a coin toss and awards +1 point to 
the user who predicted the outcome with their choice.
So if Player 1 chose heads and Player 2 - tails 
and Server tossed heads, then Player 1 gets +1 point]

```
S -> C1, C2: COIN_TOSS_RESULT {"<username1>": "<points>", "<username2>": "<points>"}
```

`<username1>`: Player 1 username
`<username2>`: Player 2 username
`<points>`: points of a player

If Player 1 had 2 points and just got +1, so totaling 3, then they win and domain.server sends the following:
```
S -> C1: COIN_TOSS_WIN
S -> C2: COIN_TOSS_LOSE
```

Otherwise, the game continues:
```
S -> C1, C2: COIN_TOSS_CHOICE
```

### 7.2.2 Unhappy flow

```
S -> C: COIN_TOSS_RESP {"status": "ERROR", "code": <error code>}
```
Possible `<error code>`:

| Error code | Description     |
|------------|-----------------|
| 9000       | Invalid game id |

# 8. File transfer

C1: sender
C2: receiver

## 8.1 Happy flow

### 8.1.1 Receiver accepts the file

User send transfer request to the server.

```
C1 -> S: TRANSFER_REQ {"receiver": "<username>", "filename": "<filename>", "checksum": "<hash>"}
S -> C1: TRANSFER_RESP {"status": "OK", "code": "0"}
```

`<username>`: username of the client who downloads the file
`<filename>`: name of the chosen file
`<hash>`: sha256 hash of the chosen file

The server then asks the C2 client who's receiving a file.
```
S -> C2: DOWNLOAD_ASK {"sender": "<username>", "filename": "<filename>", "checksum": "<hash>", "transferId": "<id>"}
C2 -> S: DOWNLOAD_ACCEPT {"transferId": "<id>"}
S -> C2: DOWNLOAD_ACCEPT_RESP {"status": "OK", "code": "0"}
```

`<username>`: username of the client who uploads the file
`<filename>`: name of the chosen file
`<hash>`: sha256 hash of the chosen file
`<id>`: id of the current file transfer

And in this case after C2 accepts C1 receives the following:
```
S -> C1: TRANSFER_ACCEPT {"transferId": "<id>"}
```

`<id>`: id of the current file transfer

Now both clients initiate a new socket connection and send the following to the server via the new sockets:
```
C1, C2 -> S: TRANSFER_FILE {"username": "<username>", "transferId": "<id>"}
```
`<username>`: current user's username
`<id>`: id of the current file transfer

Once upload/download is done:
```
S -> C1: TRANSFER_DONE
S -> C2: DOWNLOAD_DONE
```

### 8.1.2 Receiver rejects the file

```
C2 -> DOWNLOAD_REJECT {"transferId": "<id>"}
S -> C2: DOWNLOAD_REJECT_RESP {"status": "OK", "code": "0"}
S -> C1: TRANSFER_REJECT {"transferId": "<id>"}
```

`<id>`: id of the current file transfer

## 8.2 Unhappy flow

```
S -> C1, C2: TRANSFER_RESP {"status": "ERROR", "code": "<error code>"}
```

`<error code>`:

| Error code | Description                       |
|------------|-----------------------------------|
| 4000       | Invalid transfer id               |
| 6001       | Username specified does not exist |

```
S -> C2: DOWNLOAD_ACCEPT_RESP {"status": "ERROR", "code": "<error code>"}
```
[OR]
```
S -> C2: DOWNLOAD_REJECT_RESP {"status": "ERROR", "code": "<error code>"}
```

`<error code>`:

| Error code | Description              |
|------------|--------------------------|
| 4000       | Invalid transfer id      |

# 8. Invalid message header

If the domain.client sends an invalid message header (not defined above), the domain.server replies with an unknown command message. The domain.client remains connected.

Example:
```
C -> S: MSG This is an invalid message
S -> C: UNKNOWN_COMMAND
```

# 9. Invalid message body

If the domain.client sends a valid message, but the body is not valid JSON, the domain.server replies with a pars error message. The domain.client remains connected.

Example:
```
C -> S: BROADCAST_REQ {"aaaa}
S -> C: PARSE_ERROR
```