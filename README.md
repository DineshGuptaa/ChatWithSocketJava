# ChatWithSocketJava — WhatsApp-like Chat Application

Java Socket Programming based real-time chat application with private messaging, group chats, file sharing, and message deletion.
Project inspired from https://github.com/iamrohitsuthar/LiveChatServer
---

## Features

### Authentication & Profile
- User sign-up with username, password, display name, gender, avatar selection
- Secure login with SHA-256 hashed password
- Forgot password / reset password flow
- Session tracking via client ID

### Private Messaging (1-on-1 Chat)
- Real-time private messaging between any two online users
- Online contacts list with green highlight for active users
- Message delivery status (sent → delivered → read)
- Read receipts sent automatically when recipient views the message
- Load last 90 days of message history on chat open
- Date separators in chat (Today / Yesterday / Day-of-week / full date)

### File Sharing
- Send files via attachment button (`+`)
- Automatic MIME type detection (images, PDFs, documents, archives, audio, video)
- Thumbnail preview for images
- Inline file icon for non-image files
- Click to open file in system default application
- Files stored on server filesystem (`uploads/` directory)
- Progress indicator ("loading..." → checkmark on delivery)

### Group Chat
- Create named groups (auto-generated group ID)
- Invite users to groups (in-app notification to invitee)
- Accept / reject group invitations
- View pending invitations
- Group admins (creator is admin)
- Remove group members (admin only)
- View group members list
- Real-time group messaging

### Room-based Chat
- Create ad-hoc chat rooms
- Join rooms by name
- List all active rooms
- Room member tracking
- Join/leave notifications ("X has left the chat")

