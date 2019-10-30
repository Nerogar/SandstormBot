package de.nerogar.sandstormBot;

import de.nerogar.sandstormBot.oldPlayer.PlayList;
import de.nerogar.sandstormBot.oldPlayer.Song;

import java.util.Map;

public interface IPlaylistPlugin {

	default IPlaylistPlugin newInstance() {
		try {
			return getClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace(Main.LOGGER.getErrorStream());
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
