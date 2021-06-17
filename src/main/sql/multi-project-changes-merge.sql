--ATTACH DATABASE "changes.db" AS guest;

INSERT
OR IGNORE INTO main.codes
SELECT DISTINCT hash, nText
FROM guest.codes;
INSERT INTO main.revisions
SELECT software, id, date, author, message
FROM guest.revisions;
INSERT INTO main.changes
SELECT software, filepath, beforeHash, afterHash, revision
FROM guest.changes;

DETACH
guest;
