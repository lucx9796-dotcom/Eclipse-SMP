package com.eclipsesmp;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Proteção inicial: o tempo só desconta com o jogador ONLINE.
 * Concedida no primeiro login e ao reviver pela cabeça.
 */
public class ProtectionManager implements Listener {

    private final EclipseSMP plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private BukkitTask task;

    public ProtectionManager(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        for (Map.Entry<UUID, BossBar> e : bars.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) p.hideBossBar(e.getValue());
        }
        bars.clear();
        plugin.data().save();
    }

    // ---------- API ----------
    private boolean enabled() {
        return plugin.getConfig().getBoolean("protection.enabled", true);
    }

    private int total() {
        return Math.max(1, plugin.getConfig().getInt("protection.minutes", 15)) * 60;
    }

    public int minutes() {
        return total() / 60;
    }

    public boolean isProtected(UUID id) {
        return enabled() && plugin.data().getProtection(id) > 0;
    }

    public int remaining(UUID id) {
        return plugin.data().getProtection(id);
    }

    public void grant(UUID id) {
        if (!enabled()) return;
        plugin.data().setProtection(id, total());
        plugin.data().save();
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
            showBar(p);
            p.sendMessage(plugin.msg("protection-start", "time", fmt(total()), "minutes", String.valueOf(minutes())));
        }
    }

    public void remove(UUID id) {
        plugin.data().setProtection(id, 0);
        plugin.data().save();
        hideBar(id);
    }

    public static String fmt(int secs) {
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    // ---------- contagem ----------
    private void tick() {
        if (!enabled()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            int left = plugin.data().getProtection(id);
            if (left <= 0) continue;
            left--;
            plugin.data().setProtection(id, left);
            if (left <= 0) {
                hideBar(id);
                plugin.data().save();
                p.sendMessage(plugin.msg("protection-end"));
                p.playSound(p.getLocation(), "block.bell.use", SoundCategory.MASTER, 1.0f, 0.8f);
            } else {
                updateBar(p, left);
            }
        }
    }

    // ---------- bossbar ----------
    private void showBar(Player p) {
        if (!plugin.getConfig().getBoolean("protection.bossbar", true)) return;
        BossBar bar = bars.computeIfAbsent(p.getUniqueId(), k ->
                BossBar.bossBar(net.kyori.adventure.text.Component.empty(), 1f,
                        BossBar.Color.GREEN, BossBar.Overlay.PROGRESS));
        updateBar(p, plugin.data().getProtection(p.getUniqueId()));
        p.showBossBar(bar);
    }

    private void updateBar(Player p, int left) {
        BossBar bar = bars.get(p.getUniqueId());
        if (bar == null) return;
        String text = plugin.getConfig().getString("messages.protection-bar", "&aProteção &8» &e{time}")
                .replace("{time}", fmt(left));
        bar.name(plugin.legacy(text));
        bar.progress(Math.max(0f, Math.min(1f, (float) left / total())));
        bar.color(left <= 60 ? BossBar.Color.RED : left <= 180 ? BossBar.Color.YELLOW : BossBar.Color.GREEN);
    }

    private void hideBar(UUID id) {
        BossBar bar = bars.remove(id);
        Player p = Bukkit.getPlayer(id);
        if (bar != null && p != null) p.hideBossBar(bar);
    }

    // ---------- eventos ----------
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        FileConfiguration cfg = plugin.getConfig();
        boolean first = !p.hasPlayedBefore();

        if (first && enabled() && cfg.getBoolean("protection.on-first-join", true)) {
            plugin.data().setProtection(id, total());
            plugin.data().save();
        }
        if (isProtected(id)) {
            // depois da mensagem de boas-vindas
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline() || !isProtected(id)) return;
                showBar(p);
                if (first) {
                    p.sendMessage(plugin.msg("protection-start",
                            "time", fmt(remaining(id)), "minutes", String.valueOf(minutes())));
                } else {
                    p.sendMessage(plugin.msg("protection-remaining", "time", fmt(remaining(id))));
                }
            }, 30L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        bars.remove(e.getPlayer().getUniqueId());
        plugin.data().save();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!isProtected(p.getUniqueId())) return;
        // /kill dos admins ainda funciona
        if (e.getCause() == EntityDamageEvent.DamageCause.KILL
                || e.getCause() == EntityDamageEvent.DamageCause.SUICIDE) return;

        e.setCancelled(true);
        switch (e.getCause()) {
            case FIRE, FIRE_TICK, LAVA -> p.setFireTicks(0);
            case VOID -> {
                p.setFallDistance(0);
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!plugin.getConfig().getBoolean("protection.block-pvp", true)) return;
        if (!(e.getEntity() instanceof Player)) return;

        Player attacker = null;
        if (e.getDamager() instanceof Player a1) {
            attacker = a1;
        } else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player a2) {
            attacker = a2;
        }
        if (attacker == null || !isProtected(attacker.getUniqueId())) return;

        e.setCancelled(true);
        attacker.sendActionBar(plugin.legacy(plugin.getConfig()
                .getString("messages.protection-pvp", "&cVocê está protegido e não pode ferir jogadores.")));
    }
}
