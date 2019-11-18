package de.nerogar.sandstormBot.audioTrackProvider;

import de.nerogar.sandstormBotApi.IGuildMain;
import de.nerogar.sandstormBotApi.audioTrackProvider.IAudioTrackProvider;

import java.util.ArrayList;
import java.util.List;

public class AudioTrackProviders {

	private IGuildMain                guildMain;
	private List<IAudioTrackProvider> audioTrackProviders;

	public AudioTrackProviders(IGuildMain guildMain) {
		this.guildMain = guildMain;

		audioTrackProviders = new ArrayList<>();
	}

	public void addAudioTrackProvider(IAudioTrackProvider audioTrackProvider) {
		audioTrackProviders.add(audioTrackProvider);
	}

	public IAudioTrackProvider getAudioTrackProvider(String name) {
		for (IAudioTrackProvider audioTrackProvider : audioTrackProviders) {
			if (audioTrackProvider.getName().equals(name)) {
				return audioTrackProvider;
			}
		}

		return null;
	}
}
