CREATE OR REPLACE FUNCTION notify_transacao_change()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('transacoes_channel', NEW.usuario_id::TEXT);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER transacoes_after_change
AFTER INSERT OR UPDATE ON transacoes
FOR EACH ROW EXECUTE FUNCTION notify_transacao_change();
