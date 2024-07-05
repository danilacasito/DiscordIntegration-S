package me.elordenador.sponge.DiscordIntegration;

import com.google.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin("discord-integration")
public class DiscordIntegration {
    private final Logger logger;
    private final PluginContainer container;
    private ConfigManager configManager;
    private Path configDir;
    private ConfigurationNode rootNode;
    private JDA jda;
    private String cid;
    @Inject
    DiscordIntegration(final PluginContainer container, final Logger logger) {
        this.container = container;
        this.logger = logger;
    }
    @Listener
    public void onServerStarting(final StartingEngineEvent<Server> event) {
        Game game = event.game();
        this.configManager = game.configManager();
        Path dataFolder = game.gameDirectory().resolve("config").resolve(this.container.metadata().id());
        this.configDir = dataFolder.resolve(String.valueOf(this.container.metadata().name()));
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Listener
    public void onServerStart(final StartedEngineEvent<Server> event) throws ConfigurateException {
        Path configPath = configDir.resolve("config.conf");

        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().path(configPath).build();
        this.rootNode = loader.load();
        Boolean installed = this.rootNode.node("debug", "installed").getBoolean();
        if (!installed) {
            generateConfig();
            loader.save(this.rootNode);
        }
        String token = this.rootNode.node("general", "token").getString();
        if (token.equals("<YOUT BOT TOKEN")) {
            this.logger.warn("There are not bot token defined, plugin will likely fail");
        }
        this.cid = this.rootNode.node("general", "channelid").getString();
        if (this.cid.equals("0")) {
            this.logger.warn("There are not channel id defined, plugin will likely fail");
        }

        this.jda = JDABuilder.createLight(token, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS).addEventListeners(new DiscordListener(this)).build();

    }
    @Listener
    public void onServerStopping(final StoppingEngineEvent<Server> event) {
        this.logger.info("Shutting down...");
        this.jda.shutdown();
    }

    public Logger getLogger() {
        return this.logger;
    }

    public void enviarMensajeGlobal(String mensaje) {
        Sponge.game().server().sendMessage(Component.text(mensaje));
    }

    public void generateConfig() throws SerializationException {
        this.rootNode.node("general", "token").set("<YOUR BOT TOKEN>");
        this.rootNode.node("general", "channelid").set("0");
        this.rootNode.node("debug", "installed").set(true);
    }
    public String getCID() {
        return this.cid;
    }
    @Listener
    public void onMessageSend(PlayerChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String playerName = event.cause().first(Player.class).map(player -> player.profile().name().orElse("Unknown Player")).orElse("Unknown Player");
        TextChannel channel = this.jda.getTextChannelById(this.cid);
        channel.sendMessage(playerName + ": " + message).queue();
    }

    public JDA getJDA() {
        return this.jda;
    }
}