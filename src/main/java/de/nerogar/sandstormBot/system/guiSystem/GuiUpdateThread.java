package de.nerogar.sandstormBot.system.guiSystem;

import de.nerogar.sandstormBotApi.IGuildMain;

public class GuiUpdateThread extends Thread {

	private IGuildMain guildMain;

	public GuiUpdateThread(IGuildMain guildMain) {
		this.guildMain = guildMain;

		setDaemon(true);
	}

	@Override
	public void run() {
		while (true) {
			guildMain.getCommandQueue().offer(new GuiUpdateCommand());

			try {
				Thread.sleep(guildMain.getSettings().guiUpdateInterval);
			} catch (InterruptedException e) {
			}
		}
	}
}
