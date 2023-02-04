package com.roughlyunderscore.commands;

import com.roughlyunderscore.PlungingAttack;
import com.roughlyunderscore.util.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class PlungingAttackCommand implements CommandExecutor, TabCompleter {
  private final PlungingAttack plugin;
  public PlungingAttackCommand(final PlungingAttack plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
    if (!sender.hasPermission(Permissions.USE)) {
      sender.sendMessage(plugin.getMessages().noPermission);
      return true;
    }

    switch (args.length) {
      case 0 -> {
        sender.sendMessage(plugin.getMessages().invalidArgument);
        return true;
      }

      case 1 -> {
        if (!args[0].equalsIgnoreCase("reload")) {
          sender.sendMessage(plugin.getMessages().invalidArgument);
          return true;
        }

        if (!sender.hasPermission(Permissions.RELOAD)) {
          sender.sendMessage(plugin.getMessages().noPermission);
          return true;
        }

        plugin.reload();
        sender.sendMessage(plugin.getMessages().reloaded);

        return true;
      }

      default -> { return true; }
    }
  }

  @Override
  public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
    if (args.length == 1) return List.of("reload");
    else return List.of("");
  }
}
