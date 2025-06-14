package com.wish;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderPearlHCN extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private Map<UUID, Long> cooldowns = new HashMap<>();
    private int cooldownTime;
    private String cooldownMessage;
    private String cooldownExpiredMessage;
    private String placeholderNoCooldown;
    private String placeholderFormat;
    private DecimalFormat decimalFormat;

    @Override
    public void onEnable() {
        // Guardar configuración predeterminada si no existe
        saveDefaultConfig();
        // Cargar configuración
        loadConfig();

        // Registrar eventos
        getServer().getPluginManager().registerEvents(this, this);

        // Registrar placeholder
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EnderPearlPlaceholder().register();
            getLogger().info("PlaceholderAPI encontrado, registrando placeholders!");
        } else {
            getLogger().warning("PlaceholderAPI no encontrado, los placeholders no funcionarán!");
        }

        getLogger().info("EnderPearlHCN ha sido habilitado correctamente! by wwishhdev <3");
    }

    @Override
    public void onDisable() {
        getLogger().info("EnderPearlHCN ha sido deshabilitado! by wwishhdev <3");
    }

    private void loadConfig() {
        // Recargar la configuración
        reloadConfig();
        config = getConfig();

        // Si no hay configuración, establecer valores predeterminados
        if (!config.contains("cooldown")) {
            config.set("cooldown", 10);
        }
        if (!config.contains("messages.cooldown")) {
            config.set("messages.cooldown", "&cDebes esperar &e%time% segundos &cpara usar otra Ender Pearl!");
        }
        if (!config.contains("messages.cooldownExpired")) {
            config.set("messages.cooldownExpired", "&aYa puedes lanzar otra Ender Pearl!");
        }
        if (!config.contains("messages.reloadSuccess")) {
            config.set("messages.reloadSuccess", "&aConfiguración recargada correctamente!");
        }
        if (!config.contains("placeholder.noCooldown")) {
            config.set("placeholder.noCooldown", "");
        }
        if (!config.contains("placeholder.format")) {
            config.set("placeholder.format", "&eEnderpearl: &c%time%s");
        }
        if (!config.contains("placeholder.decimalFormat")) {
            config.set("placeholder.decimalFormat", "#.#");
        }

        saveConfig();

        // Cargar valores de la configuración
        cooldownTime = config.getInt("cooldown");
        cooldownMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.cooldown"));
        cooldownExpiredMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.cooldownExpired"));
        placeholderNoCooldown = config.getString("placeholder.noCooldown");
        placeholderFormat = ChatColor.translateAlternateColorCodes('&', config.getString("placeholder.format"));
        decimalFormat = new DecimalFormat(config.getString("placeholder.decimalFormat"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("enderpearlhcn")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("enderpearlhcn.reload")) {
                    loadConfig();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.reloadSuccess")));
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando!");
                    return true;
                }
            }
            sender.sendMessage(ChatColor.YELLOW + "EnderPearlHCN v" + getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "/enderpearlhcn reload - Recarga la configuración");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Comprobar si la acción es lanzar un objeto
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Comprobar si el ítem es una ender pearl
        if (player.getItemInHand() == null || player.getItemInHand().getType() != Material.ENDER_PEARL) {
            return;
        }

        UUID playerUUID = player.getUniqueId();

        // Verificar si el jugador tiene cooldown
        if (cooldowns.containsKey(playerUUID)) {
            double secondsLeft = getCooldownTimeLeft(player);

            if (secondsLeft > 0) {
                // Todavía está en cooldown, cancelar el evento
                event.setCancelled(true);
                player.sendMessage(cooldownMessage.replace("%time%", String.valueOf((int)secondsLeft)));
                return;
            }
        }

        // Establecer el cooldown
        cooldowns.put(playerUUID, System.currentTimeMillis());

        // Informar al jugador cuando el cooldown termine
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendMessage(cooldownExpiredMessage);
                }
            }
        }.runTaskLater(this, cooldownTime * 20L);
    }

    public double getCooldownTimeLeft(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (cooldowns.containsKey(playerUUID)) {
            double secondsLeft = ((cooldowns.get(playerUUID) / 1000.0) + cooldownTime) - (System.currentTimeMillis() / 1000.0);
            if (secondsLeft > 0) {
                return secondsLeft;
            }
        }
        return 0;
    }

    // Clase para el placeholder
    private class EnderPearlPlaceholder extends PlaceholderExpansion {

        @Override
        public String getIdentifier() {
            return "enderpearlhcn";
        }

        @Override
        public String getAuthor() {
            return "wwishhdev";
        }

        @Override
        public String getVersion() {
            return EnderPearlHCN.this.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onPlaceholderRequest(Player player, String identifier) {
            if (player == null) {
                return placeholderNoCooldown;
            }

            if (identifier.equals("cooldown")) {
                double timeLeft = getCooldownTimeLeft(player);
                if (timeLeft <= 0) {
                    return placeholderNoCooldown;
                }
                return placeholderFormat.replace("%time%", decimalFormat.format(timeLeft));
            }

            return null;
        }
    }
}