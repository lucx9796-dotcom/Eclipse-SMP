package com.eclipsesmp;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DeathSoundCommand implements TabExecutor {

    private static final List<String> SUBS =
            List.of("on", "off", "set", "volume", "pitch", "global", "test", "reset", "status");
    private static final List<String> SOUNDS = List.of(
            "entity.wither.spawn", "entity.wither.death", "entity.ender_dragon.death",
            "entity.lightning_bolt.thunder", "entity.warden.sonic_boom", "entity.elder_guardian.curse",
            "entity.generic.explode", "block.bell.use", "ambient.cave");

    private final EclipseSMP plugin;

    public DeathSoundCommand(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        FileConfiguration cfg = plugin.getConfig();
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();

        switch (sub) {
            case "status" -> sendStatus(sender);
            case "on", "off" -> {
                cfg.set("death-sound.enabled", sub.equals("on"));
                plugin.saveConfig();
                sender.sendMessage(plugin.msg(sub.equals("on") ? "sound-on" : "sound-off"));
            }
            case "set" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("usage", "usage", "/deathsound set <som>"));
                    return true;
                }
                String sound = normalize(args[1]);
                if (NamespacedKey.fromString(sound.contains(":") ? sound : "minecraft:" + sound) == null) {
                    sender.sendMessage(plugin.msg("sound-invalid"));
                    return true;
                }
                cfg.set("death-sound.sound", sound);
                plugin.saveConfig();
                sender.sendMessage(plugin.msg("sound-set", "sound", sound));
            }
            case "volume", "pitch" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("usage", "usage", "/deathsound " + sub + " <número>"));
                    return true;
                }
                try {
                    double v = Double.parseDouble(args[1].replace(',', '.'));
                    if (v <= 0 || v > 100) throw new NumberFormatException();
                    cfg.set("death-sound." + sub, v);
                    plugin.saveConfig();
                    sender.sendMessage(plugin.msg("sound-value", "key", sub, "value", String.valueOf(v)));
                } catch (NumberFormatException ex) {
                    sender.sendMessage(plugin.msg("usage", "usage", "/deathsound " + sub + " <número maior que 0>"));
                }
            }
            case "global" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("usage", "usage", "/deathsound global <true|false>"));
                    return true;
                }
                boolean v = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on");
                cfg.set("death-sound.global", v);
                plugin.saveConfig();
                sender.sendMessage(plugin.msg("sound-value", "key", "global", "value", String.valueOf(v)));
            }
            case "reset" -> {
                cfg.set("death-sound.enabled", true);
                cfg.set("death-sound.sound", "entity.wither.spawn");
                cfg.set("death-sound.volume", 4.0);
                cfg.set("death-sound.pitch", 1.0);
                cfg.set("death-sound.global", false);
                plugin.saveConfig();
                sendStatus(sender);
            }
            case "test" -> {
                if (sender instanceof Player p) {
                    Location loc = p.getLocation();
                    new DeathListener(plugin).playDeathSound(loc);
                } else {
                    sender.sendMessage(plugin.msg("only-online"));
                }
            }
            default -> sender.sendMessage(plugin.msg("usage",
                    "usage", "/deathsound <on|off|set|volume|pitch|global|test|reset|status>"));
        }
        return true;
    }

    /** Aceita ENTITY_WITHER_SPAWN ou entity.wither.spawn */
    private String normalize(String in) {
        String s = in.toLowerCase();
        if (s.contains("_") && !s.contains(".") && !s.contains(":")) s = s.replace('_', '.');
        return s;
    }

    private void sendStatus(CommandSender sender) {
        FileConfiguration c = plugin.getConfig();
        sender.sendMessage(plugin.msg("sound-status",
                "sound", c.getString("death-sound.sound", "entity.wither.spawn"),
                "enabled", String.valueOf(c.getBoolean("death-sound.enabled", true)),
                "volume", String.valueOf(c.getDouble("death-sound.volume", 4.0)),
                "pitch", String.valueOf(c.getDouble("death-sound.pitch", 1.0)),
                "global", String.valueOf(c.getBoolean("death-sound.global", false))));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2) {
            String p = args[1].toLowerCase();
            switch (args[0].toLowerCase()) {
                case "set" -> { for (String s : SOUNDS) if (s.startsWith(p)) out.add(s); }
                case "global" -> { for (String s : List.of("true", "false")) if (s.startsWith(p)) out.add(s); }
                default -> { }
            }
        }
        return out;
    }
}
