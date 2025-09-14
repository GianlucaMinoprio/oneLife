package com.onelife;

import com.onelife.backend.BackendClient;
import com.onelife.listeners.DeathListener;
import com.onelife.listeners.JoinListener;
import com.onelife.listeners.Restrictor;
import com.onelife.verify.QrMapRenderer;
import com.onelife.verify.VerifierPoller;
import com.onelife.util.SpawnManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OneLifePlugin extends JavaPlugin {
    private BackendClient backend;
    private QrMapRenderer qr;
    private VerifierPoller poller;
    private Restrictor restrictor;
    private SpawnManager spawnManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String base = getConfig().getString("backendBase");
        backend = new BackendClient(base, getLogger());
        qr = new QrMapRenderer(this);
        poller = new VerifierPoller(this, backend, getConfig().getInt("pollSeconds", 3));
        restrictor = new Restrictor(this);
        spawnManager = new SpawnManager(this);

        getLogger().info("OneLife: Starting with backendBase=" + base);

        getServer().getPluginManager().registerEvents(
                new JoinListener(this, backend, qr, poller, restrictor),
                this);
        getServer().getPluginManager().registerEvents(
                new DeathListener(backend),
                this);

        getCommand("verify").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player p)) {
                sender.sendMessage("Only players can run this.");
                return true;
            }
            getLogger().info("OneLife: /verify invoked by uuid=" + p.getUniqueId());
            BackendClient.Session sess = backend.createSession(p.getUniqueId().toString());
            String qrUrl = (sess != null && sess.deepLink != null && !sess.deepLink.isEmpty())
                    ? sess.deepLink
                    : com.onelife.verify.SelfUrlBuilder.makeQrUrl(
                            getConfig().getString("backendBase"),
                            getConfig().getString("configId"),
                            p.getUniqueId());
            qr.giveQrMap(p, qrUrl);
            return true;
        });

        getLogger().info("OneLife enabled.");
    }
}
