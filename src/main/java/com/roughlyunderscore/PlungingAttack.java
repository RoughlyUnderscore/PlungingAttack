package com.roughlyunderscore;

import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import com.roughlyunderscore.commands.PlungingAttackCommand;
import com.roughlyunderscore.listeners.PlungingAttackListener;
import com.roughlyunderscore.messages.Messages;
import com.roughlyunderscore.tasks.FallDetectorTask;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class PlungingAttack extends JavaPlugin {

  public static Map<UUID, Integer> plungingAlready = new HashMap<>();
  public static Set<UUID> suppressFallDamage = new HashSet<>();

  private final int BSTATS_ID = 17637;
  private final String SPIGOT_ID = "0"; // FIXME

  public Messages getMessages() {
    return messages;
  }

  private Messages messages;
  private FallDetectorTask fallDetectorTask;
  private Metrics bStats;
  private UpdateChecker updateChecker;

  @Override
  public void onEnable() {
    initConfig();

    this.messages = new Messages(this.getConfig());

    this.fallDetectorTask = new FallDetectorTask();
    fallDetectorTask.register(this);

    initListeners();
    initCommands();
    initBStats();
    initUpdateChecker();
  }

  public void initConfig() {
    this.getConfig().options().setHeader(Arrays.asList(
      "PlungingAttack Configuration",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "bStats: Whether or not to send anonymous usage statistics to bStats.org (17637) (almost no performance impact).",
      "update-checker-frequency: How often to check for updates (in hours) - set to -1 to disable.",
      "aoe-radius: The radius of the Area Of Effect (AOE) of the attack.",
      "plunging-y: The minimum height of the fall that is considered a plunge.",
      "damage-players: Whether or not to damage players in the AOE.",
      "damage-mobs: Whether or not to damage mobs in the AOE.",
      "effect-start-plunge: The particles to spawn when started plunging (see https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Particle.html).",
      "sound-start-plunge: The sound to play when started plunging (see https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html).",
      "effect-plunge: The particles to spawn upon plunging (see https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Particle.html).",
      "sound-plunge: The sound to play upon plunging (see https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html).",
      "multiplier: How much to multiply the plunge attack damage by.",
      "shield-immunity: The amount of immunity (0%-100%) to the attack when using a shield.",
      "friendly-fire: Whether or not to damage players on the same team.",
      "-------",
      "Weapon multiplier is a multiplier that will be applied to the damage",
      "if the player is holding this weapon (or, for that matter, any item)",
      "in their main hand upon plunging.",
      "Weapon multipliers syntax:",
      "weapon-multipliers:",
      "  <case_insensitive_material>: <multiplier>",
      "You can look up the materials at https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html",
      "-------",
      "Strength multiplier is a multiplier that will be applied to the damage",
      "if the player has strength effect upon plunging.",
      "Strength multipliers syntax:",
      "strength-multipliers:",
      "  <strength_level>: <multiplier>",
      "-------",
      "Sharpness multiplier is a multiplier that will be applied to the damage",
      "if the player has the sharpness enchantment on their main hand item upon plunging.",
      "Sharpness multipliers syntax:",
      "sharpness-multipliers:",
      "  <sharpness_level>: <multiplier>",
      "-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      "© 2023 RoughlyUnderscore with ❤"
    ));

    this.getConfig().addDefault("bStats", true);
    this.getConfig().addDefault("update-checker-frequency", 24);
    this.getConfig().addDefault("aoe-radius", 3);
    this.getConfig().addDefault("plunging-y", 5);
    this.getConfig().addDefault("damage-players", true);
    this.getConfig().addDefault("damage-mobs", false);
    this.getConfig().addDefault("effect-start-plunge", "FLASH");
    this.getConfig().addDefault("sound-start-plunge", "ENTITY_WARDEN_TENDRIL_CLICKS");
    this.getConfig().addDefault("effect-plunge", "EXPLOSION_LARGE");
    this.getConfig().addDefault("sound-plunge", "ENTITY_WARDEN_SONIC_BOOM");
    this.getConfig().addDefault("multiplier", 0.4);
    this.getConfig().addDefault("shield-immunity", 40);
    this.getConfig().addDefault("friendly-fire", false);

    this.getConfig().addDefault("language", "en");

    this.getConfig().addDefault("messages.en.no-permission", "&cYou do not have permission to use this command.");
    this.getConfig().addDefault("messages.en.reload", "&aReloaded the configuration.");
    this.getConfig().addDefault("messages.en.invalid-argument", "&cInvalid argument.");

    this.getConfig().addDefault("messages.ru.no-permission", "&cУ вас нет прав для использования этой команды.");
    this.getConfig().addDefault("messages.ru.reload", "&aКонфигурация перезагружена.");
    this.getConfig().addDefault("messages.ru.invalid-argument", "&cНеверный аргумент.");

    this.getConfig().options().copyDefaults(true);

    saveConfig();
  }

  public void initBStats() {
    if (getConfig().getBoolean("bStats")) {
      bStats = new Metrics(this, BSTATS_ID);
    }
  }

  public void initUpdateChecker() {
    final int updateCheckerFrequency = getConfig().getInt("update-checker-frequency");
    if (updateCheckerFrequency > 0) {
      updateChecker = new UpdateChecker(this, UpdateCheckSource.SPIGOT, SPIGOT_ID)
        .checkEveryXHours(updateCheckerFrequency)
        .checkNow();
    }
  }

  public void initCommands() {
    final PlungingAttackCommand command = new PlungingAttackCommand(this);
    getCommand("plungingattack").setExecutor(command);
    getCommand("plungingattack").setTabCompleter(command);
  }

  public void initListeners() {
    getServer().getPluginManager().registerEvents(new PlungingAttackListener(this, getConfig()), this);
  }

  public void reload() {
    HandlerList.unregisterAll(this);

    plungingAlready.clear();
    suppressFallDamage.clear();
    fallDetectorTask.unregister();

    getCommand("plungingattack").setExecutor(null);
    getCommand("plungingattack").setTabCompleter(null);



    this.reloadConfig();
    this.messages = new Messages(this.getConfig());

    fallDetectorTask = new FallDetectorTask();
    fallDetectorTask.register(this);

    initListeners();
    initCommands();
    initBStats();
    initUpdateChecker();
  }
}
