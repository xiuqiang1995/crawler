create table LINKS_TO_BE_PROCESSED
(
    link varchar(2000)
);

create table LINKS_ALREADY_PROCESSED
(
    link varchar(2000)
);

create table NEWS
(
    ID          BIGINT auto_increment,
    TITLE       text,
    CONTENT     text,
    URL         VARCHAR(2000),
    CREATED_AT  TIMESTAMP,
    MODIFIED_AT TIMESTAMP,
    constraint NEWS_PK
        primary key (ID)
);