package de.nerogar.sandstormBot.player;

import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.PlayerMain;

public class OpusPlayerEventsImpl implements OpusPlayerEvents {

	private final PlayerMain playerMain;

	public OpusPlayerEventsImpl(PlayerMain playerMain) {
		this.playerMain = playerMain;
	}

	@Override
	public void onTrackEnd(boolean mayStartNext) {
		if (mayStartNext) {
			playerMain.acceptCommand(null, null, Main.SETTINGS.commandPrefix + "next");
		}
	}
}
