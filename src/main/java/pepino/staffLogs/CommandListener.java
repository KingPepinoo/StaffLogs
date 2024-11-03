package pepino.staffLogs;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommandListener implements Listener {

    private final StaffLogs plugin;

    public CommandListener(StaffLogs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        if (message.startsWith("/tp") || message.startsWith("/ban") || message.startsWith("/gamemode")) {
            // Get current time
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Read the webhook URL from config.yml
            final String webhookUrl = plugin.getConfig().getString("webhook_url");
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                plugin.getLogger().warning("Discord webhook URL is not configured!");
                return; // Exit if webhook URL is not set
            }

            final String commandName;
            final String target;
            final String details;

            String[] args = message.split(" ");
            commandName = args[0].startsWith("/") ? args[0].substring(1) : args[0];

            if (message.startsWith("/tp")) {
                if (args.length >= 2) {
                    target = args[1];
                    details = "Teleported to " + target;
                } else {
                    target = "No target specified";
                    details = "Attempted to teleport without specifying a target.";
                }
            } else if (message.startsWith("/ban")) {
                if (args.length >= 2) {
                    target = args[1];
                    details = "Banned player " + target;
                } else {
                    target = "No target specified";
                    details = "Attempted to ban without specifying a player.";
                }
            } else if (message.startsWith("/gamemode")) {
                if (args.length >= 2) {
                    target = args[1];
                    details = "Changed gamemode to " + target;
                } else {
                    target = "No gamemode specified";
                    details = "Attempted to change gamemode without specifying a mode.";
                }
            } else {
                return; // Command not handled
            }

            // Send the message asynchronously to avoid blocking the main thread
            final String finalTarget = target;
            final String finalTime = time;
            final String finalDetails = details;

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                sendDiscordMessage(webhookUrl, player.getName(), commandName, finalTarget, finalTime, finalDetails);
            });
        }
    }

    private void sendDiscordMessage(String webhookUrl, String playerName, String commandName, String target, String time, String details) {
        try {
            // Optional: Read embed color from config.yml
            int color = plugin.getConfig().getInt("embed_color", 5814783); // Default color if not set

            String jsonPayload = String.format("{\"embeds\": [{"
                            + "\"title\": \"%s executed a command!\","
                            + "\"color\": %d,"
                            + "\"fields\": ["
                            + "{\"name\": \"Player\", \"value\": \"%s\", \"inline\": true},"
                            + "{\"name\": \"Command\", \"value\": \"%s\", \"inline\": true},"
                            + "{\"name\": \"Target\", \"value\": \"%s\", \"inline\": true},"
                            + "{\"name\": \"Time\", \"value\": \"%s\", \"inline\": true},"
                            + "{\"name\": \"Details\", \"value\": \"%s\", \"inline\": false}"
                            + "]}]}",
                    escapeJson(playerName),
                    color,
                    escapeJson(playerName),
                    escapeJson(commandName),
                    escapeJson(target),
                    escapeJson(time),
                    escapeJson(details)
            );

            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();

            if (responseCode != 204) {
                plugin.getLogger().warning("Failed to send message to Discord webhook. Response code: " + responseCode);
                InputStream is = connection.getErrorStream();
                if (is != null) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                        response.append('\r');
                    }
                    rd.close();
                    plugin.getLogger().warning("Response: " + response.toString());
                }
            }
            connection.disconnect();

        } catch (Exception e) {
            plugin.getLogger().severe("Error sending message to Discord webhook: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String escapeJson(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20 || c > 0x7E) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
