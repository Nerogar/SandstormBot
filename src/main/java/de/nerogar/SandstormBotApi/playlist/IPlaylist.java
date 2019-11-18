package de.nerogar.sandstormBotApi.playlist;

import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.opusPlayer.ISongProvider;

import java.util.List;

public interface IPlaylist extends ISongProvider {

	String getName();

	int size();

	List<Song> getSongs();

}
