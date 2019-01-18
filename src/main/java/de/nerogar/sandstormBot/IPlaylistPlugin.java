package de.nerogar.sandstormBot;

import de.nerogar.sandstormBot.player.PlayList;
import de.nerogar.sandstormBot.player.Song;

import java.util.Map;

public interface IPlaylistPlugin {

	default IPlaylistPlugin newInstance() {
		try {
			return getClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	String getName();

	void init(PlayList playList);

	Map<String, Command> addCommands();

	default void onAddSong(Song song)    {}

	default void onRemoveSong(Song song) {}

	default void onNext()                {}

	default void onPrevious()            {}

}
