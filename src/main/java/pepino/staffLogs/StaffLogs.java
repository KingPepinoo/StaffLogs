package pepino.staffLogs;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaffLogs extends JavaPlugin {

    @Override
    public void onEnable() {
        // Save the default config.yml if it doesn't exist
        this.saveDefaultConfig();

        // Register the event listener
        Bukkit.getServer().getPluginManager().registerEvents(new CommandListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
