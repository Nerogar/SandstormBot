package de.nerogar.sandstormBot.system.persistence;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.*;
import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBot.persistence.Database;
import de.nerogar.sandstormBot.persistence.DatabaseTable;
import de.nerogar.sandstormBot.persistence.entities.DefaultPlaylistEntity;
import de.nerogar.sandstormBot.persistence.entities.PlaylistsEntity;
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
	private DatabaseTable<PlaylistsEntity>       playlistsTable;

	@Override
	public void init(EventManager eventManager, IGuildMain guildMain) {
		this.eventManager = eventManager;
		this.guildMain = guildMain;

		final String guildId = guildMain.getGuild().getId();

		try {
			database = new Database(guildId + "/main.db", Database.loadMigrations("database/main/migrations"));
			songTable = database.attachTable(SongEntity.class, "Song");
			defaultPlaylistTable = database.attachTable(DefaultPlaylistEntity.class, "DefaultPlaylist");
			playlistsTable = database.attachTable(PlaylistsEntity.class, "Playlists");
			if (playlistsTable.select().size() == 0) {
				playlistsTable.insert(new PlaylistsEntity());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace(Main.LOGGER.getErrorStream());
			return;
		}

		loadSongs();

		eventManager.register(PlaylistChangeCurrentEvent.class, this::onPlaylistChangeCurrent);
		eventManager.register(PlaylistAddEvent.class, this::onPlaylistAdd);
		eventManager.register(PlaylistRemoveEvent.class, this::onPlaylistRemove);

		eventManager.register(SongAddEvent.class, this::onSongAdd);
		eventManager.register(SongRemoveEvent.class, this::onSongRemove);

		eventManager.register(SongChangeCurrentEvent.class, this::onSongChangeCurrent);
	}

	private void loadSongs() {
		final List<DefaultPlaylistEntity> defaultPlaylistEntities = defaultPlaylistTable.select();
		for (DefaultPlaylistEntity defaultPlaylistEntity : defaultPlaylistEntities) {
			List<SongEntity> songEntities = songTable.select(s -> s.playlistId == defaultPlaylistEntity.getPlaylistId());
			List<Song> songs = new ArrayList<>();
			for (SongEntity songEntity : songEntities) {
				songs.add(new Song(songEntity));
			}
			DefaultPlaylist playlist = new DefaultPlaylist(eventManager, defaultPlaylistEntity, songs);
			guildMain.getPlaylists().add(playlist);
			if (defaultPlaylistEntity.getPlaylistId() == playlistsTable.select().get(0).currentPlaylist) {
				guildMain.getPlaylists().setCurrent(playlist);
			}
		}
	}

	private void onPlaylistChangeCurrent(PlaylistChangeCurrentEvent event) {
		playlistsTable.select().get(0).currentPlaylist = ((DefaultPlaylist) event.newPlaylist).getDefaultPlaylistEntity().getPlaylistId();
		playlistsTable.update(playlistsTable.select().get(0));
	}

	private void onSongChangeCurrent(SongChangeCurrentEvent event) {
		if (event.playlist instanceof DefaultPlaylist) {
			defaultPlaylistTable.update(((DefaultPlaylist) event.playlist).getDefaultPlaylistEntity());
		}
	}

	private void onPlaylistAdd(PlaylistAddEvent event) {
		if (event.playlist.getClass() == DefaultPlaylist.class) {
			final DefaultPlaylist defaultPlaylist = (DefaultPlaylist) event.playlist;
			defaultPlaylistTable.insert(defaultPlaylist.getDefaultPlaylistEntity());
		}
	}

	private void onPlaylistRemove(PlaylistRemoveEvent event) {
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
