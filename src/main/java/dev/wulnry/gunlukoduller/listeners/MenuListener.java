package dev.wulnry.gunlukoduller.listeners;

import dev.wulnry.gunlukoduller.GunlukOduller;
import dev.wulnry.gunlukoduller.managers.DataManager;
import dev.wulnry.gunlukoduller.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class MenuListener implements Listener {

    private final GunlukOduller plugin;
    private final DataManager dataManager;

    public MenuListener(GunlukOduller plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();

        if (!(event.getInventory().getHolder() instanceof dev.wulnry.gunlukoduller.utils.DailyRewardHolder)) {
            return;
        }

        event.setCancelled(true); // Prevent item moving

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(view.getTopInventory())) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Check assistant item
        int asSlot = plugin.getConfig().getInt("menu.assistant-slot", 31);
        if (slot == asSlot) {
            player.closeInventory();
            player.performCommand("asistan");
            return;
        }

        // Check rewards
        ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("rewards");
        if (rewardsSection != null) {
            Set<String> categories = rewardsSection.getKeys(false);
            for (String category : categories) {
                ConfigurationSection sec = rewardsSection.getConfigurationSection(category);
                if (sec == null) continue;

                if (sec.getInt("slot") == slot) {
                    handleRewardClick(player, category, sec);
                    break;
                }
            }
        }
    }

    private void handleRewardClick(Player player, String category, ConfigurationSection sec) {
        String perm = sec.getString("permission");
        boolean hasPerm = perm == null || perm.isEmpty() || player.hasPermission(perm);

        String prefix = plugin.getConfig().getString("settings.prefix", "&#15FF08&lSurvival &8▸ ");

        if (!hasPerm) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ColorUtils.format(prefix + plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (!dataManager.canClaim(player.getUniqueId(), category)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ColorUtils.format(prefix + plugin.getConfig().getString("messages.already-claimed")));
            return;
        }

        // Give reward
        dataManager.updateClaimTime(player.getUniqueId(), category, System.currentTimeMillis());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage(ColorUtils.format(prefix + plugin.getConfig().getString("messages.success")));
        player.closeInventory();

        List<String> commands = sec.getStringList("commands");
        for (String cmd : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }
    }


}
