package de.nerogar.sandstormBot.musicProvider;

import de.nerogar.sandstormBot.player.Song;

import java.util.List;

public interface IMusicProvider {

	List<Song> getSongs(String query, String user);

	void doCache(Song song);

}
