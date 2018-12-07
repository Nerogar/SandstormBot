package de.nerogar.sandstormBot.listenToThis;

import de.nerogar.sandstormBot.Command;
import de.nerogar.sandstormBot.IPlaylistPlugin;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.player.PlayList;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.HashMap;
import java.util.Map;

public class ListenToThisPlugin implements IPlaylistPlugin {

	private PlayList playList;

	@Override
	public String getName() {
		return "listen to this";
	}

	@Override
	public void init(PlayList playList) {
		this.playList = playList;
	}

	@Override
	public Map<String, Command> addCommands() {
		Map<String, Command> commandMap = new HashMap<>();

		commandMap.put(Main.SETTINGS.commandPrefix + "generate", this::cmdGenerate);

		return commandMap;
	}

	private Command.CommandResult cmdGenerate(MessageChannel channel, Member member, String[] commandSplit, String command) {
		System.out.println("generate command");
		return Command.CommandResult.SUCCESS;
	}

}
