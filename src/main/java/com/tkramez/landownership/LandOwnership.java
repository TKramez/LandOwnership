package com.tkramez.landownership;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	
	private Logger log;
	private HashMap<String, Land> chunks = new HashMap<String, Land>();
	private Economy econ;
	private double price;
	private List<String> purchaseCommand;
	private List<String> sellCommand;
	private boolean markPlot;
	private int listPageSize;
	private double sellBackMultiplier;
	private String dataPath;
	private LandUtils util;
	
	public HashMap<String, Land> getChunks() {
		return chunks;
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
			dataPath = this.getDataFolder().getAbsolutePath() + File.separator + "LandOwnershipData.dat";
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
			
			new LandOwnershipListener(this);

			log.info(this.getName() + " enabled.");
		}
	}
	
	private void buildCommandUsage() {
		PluginCommand command = this.getCommand("plot");
		
		StringBuilder builder = new StringBuilder(ChatColor.GREEN + "Plot Commands " + this.getDescription().getVersion() + ChatColor.WHITE + "\n");
		builder.append("/plot [");
		
		for (String cmd : purchaseCommand) {
			builder.append(cmd).append(", ");
		}
		
		builder.setLength(builder.length() - 2);
		builder.append("] - Attempts to purchase the current plot.\n").append("/plot [");
		for (String cmd : sellCommand) {
			builder.append(cmd).append(", ");
		}
		
		builder.setLength(builder.length() - 2);
		builder.append("] <buyer> <price> - Attempts to sell the current plot. If no buyer or price then the plot is sold back to the server.\n");
		builder.append("/plot price - Tells you the price of a plot of land.\n");
		builder.append("/plot add <name> - Attempts to add the player to the current plot.\n");
		builder.append("/plot remove <name> - Attempts to remove the player from the current plot.\n");
		builder.append("/plot map - Shows a map of plots.\n");
		builder.append("/plot list <page> - Lists the plots that you own.\n");
		builder.append("/plot who - Tells you who owns the plot you are on.\n");
		builder.append("/plot server - Claims the current plot for the server. Admin only.");
		
		command.setUsage(builder.toString());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equals("plot")) {
			if (sender instanceof Player) {
				final Player player = (Player) sender;
				if (args.length == 3) {
					if (sellCommand.contains(args[0])) {
						return util.sell(player, args[1], args[2]);
					} else if (args[0].equals("set")) {
						String id = ChunkID.get(player);

						if (chunks.containsKey(id) && chunks.get(id).isOwner(player)) {
							boolean set;
							
							try {
								set = Boolean.parseBoolean(args[2]);
							} catch (Exception e) {
								player.sendMessage("That is not a valid option.");
								return true;
							}

							Toggle toggle;
							try {
								toggle = Toggle.getByName(args[1]);
							} catch (Exception e) {
								player.sendMessage("That is not a valid toggle.");
								return true;
							}

							chunks.get(id).setToggle(toggle, set);

							player.sendMessage(args[1] + " is now " + (set ? "enabled" : "disabled") + " for this plot.");
							return true;
						} else {
							player.sendMessage("You don't own this plot.");
							return true;
						}
					}
				} else if (args.length == 2) {
					if (args[0].equalsIgnoreCase("add")) {
						return util.addPlayer(player, args[1]);
					} else if (args[0].equalsIgnoreCase("remove")) {
						return util.removePlayer(player, args[1]);
					} else if (args[0].equalsIgnoreCase("list")) {
						try {
							int page = Integer.parseInt(args[1]);
							player.sendMessage(util.listLands(player, page));
						} catch (Exception e) {
							player.sendMessage("Invalid number.");
						}
						
						return true;
					}
				}else if (args.length == 1) {
					if (purchaseCommand.contains(args[0])) {
						return util.purchase(player, price);
					} else if (sellCommand.contains(args[0])) {
						return util.sell(player, price * sellBackMultiplier);
					} else if (args[0].equalsIgnoreCase("map")) {
						player.sendMessage(util.buildMap(player));
						return true;
					} else if (args[0].equalsIgnoreCase("who")) {
						String id = ChunkID.get(player);
						String name = "Nobody";
						if (chunks.containsKey(id)) {
							name = chunks.get(id).getOwner();
						}

						player.sendMessage(name);
						return true;
					} else if (args[0].equalsIgnoreCase("list")) {
						player.sendMessage(util.listLands(player, 1));
						return true;
					} else if (args[0].equalsIgnoreCase("help")) {
						return false;
					} else if (args[0].equalsIgnoreCase("server")) {
						return util.claimForServer(player);
					} else if (args[0].equalsIgnoreCase("price")) {
						player.sendMessage(String.format("%.2f %s purchase price %.2f %s sell price",
															price,
															econ.currencyNamePlural(),
															price * sellBackMultiplier,
															econ.currencyNamePlural()));
						return true;
					} else if (args[0].equalsIgnoreCase("set")) {
						String id = ChunkID.get(player);
						
						if (chunks.containsKey(id) && chunks.get(id).isOwner(player)) {
							StringBuilder builder = new StringBuilder(ChatColor.GREEN + "---Toggles---\n" + ChatColor.WHITE);
							
							for (Toggle toggle : Toggle.values()) {
								builder.append(String.format("%s: %b\n", toggle, chunks.get(id).getToggle(toggle)));
							}
							
							player.sendMessage(builder.toString());
							return true;
						} else {
							player.sendMessage("You don't own this plot.");
							return true;
						}
					}
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
			log.info("Saving data");
			File file = new File(dataPath);

			if (file.exists() || file.createNewFile()) {
				ObjectOutputStream stream = null;
				try {
					stream= new ObjectOutputStream(new FileOutputStream(file));
					stream.writeObject(chunks);
				}
				finally {
					if (stream != null)
						stream.close();
				}
				log.info("Data saved");
			} else
				log.info("Save failed.");
		} catch (Exception e) {
			log.info("Save failed. Check stderr for stacktrace.");
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void load() throws IOException, ClassNotFoundException {
		log.info("Data loading.");
		File file = new File(dataPath);

		if (file.exists()) {
			ObjectInputStream stream = null;

			try {
				stream = new ObjectInputStream(new FileInputStream(file));

				chunks = (HashMap<String, Land>) stream.readObject();
			} finally {
				if (stream != null)
					stream.close();
			}
		}
		
		log.info("Data loaded.");
	}

	public void loadConfig() {
		this.reloadConfig();

		price = this.getConfig().getDouble("price");
		purchaseCommand = this.getConfig().getStringList("purchase");
		sellCommand = this.getConfig().getStringList("sell");
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
}