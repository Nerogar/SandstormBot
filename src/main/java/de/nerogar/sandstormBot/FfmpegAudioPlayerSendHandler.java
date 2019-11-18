package de.nerogar.sandstormBot;

import de.nerogar.sandstormBot.opusPlayer.OpusPlayer;
import net.dv8tion.jda.core.audio.AudioSendHandler;

public class FfmpegAudioPlayerSendHandler implements AudioSendHandler {

	private OpusPlayer player;

	public FfmpegAudioPlayerSendHandler(OpusPlayer player) {
		this.player = player;
	}

	@Override
	public boolean canProvide() {
		return player.canProvideFrame();
	}

	@Override
	public byte[] provide20MsAudio() {
		return player.provide20MsFrame();
	}

	@Override
	public boolean isOpus() {
		return true;
	}

}
