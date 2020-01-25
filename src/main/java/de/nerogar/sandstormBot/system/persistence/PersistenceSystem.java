package de.nerogar.sandstormBot.system.persistence;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.PlaylistAddEvent;
import de.nerogar.sandstormBot.event.events.SongAddEvent;
import de.nerogar.sandstormBot.event.events.SongRemoveEvent;
import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBot.persistence.Database;
import de.nerogar.sandstormBot.persistence.DatabaseTable;
import de.nerogar.sandstormBot.persistence.entities.DefaultPlaylistEntity;
import de.nerogar.sandstormBot.persistence.entities.SongEntity;
import de.nerogar.sandstormBot.playlist.DefaultPlaylist;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.system.ISystem;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class PersistenceSystem implements ISystem {

	private EventManager eventManager;
	private IGuildMain   guildMain;

	private Database                             database;
	private DatabaseTable<SongEntity>            songTable;
	private DatabaseTable<DefaultPlaylistEntity> defaultPlaylistTable;

	@Override
	public void init(EventManager eventManager, IGuildMain guildMain) {
		this.eventManager = eventManager;
		this.guildMain = guildMain;

		final String guildId = guildMain.getGuild().getId();

		try {
			database = new Database(guildId + "/main.db", Database.loadMigrations("database/main/migrations"));
			songTable = database.attachTable(SongEntity.class, "Song");
			defaultPlaylistTable = database.attachTable(DefaultPlaylistEntity.class, "DefaultPlaylist");
		} catch (FileNotFoundException e) {
			e.printStackTrace(Main.LOGGER.getErrorStream());
			return;
		}

		loadSongs();

		eventManager.register(PlaylistAddEvent.class, this::onPlaylistAdd);

		eventManager.register(SongAddEvent.class, this::onSongAdd);
		eventManager.register(SongRemoveEvent.class, this::onSongRemove);
	}

	private void loadSongs() {
		final List<DefaultPlaylistEntity> defaultPlaylistEntities = defaultPlaylistTable.select();
		final List<DefaultPlaylist> defaultPlaylists = new ArrayList<>();
		for (DefaultPlaylistEntity defaultPlaylistEntity : defaultPlaylistEntities) {
			defaultPlaylists.add(new DefaultPlaylist(eventManager, defaultPlaylistEntity));
		}

		for (DefaultPlaylist defaultPlaylist : defaultPlaylists) {
			final List<SongEntity> songEntities = songTable.select(s -> s.playlistId == defaultPlaylist.getDefaultPlaylistEntity().getPlaylistId());
			for (SongEntity songEntity : songEntities) {
				defaultPlaylist.add(new Song(songEntity));
			}
			guildMain.getPlaylists().add(defaultPlaylist);
		}
	}

	private void onPlaylistAdd(PlaylistAddEvent event) {
		if (event.playlist.getClass() == DefaultPlaylist.class) {
			final DefaultPlaylist defaultPlaylist = (DefaultPlaylist) event.playlist;
			defaultPlaylistTable.insert(defaultPlaylist.getDefaultPlaylistEntity());
		}
	}

	private void onSongAdd(SongAddEvent event) {
		songTable.insert(event.song.getSongEntity());
	}

	private void onSongRemove(SongRemoveEvent event) {
		songTable.delete(event.song.getSongEntity());
	}

}
