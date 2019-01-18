package de.nerogar.sandstormBot.player;

public class SongIndexPredicate implements SongPredicate {

	private int index;

	public SongIndexPredicate(int index) {
		this.index = index;
	}

	@Override
	public boolean test(Song song, int index, int invocation) {
		return index == this.index;
	}

}
