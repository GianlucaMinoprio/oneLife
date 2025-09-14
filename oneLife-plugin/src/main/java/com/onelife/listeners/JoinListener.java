package com.onelife.listeners;

import com.onelife.OneLifePlugin;
import com.onelife.backend.BackendClient;
import com.onelife.verify.QrMapRenderer;
import com.onelife.verify.SelfUrlBuilder;
import com.onelife.verify.VerifierPoller;
import com.onelife.util.NationalityCosmetics;
import com.onelife.util.SpawnManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class JoinListener implements Listener {
    private final OneLifePlugin plugin;
    private final BackendClient backend;
    private final QrMapRenderer qr;
    private final VerifierPoller poller;
    private final Restrictor restrictor;
    private final SpawnManager spawnManager;

    public JoinListener(OneLifePlugin plugin, BackendClient backend, QrMapRenderer qr, VerifierPoller poller,
            Restrictor restrictor) {
        this.plugin = plugin;
        this.backend = backend;
        this.qr = qr;
        this.poller = poller;
        this.restrictor = restrictor;
        this.spawnManager = plugin.getServer().getServicesManager() == null ? new SpawnManager(plugin)
                : new SpawnManager(plugin);
    }

    // Optionally deny dead players pre-join quickly
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        BackendClient.Status s = backend.getStatus(e.getUniqueId().toString());
        if (s.dead) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("§cOneLife: You are dead in this world."));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            BackendClient.Status status = backend.getStatus(uuid.toString());
            if (status.dead) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> {
                            plugin.getLogger().info("OneLife: Kicking dead player on join uuid=" + uuid);
                            p.kick(Component.text("§cOneLife: You are dead in this world."));
                        });
                return;
            }
            if (status.verified) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    NationalityCosmetics.apply(p, status.country);
                    restrictor.clear(p);
                    plugin.getLogger().info(
                            "OneLife: Player already verified on join uuid=" + uuid + " country=" + status.country);
                });
                return;
            }

            // Not verified → create session on backend & show QR
            BackendClient.Session sess = backend.createSession(uuid.toString());
            String qrUrl = (sess != null && sess.deepLink != null && !sess.deepLink.isEmpty())
                    ? sess.deepLink
                    : SelfUrlBuilder.makeQrUrl(
                            plugin.getConfig().getString("backendBase"),
                            plugin.getConfig().getString("configId"),
                            uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                restrictor.apply(p);
                org.bukkit.Location spawnAir = new org.bukkit.Location(p.getWorld(), 0.5,
                        p.getWorld().getHighestBlockYAt(0, 0) + 6.0, 0.5);
                p.teleport(spawnAir);
                p.setAllowFlight(true);
                p.setFlying(true);
                p.setGravity(false);
                p.sendTitle("OneLife", "Scan the QR to verify", 10, 80, 10);
                qr.giveQrMap(p, qrUrl);
                plugin.getLogger().info("OneLife: Showing QR to uuid=" + uuid + " link=" + qrUrl);
            });

            // Poll
            poller.start(uuid,
                    (country) -> {
                        NationalityCosmetics.apply(p, country);
                        restrictor.clear(p);
                        // Remove any QR maps
                        if (p.getInventory().getItemInMainHand() != null
                                && p.getInventory().getItemInMainHand().getType() == Material.FILLED_MAP)
                            p.getInventory().setItemInMainHand(null);
                        if (p.getInventory().getItemInOffHand() != null
                                && p.getInventory().getItemInOffHand().getType() == Material.FILLED_MAP)
                            p.getInventory().setItemInOffHand(null);
                        p.getInventory().remove(Material.FILLED_MAP);
                        org.bukkit.Location start = spawnManager.getOrCreateSpawn(p.getUniqueId(), p.getWorld());
                        p.teleport(start);
                        BossBar bar = Bukkit.createBossBar("Verified — good luck!", BarColor.GREEN, BarStyle.SOLID);
                        bar.setProgress(1.0);
                        bar.addPlayer(p);
                        bar.setVisible(true);
                        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            bar.removeAll();
                            bar.setVisible(false);
                        }, 100L);
                        plugin.getLogger().info("OneLife: Player verified uuid=" + uuid + " country=" + country);
                    },
                    () -> {
                        plugin.getLogger()
                                .info("OneLife: Player became dead during verification, kicking uuid=" + uuid);
                        p.kick(Component.text("§cOneLife: You are dead in this world."));
                    });
        });
    }
}
