ALTER TABLE events
    ADD COLUMN tags JSON NULL AFTER apply_link,
    ADD COLUMN main_content_html LONGTEXT NULL AFTER tags;