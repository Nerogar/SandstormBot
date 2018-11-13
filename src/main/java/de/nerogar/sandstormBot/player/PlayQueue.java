package de.nerogar.sandstormBot.player;

import com.fasterxml.jackson.databind.JsonNode;

public class PlayQueue extends PlayList {

	private int currentPlaying;

	public PlayQueue(JsonNode jsonNode) {
		super(jsonNode);
	}

	public PlayQueue() {
		super("Queue");
	}

	@Override
	public void previous() {
		// previous is not allowed
	}

	public void next() {
		Song currentSong = getCurrentSong();
		remove(s -> s == currentSong);

		if (getCurrentSong() == null && size() > 0) {
			super.next();
		}
	}

	@Override
	public void setOrder(String order) {
		// the order is always default
	}

}
