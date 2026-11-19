package com.eclipsesmp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class DeathListener implements Listener {

    private final EclipseSMP plugin;

    public DeathListener(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    // ===== Imortalidade =====
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!plugin.data().isImmortal(p.getUniqueId())) return;

        double max = p.getMaxHealth();
        double min = Math.min(Math.max(0.1, plugin.getConfig().getDouble("immortal.min-health", 1.0)), max);
        double health = p.getHealth();

        // Dano que não derruba abaixo do mínimo passa normalmente
        if (health - e.getFinalDamage() >= min) return;

        e.setCancelled(true);

        if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
            p.setFallDistance(0);
            p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        if (health > min) {
            p.setHealth(min);
            p.playHurtAnimation(0f);
        }
    }

    // ===== Morte: vidas + cabeça + ban + som =====
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        UUID id = p.getUniqueId();
        if (plugin.data().isImmortal(id)) return;

        FileConfiguration cfg = plugin.getConfig();
        String real = plugin.identity().realName(p);
        Location loc = p.getLocation();

        playDeathSound(loc);

        boolean exempt = p.hasPermission("eclipse.deathban.exempt");
        boolean ban = cfg.getBoolean("death.ban", true);

        // Sistema de vidas: só perde a última vida => cabeça + ban
        if (!exempt && ban && plugin.livesEnabled()) {
            int max = plugin.maxLives();
            int left = Math.min(plugin.data().getLives(id, max), max) - 1;
            if (left > 0) {
                plugin.data().setLives(id, left);
                p.sendMessage(plugin.msg("life-lost", "lives", String.valueOf(left), "max", String.valueOf(max)));
                if (left == 1) p.sendMessage(plugin.msg("life-last"));
                return;
            }
            plugin.data().setLives(id, 0);
        }

        if (cfg.getBoolean("death.drop-head", true)) {
            loc.getWorld().dropItemNaturally(loc, plugin.createHead(p, real));
        }

        if (!ban || exempt) return;

        plugin.data().markDead(id, real);
        String reason = cfg.getString("death.ban-reason", "Você morreu!").replace("{player}", real);
        // Um tick depois, para o servidor terminar de processar a morte/drops
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + real + " " + reason));
    }

    /** Se o jogador consegue entrar com 0 vidas, ele foi desbanido por fora (ex.: /pardon). */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (plugin.data().hasLives(id) && plugin.data().getLives(id, 1) <= 0) {
            plugin.data().setLives(id, plugin.reviveLives());
            plugin.data().removeDead(id);
        }
    }

    public void playDeathSound(Location loc) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("death-sound.enabled", true)) return;
        String sound = cfg.getString("death-sound.sound", "entity.wither.spawn");
        float volume = (float) cfg.getDouble("death-sound.volume", 4.0);
        float pitch = (float) cfg.getDouble("death-sound.pitch", 1.0);

        if (cfg.getBoolean("death-sound.global", false)) {
            for (Player online : Bukkit.getOnlinePlayers())
                online.playSound(online.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
        } else {
            loc.getWorld().playSound(loc, sound, SoundCategory.MASTER, volume, pitch);
        }
    }

    // ===== Colocar a cabeça = reviver =====
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item.getType() != Material.PLAYER_HEAD) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String raw = meta.getPersistentDataContainer().get(plugin.headKey(), PersistentDataType.STRING);
        if (raw == null) return;

        UUID owner;
        try { owner = UUID.fromString(raw); } catch (IllegalArgumentException ex) { return; }

        String name = plugin.revive(owner);
        if (name == null) return; // jogador não está morto, é só uma cabeça

        Bukkit.broadcast(plugin.msg("revived-broadcast", "player", name));
        if (plugin.getConfig().getBoolean("death.revive-lightning", true)) {
            e.getBlockPlaced().getWorld().strikeLightningEffect(e.getBlockPlaced().getLocation());
        }
    }
}
