package de.nerogar.sandstormBot.player;

import java.util.function.Predicate;

public interface SongPredicate extends Predicate<Song> {

	boolean test(Song song, int index, int invocation);

	@Override
	default boolean test(Song song) {
		return test(song, 0, 0);
	}

}
