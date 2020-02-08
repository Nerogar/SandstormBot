package de.nerogar.sandstormBot.system.guiSystem;

import de.nerogar.sandstormBot.gui.MessagePanel;
import de.nerogar.sandstormBotApi.opusPlayer.PlayState;
import de.nerogar.sandstormBotApi.opusPlayer.PlayerState;
import de.nerogar.sandstormBotApi.opusPlayer.Song;
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

		IPlaylist currentPlaylist = guildMain.getPlaylists().getCurrent();
		Song currentSong = null;
		if (currentPlaylist != null) {
			currentSong = currentPlaylist.getCurrentSong();
		}

		if (currentSong == null) {
			sb.append("not playing anything\n");
			sb.append("⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
		} else {
			if (guildMain.getPlayer().getPlayState() == PlayState.PAUSED) {
				sb.append("❚❚ ");
			} else {
				sb.append("▶ ");
			}

			if (currentSong.getAlbum() != null) {
				sb.append(currentSong.getAlbum()).append(" - ").append(currentSong.getTitle());
			} else if (currentSong.getArtist() != null) {
				sb.append(currentSong.getArtist()).append(" - ").append(currentSong.getTitle());
			} else {
				sb.append(currentSong.getTitle());
			}

			sb.append('\n');

			sb.append(currentSong.getQuery()).append(" (").append(currentSong.getUser()).append(")");
			sb.append('\n');

			sb.append(currentSong.getLocation());
			sb.append('\n');

			long position = guildMain.getPlayer().getCurrentTrackProgress();
			long duration = currentSong.getDuration();
			int progress = (int) Math.round(((double) position / duration) * 26);
			String progressString = formatTime(position) + "/" + formatTime(duration);

			for (int i = 0; i < progress; i++) sb.append('⬜');
			for (int i = 0; i < (26 - progress); i++) sb.append('⬛');
			sb.append(" ").append(progressString).append('\n');
		}

		return sb.toString();
	}

}
