package de.nerogar.sandstormBot.system.guiSystem;

import de.nerogar.sandstormBot.gui.MessagePanel;
import de.nerogar.sandstormBotApi.opusPlayer.Song;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

public class PlaylistsPanel extends MessagePanel {

	private IGuildMain guildMain;

	public PlaylistsPanel(IGuildMain guildMain) {
		super(guildMain.getGuild().getTextChannelById(guildMain.getSettings().uiChannelId));
		this.guildMain = guildMain;
	}

	@Override
	protected String render() {
		StringBuilder sb = new StringBuilder();
		sb.append("====================\n");
		sb.append("Playlists\n");
		sb.append("====================\n");

		for (IPlaylist playList : guildMain.getPlaylists()) {
			if (playList == guildMain.getPlaylists().getCurrent()) sb.append("▶ ");
			else sb.append("  ");

			long duration = 0;
			for (Song song : playList.getSongs()) {
				duration += song.getDuration();
			}

			sb.append(playList.getName());
			sb.append(" [").append(playList.getSongs().size()).append(", ").append(formatTime(duration)).append("]");
			sb.append("\n");

		}

		return sb.toString();
	}

}
