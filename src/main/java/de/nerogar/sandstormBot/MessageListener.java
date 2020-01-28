package de.nerogar.sandstormBot;

import de.nerogar.sandstormBotApi.IGuildMain;
import net.dv8tion.jda.client.events.call.voice.CallVoiceJoinEvent;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getMessage().isWebhookMessage()) return;
		if (event.getJDA().getSelfUser().getId().equals(event.getAuthor().getId())) return;
		if (!Main.getGuildMain(event.getGuild()).getSettings().uiChannelId.equals(event.getChannel().getId())) return;

		String message = event.getMessage().getContentRaw();

		if (!event.getJDA().getSelfUser().getId().equals(event.getAuthor().getId())) {
			event.getChannel().deleteMessageById(event.getMessage().getId()).queue();
			IGuildMain guildMain = Main.getGuildMain(event.getGuild());

			guildMain.getUserCommands().execute(event.getMember(), message);
		}
	}

	private void reactionCommand(Guild guild, MessageChannel channel, Member member, String messageId, String command) {
		IGuildMain guildMain = Main.getGuildMain(guild);
		for (GuildSettings.EmoteCommand emoteCommand : guildMain.getSettings().emoteCommands) {
			if (command.equals(emoteCommand.emote)) {
				guildMain.getUserCommands().execute(member, emoteCommand.command);
			}
		}
	}

	@Override
	public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		if (event.getJDA().getSelfUser().getId().equals(event.getMember().getUser().getId())) return;
		if (!Main.getGuildMain(event.getGuild()).getSettings().uiChannelId.equals(event.getChannel().getId())) return;
		reactionCommand(event.getGuild(), event.getChannel(), event.getMember(), event.getMessageId(), event.getReactionEmote().getName());
	}

	@Override
	public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
		if (event.getJDA().getSelfUser().getId().equals(event.getMember().getUser().getId())) return;
		if (!Main.getGuildMain(event.getGuild()).getSettings().uiChannelId.equals(event.getChannel().getId())) return;
		reactionCommand(event.getGuild(), event.getChannel(), event.getMember(), event.getMessageId(), event.getReactionEmote().getName());
	}

	@Override
	public void onShutdown(ShutdownEvent event) {
		System.exit(0);
	}

	@Override
	public void onCallVoiceJoin(CallVoiceJoinEvent event) {
		//Main.LOGGER.log(Logger.DEBUG, "onCallVoiceJoin " + event);
	}

	@Override
	public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
		//Main.LOGGER.log(Logger.DEBUG, "onGuildVoiceJoin " + event);
	}

}
