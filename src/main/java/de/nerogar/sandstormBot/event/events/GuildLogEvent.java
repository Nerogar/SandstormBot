package de.nerogar.sandstormBot.event.events;

import de.nerogar.sandstormBotApi.event.IEvent;

public class GuildLogEvent implements IEvent {

	public final String message;

	public GuildLogEvent(String message) {
		this.message = message;
	}
}
