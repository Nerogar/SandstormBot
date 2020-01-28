package de.nerogar.sandstormBot;

import de.nerogar.sandstormBot.opusPlayer.OpusPlayer;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

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
	public ByteBuffer provide20MsAudio() {
		return ByteBuffer.wrap(player.provide20MsFrame());
	}

	@Override
	public boolean isOpus() {
		return true;
	}

}
