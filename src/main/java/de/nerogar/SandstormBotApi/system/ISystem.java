package de.nerogar.sandstormBotApi.system;

import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBotApi.IGuildMain;

public interface ISystem {

	void init(EventManager eventManager, IGuildMain guildMain);

}
