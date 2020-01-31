package de.nerogar.sandstormBotApi.opusPlayer;

import java.util.function.Predicate;

@FunctionalInterface
public interface ISongPredicate extends Predicate<Song> {

	boolean test(Song song, int index, int invocation);

	@Override
	default boolean test(Song song) {
		return test(song, 0, 0);
	}

}
