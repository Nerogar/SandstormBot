package de.nerogar.sandstormBot.player;

public class SongInvocationPredicate implements SongPredicate {

	private int invocation;

	public SongInvocationPredicate(int invocation) {
		this.invocation = invocation;
	}

	@Override
	public boolean test(Song song, int index, int invocation) {
		return invocation == this.invocation;
	}

}
