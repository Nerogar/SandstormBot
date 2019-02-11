package de.nerogar.sandstormBot.player;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;

public class AudioResultHandler implements AudioLoadResultHandler {

	private MusicPlayer musicPlayer;
	private Song        song;
	private AudioPlayer player;

	public AudioResultHandler(MusicPlayer musicPlayer, Song song, AudioPlayer player) {
		this.musicPlayer = musicPlayer;
		this.song = song;
		this.player = player;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		if (musicPlayer.getCurrentSong() == song) {
			player.playTrack(track);
		} else {
			//Main.LOGGER.log(Logger.DEBUG, "wrong track finished loading: " + song.getDisplayName() + ", expected: " + musicPlayer.getCurrentSong().getDisplayName());
		}
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
	}

	@Override
	public void noMatches() {

	}

	@Override
	public void loadFailed(FriendlyException exception) {
		Main.LOGGER.log(Logger.WARNING, "Loading of an audio track failed!");
		Main.LOGGER.log(Logger.WARNING, song);
		exception.printStackTrace(Main.LOGGER.getWarningStream());
	}

}
