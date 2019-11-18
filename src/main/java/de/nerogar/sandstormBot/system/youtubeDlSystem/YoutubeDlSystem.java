package de.nerogar.sandstormBot.system.youtubeDlSystem;

import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.system.localAudioSystem.LocalAudioTrackProvider;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.system.ISystem;

public class YoutubeDlSystem implements ISystem {

	@Override
	public void init(EventManager eventManager, IGuildMain guildMain) {
		guildMain.getUserCommands().add(new AddCommand());

		guildMain.getAudioTrackProviders().addAudioTrackProvider(new YoutubeDlAudioTrackProvider());
	}

}