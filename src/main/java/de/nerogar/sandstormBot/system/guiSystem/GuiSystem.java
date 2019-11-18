package de.nerogar.sandstormBot.system.guiSystem;

import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.SongChangeEvent;
import de.nerogar.sandstormBot.event.events.SongProviderChangeEvent;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.system.ISystem;

public class GuiSystem implements ISystem {

	private EventManager eventManager;
	private IGuildMain   guildMain;

	@Override
	public void init(EventManager eventManager, IGuildMain guildMain) {
		this.eventManager = eventManager;
		this.guildMain = guildMain;

		PlaylistPanel playlistPanel = new PlaylistPanel(guildMain);
		guildMain.getGui().addPanel(playlistPanel);

		PlayerPanel playerPanel = new PlayerPanel(guildMain);
		guildMain.getGui().addPanel(playerPanel);

		eventManager.register(SongProviderChangeEvent.class, e -> playlistPanel.setDirty());
		eventManager.register(SongChangeEvent.class, e -> playerPanel.setDirty());

		new GuiUpdateThread(guildMain).start();
	}
}
