package com.roughlyunderscore.messages;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class Messages {
  public final String noPermission, reloaded, invalidArgument;

  public Messages(final FileConfiguration config) {
    final String language = config.getString("language", "en");

    noPermission = fetch(config, "no-permission", language);
    reloaded = fetch(config, "reload", language);
    invalidArgument = fetch(config,"invalid-argument", language);
  }

  private String fetch(final FileConfiguration config, final String keyName, final String language) {
    return ChatColor.translateAlternateColorCodes('&', config.getString("messages." + language + "." + keyName, "Message " + keyName + " not found"));
  }
}
