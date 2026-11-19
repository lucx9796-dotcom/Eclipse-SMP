package com.eclipsesmp;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/** Guarda e reaplica apelido/skin (via PlayerProfile do Paper). */
public class IdentityManager implements Listener {

    private final EclipseSMP plugin;

    public IdentityManager(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    private YamlConfiguration yml() { return plugin.data().yml(); }
    private String base(UUID id) { return "identity." + id + "."; }

    /** Nome real do jogador (antes de qualquer apelido). */
    public String realName(Player p) {
        return yml().getString(base(p.getUniqueId()) + "orig-name", p.getName());
    }

    private void capture(Player p, boolean force) {
        String b = base(p.getUniqueId());
        if (!force && yml().contains(b + "orig-name")) return;
        yml().set(b + "orig-name", p.getName());
        yml().set(b + "orig-value", null);
        yml().set(b + "orig-sig", null);
        for (ProfileProperty pr : p.getPlayerProfile().getProperties()) {
            if (pr.getName().equals("textures")) {
                yml().set(b + "orig-value", pr.getValue());
                yml().set(b + "orig-sig", pr.getSignature());
                break;
            }
        }
    }

    public void setNick(Player p, String nick) {
        capture(p, false);
        yml().set(base(p.getUniqueId()) + "nick", nick); // null = reset
        plugin.data().save();
        apply(p);
    }

    public void setSkin(Player p, String skinName, ProfileProperty textures) {
        capture(p, false);
        String b = base(p.getUniqueId());
        if (textures == null) {
            yml().set(b + "skin-name", null);
            yml().set(b + "skin-value", null);
            yml().set(b + "skin-sig", null);
        } else {
            yml().set(b + "skin-name", skinName);
            yml().set(b + "skin-value", textures.getValue());
            yml().set(b + "skin-sig", textures.getSignature());
        }
        plugin.data().save();
        apply(p);
    }

    public void apply(Player p) {
        String b = base(p.getUniqueId());
        if (!yml().contains(b + "orig-name")) return;

        String name = yml().getString(b + "nick", yml().getString(b + "orig-name", p.getName()));
        String value = yml().getString(b + "skin-value", yml().getString(b + "orig-value"));
        String sig = yml().getString(b + "skin-sig", yml().getString(b + "orig-sig"));

        PlayerProfile profile = Bukkit.createProfile(p.getUniqueId(), name);
        if (value != null) profile.setProperty(new ProfileProperty("textures", value, sig));
        p.setPlayerProfile(profile);

        Component display = Component.text(name);
        p.displayName(display);
        p.playerListName(display);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!yml().contains("identity." + p.getUniqueId())) return;
        // No login o perfil ainda é o original: atualiza a "base" e reaplica
        capture(p, true);
        plugin.data().save();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) apply(p);
        }, 5L);
    }
}
