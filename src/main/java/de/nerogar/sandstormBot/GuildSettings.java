package de.nerogar.sandstormBot;

import java.util.Set;

public class GuildSettings {

	public Set<String> adminRoles;
	public String      uiChannelId;
	public long        guiUpdateInterval;

	public EmoteCommand[] emoteCommands = {};

	public static class EmoteCommand {

		public String  emote;
		public String  command;
		public boolean hidden = false;
	}

}
