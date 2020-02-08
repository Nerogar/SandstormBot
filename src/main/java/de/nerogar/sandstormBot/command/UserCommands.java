package de.nerogar.sandstormBot.command;

import de.nerogar.sandstormBot.GuildMain;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.command.userCommands.*;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.UserGroup;
import de.nerogar.sandstormBotApi.command.ICommand;
import de.nerogar.sandstormBotApi.command.IUserCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.List;

public class UserCommands {

	private IGuildMain         guildMain;
	private List<IUserCommand> userCommands;

	public UserCommands(GuildMain guildMain) {
		this.guildMain = guildMain;

		userCommands = new ArrayList<>();
		userCommands.add(new JoinCommand());
		userCommands.add(new LeaveCommand());
		userCommands.add(new PlayCommand());
		userCommands.add(new PauseCommand());
		userCommands.add(new TogglePauseCommand());
		userCommands.add(new NextCommand());
		userCommands.add(new RemoveCommand());
		userCommands.add(new PreviousCommand());
		userCommands.add(new PlaylistCommand());
		userCommands.add(new VolumeCommand());
	}

	private UserGroup getUserGroup(Member member) {
		if (Main.SETTINGS.ownerId.equals(member.getUser().getId())) {
			return UserGroup.OWNER;
		}

		for (String adminRoleId : guildMain.getSettings().adminRoles) {
			for (Role role : member.getRoles()) {
				if (role.getId().equals(adminRoleId)) {
					return UserGroup.ADMIN;
				}
			}
		}

		return UserGroup.GUEST;
	}

	public void add(IUserCommand command) {
		userCommands.add(command);
	}

	public void addBefore(IUserCommand command, Class<? extends ICommand> other) {
		for (int i = 0; i < userCommands.size(); i++) {
			if (userCommands.get(i).getClass() == other) {
				userCommands.add(i, command);
				return;
			}
		}
		userCommands.add(command);
	}

	public void addAfter(IUserCommand command, Class<? extends ICommand> other) {
		for (int i = 0; i < userCommands.size(); i++) {
			if (userCommands.get(i).getClass() == other) {
				userCommands.add(i + 1, command);
				return;
			}
		}
		userCommands.add(command);
	}

	public void execute(Member member, String command) {
		String[] commandSplit = command.split("\\s+");

		IUserCommand userCommand = null;

		for (IUserCommand c : userCommands) {
			if (c.accepts(command, commandSplit) && getUserGroup(member).permissionLevel >= c.getMinUserGroup().permissionLevel) {
				userCommand = c.newInstance();
				break;
			}
		}

		if (userCommand == null) {
			userCommand = new UnknownCommand();
		}

		userCommand.setCommandString(member.getVoiceState().getChannel(), member, command, commandSplit);
		guildMain.getCommandQueue().add(userCommand);

	}

}
