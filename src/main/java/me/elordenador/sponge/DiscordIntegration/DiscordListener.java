package me.elordenador.sponge.DiscordIntegration;

import com.google.inject.spi.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.apache.logging.log4j.Logger;

public class DiscordListener implements EventListener {
    private DiscordIntegration discordIntegration;
    private Logger logger;
    public DiscordListener(DiscordIntegration discordIntegration) {
        this.discordIntegration = discordIntegration;
        this.logger = this.discordIntegration.getLogger();
    }
    @Override
    public void onEvent(GenericEvent genericEvent) {
        if (genericEvent instanceof ReadyEvent) {

            this.logger.info("Bot is ready");
        } else if (genericEvent instanceof MessageReceivedEvent) {

            MessageReceivedEvent event = (MessageReceivedEvent) genericEvent;
            String author = event.getAuthor().getName();
            if (event.getAuthor().getId().equals(discordIntegration.getJDA().getSelfUser().getId())) {
                return;
            }
            String message = event.getMessage().getContentStripped();
            if (event.getChannel().getId().equals(this.discordIntegration.getCID())) {
                this.discordIntegration.enviarMensajeGlobal("[DC] " + author + ": " + message);
            }

        }
    }
}
