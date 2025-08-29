# Telegram Clone — Advanced Programming Final Project

**A professional, desktop chat application (JavaFX + PostgreSQL)**
This repository implements a Telegram-like desktop client with core chat functionality: registration/login, private chats, groups, channels, attachments, message edit/delete, message search, typing indicators, and simple desktop notifications.

> Built with Java 17, JavaFX, Maven, PostgreSQL and HikariCP. Passwords hashed with BCrypt. UUIDs via `uuid-ossp`.

---

## Table of contents

* [Features](#features)
* [System requirements](#system-requirements)
* [Quick start (recommended with Docker)](#quick-start-recommended-with-docker)

  * [1. Start the database (Docker)](#1-start-the-database-docker)
  * [2. Initialize the database schema](#2-initialize-the-database-schema)
  * [3. (Optional) Set environment variables](#3-optional-set-environment-variables)
  * [4. Build & run the app (Maven)](#4-build--run-the-app-maven)
  * [5. Run from IDE (IntelliJ)](#5-run-from-ide-intellij)
  * [6. Run tests](#6-run-tests)
* [Usage / UI walkthrough](#usage--ui-walkthrough)
* [Project structure & key classes](#project-structure--key-classes)
* [Database schema summary](#database-schema-summary)
* [Implementation notes & developer guidance](#implementation-notes--developer-guidance)
* [Troubleshooting & common errors](#troubleshooting--common-errors)
* [Next steps & recommended enhancements](#next-steps--recommended-enhancements)
* [Contact / support](#contact--support)

---

## Features

**User & authentication**

* Register / Login (BCrypt password hashing)
* Stronger password policy on registration (min length, uppercase, digit)

**Messaging**

* Private 1:1 chats
* Group chats (membership)
* Channels (broadcast model)
* Persistent messages stored in PostgreSQL

**Message operations**

* Send text and image attachments (saved to `~/.telegram-clone/attachments/`)
* Edit your messages (status = `EDITED`)
* Delete your messages (content replaced with `[deleted]`, image removed, status = `DELETED`)
* Message search across chats you participate in

**Real-time-ish UX**

* Typing indicators (stored in DB, polled)
* Desktop alerts (simple polling `NotificationService`)
* Responsive UI — all IO runs off JavaFX thread via `FX.runAsync(...)`

**Dev & infra**

* HikariCP connection pooling
* Docker Compose for PostgreSQL
* SQL schema + init script includes `uuid-ossp` and useful indexes
* JUnit 5 tests for DAOs

---

## System requirements

* Java 17 (JDK 17)
* Maven 3.8+
* PostgreSQL 12+ (or Docker & Docker Compose)
* Optional: IntelliJ IDEA or another Java IDE
* OS: Linux / macOS / Windows (ensure JavaFX availability — use Maven plugin for running)

Project default DB values (overridable via env vars):

* `DB_URL=jdbc:postgresql://localhost:5432/telegramdb`
* `DB_USER=telegram_user`
* `DB_PASS=telegram_pass`

Main entrypoint: `com.telegramapp.App`

---

## Quick start (recommended with Docker)

> These steps assume you have Docker and Docker Compose installed. If you prefer a local Postgres install, create the DB and user manually.

### 1. Start the database (Docker)

From project root (where `docker-compose.yml` lives):

```bash
docker compose up -d
```

This starts PostgreSQL on port `5432` with DB `telegramdb` and the default credentials.

### 2. Initialize the database schema

Make the init script executable and run it:

```bash
chmod +x scripts/init-db.sh
./scripts/init-db.sh
```

This executes `sql/schema.sql`, which creates tables and the `uuid-ossp` extension. If your Postgres instance does not allow extension creation, run `CREATE EXTENSION IF NOT EXISTS "uuid-ossp";` as a superuser first.

### 3. (Optional) Set environment variables

The app reads `DB_URL`, `DB_USER`, `DB_PASS`. Set them if you changed the defaults.

macOS / Linux:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/telegramdb
export DB_USER=telegram_user
export DB_PASS=telegram_pass
```

Windows PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/telegramdb"
$env:DB_USER = "telegram_user"
$env:DB_PASS = "telegram_pass"
```

### 4. Build & run the app (Maven — recommended)

From project root:

```bash
mvn clean javafx:run
```

The JavaFX application will start and present the login screen.

### 5. Run from your IDE (IntelliJ recommended)

* Import project as a Maven project.
* Ensure Project SDK = Java 17.
* Run the `com.telegramapp.App` main class.
* If JavaFX runtime is missing in the IDE run config, use `mvn javafx:run` or add JavaFX SDK to the run configuration.

### 6. Run tests

Tests connect to the configured DB. Use a test DB or Docker instance:

```bash
mvn -DskipTests=false test
```

---

## Usage / UI walkthrough

1. **Login / Register**

   * Launch app → `login.fxml`.
   * Register using username + password (password policy: ≥8 chars, one uppercase, one digit).

2. **Main view**

   * Left column lists: Chats, Groups, Channels.
   * Center pane provides message search.

3. **Open chat**

   * Click a chat/group/channel → opens chat view.
   * Type and send messages; attach images (saved to `~/.telegram-clone/attachments`).

4. **Edit / Delete**

   * Right-click your message → Edit / Delete options.
   * Edit updates `content` and sets `read_status = 'EDITED'`.
   * Delete replaces `content` with `[deleted]`, clears image path, sets `read_status = 'DELETED'`.

5. **Typing indicators**

   * When you type, typing status updates in DB. The chat poll shows other users typing.

6. **Notifications**

   * Background `NotificationService` polls for relevant new messages and displays a JavaFX Alert popup.

7. **Search**

   * Enter a query in the main window search box to search messages across chats you participate in.

---

## Project structure & key classes

```
src/main/java/com/telegramapp
  App.java
  model/
    User.java
    Message.java
    GroupChat.java
    Channel.java
  dao/
    UserDAO.java
    MessageDAO.java
    GroupDAO.java
    ChannelDAO.java
    ChatDAO.java
    TypingDAO.java
  service/
    AuthService.java
    NotificationService.java
  controller/
    LoginController.java
    MainController.java
    ChatController.java
  ui/
    MessageCell.java
  util/
    DB.java          (HikariCP pool)
    FX.java          (async helper)
    Config.java
    ImageStorage.java
  tools/
    PasswordTool.java
src/main/resources/
  fxml/*.fxml
  css/styles.css
sql/
  schema.sql
  sample_data.sql
scripts/init-db.sh
docker-compose.yml
pom.xml
```

**Layer responsibilities**

* **Model:** POJOs representing domain entities.
* **DAO:** All DB access (prepared statements, mapping).
* **Service:** Business rules (auth, notification polling).
* **Controller:** JavaFX controllers — UI glue and orchestration.
* **Util:** DB pool, async helper to keep UI responsive, file storage helper.

---

## Database schema summary

Principal tables:

* `users` — `id (UUID)`, `username`, `password_hash`, `profile_name`, ...
* `private_chats` — private chat pairing (unique ordered pair)
* `groups`, `group_members` — group metadata and membership
* `channels`, `channel_subscribers` — channels and their subscribers
* `messages` — `id`, `sender_id`, receiver columns (`receiver_private_chat`, `receiver_group`, `receiver_channel`), `content`, `image_path`, `timestamp`, `read_status`
* `typing_status` — `chat_id`, `user_id`, `last_ts` for typing indicator
* Indexes: `messages(sender_id)`, `messages(timestamp)` for basic performance

Notes:

* UUIDs generated by `uuid_generate_v4()` from `uuid-ossp` (ensure the extension exists).
* Messages are immutable history-wise except for edit/delete operations stored as state changes.

---

## Implementation notes & developer guidance

* **Threading:** All heavy IO (DB, file) runs off the JavaFX thread using `FX.runAsync(...)`. UI updates occur via callbacks on the JavaFX Application Thread.
* **DB pooling:** HikariCP is used; tune pool size for your environment.
* **Security:** Passwords hashed with BCrypt; prepared statements used everywhere.
* **Attachments:** Stored as files under `~/.telegram-clone/attachments` with randomized names. Validate file types and sizes — current limit = 10 MB.
* **Messaging semantics:** Edit marks a message `EDITED`, delete masks the content as `[deleted]`.
* **Real-time:** Typing/notifications are implemented by polling. For production, consider WebSocket or push-based approach.

---

## Troubleshooting & common errors

**`uuid-ossp` missing or permission denied**

* Run as DB superuser:

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

`scripts/init-db.sh` executes the schema which includes this; ensure the DB user has extension creation rights or run as superuser.

**DB connection errors**

* Confirm Postgres is running and env vars (`DB_URL`, `DB_USER`, `DB_PASS`) are correct.
* Use `docker compose ps` and `docker logs <container>` to inspect.

**JavaFX runtime issues**

* Prefer `mvn javafx:run`. If running directly from IDE, ensure JavaFX SDK is on the module path.

**Tests failing**

* Initialize DB first (`scripts/init-db.sh`) and ensure tests point to the intended DB instance.

**Attachments fail to save**

* Ensure `~/.telegram-clone/attachments` exists and the app has write permission.

---

## Next steps & recommended enhancements

* Replace polling with WebSocket server for real-time updates (typing, new messages).
* Add message version history for edits.
* Store attachments in cloud storage (S3) with signed URLs.
* Add read receipts per recipient (for group messaging).
* Comprehensive integration tests with Testcontainers.

---

## Contact / support

If you need help with any of the following, I can implement them:

* Map search results to open the exact chat and scroll to a message.
* Replace polling-based notifications with a WebSocket prototype.
* Add Testcontainers-based integration tests.
* Package as an installable desktop application (platform bundles).

---

**Enjoy — good luck with your final project submission!**

*Save this content as `README.md` in your project root.*
