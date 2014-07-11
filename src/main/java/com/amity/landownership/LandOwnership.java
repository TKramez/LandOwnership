package com.amity.landownership;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class LandOwnership extends JavaPlugin {
	
	public static final String ADMIN_PERM = "land.admin";
	public static final String MANAGER_PERM = "land.manager";
	public static final String READER_PERM = "land.reader";
	
	public static ChatColor successColor = ChatColor.GREEN;
	public static ChatColor failColor = ChatColor.RED;
	public static ChatColor parmColor = ChatColor.YELLOW;
	
	
	private Logger log;
	private HashMap<String, Land> chunks = new HashMap<String, Land>();
	private HashMap<String, LandGroup> groups = new HashMap<String, LandGroup>();
	private HashMap<String, List<String>> groupLookup = new HashMap<String, List<String>>();
	private Economy econ;
	private double price;
	private boolean markPlot;
	private int listPageSize;
	private double sellBackMultiplier;
	private String plotDataPath;
	private String groupDataPath;
	private LandUtils util;
	
	public HashMap<String, Land> getChunks() {
		return chunks;
	}
	
	public HashMap<String, LandGroup> getGroups() {
		return groups;
	}	
	
	public HashMap<String, List<String>> getGroupLookup() {
		return groupLookup;
	}		

	protected final Economy getEcon() {
		return econ;
	}
	
	protected boolean getMarkPlot() {
		return markPlot;
	}
	
	protected int getListPageSize() {
		return listPageSize;
	}

	@Override
	public void onEnable() {
		log = this.getLogger();

		if (!setupEcon()) {
			log.info("Failed to initialize economy.");
			this.setEnabled(false);
		} else {
			plotDataPath = this.getDataFolder().getAbsolutePath() + File.separator + "LandOwnershipData.dat";
			groupDataPath = this.getDataFolder().getAbsolutePath() + File.separator + "LandOwnershipGroupData.dat";
			try {
				load();
			} catch (Exception e) {
				log.info("Load failed. Check stderr for stacktrace.");
				e.printStackTrace();
				this.setEnabled(false);
				return;
			}
			this.saveDefaultConfig();
			loadConfig();
			util = new LandUtils(this);
			new BukkitRunnable() {

				@Override
				public void run() {
					save();
				}
			}.runTaskTimer(this, 0, 20 * 60 * 60);
			
			initialConverts();
			
			new LandOwnershipListener(this);

			log.info(this.getName() + " enabled.");
		}
	}
	
	// future use
	private void buildGroupLookup() {
		
		for (LandGroup group : groups.values()) {
			for (String land : group.getLands()) {

				if (!groupLookup.containsKey(land)) {
					groupLookup.get(land).add(group.getName());
				} else {
					groupLookup.put(land, new ArrayList<String>());
					groupLookup.get(land).add(group.getName());
				}
			}
		}
		
	}
	
	private void buildCommandUsage() {
		
		PluginCommand command = this.getCommand("plot");

		StringBuilder builder = new StringBuilder(ChatColor.GREEN + "Plot Commands " + this.getDescription().getVersion() + ChatColor.WHITE + "\n");

		builder.append(this.helpBuilder("/plot [buy,b,purchase,p]","Attempts to purchase the current plot."));
		builder.append(this.helpBuilder("/plot [sell,s]","Attempts to sell the current plot. If no buyer or price then the plot is sold back to the server."));
		
		builder.append(this.helpBuilder("/plot add <playername>","Attempts to add the player to the current plot."));
		builder.append(this.helpBuilder("/plot remove <playername>","Attempts to remove the player from the current plot."));
		builder.append(this.helpBuilder("/plot [group,g]","Plot grouping commands. See /plot group help."));					
		builder.append(this.helpBuilder("/plot help","Comprehensive list of additional commands."));					


		command.setUsage(builder.toString());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equals("plot")) {
			if (sender instanceof Player) {
				final Player player = (Player) sender;
				if (args.length==0) 
					return false;
				
				if (args.length==1 && args[0].equalsIgnoreCase("help")) {

					StringBuilder builder = new StringBuilder(ChatColor.GREEN + "Plot Commands " + this.getDescription().getVersion() + ChatColor.WHITE + "\n");

					builder.append(this.helpBuilder("/plot [buy,b,purchase,p]","Attempts to purchase the current plot."));
					builder.append(this.helpBuilder("/plot [sell,s]","Attempts to sell the current plot. If no buyer or price then the plot is sold back to the server."));
					builder.append(this.helpBuilder("/plot add <playername>","Attempts to add the player to the current plot."));
					builder.append(this.helpBuilder("/plot addall <playername>","Attempts to add the player to all plots you own."));
					builder.append(this.helpBuilder("/plot remove <playername>","Attempts to remove the player from the current plot."));
					builder.append(this.helpBuilder("/plot [listmembers,lm] <page>","Lists the members of the current plot."));					
					builder.append(this.helpBuilder("/plot map","Shows a map of plots."));
					builder.append(this.helpBuilder("/plot [listtoggles,lt]", "Lists the toggles set for the plot."));					
					builder.append(this.helpBuilder("/plot [settoggle,st] <true|false>","Attempts to set the given toggle to the given value on the current plot."));
					builder.append(this.helpBuilder("/plot who","Tells you who owns the plot you are on."));
					builder.append(this.helpBuilder("/plot list <page>","Lists the plots that you own."));
					builder.append(this.helpBuilder("/plot [group,g]","Plot grouping commands. See /plot group help."));					
					builder.append(this.helpBuilder("/plot degroup","Removes a plot from all groups."));
					builder.append(this.helpBuilder("/plot [listgroups,lg] <page>","Lists the groups the plot belongs to."));					
					builder.append(this.helpBuilder("/plot [listaccess,la] <page>","Lists all players with access to the plot."));					
					builder.append(this.helpBuilder("/plot [checkaccess,ca] <playername>","Lists all players with access and how. Optional <playername>."));
					builder.append(this.helpBuilder("/plot price","Tells you the price of a plot of land."));					
					
					if (util.isAuth(player, "AMR")) {
						builder.append(this.helpBuilder("/plot list <playername> <page>","Lists the plots owned by <playername>."));
						builder.append(this.helpBuilder("/plot disband","Clears the owners and members of current plot."));
						builder.append(this.helpBuilder("/plot disbandall", "Clears all plots belonging to <playername> and returns them to the wild."));
						builder.append(this.helpBuilder("/plot changeowner <playername>","Changes the plot owner to <playername>."));
						builder.append(this.helpBuilder("/plot server","Claims the current plot for the server."));
					}
					
					player.sendMessage(builder.toString());
					
					return true;
					
				// group commands
				} else if (args[0].equalsIgnoreCase("group") || args[0].equalsIgnoreCase("g")) {
					// default help
					if (args.length == 1 || args[1].equalsIgnoreCase("help")) {

						StringBuilder builder = new StringBuilder(ChatColor.GREEN + "Plot Group Commands " + this.getDescription().getVersion() + ChatColor.WHITE + "\n");

						builder.append(this.helpBuilder("/plot group list <page>", "Lists the groups you own."));
						builder.append(this.helpBuilder("/plot group [listplots,lp] <groupname> <page>", "Lists the plots in <groupname>."));
						builder.append(this.helpBuilder("/plot group [listmembers,lm] <groupname> <page>", "Lists the plots in <groupname>."));
						builder.append(this.helpBuilder("/plot group create <groupname>", "Creates new group <groupname>."));
						builder.append(this.helpBuilder("/plot group disband <groupname>", "Disbands <groupname>. Does not impact member plots."));
						builder.append(this.helpBuilder("/plot group rename <oldgroupname> <newgroupname>", "Renames <oldgroupname> to <newgroupname>."));
						builder.append(this.helpBuilder("/plot group [addplot,ap] <groupname>", "Adds the current plot to <groupname>."));
						builder.append(this.helpBuilder("/plot group addallplots <groupname>", "Adds all plots you own to <groupname>."));
						builder.append(this.helpBuilder("/plot group [removeplot,rp] <groupname>", "Removes the current plot from <groupname>."));
						builder.append(this.helpBuilder("/plot group [addmember,am] <playername> <groupname>", "Adds <playername> as a member of <groupname>."));
						builder.append(this.helpBuilder("/plot group [removemember,rm] <playername> <groupname>", "Removes <playername> as a member of <groupname>."));
						builder.append(this.helpBuilder("/plot group [listtoggles,lt]", "Lists the toggles set for the group."));
						builder.append(this.helpBuilder("/plot group [settoggle,st] <groupname> <toggleName> <true|false>", "Attempts to set <toggleName> to <true|false> for <groupname>."));
						builder.append(this.helpBuilder("/plot group sell <groupname> <playername> <price>","Attempts to sell a group and all plots in <groupname> to <playername> for <price>."));

						if (util.isAuth(player, "AMR")) {
							builder.append(this.helpBuilder("/plot group changeowner <groupname> <playername>", "Changes owner of <groupname> to <playername>."));
							builder.append(this.helpBuilder("/plot group addallplots <playername> <groupname>", "Adds all plots owned by <playername> to <groupname>."));
						}
						
						player.sendMessage(builder.toString());
						
						return true;
						
					} 
					
					// group lists
					else if (args[1].equalsIgnoreCase("list")) {
						
						// default list for calling player, no page
						if (args.length == 2) {
							player.sendMessage(util.groupList(player.getName(), 1));
							return true;
						}
						
						// default list for calling player, with a page
						else if (args.length == 3 && isInt(args[2].toString())) {
							int page = Integer.parseInt(args[2]);
							player.sendMessage(util.groupList(player.getName(), page));
							return true;							
						}
						
						// list groups for specific player, no page
						else if (args.length == 3 && !isInt(args[2].toString())) {
							player.sendMessage(util.groupListByPlayerName(player, args[2].toString(), 1));
							return true;							
						}
						
						// list groups for specific player, with a page
						else if (args.length == 4 && !isInt(args[2].toString())) {
								int page = Integer.parseInt(args[2]);
								player.sendMessage(util.groupListByPlayerName(player, args[2].toString(), page));
								return true;
							}
							else {
							player.sendMessage(failColor + "Invalid arguments specified.");
							return true;
						}
					
					// create a new group
					} else if (args[1].equalsIgnoreCase("create")) {
						if (args.length == 2) {
							player.sendMessage(failColor + "You must specifiy a group name.");
							return true;
						} else {
							util.groupCreate(player, args[2].toString());
							return true;
						}

					// disband a group
					} else if (args[1].equalsIgnoreCase("disband")) {
						if (args.length == 2) {
							player.sendMessage(failColor + "You must specifiy a group name.");
							return true;
						} else {
							util.groupDisband(player, args[2].toString());
							return true;
						}

					// disband all groups by owner
					} else if (args[1].equalsIgnoreCase("disbandall")) {
						player.sendMessage(failColor + "Not yet implemented.");
						return true;
						
					// change owner
					} else if (args[1].equalsIgnoreCase("changeowner")) {
						if (args.length < 4) {
							player.sendMessage(failColor + "Invalid arguments specified.");
							return true;
						} else {						
							util.groupChangeOwner(player, args[2].toString(), args[3].toString());
							return true;
						}
						
					} else if (args[1].equalsIgnoreCase("rename")) {
						if (args.length < 4) {
							player.sendMessage(failColor + "Invalid arguments specified.");
							return true;
						} else {
							util.groupRename(player, args[2].toString(), args[3].toString());
							return true;
						}
					
					// group lists
					} else if (args[1].equalsIgnoreCase("listplots") || 
							   args[1].equalsIgnoreCase("lp")) {
						
						// default list for group, no page
						if (args.length == 3) {
							player.sendMessage(util.groupListLands(player, args[2].toString(), 1));
							return true;
						}
						
						// default list for group, with a page
						else if (args.length == 4 && isInt(args[3].toString())) {
							int page = Integer.parseInt(args[3]);
							player.sendMessage(util.groupListLands(player, args[2].toString(), page));
							return true;							
						}

						else {
						player.sendMessage(failColor + "Invalid arguments specified.");
						return true;

						}						
					
					// add a plot to group
					} else if (args[1].equalsIgnoreCase("addplot") || 
							   args[1].equalsIgnoreCase("ap")) {
						
						if (!(args.length == 3)) {
							player.sendMessage(failColor + "Invalid arguments specified.");
							return true;							
						} else {
							util.groupAddChunk(player, args[2].toString());
							return true;
						}

					// add all your plots to a group
					} else if (args[1].equalsIgnoreCase("addallplots")) {
						
						if (!(args.length == 3 || args.length == 4)) {
							player.sendMessage(failColor + "Invalid arguments specified.");
							return true;							
						} else if (args.length == 4) {
							util.groupAddAllChunks(player, args[2].toString(), args[3].toString());
							return true;
						} else {
							util.groupAddAllChunks(player, args[2].toString());
							return true;
						}
						
						
					// remove a plot from group
					} else if (args[1].equalsIgnoreCase("removeplot") || args[1].equalsIgnoreCase("rp")) {
						if (!(args.length == 3)) {
							player.sendMessage(failColor + "Invalid arguments specified.");
							return true;							
						} else {
							util.groupRemoveChunk(player, args[2].toString());
							return true;
						}
						
					// add a member to group
					} else if (args[1].equalsIgnoreCase("addmember") || args[1].equalsIgnoreCase("am")) {
						if (!(args.length == 4)) {
							player.sendMessage(failColor + "Invalid arguments specified.");
							return true;							
						} else {
							util.groupAddPlayer(player, args[3].toString(), args[2].toString());
							return true;
						}
						
					// remove a member from group
					} else if (args[1].equalsIgnoreCase("removemember") || args[1].equalsIgnoreCase("rm")) {
						if (!(args.length == 4)) {
							player.sendMessage(failColor + "Invalid arguments specified.");
							return true;							
						} else {
							util.groupRemovePlayer(player, args[3].toString(), args[2].toString());
							return true;
						}
						
					
					// list members in a group
					} else if (args[1].equalsIgnoreCase("listmembers") || args[1].equalsIgnoreCase("lm")) {
						if (args.length == 3) {
							player.sendMessage(util.groupListMembers(player, args[2].toString(), 1));
							return true;
						} else if (args.length == 4 && isInt(args[3].toString())){
							int page = Integer.parseInt(args[3]);							
							player.sendMessage(util.groupListMembers(player, args[2].toString(), page));
							return true;							
						} else {
							player.sendMessage(failColor + "Invalid arguments specified.");
							return true;							
						}						
						
					// sell group to player
					} else if (args[1].equalsIgnoreCase("sell")) {
						player.sendMessage(failColor + "Not yet implemented.");
						return true;	
						
					} else if (args[1].equalsIgnoreCase("listtoggles") || args[1].equalsIgnoreCase("lt")) {
						if (args.length >= 3) {
							player.sendMessage(util.groupListToggles(player, args[2].toString()));
							return true;
						} 						
						
					// set group toggles
					} else if (args[1].equalsIgnoreCase("settoggle") || args[1].equalsIgnoreCase("st")) {

						if (args.length == 5 ){
							util.groupSetToggle(player, args[2].toString(), args[3].toString(), args[4].toString());
							return true;
						} else {
							player.sendMessage(failColor + "Invalid arguments specified.");
							return true;	
						}
						
					// get basic info
					} else if (args[1].equalsIgnoreCase("info")) {
						player.sendMessage(failColor + "Not yet implemented.");
						return true;
						
					// other
					} else { 
						player.sendMessage(failColor + "Invalid command specified.");
						return true;	
					}
	

				} else if (args[0].equalsIgnoreCase("listtoggles") || args[0].equalsIgnoreCase("lt")) { 
					
					String id = ChunkID.get(player);
					
					if (args.length == 1) {
						player.sendMessage(util.landListToggles(player, id));
						return true;
					}
					
				// set toggles
				} else if (args[0].equalsIgnoreCase("settoggle") || args[0].equalsIgnoreCase("st")) {

					String id = ChunkID.get(player);
					
					if (args.length == 3 ){
						util.landSetToggle(player, id, args[1].toString(), args[2].toString());
						return true;
					} else {
						player.sendMessage(failColor + "Invalid arguments specified.");
						return true;	
					}

				
				// plot add
				} else if (args[0].equalsIgnoreCase("add")) {
					util.landAddPlayer(player, args[1]);
					return true;

				// plot add to all
				} else if (args[0].equalsIgnoreCase("addtoall")) {
					util.landAddPlayerToAll(player, args[1]);
					return true;
					
					
				// plot remove
				} else if (args[0].equalsIgnoreCase("remove")) {
					util.landRemovePlayer(player, args[1]);
					return true;
					
				// plot disband 
				} else if (args[0].equalsIgnoreCase("disband")) {
					util.landDisband(player);
					return true;

				} else if (args[0].equalsIgnoreCase("disbandall")) {
					if (args.length == 2) { 
						util.landDisbandAllByOwnerName(player, args[1]);
						return true;
					}
					
				// change the owner of a plot
				} else if (args[0].equalsIgnoreCase("changeowner")) {
					if (args.length < 2) {
						player.sendMessage(failColor + "Invalid arguments specified.");
						return true;
					} else {						
						util.landChangeOwner(player, args[1].toString());
						return true;
					}
				
					
				// plot list page
				} else if (args[0].equalsIgnoreCase("list")) {

					// calling player list
					if (args.length == 1) {
						player.sendMessage(util.landListByPlayer(player, 1));
						return true;

					// calling player list with page
					} else if (args.length == 2 && this.isInt(args[1].toString())) {
						int page = Integer.parseInt(args[1]);
						player.sendMessage(util.landListByPlayer(player, page));
						return true;
						
					// playername list without page
					} else if (args.length == 2 && !this.isInt(args[1].toString())) {
						player.sendMessage(util.landListByPlayerName(player, args[1].toString(), 1));
						return true;

					// player list with page
					} else if (args.length == 3 && this.isInt(args[2].toString())) {
						int page = Integer.parseInt(args[2]);						
						player.sendMessage(util.landListByPlayerName(player, args[1].toString(), page));						
						return true;
					}
					
				// list groups a plot belongs to
				} else if (args[0].equalsIgnoreCase("listgroups") || args[0].equalsIgnoreCase("lg")) {
					
					String id = ChunkID.get(player);		
					int page = 1;
					
					if (args.length == 2 && this.isInt(args[1].toString())) 
						page = Integer.parseInt(args[1]);
					
					player.sendMessage(util.listLandGroups(player, id, page));
					return true;

					
				// list members in a plot
				} else if (args[0].equalsIgnoreCase("listmembers") || args[0].equalsIgnoreCase("lm")) {
					String id = ChunkID.get(player);		
					int page = 1;
					
					if (args.length == 2 && this.isInt(args[1].toString())) 
						page = Integer.parseInt(args[1]);
					
					player.sendMessage(util.landListPlayers(player, id, page));
					return true;
					
				// list all players with access
				} else if (args[0].equalsIgnoreCase("listaccess") || args[0].equalsIgnoreCase("la")) {

					int page = 1;
					
					if (args.length==2 && isInt(args[1].toString())) {
						page = Integer.parseInt(args[1]);
					} 
					
					player.sendMessage(util.landListAccess(player, page));
					return true;
					
				// see how a single player has access
				} else if (args[0].equalsIgnoreCase("checkaccess") || args[0].equalsIgnoreCase("ca")) {
	
					int page = 1;
					
					if (args.length==3 && isInt(args[2].toString())) {
						page = Integer.parseInt(args[2]);
					} 
					
					util.landCheckAccess(player, args[1].toString(), page);	
					return true;
					
					
				// plot buy
				} else if (args[0].equalsIgnoreCase("purchase") || 
						   args[0].equalsIgnoreCase("p") || 
						   args[0].equalsIgnoreCase("buy") || 
						   args[0].equalsIgnoreCase("b")) {
					return util.landBuy(player, price);
					
				// plot sell to server
				} else if (args[0].equalsIgnoreCase("sell") || 
						   args[0].equalsIgnoreCase("s")) {
					
					if (args.length == 3) {
							util.landSell(player, args[1].toString(), args[2].toString());
							return true;
						} else {
							util.landSell(player, price * sellBackMultiplier);
							return true;
						}
					
				// plot map
				} else if (args[0].equalsIgnoreCase("map")) {
					player.sendMessage(util.buildMap(player));
					return true;
					
				// plot who
				} else if (args[0].equalsIgnoreCase("who")) {
					String id = ChunkID.get(player);
					if (chunks.containsKey(id)) {
						player.sendMessage(String.format("This plot is owned by %s",chunks.get(id).getOwner()));
					} else {
						player.sendMessage(String.format("This plot is unowned."));				
					}
		
					return true;
					
				// plot help
				} else if (args[0].equalsIgnoreCase("help")) {
					return false;
					
				// plot claim for server
				} else if (args[0].equalsIgnoreCase("server")) {
					return util.landClaimForServer(player);
					
				// plot price
				} else if (args[0].equalsIgnoreCase("price")) {
					player.sendMessage(String.format("%.2f %s purchase price %.2f %s sell price",
														price,
														econ.currencyNamePlural(),
														price * sellBackMultiplier,
														econ.currencyNamePlural()));
					return true;
					
				}
			}	
		}

		return false;
	}

	@Override
	public void onDisable() {
		this.getServer().getScheduler().cancelTasks(this);
		save();
		log.info(this.getName() + " disabled.");
	}

	public void save() {
		try {
			log.info("Saving plot data...");
			File plotFile = new File(plotDataPath);

			if (plotFile.exists() || plotFile.createNewFile()) {
				ObjectOutputStream stream = null;
				try {
					stream= new ObjectOutputStream(new FileOutputStream(plotFile));
					stream.writeObject(chunks);
				}
				finally {
					if (stream != null)
						stream.close();
				}
				log.info("Plot data saved");
			} else
				log.info("Plot data save failed.");
		} catch (Exception e) {
			log.info("Plot data save failed. Check stderr for stacktrace.");
			e.printStackTrace();
		}
		
		try {
			log.info("Saving group data...");
			File groupFile = new File(groupDataPath);

			if (groupFile.exists() || groupFile.createNewFile()) {
				ObjectOutputStream stream = null;
				try {
					stream= new ObjectOutputStream(new FileOutputStream(groupFile));
					stream.writeObject(groups);
				}
				finally {
					if (stream != null)
						stream.close();
				}
				log.info("Group data saved");
			} else
				log.info("Group data save failed.");
		} catch (Exception e) {
			log.info("Group data save failed. Check stderr for stacktrace.");
			e.printStackTrace();
		}		
		
	}

	@SuppressWarnings("unchecked")
	public void load() throws IOException, ClassNotFoundException {
		log.info("Data loading.");
		
		File plotFile = new File(plotDataPath);

		if (plotFile.exists()) {
			ObjectInputStream stream = null;

			try {
				stream = new ObjectInputStream(new FileInputStream(plotFile));

				chunks = (HashMap<String, Land>) stream.readObject();
			} finally {
				if (stream != null)
					stream.close();
			}
		}
		
		File groupFile = new File(groupDataPath);

		if (groupFile.exists()) {
			ObjectInputStream stream = null;

			try {
				stream = new ObjectInputStream(new FileInputStream(groupFile));

				groups = (HashMap<String, LandGroup>) stream.readObject();
			} finally {
				if (stream != null)
					stream.close();
			}
		}		
		
		log.info("Data loaded.");
	}
	
	public void initialConverts() {
		
		if (!groups.containsKey("Server")) {
			groups.put("Server", new LandGroup("Server", "Server"));
			
			LandGroup group = groups.get("Server");
			
			for (Land land : chunks.values()) {
				if (land.getOwner().toString().equalsIgnoreCase("Server") && !group.getLands().contains(land.getChunkID())) {
					group.addLand(land.getChunkID());
				}
			}
			
		}
		
	}
	

	public void loadConfig() {
		this.reloadConfig();

		price = this.getConfig().getDouble("price");
		markPlot = this.getConfig().getBoolean("markPlot");
		listPageSize = this.getConfig().getInt("listPageSize");
		sellBackMultiplier = this.getConfig().getDouble("sellBackMultiplier");
		
		buildCommandUsage();
	}

	private boolean setupEcon() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			econ = economyProvider.getProvider();
		}

		return (econ != null);
	}
	
	public boolean isInt(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}
	
	public String helpBuilder(String command, String description){
		StringBuilder builder = new StringBuilder(ChatColor.GREEN + command + " - " + ChatColor.WHITE + description  + "\n");
		return builder.toString();
	}
	
	
}