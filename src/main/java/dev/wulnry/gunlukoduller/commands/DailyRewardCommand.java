package dev.wulnry.gunlukoduller.commands;

import dev.wulnry.gunlukoduller.GunlukOduller;
import dev.wulnry.gunlukoduller.managers.DataManager;
import dev.wulnry.gunlukoduller.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class DailyRewardCommand implements CommandExecutor {

    private final GunlukOduller plugin;
    private final DataManager dataManager;

    public DailyRewardCommand(GunlukOduller plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("sıfırla")) {
            if (!sender.hasPermission("gunlukodul.admin")) {
                sender.sendMessage(ColorUtils.format(plugin.getConfig().getString("settings.prefix", "") + plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            
            String targetName = args[1];
            org.bukkit.OfflinePlayer offlineTarget = Bukkit.getPlayer(targetName);
            if (offlineTarget == null) {
                offlineTarget = Bukkit.getOfflinePlayer(targetName);
            }
            
            if (offlineTarget == null || (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline())) {
                sender.sendMessage(ColorUtils.format(plugin.getConfig().getString("settings.prefix", "") + "&cOyuncu bulunamadı."));
                return true;
            }
            
            String category = args.length == 3 ? args[2] : null;
            dataManager.resetClaimTime(offlineTarget.getUniqueId(), category);
            
            sender.sendMessage(ColorUtils.format(plugin.getConfig().getString("settings.prefix", "") + "&a" + offlineTarget.getName() + " adlı oyuncunun " + (category == null ? "tüm ödülleri" : category + " ödülü") + " sıfırlandı."));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.format("&cBu komut sadece oyuncular tarafindan kullanilabilir."));
            return true;
        }

        Player player = (Player) sender;
        openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        String title = ColorUtils.format(plugin.getConfig().getString("menu.title", "&0Günlük Ödül"));
        int size = plugin.getConfig().getInt("menu.size", 36);
        dev.wulnry.gunlukoduller.utils.DailyRewardHolder holder = new dev.wulnry.gunlukoduller.utils.DailyRewardHolder();
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("rewards");
        if (rewardsSection != null) {
            Set<String> categories = rewardsSection.getKeys(false);
            for (String category : categories) {
                ConfigurationSection sec = rewardsSection.getConfigurationSection(category);
                if (sec == null) continue;

                int slot = sec.getInt("slot");
                String perm = sec.getString("permission");
                String name = ColorUtils.format(sec.getString("name"));
                List<String> loreFormat = sec.getStringList("lore");

                boolean hasPerm = perm == null || perm.isEmpty() || player.hasPermission(perm);
                boolean canClaim = hasPerm && dataManager.canClaim(player.getUniqueId(), category);

                String statusLore;
                Material material;

                if (canClaim) {
                    statusLore = ColorUtils.format(plugin.getConfig().getString("menu.available-item.status-lore", "&#00D420Alınabilir"));
                    material = Material.matchMaterial(plugin.getConfig().getString("menu.available-item.material", "CHEST_MINECART"));
                } else {
                    statusLore = ColorUtils.format(plugin.getConfig().getString("menu.unavailable-item.status-lore", "&cAlınamaz"));
                    material = Material.matchMaterial(plugin.getConfig().getString("menu.unavailable-item.material", "TNT_MINECART"));
                }

                if (material == null) material = Material.CHEST_MINECART;

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(name);
                    List<String> lore = new ArrayList<>();
                    for (String l : loreFormat) {
                        lore.add(ColorUtils.format(l).replace("%status%", statusLore));
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }

                if (slot >= 0 && slot < size) {
                    inventory.setItem(slot, item);
                }
            }
        }

        // Assistant Item
        int asSlot = plugin.getConfig().getInt("menu.assistant-slot", 31);
        String asMatName = plugin.getConfig().getString("menu.assistant-item.material", "ARROW");
        String asName = ColorUtils.format(plugin.getConfig().getString("menu.assistant-item.name", "&#00D420Asistan Menüsüne Dön"));
        Material asMat = Material.matchMaterial(asMatName);
        if (asMat == null) asMat = Material.ARROW;

        ItemStack asItem = new ItemStack(asMat);
        ItemMeta asMeta = asItem.getItemMeta();
        if (asMeta != null) {
            asMeta.setDisplayName(asName);
            asItem.setItemMeta(asMeta);
        }
        if (asSlot >= 0 && asSlot < size) {
            inventory.setItem(asSlot, asItem);
        }

        player.openInventory(inventory);
    }


}
