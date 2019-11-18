package de.nerogar.sandstormBot.system.localAudioSystem;

import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.system.ISystem;

public class LocalAudioSystem implements ISystem {

	private EventManager eventManager;
	private IGuildMain   guildMain;

	@Override
	public void init(EventManager eventManager, IGuildMain guildMain) {
		this.eventManager = eventManager;
		this.guildMain = guildMain;

		guildMain.getAudioTrackProviders().addAudioTrackProvider(new LocalAudioTrackProvider());
	}
}
