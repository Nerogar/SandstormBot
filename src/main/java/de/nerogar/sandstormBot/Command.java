package de.nerogar.sandstormBot;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;

public interface Command {

	void execute(MessageChannel channel, Member member, String[] commandSplit, String command);

}
