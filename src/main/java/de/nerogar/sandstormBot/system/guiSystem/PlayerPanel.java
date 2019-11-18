package de.nerogar.sandstormBot.system.guiSystem;

import de.nerogar.sandstormBot.gui.MessagePanel;
import de.nerogar.sandstormBot.opusPlayer.PlayerState;
import de.nerogar.sandstormBot.opusPlayer.Song;
import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.playlist.IPlaylist;

public class PlayerPanel extends MessagePanel {

	private IGuildMain guildMain;

	public PlayerPanel(IGuildMain guildMain) {
		super(guildMain.getGuild().getTextChannelById(guildMain.getSettings().uiChannelId));
		this.guildMain = guildMain;
	}

	@Override
	protected String render() {
		StringBuilder sb = new StringBuilder();

		IPlaylist currentPlaylist = guildMain.getCurrentPlaylist();
		Song currentSong = null;
		if (currentPlaylist != null) {
			currentSong = currentPlaylist.getCurrentSong();
		}

		if (currentSong == null) {
			sb.append("not playing anything\n");
			sb.append("⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
		} else {
			if (guildMain.getPlayer().getState() == PlayerState.PAUSED) {
				sb.append("❚❚ ");
			} else {
				sb.append("▶ ");
			}

			if (currentSong.album != null) {
				sb.append(currentSong.album).append(" - ").append(currentSong.title);
			} else if (currentSong.artist != null) {
				sb.append(currentSong.artist).append(" - ").append(currentSong.title);
			} else {
				sb.append(currentSong.title);
			}

			sb.append('\n');

			sb.append(currentSong.query).append(" (").append(currentSong.user).append(")");
			sb.append('\n');

			long position = guildMain.getPlayer().getCurrentTrackProgress();
			long duration = currentSong.duration;
			int progress = (int) Math.round(((double) position / duration) * 26);
			String progressString = formatTime(position) + "/" + formatTime(duration);

			for (int i = 0; i < progress; i++) sb.append('⬜');
			for (int i = 0; i < (26 - progress); i++) sb.append('⬛');
			sb.append(" ").append(progressString).append('\n');
		}

		return sb.toString();
	}

}
