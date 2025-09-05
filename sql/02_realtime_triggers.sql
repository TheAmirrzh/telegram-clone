CREATE OR REPLACE FUNCTION notify_new_message() RETURNS trigger AS $$
DECLARE
  ch TEXT;
  payload JSON;
  typ TEXT;
BEGIN
  -- Use the modern, generic receiver_type column
  typ := lower(NEW.receiver_type);

  -- Build the notification channel name based on the receiver type and ID
  ch := typ || '_' || replace(NEW.receiver_id::text, '-', '');

  payload := json_build_object(
    'chatType', typ,
    'id', NEW.receiver_id::text,
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
