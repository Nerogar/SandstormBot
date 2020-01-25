create table Song
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

create table DefaultPlaylist
(
    "DefaultPlaylistId" integer primary key autoincrement,
    "PlaylistId"        integer not null,
    "Name"              text    not null,
    "Order"             text    not null,
    "CurrentPosition"   integer not null
);

insert into DefaultPlaylist ("PlaylistId", "Name", "Order", "CurrentPosition")
VALUES (0, 'default playlist', 'DEFAULT', -1);
