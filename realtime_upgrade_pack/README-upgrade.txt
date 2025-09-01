
Realtime Upgrade Pack

What this adds
--------------
1) **Database triggers** to broadcast NOTIFY events on new messages.
   - Apply: psql -f sql/realtime_triggers.sql

2) **Java realtime client** using Postgres LISTEN/NOTIFY:
   - com.telegramapp.realtime.PgNotifyClient
   - com.telegramapp.service.RealtimeService

3) **Patches**:
   - message_dao.patch: Adds NOTIFY fallback after insert
   - chat_controller.patch: Subscribes to realtime channels & refreshes history

4) **CSS additions** in css_additions.css for smoother rounded UI.

How to apply
------------
A) SQL
   - Run `sql/realtime_triggers.sql` against your project database.

B) Java sources
   - Copy `src/main/java/com/telegramapp/realtime/PgNotifyClient.java`
     and `src/main/java/com/telegramapp/service/RealtimeService.java`
     into your source tree at the same package paths.

C) Patches (optional but recommended)
   - Use `git apply < patchfile>` from project root:
       git apply message_dao.patch
       git apply chat_controller.patch

D) CSS (optional)
   - Append the content of `css_additions.css` to your existing `styles.css`.

Build & Run
-----------
- Ensure your pom.xml includes PostgreSQL 42.2.6+ (you have 42.7.1) and JavaFX.
- Rebuild the project: `mvn clean javafx:run`

Notes
-----
- LISTEN/NOTIFY uses a dedicated connection; we keep one daemon thread open.
- If triggers aren’t installed, the DAO fallback will still emit NOTIFY after insert.
- Channel names strip hyphens from UUIDs to satisfy Postgres channel naming rules.
