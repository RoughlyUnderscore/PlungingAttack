package com.roughlyunderscore.listeners;

import com.roughlyunderscore.tasks.FallDetectorTask;
import com.roughlyunderscore.util.Permissions;
import com.roughlyunderscore.events.PlayerPlungeEvent;
import com.roughlyunderscore.PlungingAttack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PlungingAttackListener implements Listener {
  private final PlungingAttack plugin;
  private final int radius;
  private final int minPlunge;
  private final boolean damagePlayers;
  private final boolean damageMobs;
  private final Particle startPlungeParticle;
  private final Particle plungeParticle;
  private final Sound startPlungeSound;
  private final Sound plungeSound;
  private final double multiplier;
  private final double shieldImmunity;
  private final boolean friendlyFire;
  private final Map<Material, Double> weaponMultipliers;
  private final Map<Integer, Double> sharpnessMultipliers;
  private final Map<Integer, Double> strengthMultipliers;
  public PlungingAttackListener(final PlungingAttack plugin, final FileConfiguration config) {
    this.plugin = plugin;
    this.radius = config.getInt("aoe-radius");
    this.minPlunge = config.getInt("plunging-y");
    this.damagePlayers = config.getBoolean("damage-players");
    this.damageMobs = config.getBoolean("damage-mobs");
    this.startPlungeParticle = Particle.valueOf(config.getString("effect-start-plunge"));
    this.startPlungeSound = Sound.valueOf(config.getString("sound-start-plunge"));
    this.plungeParticle = Particle.valueOf(config.getString("effect-plunge"));
    this.plungeSound = Sound.valueOf(config.getString("sound-plunge"));
    this.multiplier = config.getDouble("multiplier");
    this.friendlyFire = config.getBoolean("friendly-fire");

    // 1) Clamp between 0 and 100
    // 4 -> 4, 151 -> 100, -1 -> 0
    // 2) Divide by 100 to make it a percentage multiplier
    // 4 -> 0.04, 100 -> 1, 0 -> 0
    // 3) Subtract from 1 to make it a damage multiplier
    // 0.04 -> 0.96, 1 -> 0, 0 -> 1
    // Now damage can be multiplied by shieldImmunity to get the damage to deal to the shielded entity
    this.shieldImmunity = 1 - (Math.min(100D, Math.max(0D, config.getDouble("shield-immunity"))) / 100);

    this.weaponMultipliers = new HashMap<>();
    final ConfigurationSection weaponMultipliersSection = config.getConfigurationSection("weapon-multipliers");
    if (weaponMultipliersSection != null) {
      for (final String key : weaponMultipliersSection.getKeys(false)) {
        final Material material = Material.valueOf(key.toUpperCase().replace("-", "_"));
        final double multiplier = config.getDouble("weapon-multipliers." + key);
        this.weaponMultipliers.put(material, multiplier);
      }
    }

    this.sharpnessMultipliers = new HashMap<>();
    final ConfigurationSection sharpnessMultipliersSection = config.getConfigurationSection("sharpness-multipliers");
    if (sharpnessMultipliersSection != null) {
      for (final String key : sharpnessMultipliersSection.getKeys(false)) {
        final int level = Integer.parseInt(key);
        final double multiplier = config.getDouble("sharpness-multipliers." + key);
        this.sharpnessMultipliers.put(level, multiplier);
      }
    }

    this.strengthMultipliers = new HashMap<>();
    final ConfigurationSection strengthMultipliersSection = config.getConfigurationSection("strength-multipliers");
    if (strengthMultipliersSection != null) {
      for (final String key : strengthMultipliersSection.getKeys(false)) {
        final int level = Integer.parseInt(key);
        final double multiplier = config.getDouble("strength-multipliers." + key);
        this.strengthMultipliers.put(level, multiplier);
      }
    }
  }

  @EventHandler
  public void onPlayerInteract(final PlayerInteractEvent event) {
    if (event.getAction() != Action.LEFT_CLICK_AIR) return;

    final Player player = event.getPlayer();
    if (!player.hasPermission(Permissions.USE)) return; // Doesn't have permission to use the ability
    if (player.isFlying()) return; // Flying does not count
    if (player.getVelocity().getY() >= -0.1) return; // Isn't falling
    if (PlungingAttack.plungingAlready.containsKey(player.getUniqueId())) return; // Already plunging

    int distanceToSolidBlockY = FallDetectorTask.distanceToSolidBlockY(player.getLocation());
    if (distanceToSolidBlockY < minPlunge) return; // Not a plunge

    // Now it's totally a plunge!
    PlungingAttack.plungingAlready.put(player.getUniqueId(), distanceToSolidBlockY);
    PlungingAttack.suppressFallDamage.add(player.getUniqueId());

    // Effect of a plunge
    player.getWorld().spawnParticle(startPlungeParticle, player.getLocation(), 2);
    player.getWorld().playSound(player.getLocation(), startPlungeSound, 1, 1);
    player.setVelocity(new Vector());
  }

  @EventHandler
  public void onStartFlying(final PlayerToggleFlightEvent event) {
    final Player player = event.getPlayer();
    if (!PlungingAttack.plungingAlready.containsKey(player.getUniqueId())) return; // Not a plunge

    // Cannot start flying during a plunge!
    event.setCancelled(true);
  }

  @EventHandler
  public void onDamage(final EntityDamageEvent event) {
    if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
    if (!(event.getEntity() instanceof Player player)) return;
    if (!PlungingAttack.suppressFallDamage.contains(player.getUniqueId())) return;

    // Fall damage is suppressed
    event.setCancelled(true);
    PlungingAttack.suppressFallDamage.remove(player.getUniqueId());
  }

  @EventHandler
  public void onGamemodeChange(final PlayerGameModeChangeEvent event) {
    final Player player = event.getPlayer();
    if (!PlungingAttack.plungingAlready.containsKey(player.getUniqueId())) return; // Not a plunge

    // Cannot change gamemode during a plunge!
    event.setCancelled(true);
  }

  @EventHandler
  public void onDamage(final PlayerPlungeEvent ev) {
    final Player player = ev.getPlayer();

    final int fallDistance = PlungingAttack.plungingAlready.remove(player.getUniqueId());
    // Since the player has landed, we gotta do some damage to the area
    if (damagePlayers) handleAOEPlayerDamage(player, radius, fallDistance);

    if (damageMobs) handleAOEMobDamage(player, radius, fallDistance);

    // It's never certain that the player receives fall damage.
    // If they don't, they stay in "suppressFallDamage" before their next fall.
    // To prevent this, we wait 1-3 ticks before removing them from the list.
    // They won't have time to fall again before they are out of the list.
    Bukkit.getScheduler().runTaskLater(plugin, () ->
      PlungingAttack.suppressFallDamage.remove(player.getUniqueId()), ThreadLocalRandom.current().nextInt(3)
    );

    // Effects
    player.getWorld().spawnParticle(plungeParticle, player.getLocation(), 2, 1, 1, 1);
    player.getWorld().playSound(player.getLocation(), plungeSound, 1, 1);
  }

  public List<Player> getNearbyPlayers(final Player player, final int radius) {
    final List<Player> nearbyPlayers = new ArrayList<>();
    for (final Entity other : player.getNearbyEntities(radius, radius, radius)) {
      if (other instanceof Player pl) nearbyPlayers.add(pl);
    }
    return nearbyPlayers;
  }

  public List<LivingEntity> getNearbyMobs(final Player player, final int radius) {
    final List<LivingEntity> nearbyMobs = new ArrayList<>();
    for (final Entity other : player.getNearbyEntities(radius, radius, radius)) {
      if (!(other instanceof Player) && other instanceof LivingEntity ent) nearbyMobs.add(ent);
    }
    return nearbyMobs;
  }

  public void handleAOEPlayerDamage(final Player pl, final int radius, final int fallDistance) {
    for (final Player other : getNearbyPlayers(pl, radius)) {
      if (sameTeam(pl, other) && !friendlyFire) continue;
      if (other.getLocation().distance(pl.getLocation()) > radius) continue;

      double damage = fallDistance * multiplier * findWeaponMultiplier(pl) * findSharpnessMultiplier(pl) * findStrengthEffectMultiplier(pl);
      // (debug) pl.sendMessage("Damage: " + damage);
      if (other.isBlocking()) damage = damage * shieldImmunity;
      other.damage(damage, pl);
    }
  }

  public void handleAOEMobDamage(final Player pl, final int radius, final int fallDistance) {
    for (final LivingEntity other : getNearbyMobs(pl, radius)) {
      if (other.getLocation().distance(pl.getLocation()) > radius) continue;

      double damage = fallDistance * multiplier * findWeaponMultiplier(pl) * findSharpnessMultiplier(pl) * findStrengthEffectMultiplier(pl);
      // (debug) pl.sendMessage("Damage: " + damage);
      other.damage(damage, pl);
    }
  }

  public boolean sameTeam(final Player player, final Player other) {
    return player
      .getScoreboard()
      .getTeams()
      .stream()
      .anyMatch(t -> t.hasEntry(player.getName()) && t.hasEntry(other.getName()));
  }

  public double findWeaponMultiplier(final Player player) {
    final ItemStack item = player.getInventory().getItemInMainHand();
    return weaponMultipliers.getOrDefault(item.getType(), 1D);
  }

  public double findStrengthEffectMultiplier(final Player player) {
    final PotionEffect effect = player.getPotionEffect(PotionEffectType.INCREASE_DAMAGE);
    if (effect == null) return 1D;
    return strengthMultipliers.getOrDefault(effect.getAmplifier(), 1D);
  }

  public double findSharpnessMultiplier(final Player player) {
    final ItemStack item = player.getInventory().getItemInMainHand();
    final int level = item.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
    return sharpnessMultipliers.getOrDefault(level, 1D);
  }
}
