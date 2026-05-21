package dev.wulnry.gunlukoduller.managers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final JavaPlugin plugin;
    private Connection connection;
    
    // Cache: UUID -> (Category -> Last Claimed Timestamp)
    private final Map<UUID, Map<String, Long>> playerCache = new ConcurrentHashMap<>();

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        connect();
        createTable();
    }

    private void connect() {
        File dataFolder = new File(plugin.getDataFolder(), "database.db");
        if (!dataFolder.getParentFile().exists()) {
            dataFolder.getParentFile().mkdirs();
        }
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite connection could not be established!");
            e.printStackTrace();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS daily_rewards (" +
                "uuid VARCHAR(36) NOT NULL," +
                "category VARCHAR(50) NOT NULL," +
                "last_claimed BIGINT NOT NULL," +
                "PRIMARY KEY(uuid, category)" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                // Wait for async tasks or just save remaining cache if needed.
                // Normally we save players on quit, so cache might be empty for offline players.
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadPlayerAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT category, last_claimed FROM daily_rewards WHERE uuid = ?";
            Map<String, Long> categoryMap = new ConcurrentHashMap<>();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    categoryMap.put(rs.getString("category"), rs.getLong("last_claimed"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            playerCache.put(uuid, categoryMap);
        });
    }

    public boolean canClaim(UUID uuid, String category) {
        long lastClaimed = getLastClaimed(uuid, category);
        if (lastClaimed == 0) return true;

        String type = plugin.getConfig().getString("settings.cooldown-type", "HOURS");
        if (type.equalsIgnoreCase("MIDNIGHT")) {
            java.util.Calendar last = java.util.Calendar.getInstance();
            last.setTimeInMillis(lastClaimed);
            java.util.Calendar now = java.util.Calendar.getInstance();

            return last.get(java.util.Calendar.YEAR) != now.get(java.util.Calendar.YEAR) ||
                    last.get(java.util.Calendar.DAY_OF_YEAR) != now.get(java.util.Calendar.DAY_OF_YEAR);
        } else {
            int hours = plugin.getConfig().getInt("settings.cooldown-hours", 24);
            long cooldownMillis = hours * 3600000L;
            return System.currentTimeMillis() - lastClaimed >= cooldownMillis;
        }
    }

    public long getLastClaimed(UUID uuid, String category) {
        Map<String, Long> map = playerCache.get(uuid);
        if (map != null) {
            return map.getOrDefault(category, 0L);
        }
        return 0L;
    }

    public void updateClaimTime(UUID uuid, String category, long timestamp) {
        Map<String, Long> map = playerCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        map.put(category, timestamp);
        
        // Save immediately async to ensure no data loss on crash
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT OR REPLACE INTO daily_rewards(uuid, category, last_claimed) VALUES(?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, category);
                ps.setLong(3, timestamp);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void resetClaimTime(UUID uuid, String category) {
        if (category == null) {
            playerCache.remove(uuid);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String sql = "DELETE FROM daily_rewards WHERE uuid = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        } else {
            Map<String, Long> map = playerCache.get(uuid);
            if (map != null) {
                map.remove(category);
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String sql = "DELETE FROM daily_rewards WHERE uuid = ? AND category = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, category);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    public void unloadPlayerCache(UUID uuid) {
        playerCache.remove(uuid);
    }
}
