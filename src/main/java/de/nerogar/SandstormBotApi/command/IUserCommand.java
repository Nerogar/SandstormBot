package de.nerogar.sandstormBotApi.command;

import de.nerogar.sandstormBot.UserGroup;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;

public interface IUserCommand extends ICommand {

	boolean accepts(String command, String[] commandSplit);

	UserGroup getMinUserGroup();

	void setCommandString(VoiceChannel voiceChannel, Member member, String command, String[] commandSplit);

	IUserCommand newInstance();
}
