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
        
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Player p = event.getPlayer();
            if (p.isOnline() && hasAvailableRewards(p)) {
                String msg = getConfig().getString("messages.join-reminder");
                if (msg != null && !msg.isEmpty()) {
                    p.sendMessage(dev.wulnry.gunlukoduller.utils.ColorUtils.format(getConfig().getString("settings.prefix", "") + msg));
                }
            }
        }, 60L);
    }

    public boolean hasAvailableRewards(Player player) {
        org.bukkit.configuration.ConfigurationSection rewardsSection = getConfig().getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String category : rewardsSection.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection sec = rewardsSection.getConfigurationSection(category);
                if (sec == null) continue;
                String perm = sec.getString("permission");
                boolean hasPerm = perm == null || perm.isEmpty() || player.hasPermission(perm);
                if (hasPerm && dataManager.canClaim(player.getUniqueId(), category)) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dataManager.unloadPlayerCache(event.getPlayer().getUniqueId());
    }
}
