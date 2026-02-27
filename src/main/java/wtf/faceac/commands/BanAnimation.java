package wtf.faceac.commands;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import wtf.faceac.util.ColorUtil;

public class BanAnimation {

    private final JavaPlugin plugin;
    private final Player player;
    private final String banCommand;

    public BanAnimation(JavaPlugin plugin, Player player, String banCommand) {
        this.plugin = plugin;
        this.player = player;
        this.banCommand = banCommand;
    }

    public void run() {
        final int[] timer = {0};
        World world = player.getWorld();
        final boolean[] finished = {false};

        new BukkitRunnable() {
            @Override
            public void run() {
                if(!player.isOnline() || !player.isValid() || finished[0]) {
                    this.cancel();
                    return;
                }
                player.setVelocity(new Vector(0, 0.3, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 2, 1, false, false, false));
            }
        }.runTaskTimer(plugin, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                if(!player.isOnline() || !player.isValid()) {
                    finished[0] = true;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banCommand);
                    Bukkit.broadcastMessage(ColorUtil.colorize("&7[&#6aff00FaceAC&7] &f" + player.getName() + " был &#6aff00наказан &fантичитом"));
                    this.cancel();
                    return;
                }
                timer[0]++;
                if(timer[0] >= 7) {
                    world.spawnParticle(Particle.FLASH, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 30, 0.1, 0.1, 0.1, 3, null);
                    world.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 10, 2);
                    finished[0] = true;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banCommand);
                    Bukkit.broadcastMessage(ColorUtil.colorize("&7[&#6aff00FaceAC&7] &f" + player.getName() + " был &#6aff00наказан &fантичитом"));
                    this.cancel();
                    return;
                }
                world.spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 50, 0.5, 1, 0.5, 0, null);
                world.spawnParticle(Particle.LAVA, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 30, 1, 1, 1, 0, null);
                world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 2);
                ItemStack[] itemStacks = player.getInventory().getContents().clone();
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                for(ItemStack itemStack : itemStacks) {
                    if(itemStack == null) continue;
                    world.dropItem(player.getLocation(), itemStack);
                }
            }
        }.runTaskTimer(plugin, 0, 10);
    }
}
