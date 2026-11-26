-- V7: shop double-entry ledger. Append-only; every entry must balance (enforced by the database itself).

CREATE TABLE ledger_accounts (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    normal_side VARCHAR(6) NOT NULL CHECK (normal_side IN ('DEBIT', 'CREDIT'))
);

INSERT INTO ledger_accounts (code, name, type, normal_side) VALUES
    ('1130', 'Phải thu MoMo (tiền khách đã trả qua ví MoMo)', 'ASSET', 'DEBIT'),
    ('3388', 'Phải trả khách: tiền nhận thừa/sau huỷ, chờ hoàn', 'LIABILITY', 'CREDIT'),
    ('5110', 'Doanh thu bán hàng', 'REVENUE', 'CREDIT');

CREATE TABLE journal_entries (
    id BIGSERIAL PRIMARY KEY,
    source_type VARCHAR(40) NOT NULL,
    source_id VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    posted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- One business event can be posted at most once (idempotent posting).
    CONSTRAINT uq_journal_entries_source UNIQUE (source_type, source_id)
);

CREATE TABLE journal_lines (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT NOT NULL REFERENCES journal_entries(id) ON DELETE RESTRICT,
    account_code VARCHAR(10) NOT NULL REFERENCES ledger_accounts(code) ON DELETE RESTRICT,
    direction VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount BIGINT NOT NULL CHECK (amount > 0)
);

CREATE INDEX idx_journal_lines_entry_id ON journal_lines(entry_id);
CREATE INDEX idx_journal_lines_account_code ON journal_lines(account_code);

-- Balance check runs at COMMIT (deferred), after all lines of the entry are inserted.
CREATE FUNCTION ledger_check_entry_balanced() RETURNS trigger AS $$
DECLARE
    debit_total BIGINT;
    credit_total BIGINT;
    line_count INT;
BEGIN
    SELECT COALESCE(SUM(amount) FILTER (WHERE direction = 'DEBIT'), 0),
           COALESCE(SUM(amount) FILTER (WHERE direction = 'CREDIT'), 0),
           COUNT(*)
      INTO debit_total, credit_total, line_count
      FROM journal_lines
     WHERE entry_id = NEW.entry_id;

    IF line_count < 2 OR debit_total <> credit_total THEN
        RAISE EXCEPTION 'Journal entry % is not balanced (debit %, credit %, lines %)',
            NEW.entry_id, debit_total, credit_total, line_count;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_journal_lines_balanced
    AFTER INSERT ON journal_lines
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ledger_check_entry_balanced();

-- Append-only: corrections are new reversing entries, never edits.
CREATE FUNCTION ledger_forbid_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Ledger is append-only: % on % is not allowed', TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_journal_entries_append_only
    BEFORE UPDATE OR DELETE ON journal_entries
    FOR EACH ROW EXECUTE FUNCTION ledger_forbid_change();

CREATE TRIGGER trg_journal_lines_append_only
    BEFORE UPDATE OR DELETE ON journal_lines
    FOR EACH ROW EXECUTE FUNCTION ledger_forbid_change();
