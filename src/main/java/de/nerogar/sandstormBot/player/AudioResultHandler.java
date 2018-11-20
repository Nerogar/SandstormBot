package de.nerogar.sandstormBot.player;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

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
			System.out.println("wrong track finished loading: " + song.getDisplayName() + ", expected: " + musicPlayer.getCurrentSong().getDisplayName());
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
		System.out.println("Loading of an audio track failed!");
		System.out.println(song);
		exception.printStackTrace();
	}

}
