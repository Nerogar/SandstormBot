package de.nerogar.sandstormBot;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;

public interface Command {

	CommandResult execute(MessageChannel channel, Member member, String[] commandSplit, String command);

	static class CommandResult {

		public static CommandResult SUCCESS          = new CommandResult(false, "success");
		public static CommandResult ERROR_PERMISSION = new CommandResult(true, "you don't have permission to use this command");

		static class UnknownCommandResult extends CommandResult {

			public UnknownCommandResult(String command) {
				super(false, "unknown command: " + command);
			}
		}

		private boolean success;
		private String  message;

		public CommandResult(boolean success, String message) {
			this.success = success;
			this.message = message;
		}

		public boolean isSuccess() { return success; }

		public String getMessage() { return message; }

	}

}
