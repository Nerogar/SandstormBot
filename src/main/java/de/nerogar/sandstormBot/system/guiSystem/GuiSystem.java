package de.nerogar.sandstormBot.system.guiSystem;

import de.nerogar.sandstormBot.event.EventManager;
import de.nerogar.sandstormBot.event.events.*;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.system.ISystem;

public class GuiSystem implements ISystem {

	private EventManager eventManager;
	private IGuildMain   guildMain;

	@Override
	public void init(EventManager eventManager, IGuildMain guildMain) {
		this.eventManager = eventManager;
		this.guildMain = guildMain;

		LogPanel logPanel = new LogPanel(guildMain);
		guildMain.getGui().addPanel(logPanel);

		PlaylistsPanel playlistsPanel = new PlaylistsPanel(guildMain);
		guildMain.getGui().addPanel(playlistsPanel);

		PlaylistPanel playlistPanel = new PlaylistPanel(guildMain);
		guildMain.getGui().addPanel(playlistPanel);

		PlayerPanel playerPanel = new PlayerPanel(guildMain);
		guildMain.getGui().addPanel(playerPanel);

		// log panel
		eventManager.register(GuildLogEvent.class, e -> logPanel.setDirty());

		// playlists panel
		eventManager.register(PlaylistAddEvent.class, e -> playlistsPanel.setDirty());
		eventManager.register(PlaylistRemoveEvent.class, e -> playlistsPanel.setDirty());
		eventManager.register(PlaylistChangeCurrentEvent.class, e -> playlistsPanel.setDirty());

		// playlist panel
		eventManager.register(SongAddEvent.class, e -> playlistPanel.setDirty());
		eventManager.register(SongRemoveEvent.class, e -> playlistPanel.setDirty());
		eventManager.register(SongChangeCurrentEvent.class, e -> playlistPanel.setDirty());
		eventManager.register(PlaylistChangeCurrentEvent.class, e -> playlistPanel.setDirty());

		// player panel
		eventManager.register(SongChangeEvent.class, e -> playerPanel.setDirty());
		eventManager.register(SongChangeCurrentEvent.class, e -> playerPanel.setDirty());

		// start thread
		new GuiUpdateThread(guildMain).start();
	}
}
