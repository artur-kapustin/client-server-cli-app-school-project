# Java Client-Server CLI Chat Application

A networked client-server chat application built in Java using sockets, for the course called Internet Technoglogy at Saxion Univeristy. 
The project demonstrates core networking concepts, custom communication protocols, and handling multiple clients in a real-time system.

## Features
- Real-time messaging between multiple clients
- Client-server architecture using Java sockets
- Custom communication protocol
- Session handling for connected users
- File transfer between clients
- Simple interactive feature (coin toss minigame)

## How It Works
The application consists of a server and multiple clients:

- The server listens for incoming connections and manages communication between connected clients
- Each client connects to the server and can send and receive messages
- Communication is handled through a custom protocol over TCP sockets

## What I Learned
- Designing and implementing a client-server architecture
- Working with sockets and network communication in Java
- Handling multiple client connections
- Structuring communication protocols
- Debugging real-time systems

## How to Run

Note: The project was developed using IntelliJ IDEA, therefore the instructions are for it

1. Open the project in IntelliJ IDEA or idea of your choice
2. Locate the `main/src/domain/client/Main.java` and `main/src/domain/server/Main.java` classes
3. Run 1 server `Main` and 1 or multiple client `Main`s
