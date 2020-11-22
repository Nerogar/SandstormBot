package de.nerogar.sandstormBot;

import de.nerogar.sandstormBot.player.OpusPlayer;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

public class FFmpegAudioPlayerSendHandler implements AudioSendHandler {

	private OpusPlayer player;

	public FFmpegAudioPlayerSendHandler(OpusPlayer player) {
		this.player = player;
	}

	@Override
	public boolean canProvide() {
		return player.canProvide();
	}

	@Override
	public ByteBuffer provide20MsAudio() {
		return ByteBuffer.wrap(player.nextFrame());
	}

	@Override
	public boolean isOpus() {
		return true;
	}

}
