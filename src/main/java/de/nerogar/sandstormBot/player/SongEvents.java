package de.nerogar.sandstormBot.player;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import de.nerogar.sandstormBot.Main;

public class SongEvents extends AudioEventAdapter {

	private Main        main;
	private MusicPlayer musicPlayer;

	public SongEvents(Main main, MusicPlayer musicPlayer) {
		this.main = main;
		this.musicPlayer = musicPlayer;
	}

	@Override
	public void onPlayerPause(AudioPlayer player) {
	}

	@Override
	public void onPlayerResume(AudioPlayer player) {
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if (endReason.mayStartNext) {
			main.cmdNext(null, null, null, null);
		}
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
	}

}
