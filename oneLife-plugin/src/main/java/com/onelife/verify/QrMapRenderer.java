package com.onelife.verify;

import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public class QrMapRenderer {
    private final org.bukkit.plugin.Plugin plugin;

    public QrMapRenderer(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack giveQrMap(Player p, String url) {
        ItemStack map = new ItemStack(Material.FILLED_MAP, 1);
        MapMeta meta = (MapMeta) map.getItemMeta();
        MapView view = Bukkit.createMap(p.getWorld());
        view.getRenderers().forEach(view::removeRenderer);
        view.addRenderer(new QRRenderer(url));
        meta.setMapView(view);
        map.setItemMeta(meta);
        p.getInventory().setItemInMainHand(map);
        return map;
    }

    static class QRRenderer extends MapRenderer {
        private final String url;
        private boolean drawn = false;

        QRRenderer(String url) {
            this.url = url;
        }

        @Override
        public void render(MapView view, MapCanvas canvas, Player player) {
            if (drawn)
                return;
            try {
                int size = 128;
                BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, size, size);
                // Increase content area ~15% by reducing margins
                int margin = 2; // was 4
                int qrSize = size - (margin * 2);
                BitMatrix m = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, qrSize, qrSize);
                g.setColor(Color.BLACK);
                for (int x = 0; x < m.getWidth(); x++)
                    for (int y = 0; y < m.getHeight(); y++)
                        if (m.get(x, y))
                            g.fillRect(x + margin, y + margin, 1, 1);
                g.dispose();
                canvas.drawImage(0, 0, MapPalette.resizeImage(img));
                drawn = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
