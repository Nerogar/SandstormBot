create table SongCache
(
    "SongId"                 integer primary key autoincrement,
    "AudioTrackProviderName" text      not null,
    "Location"               text      not null,
    "PlaylistId"             integer   not null,
    "Title"                  text      not null,
    "Artist"                 text      null,
    "Album"                  text      null,
    "Duration"               integer   not null,
    "Query"                  text      not null,
    "User"                   text      not null,
    "PlayCount"              integer   not null,
    "LastPlayed"             timestamp not null
);

create table QueryCache
(
    "QueryCacheId" integer primary key autoincrement,
    "Query"        text    not null,
    "Location"     text    not null
);
