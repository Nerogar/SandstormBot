package de.nerogar.sandstormBot.system.guiSystem;

import de.nerogar.sandstormBot.gui.MessagePanel;
import de.nerogar.sandstormBotApi.IGuildMain;

public class LogPanel extends MessagePanel {

	private IGuildMain guildMain;

	public LogPanel(IGuildMain guildMain) {
		super(guildMain.getGuild().getTextChannelById(guildMain.getSettings().uiChannelId));
		this.guildMain = guildMain;
	}

	@Override
	protected String render() {
		return String.join("\n", guildMain.getLog());
	}
}
