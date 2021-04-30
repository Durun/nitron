CREATE TABLE codes
(
    hash blob,
    text string,
    primary key (hash)
);
CREATE TABLE revisions
(
    software string,
    id       string,
    date     string,
    message  string,
    author   string,
    primary key (software, id)
);
CREATE TABLE changes
(
    software   string,
    filepath   string,
    beforeHash blob,
    afterHash  blob,
    revision   string
);
CREATE TABLE patterns
(
    software   string,
    beforeHash blob,
    afterHash  blob,
    support    integer,
    confidence real,
    primary key (software, beforeHash, afterHash)
);
CREATE TABLE globalPatterns
(
    beforeHash blob,
    afterHash  blob,
    support    integer,
    confidence real,
    primary key (beforeHash, afterHash)
);

CREATE
INDEX index_patterns_beforeHash on patterns(beforeHash);
CREATE
INDEX index_patterns_afterHash on patterns(afterHash);
CREATE
INDEX index_patterns_beforeHash_afterHash on patterns(beforeHash, afterHash);
CREATE
INDEX index_nTexts_hash on codes(hash);
