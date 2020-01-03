ALTER TABLE entries
ADD CONSTRAINT no_duplicates UNIQUE (datetime);