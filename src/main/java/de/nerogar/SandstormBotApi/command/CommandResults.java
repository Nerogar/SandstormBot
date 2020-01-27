package de.nerogar.sandstormBotApi.command;

public class CommandResults {

	public static ICommandResult success() {
		return new ICommandResult() {
			@Override
			public boolean success() {
				return true;
			}

			@Override
			public String commandFeedback() {
				return null;
			}
		};
	}

	public static ICommandResult errorMessage(String message) {
		return new ICommandResult() {
			@Override
			public boolean success() {
				return false;
			}

			@Override
			public String commandFeedback() {
				return message;
			}
		};
	}

	public static ICommandResult unknownCommand(String command) {
		return new ICommandResult() {
			@Override
			public boolean success() {
				return false;
			}

			@Override
			public String commandFeedback() {
				return "unknown command: " + command;
			}
		};
	}

	public static ICommandResult insufficientPermission(String command) {
		return new ICommandResult() {
			@Override
			public boolean success() {
				return false;
			}

			@Override
			public String commandFeedback() {
				return "insufficient permission for command: " + command;
			}
		};
	}

	public static ICommandResult exception(ICommand command, Exception exception) {
		return new ICommandResult() {
			@Override
			public boolean success() {
				return false;
			}

			@Override
			public String commandFeedback() {
				return "error while executing command: " + command.getClass().getSimpleName() + " (" + exception.getClass().getSimpleName() + ")";
			}
		};
	}



}
