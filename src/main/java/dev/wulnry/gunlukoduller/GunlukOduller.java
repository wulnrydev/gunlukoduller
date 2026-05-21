package dev.wulnry.gunlukoduller;

import dev.wulnry.gunlukoduller.commands.DailyRewardCommand;
import dev.wulnry.gunlukoduller.listeners.MenuListener;
import dev.wulnry.gunlukoduller.managers.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class GunlukOduller extends JavaPlugin implements Listener {

    private DataManager dataManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataManager = new DataManager(this);
        dataManager.initialize();

        getCommand("günlüködül").setExecutor(new DailyRewardCommand(this, dataManager));
        getServer().getPluginManager().registerEvents(new MenuListener(this, dataManager), this);
        getServer().getPluginManager().registerEvents(this, this);

        // Load data for players already online (in case of reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            dataManager.loadPlayerAsync(p.getUniqueId());
        }

        getLogger().info("GunlukOduller eklentisi basariyla aktif edildi! (SQLite & 1.21+)");
    }

    @Override
    public void onDisable() {
        // Save and cleanup data
        if (dataManager != null) {
            dataManager.disconnect();
        }
        getLogger().info("GunlukOduller eklentisi devre disi birakildi.");
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        dataManager.loadPlayerAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dataManager.unloadPlayerCache(event.getPlayer().getUniqueId());
    }
}
