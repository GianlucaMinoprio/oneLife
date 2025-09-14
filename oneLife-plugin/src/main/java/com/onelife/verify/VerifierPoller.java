package com.onelife.verify;

import com.onelife.backend.BackendClient;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class VerifierPoller {
    private final org.bukkit.plugin.Plugin plugin;
    private final BackendClient backend;
    private final int pollSeconds;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    public VerifierPoller(org.bukkit.plugin.Plugin plugin, BackendClient backend, int pollSeconds) {
        this.plugin = plugin;
        this.backend = backend;
        this.pollSeconds = Math.max(pollSeconds, 2);
    }

    public void start(UUID uuid, Consumer<String> onVerified, Runnable onDead) {
        cancel(uuid);
        BukkitTask t = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            BackendClient.Status s = backend.getStatus(uuid.toString());
            if (s.dead) {
                cancel(uuid);
                Bukkit.getScheduler().runTask(plugin, onDead);
                return;
            }
            if (s.verified) {
                cancel(uuid);
                Bukkit.getScheduler().runTask(plugin, () -> onVerified.accept(s.country));
            }
        }, 40L, pollSeconds * 20L);
        tasks.put(uuid, t);
    }

    public void cancel(UUID uuid) {
        BukkitTask t = tasks.remove(uuid);
        if (t != null)
            t.cancel();
    }
}
