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

CREATE
INDEX index_nTexts_hash on codes(hash);
