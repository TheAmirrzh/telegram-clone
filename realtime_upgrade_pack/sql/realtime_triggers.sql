
-- Enable realtime notifications for new messages using LISTEN/NOTIFY
-- Channel names use a prefix plus UUID without hyphens for compatibility with Postgres channel naming.
-- Example channel: private_0d6b2a6e7b2a4b7c9d2f3a4b5c6d7e8f

CREATE OR REPLACE FUNCTION notify_new_message() RETURNS trigger AS $$
DECLARE
  ch TEXT;
  payload JSON;
  rid UUID;
  typ TEXT;
BEGIN
  IF NEW.receiver_private_chat IS NOT NULL THEN
    rid := NEW.receiver_private_chat;
    typ := 'private';
    ch := 'private_' || replace(NEW.receiver_private_chat::text, '-', '');
  ELSIF NEW.receiver_group IS NOT NULL THEN
    rid := NEW.receiver_group;
    typ := 'group';
    ch := 'group_' || replace(NEW.receiver_group::text, '-', '');
  ELSIF NEW.receiver_channel IS NOT NULL THEN
    rid := NEW.receiver_channel;
    typ := 'channel';
    ch := 'channel_' || replace(NEW.receiver_channel::text, '-', '');
  ELSE
    RETURN NEW;
  END IF;

  payload := json_build_object(
    'chatType', typ,
    'id', rid::text,
    'messageId', NEW.id::text
  );

  PERFORM pg_notify(ch, payload::text);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_notify_new_message ON messages;
CREATE TRIGGER trg_notify_new_message
AFTER INSERT ON messages
FOR EACH ROW EXECUTE FUNCTION notify_new_message();
