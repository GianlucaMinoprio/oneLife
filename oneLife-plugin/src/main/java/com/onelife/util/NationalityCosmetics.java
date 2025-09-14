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

import java.util.ArrayList;
import java.util.List;

public final class NationalityCosmetics {
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

        // Give an unbreakable shield themed to nationality
        ItemStack shield = makeShieldFor(cc);
        // Prefer offhand if empty, otherwise add to inventory
        if (p.getInventory().getItemInOffHand() == null
                || p.getInventory().getItemInOffHand().getType() == Material.AIR)
            p.getInventory().setItemInOffHand(shield);
        else
            p.getInventory().addItem(shield);
    }

    private static ItemStack makeShieldFor(String cc) {
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta im = shield.getItemMeta();
        im.setUnbreakable(true);
        im.setDisplayName("§f" + cc.toUpperCase() + " Passport Shield");

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
        switch (cc.toUpperCase()) {
            case "AR":
                return DyeColor.LIGHT_BLUE;
            case "US":
                return DyeColor.BLUE;
            case "FR":
                return DyeColor.BLUE;
            case "BR":
                return DyeColor.GREEN;
            default:
                return DyeColor.GRAY;
        }
    }

    private static List<Pattern> patternsFor(String cc, DyeColor base) {
        List<Pattern> list = new ArrayList<>();
        String c = cc.toUpperCase();
        switch (c) {
            case "FR":
                // French flag: Blue-White-Red vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                break;
            case "US":
                // Simplified: blue corner + white stripes
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_DOWNLEFT));
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_DOWNRIGHT));
                list.add(new Pattern(DyeColor.BLUE, PatternType.SQUARE_TOP_LEFT));
                break;
            case "BR":
                // Simplified Brazil: yellow rhombus + blue center
                list.add(new Pattern(DyeColor.YELLOW, PatternType.RHOMBUS));
                list.add(new Pattern(DyeColor.BLUE, PatternType.CIRCLE));
                break;
            case "AR":
                // Argentina: light blue-white-light blue horizontal
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.YELLOW, PatternType.CIRCLE));
                break;
            case "ITA":
                // Italy: Green-White-Red vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                break;
            case "IE":
                // Ireland: Green-White-Orange vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.ORANGE, PatternType.STRIPE_RIGHT));
                break;
            case "BE":
                // Belgium: Black-Yellow-Red vertical
                list.add(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                break;
            case "RO":
                // Romania: Blue-Yellow-Red vertical
                list.add(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.BLUE, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                break;
            case "DE":
                // Germany: Black-Red-Yellow horizontal
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_BOTTOM));
                break;
            case "RU":
                // Russia: White-Blue-Red horizontal
                list.add(new Pattern(DyeColor.BLUE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM));
                break;
            case "NL":
                // Netherlands: Red-White-Blue horizontal
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.BLUE, PatternType.STRIPE_BOTTOM));
                break;
            case "PL":
                // Poland: White top, Red bottom
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM));
                break;
            case "UA":
                // Ukraine: Blue top, Yellow bottom
                list.add(new Pattern(DyeColor.BLUE, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_BOTTOM));
                break;
            case "AT":
                // Austria: Red-White-Red horizontal
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM));
                break;
            case "BG":
                // Bulgaria: White-Green-Red horizontal
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM));
                break;
            case "HU":
                // Hungary: Red-White-Green horizontal
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_BOTTOM));
                break;
            case "ES":
                // Spain simplified:
                // Red-Yhttps://docs.self.xyz/use-self/self-map-countries-listellow-Red
                // horizontal
                list.add(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_BOTTOM));
                break;
            case "PT":
                // Portugal simplified: Red-Green vertical split
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_LEFT));
                break;
            case "MX":
                // Mexico simplified: Green-White-Red vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                break;
            case "CA":
                // Canada simplified: Red-White-Red vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.RED, PatternType.STRIPE_RIGHT));
                list.add(new Pattern(DyeColor.RED, PatternType.RHOMBUS));
                break;
            case "JP":
                // Japan: White banner + Red circle
                list.add(new Pattern(DyeColor.WHITE, PatternType.BASE));
                list.add(new Pattern(DyeColor.RED, PatternType.CIRCLE));
                break;
            case "KR":
                // South Korea simplified: White with Red/Blue yin-yang
                list.add(new Pattern(DyeColor.WHITE, PatternType.BASE));
                list.add(new Pattern(DyeColor.RED, PatternType.HALF_HORIZONTAL));
                list.add(new Pattern(DyeColor.BLUE, PatternType.HALF_HORIZONTAL_BOTTOM));
                break;
            case "CN":
                // China simplified: Red background, Yellow circle
                list.add(new Pattern(DyeColor.RED, PatternType.BASE));
                list.add(new Pattern(DyeColor.YELLOW, PatternType.CIRCLE));
                break;
            case "IN":
                // India simplified: Orange-White-Green horizontal
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
                list.add(new Pattern(DyeColor.ORANGE, PatternType.STRIPE_TOP));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_BOTTOM));
                break;
            case "NG":
                // Nigeria: Green-White-Green vertical
                list.add(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_LEFT));
                list.add(new Pattern(DyeColor.GREEN, PatternType.STRIPE_RIGHT));
                break;
            case "SE":
                // Sweden: Blue with Yellow cross
                list.add(new Pattern(DyeColor.BLUE, PatternType.BASE));
                list.add(new Pattern(DyeColor.YELLOW, PatternType.CROSS));
                break;
            case "CH":
                // Switzerland: Red with White cross
                list.add(new Pattern(DyeColor.RED, PatternType.BASE));
                list.add(new Pattern(DyeColor.WHITE, PatternType.CROSS));
                break;
            case "GB":
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
