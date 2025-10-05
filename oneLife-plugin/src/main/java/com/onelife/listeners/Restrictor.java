package com.onelife.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Restrictor implements Listener {
    private final org.bukkit.plugin.Plugin plugin;
    private final Map<UUID, BukkitTask> lookTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    // Store inventories for existing players during verification
    private final Map<UUID, SavedInventory> savedInventories = new ConcurrentHashMap<>();

    private static class SavedInventory {
        ItemStack[] contents;
        ItemStack[] armorContents;
        ItemStack offHand;
        int heldItemSlot;

        SavedInventory(Player p) {
            this.contents = p.getInventory().getContents().clone();
            this.armorContents = p.getInventory().getArmorContents().clone();
            this.offHand = p.getInventory().getItemInOffHand();
            this.heldItemSlot = p.getInventory().getHeldItemSlot();
        }

        void restore(Player p) {
            p.getInventory().setContents(contents);
            p.getInventory().setArmorContents(armorContents);
            p.getInventory().setItemInOffHand(offHand);
            p.getInventory().setHeldItemSlot(heldItemSlot);
        }
    }

    public Restrictor(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void apply(Player p) {
        // For existing players, save their inventory before restricting
        // This prevents data loss if backend database gets reset
        if (p.hasPlayedBefore()) {
            savedInventories.put(p.getUniqueId(), new SavedInventory(p));
            plugin.getLogger().info("OneLife: Saved inventory for existing player uuid=" + p.getUniqueId());
        }

        // Start from a clean slate
        p.getActivePotionEffects().forEach(pe -> p.removePotionEffect(pe.getType()));
        p.getInventory().clear();
        p.getInventory().setItemInOffHand(null);
        p.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);

        p.setGameMode(GameMode.ADVENTURE);
        p.setInvulnerable(true);
        p.setCollidable(false);
        p.setCanPickupItems(false);
        p.setSilent(true);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setWalkSpeed(0f);
        p.setFlySpeed(0f);
        p.setGravity(false);
        p.setFallDistance(0f);
        // Ensure no freeze overlay
        p.setFreezeTicks(0);

        // Personal visuals
        p.setPlayerTime(6000L, false);
        p.setPlayerWeather(WeatherType.CLEAR);

        BossBar bar = bossBars.computeIfAbsent(p.getUniqueId(),
                id -> Bukkit.createBossBar("Scan QR code & proof life", BarColor.BLUE, BarStyle.SOLID));
        bar.setProgress(1.0);
        bar.addPlayer(p);
        bar.setVisible(true);

        Bukkit.getOnlinePlayers().forEach(other -> {
            if (!other.getUniqueId().equals(p.getUniqueId()))
                other.hidePlayer(plugin, p);
        });

        cancelTask(p.getUniqueId());
        BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isRestricted(p))
                return;
            int mapSlot = p.getInventory().first(Material.FILLED_MAP);
            if (mapSlot >= 0) {
                if (mapSlot != 0) {
                    var mapItem = p.getInventory().getItem(mapSlot);
                    p.getInventory().setItem(0, mapItem);
                    p.getInventory().setItem(mapSlot, null);
                }
                // Ensure map is in main hand center and nothing in offhand to avoid side
                // overlay
                if (p.getInventory().getHeldItemSlot() != 0)
                    p.getInventory().setHeldItemSlot(0);
                if (p.getInventory().getItemInOffHand() != null
                        && p.getInventory().getItemInOffHand().getType() != Material.AIR)
                    p.getInventory().setItemInOffHand(null);
            }
        }, 10L, 10L);
        lookTasks.put(p.getUniqueId(), t);
    }

    public void clear(Player p) {
        // Restore saved inventory for existing players
        SavedInventory saved = savedInventories.remove(p.getUniqueId());
        if (saved != null) {
            saved.restore(p);
            plugin.getLogger().info("OneLife: Restored inventory for existing player uuid=" + p.getUniqueId());
        }

        p.setGameMode(GameMode.SURVIVAL);
        p.setInvulnerable(false);
        p.setCollidable(true);
        p.setCanPickupItems(true);
        p.setSilent(false);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setWalkSpeed(0.2f);
        p.setFlySpeed(0.1f);
        p.setGravity(true);
        // Unfreeze not needed since we don't use freeze ticks
        p.setFreezeTicks(0);

        // Reset personal visuals
        p.resetPlayerTime();
        p.resetPlayerWeather();

        Bukkit.getOnlinePlayers().forEach(other -> {
            if (!other.getUniqueId().equals(p.getUniqueId()))
                other.showPlayer(plugin, p);
        });

        cancelTask(p.getUniqueId());
        BossBar bar = bossBars.remove(p.getUniqueId());
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    private void cancelTask(UUID id) {
        BukkitTask t = lookTasks.remove(id);
        if (t != null)
            t.cancel();
    }

    private boolean isRestricted(Player p) {
        return p.getGameMode() == GameMode.ADVENTURE && p.isInvulnerable();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent e) {
        if (isRestricted(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent e) {
        if (isRestricted(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        if (isRestricted(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p && isRestricted(p))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && isRestricted(p))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p && isRestricted(p))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (isRestricted(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (isRestricted(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHeldChange(PlayerItemHeldEvent e) {
        if (isRestricted(e.getPlayer()) && e.getNewSlot() != 0) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> e.getPlayer().getInventory().setHeldItemSlot(0));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && isRestricted(p))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if (isRestricted(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCmd(PlayerCommandPreprocessEvent e) {
        if (isRestricted(e.getPlayer()) && !e.getMessage().toLowerCase().startsWith("/verify")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cOneLife: Verify first. Use /verify to show the QR again.");
        }
    }
}
