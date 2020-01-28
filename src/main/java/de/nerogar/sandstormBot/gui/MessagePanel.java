package de.nerogar.sandstormBot.gui;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

public abstract class MessagePanel {

	private Message message;
	private boolean isDirty;
	private long    lastUpdate;

	public MessagePanel(TextChannel uiChannel) {
		message = uiChannel.sendMessage("``` ```").complete();
		isDirty = true;
	}

	public Message getMessage()  { return message; }

	public final void setDirty() { isDirty = true; }

	protected abstract String render();

	public final void update() {
		if (isDirty) {
			String message = render();
			String messageString = message.substring(0, Math.min(1800, message.length()));
			this.message.editMessage("```" + messageString + "```").queue();
			lastUpdate = System.currentTimeMillis();
		}
		isDirty = false;
	}

	protected String formatTime(long ms) {
		long hours = ms / (1000 * 60 * 60);
		ms -= (hours * 1000 * 60 * 60);
		long minutes = ms / (1000 * 60);
		ms -= (minutes * 1000 * 60);
		long seconds = ms / (1000);

		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		} else {
			return String.format("%d:%02d", minutes, seconds);
		}
	}

}
