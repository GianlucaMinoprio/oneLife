package com.onelife.util;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

public class SpawnManager {
    private final org.bukkit.plugin.Plugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Random random = new Random();

    public SpawnManager(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists())
            plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "spawns.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public Location getOrCreateSpawn(UUID playerId, World overworld) {
        String key = "players." + playerId;
        if (data.contains(key + ".world")) {
            String worldName = data.getString(key + ".world");
            World w = Bukkit.getWorld(worldName);
            double x = data.getDouble(key + ".x");
            double y = data.getDouble(key + ".y");
            double z = data.getDouble(key + ".z");
            float yaw = (float) data.getDouble(key + ".yaw", 0.0);
            float pitch = (float) data.getDouble(key + ".pitch", 0.0);
            if (w != null)
                return new Location(w, x, y, z, yaw, pitch);
        }

        Location safe = findSafeDeterministicSpawn(overworld, playerId);
        saveSpawn(playerId, safe);
        return safe;
    }

    private void saveSpawn(UUID playerId, Location loc) {
        String key = "players." + playerId;
        data.set(key + ".world", loc.getWorld().getName());
        data.set(key + ".x", loc.getX());
        data.set(key + ".y", loc.getY());
        data.set(key + ".z", loc.getZ());
        data.set(key + ".yaw", loc.getYaw());
        data.set(key + ".pitch", loc.getPitch());
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("OneLife: Failed to save spawn for " + playerId + ": " + e.getMessage());
        }
    }

    private Location findSafeDeterministicSpawn(World world, UUID playerId) {
        // Ensure overworld
        if (world.getEnvironment() != World.Environment.NORMAL) {
            world = Bukkit.getWorlds().stream()
                    .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                    .findFirst()
                    .orElse(world);
        }

        // Simple deterministic ring placement to avoid heavy loops
        long h = playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits();
        double angle = ((h >>> 16) & 0xFFFF) / 65535.0 * Math.PI * 2;
        int radius = 1000 + (int) (((h >>> 32) & 0xFFFF) / 65535.0 * 2000);
        Location ws = world.getSpawnLocation();
        int x = ws.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
        int z = ws.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
        int y = world.getHighestBlockYAt(x, z);

        // If unsuitable ground, nudge halfway to world spawn once
        Material below = world.getBlockAt(x, y - 1, z).getType();
        if (!isGoodGround(below)) {
            x = (x + ws.getBlockX()) / 2;
            z = (z + ws.getBlockZ()) / 2;
            y = world.getHighestBlockYAt(x, z);
        }

        return new Location(world, x + 0.5, y + 0.1, z + 0.5);
    }

    private boolean isGoodGround(Material m) {
        if (m == Material.WATER || m == Material.KELP || m == Material.SEAGRASS || m == Material.LAVA)
            return false;
        if (m == Material.MAGMA_BLOCK || m == Material.CACTUS)
            return false;
        // Accept common natural blocks
        return m.isSolid();
    }
}
