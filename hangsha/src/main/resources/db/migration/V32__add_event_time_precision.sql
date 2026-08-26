ALTER TABLE events
    ADD COLUMN apply_end_time_known BOOLEAN NOT NULL DEFAULT FALSE AFTER apply_end,
    ADD COLUMN event_start_time_known BOOLEAN NOT NULL DEFAULT FALSE AFTER event_start,
    ADD COLUMN event_end_time_known BOOLEAN NOT NULL DEFAULT FALSE AFTER event_end;

UPDATE events
SET event_start_time_known = CASE
        WHEN event_start IS NOT NULL AND TIME(event_start) <> '00:00:00' THEN TRUE
        ELSE FALSE
    END,
    event_end_time_known = CASE
        WHEN event_end IS NOT NULL AND TIME(event_end) <> '23:59:59' THEN TRUE
        ELSE FALSE
    END;