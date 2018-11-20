package de.nerogar.sandstormBot.player;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBot.PlayerMain;

public class SongEvents extends AudioEventAdapter {

	private PlayerMain  playerMain;
	private MusicPlayer musicPlayer;

	public SongEvents(PlayerMain playerMain, MusicPlayer musicPlayer) {
		this.playerMain = playerMain;
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
			playerMain.acceptCommand(null, null, Main.SETTINGS.commandPrefix + "next");
		}
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
	}

}