### Message Deletion (WhatsApp-style)
- Long-press on any message → enters selection mode
- Select multiple messages with blue highlight border
- **Delete for me**: removes messages only from your side (`deleted=1` with sender filter)
- **Delete for everyone**: removes messages for both participants (pushes live notification to the other user's screen)
- 5-second undo window with countdown timer
- Undo restores messages on both sides

### Multi-window Support
- Back button navigates between screens without data loss
- `ContactListActivity.showExisting()` reuses open windows
- Multiple chat windows can be open simultaneously

---

## Project Structure

```
src/com/chatroom/
├── client/           # Client-side networking
│   ├── Client.java        # Console-based client
│   ├── ClientExec.java    # Client entry point
│   └── ClientModel.java   # Shared socket / streams model
├── configuration/
│   └── Config.java        # App-wide constants & DB config
├── Database/
│   └── createdb.java      # [REMOVED] DB tables auto-creation (now manual via schema.sql)
├── models/
│   ├── Request.java       # Serializable request object
│   ├── Response.java      # Serializable response object
│   ├── InviteInfo.java    # Group invitation model
│   └── MessageTrackObject.java  # Message tracking model
├── others/
│   ├── Hash.java          # SHA-256 hashing
│   ├── LogFileWriter.java # File-based logging
│   ├── Message.java       # Console output helper
│   └── TextBubbleBorder.java  # Chat bubble border
├── server/
│   ├── ServerExec.java    # Server entry point
│   ├── Server.java        # Main server: accept, analyse, respond, message handling
│   ├── ClientThread.java  # Per-client socket reader
│   ├── ClientRequest.java # Thread-safe request wrapper (ClientThread + Request pair)
│   ├── ResponseHolder.java # Response + output stream pair for ResponseMaker
│   └── ServerOperations.java # Server admin console (logs, shutdown, stats)
└── ui/
    ├── MainSplash.java         # Splash screen with connecting animation
    ├── SignInActivity.java     # Login screen
    ├── SignUpActivity.java     # Registration screen
    ├── MainMenuOptions.java    # Home: contacts, rooms, groups
    ├── ContactListActivity.java # Online users list → open private chat
    ├── PrivateChatActivity.java # 1-on-1 chat with file sharing & selection/deletion
    ├── ChatActivity.java       # Room-based chat
    ├── ViewRoomsActivity.java  # List / join rooms
    ├── GroupsListActivity.java # List / create / manage groups
    └── GroupChatActivity.java  # Group chat window
```

---

## Database Setup (MariaDB / MySQL)

### Step 1: Create a dedicated MySQL user

```sql
CREATE USER IF NOT EXISTS 'chatroom'@'localhost' IDENTIFIED BY 'chatroom_pwd';
GRANT ALL PRIVILEGES ON chatroom.* TO 'chatroom'@'localhost';
FLUSH PRIVILEGES;
```

> Replace `'chatroom'` and `'chatroom_pwd'` with your preferred credentials.  
> For remote access, replace `'localhost'` with `'%'`.

### Step 2: Create database and tables

```bash
mysql -u chatroom -p chatroom_pwd < schema.sql
```

Or manually:

```sql
CREATE DATABASE IF NOT EXISTS chatroom
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE chatroom;

CREATE TABLE IF NOT EXISTS users (
    client_id      INT          NOT NULL AUTO_INCREMENT,
    client_name    VARCHAR(50)  NOT NULL,
    client_pwd     VARCHAR(150)          DEFAULT NULL,
    display_name   VARCHAR(100)          DEFAULT '',
    gender         VARCHAR(10)           DEFAULT 'Other',
    profile_avatar INT                   DEFAULT 0,
    PRIMARY KEY (client_id),
    UNIQUE KEY uq_client_name (client_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_groups (
    group_id   INT          NOT NULL AUTO_INCREMENT,
    group_name VARCHAR(100) NOT NULL,
    created_by INT          NOT NULL,
    PRIMARY KEY (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS group_members (
    group_id   INT          NOT NULL,
    member_id  INT          NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'pending',
    invited_by INT                   DEFAULT NULL,
    PRIMARY KEY (group_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS private_messages (
    pm_id       INT       NOT NULL AUTO_INCREMENT,
    sender_id   INT       NOT NULL,
    receiver_id INT       NOT NULL,
    message     TEXT              DEFAULT NULL,
    sent_at     DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status      TINYINT   NOT NULL DEFAULT 0,
    deleted     TINYINT   NOT NULL DEFAULT 0,
    PRIMARY KEY (pm_id),
    KEY ix_sender_receiver (sender_id, receiver_id),
    KEY ix_receiver_sender (receiver_id, sender_id),
    KEY ix_sent_at (sent_at),
    KEY ix_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS file_messages (
    file_id      INT           NOT NULL AUTO_INCREMENT,
    sender_id    INT           NOT NULL,
    receiver_id  INT           NOT NULL,
    file_name    VARCHAR(255)  NOT NULL,
    file_size    BIGINT        NOT NULL,
    mime_type    VARCHAR(100)  NOT NULL,
    stored_path  VARCHAR(500)  NOT NULL,
    sent_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (file_id),
    KEY ix_sender_receiver (sender_id, receiver_id),
    KEY ix_receiver_sender (receiver_id, sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## How to Run

### Prerequisites
- Java 8+
- MariaDB or MySQL server running
- MySQL Connector/J (`lib/mysql-connector-java-8.0.17.jar`)

### Compile

```bash
cd ChatWithSocketJava
mkdir -p bin
find src -name '*.java' > sources.txt
javac -cp lib/mysql-connector-java-8.0.17.jar -d bin @sources.txt
```

### Start the Server

```bash
java -cp bin:lib/mysql-connector-java-8.0.17.jar \
  com.chatroom.server.ServerExec  <port> <db_host> <db_user> <db_pass>
```

Example:
```bash
java -cp bin:lib/mysql-connector-java-8.0.17.jar \
  com.chatroom.server.ServerExec 9999 localhost:3306 chatroom chatroom_pwd
```

### Start a Client

```bash
java -cp bin:lib/mysql-connector-java-8.0.17.jar \
  com.chatroom.client.ClientExec <port> <db_host>
```

Example:
```bash
java -cp bin:lib/mysql-connector-java-8.0.17.jar \
  com.chatroom.client.ClientExec 9999 localhost
```

---

## Architecture Notes

### Threading Model
| Thread | Purpose | Limitation |
|---|---|---|
| `ClientThread` (per client) | Reads serialized `Request` from socket, enqueues it | 1 OS thread per client |
| `RequestAnalyser` (single) | Dequeues & processes all requests sequentially | Single-threaded bottleneck |
| `ResponseMaker` (single) | Writes responses back to clients | Single-threaded bottleneck |
| `MessageHandler` (single) | Broadcasts room/group messages | Single-threaded bottleneck |

### Key Design Decisions
- **Swing GUI** with SwingWorker for async server calls
- **Static shared `ObjectInputStream`/`ObjectOutputStream`** — all UI activities share the same socket streams
- **`ClientRequest` wrapper** eliminates race condition between `ClientThread` writing and `RequestAnalyser` reading the `request` field
- **Soft-delete + scheduled hard-delete** for message deletion with 5-second undo grace period

### Capacity Estimate
~100–200 concurrent users with the single-threaded `RequestAnalyser`/`ResponseMaker` architecture. Beyond that, the database connection and thread-per-client model become bottlenecks.

---

## Troubleshooting

| Error | Likely Cause | Fix |
|---|---|---|
| `SocketTimeoutException: Read timed out` | Server not responding; request dropped by race condition | Restart with fixed `ClientRequest` wrapper |
| `SQLException: Illegal operation on empty result set` | `resultSet.next()` not checked before `getString()` | Fixed in current code |
| `NullPointerException: comp is null` | `selectionToolbar` / `undoPanel` used before init | Fixed — build methods moved before `initializeAllWithProperties` |
| `NoSuchElementException` in `ServerOperations` | Running headless without stdin | Caught and logged in current code |
| `Access denied for user` | DB credentials wrong | Check `Config.USER_NAME` / `Config.USER_PWD` |
| `Unknown database 'chatroom'` | Database not created | Run `mysql -u root -p < schema.sql` first |
