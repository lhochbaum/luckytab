package io.github.lhochbaum.luckytab

import me.lucko.luckperms.LuckPerms
import me.lucko.luckperms.api.Contexts
import me.lucko.luckperms.api.event.sync.PostSyncEvent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team
import org.bukkit.event.EventHandler as event

class LuckytabPlugin : JavaPlugin(), Listener {
  private val luckPerms by lazy { LuckPerms.getApi() }
  private val scoreboard = Bukkit.getScoreboardManager().mainScoreboard

  override fun onEnable() {
    // Unregister old teams.
    scoreboard.teams.forEach(Team::unregister)

    // Register new teams.
    luckPerms.groups.forEach { group ->
      val team =
        if (scoreboard.getTeam(group.name) == null)
          scoreboard.registerNewTeam(group.name)
        else
          scoreboard.getTeam(group.name)

      val prefix = group.cachedData.getMetaData(Contexts.global()).prefix
      if (prefix != null) {
        team.prefix = ChatColor.translateAlternateColorCodes('&', prefix)
      }
    }

    // If restarted, reload every player.
    server.onlinePlayers.forEach { it.update() }

    // Handle LuckPerms synchronization.
    luckPerms.eventBus.subscribe(PostSyncEvent::class.java) { server.onlinePlayers.forEach { it.update() } }

    // Register join handler.
    server.pluginManager.registerEvents(this, this)
  }

  // Add new players to their team.
  @event fun onJoin(event: PlayerJoinEvent) = event.player.update()

  private fun Player.update() {
    val group = luckPerms.getUser(uniqueId)?.primaryGroup
    val team = scoreboard.getEntryTeam(name)

    team?.removeEntry(name)
    scoreboard.getTeam(group).addEntry(name)
  }
}
