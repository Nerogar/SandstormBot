package de.nerogar.sandstormBot;

import de.nerogar.sandstormBot.oldPlayer.OpusPlayer;
import net.dv8tion.jda.core.audio.AudioSendHandler;

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
	public byte[] provide20MsAudio() {
		return player.nextFrame();
	}

	@Override
	public boolean isOpus() {
		return true;
	}

}
