package com.onelife.listeners;

import com.onelife.backend.BackendClient;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import net.kyori.adventure.text.Component;

public class DeathListener implements Listener {
    private final BackendClient backend;

    public DeathListener(BackendClient backend) {
        this.backend = backend;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        String uuid = e.getEntity().getUniqueId().toString();
        backend.reportDeath(uuid);
        e.getEntity().getServer().getLogger().info("OneLife: Player died, reporting and kicking uuid=" + uuid);
        e.getEntity().kick(Component.text("§cOneLife: You died. No second chance."));
    }
}
