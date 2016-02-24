
package playcraft.dimon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

public class Main extends JavaPlugin implements Listener{
    private int version = 2;
    private String scoreboard = "TeamChatPlugin";
    private boolean color = true;
    private int messageDelay = 0;
    private String mTeamChat = "§7[§aTeam§9Chat§7]§r ";
    private String globalPrefix = "§7[§aG§7]§r ";
    private String teamPrefix = "§7[§9T§7]§r ";
    private String globalSufix = " §a➡§r ";
    private String teamSufix = " §9➡§r ";
    private String messageGlobal = "§a§lGlobal Chat. §r§e/team §f§omessage §r- §9To team.";
    private String messageTeam = "§9§lTeam Chat. §r§e/global §f§omessage §r- §aTo global.";
    private String mDelay = "§7Don't flood! §8Delay ";
    private final Map<String, Long> delays = new ConcurrentHashMap<>();
    
    
            
    @Override
    public void onEnable(){
        File cfgFile = new File(this.getDataFolder(), "config.yml");
        if (!cfgFile.exists()) {
            try {
                this.copyFromInputStream(cfgFile, this.getResource("config.yml"));
            } catch (IOException ex) {
                this.getLogger().log(Level.SEVERE, "Failed to write config.yml", ex);
                this.getConfig().options().copyDefaults(true);
                this.saveConfig();
            }
        }
        reloadConfigTeamChat();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getLogger().info("[TeamChat] Plugin - Enable");
        if (Bukkit.getScoreboardManager().getMainScoreboard().getObjective(this.scoreboard) != null){
            Bukkit.getLogger().info("[TeamChat] Objective Allready Created!");
        } else {
            Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective(this.scoreboard, "dummy");
        }
    }
    
    private void reloadConfigTeamChat(){
        this.reloadConfig();
        FileConfiguration config = this.getConfig();
        if (this.version != config.getInt("Version", 0)){
            try {
            File src = new File(this.getDataFolder(), "config.yml");
            File dst = new File(this.getDataFolder(), "backup-" + config.getInt("Version", 0) + ".yml");
            InputStream is = new FileInputStream(src);
            try {
                OutputStream os = new FileOutputStream(dst);
                try {
                    byte[] data= new byte[81920];
                    while (true) {
                        int size = is.read(data);
                        if (size < 0) {
                            break;
                        }os.write(data, 0, size);
                    }
            }finally{
            os.close();
                }
            } finally {
                is.close();
            }
            src.delete();
            this.copyFromInputStream(src, this.getResource("config.yml"));
            } catch(IOException e) {
                System.out.println("ERROR");
            }
            config.set("Version", this.version);
            this.reloadConfigTeamChat();
        }else {
            this.scoreboard = config.getString("Scoreboard");
            this.color = config.getBoolean("Color");
            this.messageDelay = config.getInt("Delay");
            this.globalPrefix = config.getString("GlobalPrefix");
            this.teamPrefix = config.getString("TeamPrefix");
            this.globalSufix = config.getString("GlobalSufix");
            this.teamSufix = config.getString("TeamSufix");
            this.messageGlobal = config.getString("MessageGlobal");
            this.messageTeam = config.getString("MessageTeam");
            this.mDelay = config.getString("MessageDelay");
        }
    }
    
    public void copyFromInputStream(File dst, InputStream is) throws IOException {
        dst.getParentFile().mkdirs();
        dst.createNewFile();
        OutputStream out = new FileOutputStream(dst);
        byte[] data = new byte[81920];
        while (true) {
            int amount = is.read(data);
            if (amount < 0) {
                is.close();
                out.close();
                return;
            }
            out.write(data, 0, amount);
        }
    }

    @Override
    public void onDisable(){
        Bukkit.getLogger().info("[TeamChat] Plugin - Disable");
    }
    
    @EventHandler
    public void playerQuitEvent(PlayerQuitEvent quit){
        this.delays.remove(quit.getPlayer().getName());
    }
    
