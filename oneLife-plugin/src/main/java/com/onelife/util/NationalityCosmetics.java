package com.onelife.util;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.Banner;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class NationalityCosmetics {
    private static final NamespacedKey SHIELD_GRANTED_KEY = new NamespacedKey(
            JavaPlugin.getProvidingPlugin(NationalityCosmetics.class),
            "passport_shield_granted");

    public static void apply(Player p, String cc) {
        if (cc == null)
            return;
        Scoreboard sb = p.getScoreboard();
        if (sb == null || sb == Bukkit.getScoreboardManager().getMainScoreboard()) {
            sb = Bukkit.getScoreboardManager().getNewScoreboard();
            p.setScoreboard(sb);
        }
        String teamName = "cc_" + cc;
        Team t = sb.getTeam(teamName);
        if (t == null)
            t = sb.registerNewTeam(teamName);
        t.setPrefix("[" + cc + "] ");
        t.color(NamedTextColor.WHITE);
        if (!t.hasEntry(p.getName()))
            t.addEntry(p.getName());

        // Remove legacy cap if present
        if (p.getInventory().getHelmet() != null && p.getInventory().getHelmet().getType() == Material.LEATHER_HELMET) {
            ItemMeta hm = p.getInventory().getHelmet().getItemMeta();
            if (hm != null && hm.hasDisplayName() && hm.getDisplayName().contains("Passport Cap"))
                p.getInventory().setHelmet(null);
        }

        // Give an unbreakable shield themed to nationality only once
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (!pdc.has(SHIELD_GRANTED_KEY, PersistentDataType.BYTE)) {
            ItemStack shield = makeShieldFor(cc);
            // Prefer offhand if empty, otherwise add to inventory
            if (p.getInventory().getItemInOffHand() == null
                    || p.getInventory().getItemInOffHand().getType() == Material.AIR)
                p.getInventory().setItemInOffHand(shield);
            else
                p.getInventory().addItem(shield);
            pdc.set(SHIELD_GRANTED_KEY, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private static ItemStack makeShieldFor(String cc) {
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta im = shield.getItemMeta();
        im.setUnbreakable(true);
        im.setDisplayName("§f" + cc.toUpperCase() + " Passport");

        // Apply banner patterns onto the shield
        BlockStateMeta bsm = (BlockStateMeta) im;
        Banner banner = (Banner) bsm.getBlockState();
        DyeColor base = dyeFor(cc);
        banner.setBaseColor(base);
        List<Pattern> patterns = patternsFor(cc, base);
        banner.setPatterns(patterns);
        banner.update();
        bsm.setBlockState(banner);
        shield.setItemMeta(bsm);
        return shield;
    }

    private static DyeColor dyeFor(String cc) {
        String c = cc == null ? "" : cc.trim().toUpperCase();
        switch (c) {
            case "AR":
            case "ARG":
                return DyeColor.LIGHT_BLUE;
            case "US":
            case "USA":
                return DyeColor.BLUE;
            case "FR":
            case "FRA":
                return DyeColor.BLUE;
            case "BR":
            case "BRA":
                return DyeColor.GREEN;
            case "IT":
            case "ITA":
                return DyeColor.GREEN;
            case "IE":
            case "IRL":
                return DyeColor.GREEN;
            case "BE":
            case "BEL":
                return DyeColor.BLACK;
            case "RO":
            case "ROU":
                return DyeColor.BLUE;
            case "DE":
            case "DEU":
                return DyeColor.BLACK;
            case "RU":
            case "RUS":
                return DyeColor.BLUE;
            case "NL":
            case "NLD":
                return DyeColor.RED;
            case "PL":
            case "POL":
                return DyeColor.WHITE;
            case "UA":
            case "UKR":
                return DyeColor.BLUE;
            case "AT":
            case "AUT":
                return DyeColor.RED;
            case "BG":
            case "BGR":
                return DyeColor.WHITE;
            case "HU":
            case "HUN":
                return DyeColor.RED;
            case "ES":
            case "ESP":
                return DyeColor.RED;
            case "PT":
            case "PRT":
                return DyeColor.RED;
            case "MX":
            case "MEX":
                return DyeColor.GREEN;
            case "CA":
            case "CAN":
                return DyeColor.RED;
            case "JP":
            case "JPN":
                return DyeColor.WHITE;
            case "KR":
            case "KOR":
                return DyeColor.WHITE;
            case "CN":
            case "CHN":
                return DyeColor.RED;
            case "IN":
            case "IND":
                return DyeColor.ORANGE;
            case "NG":
            case "NGA":
                return DyeColor.GREEN;
            case "SE":
            case "SWE":
                return DyeColor.BLUE;
            case "CH":
            case "CHE":
                return DyeColor.RED;
            case "GB":
            case "GBR":
                return DyeColor.BLUE;
            default:
                return DyeColor.GRAY;
        }
    }

    private static List<Pattern> patternsFor(String cc, DyeColor base) {
        List<Pattern> list = new ArrayList<>();
        String c = cc == null ? "" : cc.trim().toUpperCase();
        switch (c) {
            case "FR":
            case "FRA":
                // French flag: Blue-White-Red vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                break;
            case "US":
            case "USA":
                // Simplified: blue corner + white stripes
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_DOWNLEFT));
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_DOWNRIGHT));
                list.add(new Pattern(DyeColor.BLUE, PatternType.SQUARE_TOP_LEFT));
                break;
            case "BR":
            case "BRA":
                // Simplified Brazil: yellow rhombus + blue center
                list.add(new Pattern(DyeColor.YELLOW, PatternType.RHOMBUS));
                list.add(new Pattern(DyeColor.BLUE, PatternType.CIRCLE));
                break;
            case "AR":
            case "ARG":
                // Argentina: light blue-white-light blue horizontal
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.YELLOW, PatternType.CIRCLE));
                break;
            case "IT":
            case "ITA":
                // Italy: Green-White-Red vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                break;
            case "IE":
            case "IRL":
                // Ireland: Green-White-Orange vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.ORANGE, PatternType.STRIPE_RIGHT));
                break;
            case "BE":
            case "BEL":
                // Belgium: Black-Yellow-Red vertical
                list.add(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                break;
            case "RO":
            case "ROU":
                // Romania: Blue-Yellow-Red vertical
                list.add(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.BLUE, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                break;
            case "DE":
            case "DEU":
                // Germany: Black-Red-Yellow horizontal
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_BOTTOM));
                break;
            case "RU":
            case "RUS":
                // Russia: White-Blue-Red horizontal
                list.add(new Pattern(DyeColor.BLUE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM));
                break;
            case "NL":
            case "NLD":
                // Netherlands: Red-White-Blue horizontal
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.BLUE, PatternType.STRIPE_BOTTOM));
                break;
            case "PL":
            case "POL":
                // Poland: White top, Red bottom
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM));
                break;
            case "UA":
            case "UKR":
                // Ukraine: Blue top, Yellow bottom
                list.add(new Pattern(DyeColor.BLUE, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_BOTTOM));
                break;
            case "AT":
            case "AUT":
                // Austria: Red-White-Red horizontal
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM));
                break;
            case "BG":
            case "BGR":
                // Bulgaria: White-Green-Red horizontal
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM));
                break;
            case "HU":
            case "HUN":
                // Hungary: Red-White-Green horizontal
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_BOTTOM));
                break;
            case "ES":
            case "ESP":
                // Spain simplified:
                // Red-Yhttps://docs.self.xyz/use-self/self-map-countries-listellow-Red
                // horizontal
                list.add(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM));
                break;
            case "PT":
            case "PRT":
                // Portugal simplified: Red-Green vertical split
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_LEFT));
                break;
            case "MX":
            case "MEX":
                // Mexico simplified: Green-White-Red vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                break;
            case "CA":
            case "CAN":
                // Canada simplified: Red-White-Red vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                list.add(new Pattern(DyeColor.RED, PatternType.RHOMBUS));
                break;
            case "JP":
            case "JPN":
                // Japan: White banner + Red circle
                list.add(new Pattern(DyeColor.WHITE, PatternType.BASE));
                list.add(new Pattern(DyeColor.RED, PatternType.CIRCLE));
                break;
            case "KR":
            case "KOR":
                // South Korea simplified: White with Red/Blue yin-yang
                list.add(new Pattern(DyeColor.WHITE, PatternType.BASE));
                list.add(new Pattern(DyeColor.RED, PatternType.HALF_HORIZONTAL));
                list.add(new Pattern(DyeColor.BLUE, PatternType.HALF_HORIZONTAL_BOTTOM));
                break;
            case "CN":
            case "CHN":
                // China simplified: Red background, Yellow circle
                list.add(new Pattern(DyeColor.RED, PatternType.BASE));
                list.add(new Pattern(DyeColor.YELLOW, PatternType.CIRCLE));
                break;
            case "IN":
            case "IND":
                // India simplified: Orange-White-Green horizontal
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.ORANGE, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_BOTTOM));
                break;
            case "NG":
            case "NGA":
                // Nigeria: Green-White-Green vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_RIGHT));
                break;
            case "SE":
            case "SWE":
                // Sweden: Blue with Yellow cross
                list.add(new Pattern(DyeColor.BLUE, PatternType.BASE));
                list.add(new Pattern(DyeColor.YELLOW, PatternType.CROSS));
                break;
            case "CH":
            case "CHE":
                // Switzerland: Red with White cross
                list.add(new Pattern(DyeColor.RED, PatternType.BASE));
                list.add(new Pattern(DyeColor.WHITE, PatternType.CROSS));
                break;
            case "GB":
            case "GBR":
                // United Kingdom simplified: Blue + Red cross with White edges
                list.add(new Pattern(DyeColor.BLUE, PatternType.BASE));
                list.add(new Pattern(DyeColor.WHITE, PatternType.CROSS));
                list.add(new Pattern(DyeColor.RED, PatternType.STRAIGHT_CROSS));
                break;
            default:
                // Plain dyed shield
                break;
        }
        return list;
    }
}
