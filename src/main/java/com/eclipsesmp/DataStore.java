package com.eclipsesmp;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataStore {

    private final EclipseSMP plugin;
    private File file;
    private YamlConfiguration yml = new YamlConfiguration();
    private final Set<UUID> immortals = new LinkedHashSet<>();
    private final Map<UUID, String> dead = new LinkedHashMap<>();
    private final Map<UUID, Integer> protection = new HashMap<>();

    public DataStore(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), "data.yml");
        yml = YamlConfiguration.loadConfiguration(file);
        immortals.clear();
        dead.clear();
        protection.clear();
        ConfigurationSection prot = yml.getConfigurationSection("protection");
        if (prot != null) {
            for (String k : prot.getKeys(false)) {
                try { protection.put(UUID.fromString(k), prot.getInt(k)); } catch (IllegalArgumentException ignored) { }
            }
        }
        for (String s : yml.getStringList("immortal")) {
            try { immortals.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection sec = yml.getConfigurationSection("dead");
        if (sec != null) {
            for (String k : sec.getKeys(false)) {
                try { dead.put(UUID.fromString(k), sec.getString(k, k)); } catch (IllegalArgumentException ignored) { }
            }
        }
    }

    public synchronized void save() {
        List<String> list = new ArrayList<>();
        for (UUID id : immortals) list.add(id.toString());
        yml.set("immortal", list);
        yml.set("dead", null);
        for (Map.Entry<UUID, String> e : dead.entrySet()) yml.set("dead." + e.getKey(), e.getValue());
        yml.set("protection", null);
        for (Map.Entry<UUID, Integer> e : protection.entrySet())
            if (e.getValue() > 0) yml.set("protection." + e.getKey(), e.getValue());
        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Falha ao salvar data.yml: " + ex.getMessage());
        }
    }

    public YamlConfiguration yml() { return yml; }

    // ---- imortais ----
    public boolean isImmortal(UUID id) { return immortals.contains(id); }
    public Set<UUID> immortals() { return immortals; }

    public void setImmortal(UUID id, boolean value) {
        if (value) immortals.add(id); else immortals.remove(id);
        save();
    }

    // ---- proteção (segundos restantes) ----
    public int getProtection(UUID id) { return protection.getOrDefault(id, 0); }

    /** Só altera a memória; chame save() para persistir. */
    public void setProtection(UUID id, int seconds) {
        if (seconds <= 0) protection.remove(id); else protection.put(id, seconds);
    }

    // ---- mortos ----
    public void markDead(UUID id, String name) {
        dead.put(id, name);
        save();
    }

    public String removeDead(UUID id) {
        String name = dead.remove(id);
        if (name != null) save();
        return name;
    }

    public UUID deadByName(String name) {
        for (Map.Entry<UUID, String> e : dead.entrySet())
            if (e.getValue().equalsIgnoreCase(name)) return e.getKey();
        return null;
    }

    public Collection<String> deadNames() { return dead.values(); }
}