    @EventHandler
    public void playerChatEvent(AsyncPlayerChatEvent chat) {
        String getPrefix = "";
        Player player = chat.getPlayer();
        Team team = player.getScoreboard().getPlayerTeam(player);
        Set<Player> recipients = chat.getRecipients();
        Iterator<Player> iterator = recipients.iterator();
        if (team == null){
        } else {
            if (this.color){
                getPrefix = team.getPrefix();
            } else {getPrefix = "";}
            if (player.getScoreboard().getObjective(this.scoreboard).getScore(player).getScore() == 1){
                String message = this.teamPrefix + getPrefix + player.getDisplayName() + team.getSuffix() + this.teamSufix + "%2$s";
                chat.setFormat(message);
                while (iterator.hasNext()) {
                    if (team.hasPlayer(iterator.next())) {
                    } else {
                        iterator.remove();
                    }
                }
            } else {
                Long i = this.delays.get(player.getName());
                if (this.messageDelay != 0 && i != null && i > System.currentTimeMillis()) {
                    chat.setCancelled(true);
                    Long t = (this.delays.get(player.getName()) - System.currentTimeMillis())/1000;
                    player.sendMessage(this.mTeamChat + this.mDelay + t + "s.");
                } else {
                    String message = this.globalPrefix + getPrefix + player.getDisplayName() + team.getSuffix() + this.globalSufix + "%2$s";
                    chat.setFormat(message);
                    if (this.messageDelay > 0 && !player.hasPermission("teamchat.player.delay")){
                        this.delays.put(player.getName(), System.currentTimeMillis() + this.messageDelay * 1000);
                    }
                }
            }
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isPlayer = sender instanceof Player;
        String mChat = "chat";
        if (sender.hasPermission("teamchat.admin.reload")){
            if (args.length == 1 && command.getName().equals(mChat) && args[0].equals("reload")){
                File cfgFile = new File(this.getDataFolder(), "config.yml");
                if (!cfgFile.exists()){
                    sender.sendMessage(mTeamChat + "ERROR. Restart or Reload Server!");
                }else{
                    this.reloadConfig();
                    sender.sendMessage(mTeamChat + "Reloaded.");
                    this.reloadConfigTeamChat();
                    return true;
                }
            }
        }
        if (sender.hasPermission("teamchat.admin.delay")){
            if (args.length == 2 && command.getName().equals(mChat) && args[0].equals("delay")){
                this.messageDelay = Integer.parseInt(args[1]);
                sender.sendMessage(mTeamChat + "§aMessage Delay Set " + this.messageDelay + " sec.");
                return true;
            }
        }
        if (sender.hasPermission("teamchat.player.chat")){
            if (args.length == 1 && command.getName().equals(mChat) && args[0].equals("help")){
                sender.sendMessage(mTeamChat + "§e/chat §b- Change Chat (global/team).");
                sender.sendMessage(mTeamChat + "§e/chat global §a- Change to Global Chat.");
                sender.sendMessage(mTeamChat + "§e/chat team §9- Change to Team Chat.");
                if (sender.hasPermission("teamchat.admin.reload")){
                    sender.sendMessage(mTeamChat + "§e/chat reload §b- Reload Configuration.");
                }
                if (sender.hasPermission("teamchat.admin.delay")){
                    sender.sendMessage(mTeamChat + "§e/chat delay §o3 §r§b- Set Global Chat delay to 3 sec.");
                }
                sender.sendMessage(mTeamChat + "§aDonwload: §bhttp://dev.bukkit.org/bukkit-plugins/team-chat-scoreboard/");
                return true;
            }
            if (isPlayer){
                Player player = (Player) sender;
                String getPrefix = "";
                if (args.length == 0 && command.getName().equals(mChat)){
                    if (player.getScoreboard().getObjective(this.scoreboard).getScore(player).getScore() == 1){
                        player.getScoreboard().getObjective(this.scoreboard).getScore(player).setScore(0);
                        sender.sendMessage(this.mTeamChat + this.messageGlobal);
                        return true;
                    } else {
                        player.getScoreboard().getObjective(this.scoreboard).getScore(player).setScore(1);
                        sender.sendMessage(this.mTeamChat + this.messageTeam);
                        return true;
                    }
                }
                if (args.length == 1 && command.getName().equals(mChat) && "team".startsWith(args[0])){
                    player.getScoreboard().getObjective(this.scoreboard).getScore(player).setScore(1);
                    sender.sendMessage(this.mTeamChat + this.messageTeam);
                    return true;
                }
                if (args.length == 1 && command.getName().equals(mChat) && "global".startsWith(args[0])){
                    player.getScoreboard().getObjective(this.scoreboard).getScore(player).setScore(0);
                    sender.sendMessage(this.mTeamChat + this.messageGlobal);
                    return true;
                }
                
                if (args.length > 0 && command.getName().equals("team")){
                    
                    Team team = player.getScoreboard().getPlayerTeam(player);
                    Set<OfflinePlayer> players = team.getPlayers();
                    String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
                    if (this.color){
                        getPrefix = team.getPrefix();
                    } else {getPrefix = "";}
                    String nameChat = this.teamPrefix + getPrefix + player.getName() + team.getSuffix() + this.teamSufix;
                    for (OfflinePlayer current : players) {
                        if (current.getPlayer() != null) {
                            current.getPlayer().sendMessage(nameChat + message);
                        }
                    }
                    return true;
                }
                if (args.length > 0 && command.getName().equals("global")){
                    Team team = player.getScoreboard().getPlayerTeam(player);
                    String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
                    if (this.color){
                        getPrefix = team.getPrefix();
                    } else {getPrefix = "";}
                    Long i = this.delays.get(player.getName());
                    if (this.messageDelay != 0 && i != null && i > System.currentTimeMillis()) {
                        Long t = (this.delays.get(player.getName()) - System.currentTimeMillis())/1000;
                        player.sendMessage(this.mTeamChat + this.mDelay + t + "s.");
                        return true;
                    } else {
                        String nameChat = this.globalPrefix + getPrefix + player.getName() + team.getSuffix() + this.globalSufix;
                        Bukkit.broadcastMessage(nameChat + message);
                        if (this.messageDelay > 0 && !sender.hasPermission("teamchat.player.delay")){
                            this.delays.put(player.getName(), System.currentTimeMillis() + this.messageDelay * 1000);
                        }
                    return true;
                    }
                }
            }
        }
        return true;
    }
    
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> complitions = new LinkedList<>();
        if (sender.hasPermission("teamchat.admin.reload")){
            if (args.length == 1){
                if ("reload".startsWith(args[0])){
                    complitions.add("reload");
                }
            }
        }
        if (sender.hasPermission("teamchat.admin.delay")){
            if (args.length == 1){
                if ("delay".startsWith(args[0])){
                    complitions.add("delay");
                }
            }
             if (args.length == 2 && "delay".equals(args[0])){
                complitions.add("0");
            }
        }
        if (sender.hasPermission("teamchat.player.chat")){
            if (args.length == 1){
                if ("help".startsWith(args[0])){
                    complitions.add("help");
                }
            }
            if ("team".startsWith(args[0])){
                    complitions.add("team");
                }
                if ("global".startsWith(args[0])){
                    complitions.add("global");
                }
        }
        return complitions;
    }
    
    
}
