package com.eclipsesmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class EclipseSMP extends JavaPlugin {

    private DataStore data;
    private IdentityManager identity;
    private NamespacedKey headKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        data = new DataStore(this);
        data.load();
        identity = new IdentityManager(this);
        headKey = new NamespacedKey(this, "head_owner");

        var pm = getServer().getPluginManager();
        pm.registerEvents(new DeathListener(this), this);
        pm.registerEvents(new PluginHideListener(this), this);
        pm.registerEvents(identity, this);

        register("immortal", new ImmortalCommand(this));
        register("revive", new ReviveCommand(this));
        register("nickname", new NicknameCommand(this));
        register("skinset", new SkinSetCommand(this));
        register("deathsound", new DeathSoundCommand(this));
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
    }

    public void reloadAll() {
        reloadConfig();
        data.load();
    }

    public DataStore data() { return data; }
    public IdentityManager identity() { return identity; }
    public NamespacedKey headKey() { return headKey; }

    /** Mensagem de config.yml com prefixo e placeholders {chave} -> valor. */
    public Component msg(String key, String... kv) {
        String s = getConfig().getString("messages." + key, "&c[mensagem ausente: " + key + "]");
        for (int i = 0; i + 1 < kv.length; i += 2) s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(getConfig().getString("messages.prefix", "") + s);
    }

    public Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    public ItemStack createHead(Player p, String realName) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setPlayerProfile(p.getPlayerProfile());
        meta.displayName(Component.text("Cabeça de " + realName, NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Coloque no chão para reviver", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(headKey, PersistentDataType.STRING, p.getUniqueId().toString());
        item.setItemMeta(meta);
        return item;
    }

    /** Remove da lista de mortos e desbane. Retorna o nome, ou null se não estava na lista. */
    public String revive(UUID id) {
        String name = data.removeDead(id);
        if (name == null) return null;
        pardon(name);
        return name;
    }

    public void pardon(String name) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon " + name);
    }
}
