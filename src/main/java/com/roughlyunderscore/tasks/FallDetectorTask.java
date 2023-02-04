package com.roughlyunderscore.tasks;

import com.roughlyunderscore.PlungingAttack;
import com.roughlyunderscore.events.PlayerPlungeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FallDetectorTask extends BukkitRunnable {
  @Override
  public void run() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.isFlying()) continue; // A player is flying, def not ending a plunge
      if (player.getVelocity().getY() > -0.04) continue; // A player is falling, def not ending a plunge
      if (!player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) continue; // A player is not on the ground, def not ending a plunge
      if (!PlungingAttack.plungingAlready.containsKey(player.getUniqueId())) continue; // A player is not plunging, def not ending a plunge
      final PlayerPlungeEvent event = new PlayerPlungeEvent(player);
      Bukkit.getPluginManager().callEvent(event);
    }
  }

  public void register(final PlungingAttack plugin) {
    this.runTaskTimer(plugin, 0, 1);
  }

  public void unregister() {
    this.cancel();
  }

  /**
   * Given a location with Y, finds the distance between Y and the nearest solid block before it.
   */
  public static int distanceToSolidBlockY(final Location loc) {
    if (loc.getWorld() == null) return 0;

    int y = loc.getBlockY();
    while (y > -65) {
      if (loc.getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).getType().isSolid()) {
        return loc.getBlockY() - y;
      }
      y--;
    }

    return 0;
  }
}
