package de.nerogar.sandstormBot.command.userCommands;

import de.nerogar.sandstormBot.UserGroup;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class PauseCommand implements IUserCommand {

	@Override
	public boolean accepts(String command, String[] commandSplit) {
		if (command.isBlank()) return false;
		return commandSplit[0].equals("pause");
	}

	@Override
	public UserGroup getMinUserGroup() {
		return UserGroup.OWNER;
	}

	@Override
	public void setCommandString(VoiceChannel voiceChannel, Member member, String command, String[] commandSplit) {
	}

	@Override
	public void execute(IGuildMain guildMain) {
		guildMain.getPlayer().pause();
	}
}
