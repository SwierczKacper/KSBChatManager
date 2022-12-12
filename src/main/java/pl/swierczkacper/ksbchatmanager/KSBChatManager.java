package pl.swierczkacper.ksbchatmanager;

import com.sun.org.apache.xerces.internal.xs.StringList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class KSBChatManager extends JavaPlugin implements Listener {

    private static Permission perms = null;

    @Override
    public void onEnable() {
        getLogger().info("KSBChatManager starting...");

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();

        getServer().getPluginManager().registerEvents(this, this); // Registering events

        getConfig().options().copyDefaults(); // Copy default config
        saveDefaultConfig(); // Save default config
    }

    @Override
    public void onDisable() {
        getLogger().info("KSBChatManager stopping...");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e)
    {
        Player player = e.getPlayer();
        String prefix = getPlayerPrefix(player);
        String playerDisplayName = ChatColor.translateAlternateColorCodes('&', prefix + player.getName());
        String message = e.getMessage();

        if(!perms.has(player, "ksbchatmanager.allowads")) {
            if(checkIfMessageContainsBlockedDomainSuffix(message)) { // If message contains blocked domain suffix
                e.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.blocked_domains")));

                informAdministrators(player, message);
                return;
            }
        }

        e.setCancelled(true);

        if(perms.has(player, "ksbchatmanager.chat.color")) {
            Bukkit.broadcastMessage(playerDisplayName + " >>> " + ChatColor.translateAlternateColorCodes('&', message));
        } else {
            Bukkit.broadcastMessage(playerDisplayName + " >>> " + message);
        }
    }

    public void informAdministrators(Player sender, String message) {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (perms.has(player, "ksbchatmanager.receive_ads_alert")) {
                String prefix = getPlayerPrefix(sender);

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l[REKLAMA] " + prefix + sender.getName() + " >>> " + message));
            }
        }
    }

    public boolean checkIfMessageContainsBlockedDomainSuffix(String message) {
        List<String> blockedDomains = getConfig().getStringList("ads_blocked_domains");

        for(String domain : blockedDomains) {
            if(message.contains(domain)) {
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        String prefix = getPlayerPrefix(event.getEntity());

        String msg = ChatColor.translateAlternateColorCodes('&', prefix + event.getEntity().getName() + " " + getConfig().getString("death_message"));

        event.setDeathMessage(msg);
    }

    public String getPlayerPrefix(Player player) {
        String group = perms.getPrimaryGroup(player); // Get player group
        String prefix = getConfig().getString("group_prefixes." + group); // Get prefix from config

        return prefix == null ? getConfig().getString("group_prefixes.default") : prefix; // Return default prefix if player group prefix is null
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        return true;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
}
