package com.amity.landownership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.conversations.BooleanPrompt;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.PluginNameConversationPrefix;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;


import net.milkbowl.vault.economy.Economy;

public class LandUtils {
	
	private LandOwnership plugin;
	private Economy econ;
	private HashMap<String, Land> chunks;
	private HashMap<String, LandGroup> groups;
	private HashMap<String, List<String>> groupLookup;	
	private ChatColor successColor;
	private ChatColor failColor;
	private ChatColor parmColor;
	
	
	
	public LandUtils(LandOwnership pl) {
		plugin = pl;
		econ = plugin.getEcon();
		chunks = plugin.getChunks();
		groups = plugin.getGroups();
		groupLookup = plugin.getGroupLookup();
		successColor = LandOwnership.successColor;
		failColor = LandOwnership.failColor;
		parmColor = LandOwnership.parmColor;
	}
	
	//************************************
	//   plot utilities 
	//************************************	
	
	protected String landList(String name, int page) {

		List<String> list = new ArrayList<String>();
		for (Land chunk : chunks.values()) {
			if (chunk.isOwner(name)) {
				list.add(chunk.toString());
			}
		}

		String header = formatHeader(String.format("%d plots owned by %s", list.size(), name));
		return this.pageFormat(header, list, page);
		
	}
	
	protected String landListByPlayerName(Player callingPlayer, String playerName, int page) {
		if (!this.isAuth(callingPlayer, "AMR")) {
			return failColor + "You do not have access to list plots by playername.";
		}

		return landList(playerName, page);
	
	}
	
	protected String landListByPlayer(Player player, int page) {
		return landList(player.getName(), page);
	}
	
	protected String listLandGroups(Player callingPlayer, String chunkID, int page) {
		
		if (!chunks.get(chunkID).isOwner(callingPlayer) && !this.isAuth(callingPlayer, "AMR")) {
			return failColor + "You do not have access to list groups for this plot.";
		}
		
		List<String> list = new ArrayList<String>();
		for (LandGroup group : groups.values()) {
			if (group.getLands().contains(chunkID)) {
				list.add(group.getName());
			}
		}

		String header = formatHeader(String.format("Plot is in %d groups", list.size()));			
		return this.pageFormat(header, list, page);		
	
	}	

	public String listAbsentPlotOwners(Player callingPlayer, int page) {
		
		if (!this.isAuth(callingPlayer, "AM")) {
			return failColor + "You do not have access to list absent plot owners.";
		}		
		
		long baseTime = System.currentTimeMillis();		
		List<String> list = new ArrayList<String>();
		List<String> ownerList =  new ArrayList<String>(); 
		
		for (Land land : chunks.values()) {
	
			String owner = land.getOwner();
			if (!ownerList.contains(owner))
				ownerList.add(owner);
		}
		
			
		for (String owner : ownerList) {
			
			OfflinePlayer oPlayer = callingPlayer.getServer().getOfflinePlayer(owner);
			
			long playerLastSeen = oPlayer.getLastPlayed();
			
			long diffDays = (playerLastSeen - baseTime) / (24 *  60 * 60 * 1000);
			
			if (diffDays >= 30) {
				list.add(String.format("%s : %d days",owner,diffDays));
			}
			
		}
		
		
		String header = formatHeader(String.format("Absent Plot Owners ", list.size()));			
		return this.pageFormat(header, list, page);		
		
		
	}
	
	
	public boolean landDegroup(Player callingPlayer, String chunkID) {
		
		if (this.isAuth(callingPlayer, "AM") || chunks.get(chunkID).isOwner(callingPlayer)) {

			int i = 0;
			
			for (Iterator<Entry<String, LandGroup>> group = groups.entrySet().iterator(); group.hasNext();) {
				Entry<String, LandGroup> thisGroup = group.next();
				
				if (thisGroup.getValue().getLands().contains(chunkID)) {
					groupRemoveChunk(callingPlayer, thisGroup.getValue().getName());
					i++;
				}
			}
			
			callingPlayer.sendMessage(String.format(successColor + "Plot has been removed from %s groups.",i));				
			return true;
			
			
		} else {
			callingPlayer.sendMessage(failColor + "You do not have access to modify this plot's group membership.");
			return false;
		}
		
	}
	
	
	public String landListAccess(Player callingPlayer, int page) {
		

		String id = ChunkID.get(callingPlayer);
		
		if ((chunks.containsKey(id) && !chunks.get(id).isOwner(callingPlayer)) && !this.isAuth(callingPlayer, "AMR")) {		
			return failColor + "You do not have access to view this plot's membership.";
		}			
			
		List<String> list = new ArrayList<String>();		
		
		if (chunks.containsKey(id)) {
			Land chunk = chunks.get(id);
			list.addAll(chunk.getMembers());
			list.add(chunk.getOwner());
		}
		
		for (LandGroup group : groups.values()) {
			if (group.getLands().contains(id)) {
					list.addAll(group.getMembers());
					list.add(group.getOwner());
				}
			}


		HashSet<String> hs = new HashSet<String>();
		hs.addAll(list);
		list.clear();
		list.addAll(hs);
		Collections.sort(list);
		
		String header = formatHeader(String.format("%d players with access", list.size()));
		return this.pageFormat(header, list, page);		
		
	}
	
	public boolean landCheckAccess(Player callingPlayer, String checkPlayerName, int page) {

		boolean hasAccess = false;
		String id = ChunkID.get(callingPlayer);
		
		if (!chunks.get(id).isOwner(callingPlayer) && !this.isAuth(callingPlayer, "AMR")) {		
			callingPlayer.sendMessage(failColor + "You do not have access to view this plot's membership.");
			return false;
		}			
			
		List<String> list = new ArrayList<String>();
			
		if (chunks.containsKey(id)) {
			if (chunks.get(id).isMember(checkPlayerName)) {
				list.add("Plot Member");
				hasAccess = true;
			} else if (chunks.get(id).isOwner(checkPlayerName)) {
				list.add("Plot Owner");
				hasAccess = true;	
			}
		}
		
		
		for (LandGroup group : groups.values()) {
			if (group.getLands().contains(id)) {
				if (group.isMember(checkPlayerName)) {
					list.add(String.format("Group: " + group.getName()));
					hasAccess = true;
				}
			}
		}		

		if (hasAccess) {
			String header = formatHeader(String.format("%s has access via", checkPlayerName));
			callingPlayer.sendMessage(this.pageFormat(header, list, page));
			return hasAccess; 
		} else {
			callingPlayer.sendMessage(String.format("%s does not have access to this plot.",checkPlayerName));
			return false;
		}
		

	}
	
	
	private void markChunk(Chunk chunk) {
		if (plugin.getMarkPlot()) {
			for (int x = 0; x < 16; x += 15) {
				for (int z = 0; z < 16; z += 15) {
					int y;
					for (y = chunk.getWorld().getMaxHeight() - 1; chunk.getBlock(x, y, z).getType() == Material.AIR; y--) ;
					Block block = chunk.getBlock(x, y, z);
					switch (block.getType()) {
					case WATER:
					case LAVA:
					case STATIONARY_WATER:
					case STATIONARY_LAVA:
						block.setType(Material.COBBLESTONE);
						break;
					default:
					}
					block = block.getRelative(BlockFace.UP);
					block.setType(Material.REDSTONE_TORCH_ON);
				}
			}
		}
	}

	public boolean landAddPlayer(Player player, String name) {
		String id = ChunkID.get(player);
		
		if (chunks.containsKey(id) && chunks.get(id).isOwner(player.getName())) {
			chunks.get(id).addMember(name);
			player.sendMessage("Player added.");
			return true;
		} else {
			player.sendMessage(failColor + "You don't own this plot.");
			return false;
		}

	}
	
	public boolean landAddPlayerToAll(Player player, String name) {

		int i = 0;

		for (Land chunk : chunks.values()) {
			if (chunk.isOwner(player)) {
				chunk.addMember(name);
				i++;
			}
		}			

		
		player.sendMessage(String.format(successColor + "Added [%s] to %s plots.",formatName(name),i));
		return true;

	}	
	
	public boolean landRemovePlayer(Player player, String name) {
		String id = ChunkID.get(player);
		
		if (chunks.containsKey(id) && chunks.get(id).isOwner(player.getName())) {
			chunks.get(id).removeMember(name);
			player.sendMessage("Player removed.");
		} else
			player.sendMessage(failColor + "You don't own this plot.");
		
		return true;
	}
	
	public boolean landChangeOwner(Player player, String newOwnerName) {
		String id = ChunkID.get(player);

		if (!chunks.containsKey(id)) {
			player.sendMessage(failColor + "This plot does not have an owner.");
			return false;
		}
		
		if (!this.isAuth(player, "A")) {
			player.sendMessage(failColor + "You do not have permission to change plot owners.");
			return false;			
		}
		
		Land chunk = chunks.get(id); 
		String oldOwnerName = chunk.getOwner();
		chunk.setOwner(newOwnerName);
		
		player.sendMessage(String.format(successColor + "Plot owner changed from [%s] to [%s].",formatName(oldOwnerName),formatName(newOwnerName)));
		return true;			
		
	}
	
	protected String landListPlayers(Player callingPlayer, String chunkID, int page) {
		
		if (!chunks.containsKey(chunkID))
			return "This plot is not owned.";
		
		Land chunk = chunks.get(chunkID);
		
		if (this.isAuth(callingPlayer, "AMR") || chunk.isOwner(callingPlayer)) {

			List<String> list = chunk.getMembers();
			list.add("Owner: " + chunk.getOwner());
	
			String header = formatHeader(String.format("%d members of plot", list.size()));			
			return this.pageFormat(header, list, page);

		} else {
			return failColor + "You do not have access to list members of this plot.";
		}
	}		
	
	
	public String buildMap(Player player) {
		StringBuilder builder = new StringBuilder(failColor + "--Map--\n" + successColor);
		int currentX = player.getLocation().getChunk().getX(), currentZ = player.getLocation().getChunk().getZ();
		for (int j = 0, z = currentZ - 3; j < 7; j++, z++) {
			for (int k = 0, x = currentX - 3; k < 7; k++, x++) {
				String id = ChunkID.get(player.getWorld().getChunkAt(x, z));
				boolean isCurrent = x == currentX && z == currentZ;
				ChatColor color = ChatColor.GRAY;
				char letter = 'X';
				
				if (chunks.containsKey(id)) {
					if (isCurrent)
						color = successColor;
					else if (chunks.get(id).isServerLand())
						color = ChatColor.GOLD;
					else if (chunks.get(id).isOwner(player) || chunks.get(id).isMember(player))
						color = successColor;
					else
						color = failColor;
					letter = chunks.get(id).getOwner().charAt(0);
				} else if (isCurrent)
					color = ChatColor.WHITE;
					
				
				builder.append(color).append(letter);
			}
			builder.append("\n");
		}
		
		return builder.toString();
	}
	
	public boolean landClaimForServer(Player player) {
		String id = ChunkID.get(player);
		
		if (chunks.containsKey(id)) {
			player.sendMessage(failColor + "This chunk is already owned.");
		} else {
			if (this.isAuth(player, "A")) {
				player.sendMessage(successColor + "This land has been claimed for the server.");
				chunks.put(id, new Land("Server", player.getLocation().getChunk()));
				groups.get("Server").addLand(id);
				
			} else {
				player.sendMessage(failColor + "You don't have permission to use that command.");
			}
		}
		
		return true;
	}

	public boolean landDisband(Player player) {
		
		String id = ChunkID.get(player);
		
		if (!chunks.containsKey(id)) {
			player.sendMessage(failColor + "This chunk is not owned.");
		} else {
			if (this.isAuth(player, "A")) {
				player.sendMessage(successColor + "This land has been returned to the wild.");
				chunks.remove(id);
			} else {
				player.sendMessage(failColor + "You do not have permission to use that command.");
			}
		}
		
		return true;
	}
	
	public boolean disbandAllByOwnerName(Player player, String ownerName) {
		int i = 0;
		
		if (this.isAuth(player, "A")) {

			for (Iterator<Entry<String, Land>> chunk = chunks.entrySet().iterator(); chunk.hasNext();) {
				Entry<String, Land> thisChunk = chunk.next();
				if (thisChunk.getValue().isOwner(ownerName)) {
					chunk.remove();
					i++;
				}
			}
			player.sendMessage(String.format(successColor + "The %s lands of [%s] have been returned to the wild.",i,formatName(ownerName)));

			i = 0;
			for (Iterator<Entry<String, LandGroup>> group = groups.entrySet().iterator(); group.hasNext();) {
				Entry<String, LandGroup> thisGroup = group.next();
				if (thisGroup.getValue().isOwner(ownerName)) {
					group.remove();
					i++;
				}
			}
			player.sendMessage(String.format(successColor + "The %s groups of [%s] have been disbanded.",i,formatName(ownerName)));

						
			
			
		} else {
			player.sendMessage(failColor + "You do not have permission to use that command.");
		}
		
		return true;
	}
	
	
	public String landListToggles(Player player, String chunkID) {

		if (!chunks.containsKey(chunkID)) 
			return (failColor + "This plot is not owned.");

		Land chunk = chunks.get(chunkID);

		if (!chunk.isOwner(player) && !this.isAuth(player, "AMR"))
			return (failColor + "You do not have permission to view toggles for this plot.");
		
		List<String> list = new ArrayList<String>();
		
		for (Toggle toggle : Toggle.values()) {
			list.add(String.format("%s: %b\n", toggle, chunk.getToggle(toggle)));
		}
		
		String header = formatHeader(String.format("Toggles for plot"));			
		return this.listFormat(header, list);		
		
	}
	
	
	public boolean landSetToggle(Player callingPlayer, String chunkID, String toggleName, String toggleSetting) {

		if (!chunks.containsKey(chunkID)) {
			callingPlayer.sendMessage(failColor + "This plot is not owned.");
			return false;
		}

		Land chunk = chunks.get(chunkID);

		if (!chunk.isOwner(callingPlayer) && !this.isAuth(callingPlayer, "AMR")) {
			callingPlayer.sendMessage(failColor + "You do not have permission to view toggles for this plot.");
			return false;
		}
		
		Toggle chunkToggle;
		
		try {
			chunkToggle = Toggle.getByName(toggleName);
		} catch (Exception e) {
			callingPlayer.sendMessage(failColor + "That is not a valid toggle.");
			return true;
		}		
		
		boolean setValue;
		
		if (toggleSetting.equalsIgnoreCase("true") ||
			toggleSetting.equalsIgnoreCase("t") ||	
			toggleSetting.equalsIgnoreCase("on") ||	
			toggleSetting.equalsIgnoreCase("yes"))
			setValue = true;
		else if 
			(toggleSetting.equalsIgnoreCase("false") ||
			toggleSetting.equalsIgnoreCase("f") ||	
			toggleSetting.equalsIgnoreCase("off") ||	
			toggleSetting.equalsIgnoreCase("no"))
			setValue = false;
		else {
			callingPlayer.sendMessage(failColor + "That is not a valid setting.");
			return false;
		}
		
		chunk.setToggle(chunkToggle, setValue);
		callingPlayer.sendMessage(String.format(successColor + "Set toggle [%s] to [%s] for plot.",formatName(toggleName), formatName(toggleSetting)));
		return true;
		
	}
	
	
	public boolean landBuy(Player player, double price) {
		if (econ.getBalance(player.getName()) >= price) {
			String id = ChunkID.get(player);
			Chunk chunk = player.getLocation().getChunk();
			if (!chunks.containsKey(id)) {
				if (econ.withdrawPlayer(player.getName(), price).transactionSuccess()) {
					chunks.put(id, new Land(player, chunk));
					player.sendMessage(successColor + "This chunk is now yours!");
					player.sendMessage(String.format("You have been charged %,.2f %s", price, econ.currencyNamePlural() + "."));
					player.sendMessage(String.format("Your new balance is %,.2f %s", econ.getBalance(player.getName()), econ.currencyNamePlural() + "."));
					markChunk(chunk);
					return true;
				} else {
					player.sendMessage(failColor + "Could not withdraw money from your account.");
					return true;
				}
			} else {
				player.sendMessage(failColor + "This chunk is already owned.");
				return true;
			}
		} else {
			player.sendMessage(failColor + "You don't have enough money to purchase this plot.");
			return true;
		}
	}
	
	public boolean landSell(Player seller, double price) {
		String id = ChunkID.get(seller);
		
		if (chunks.containsKey(id) && chunks.get(id).isOwner(seller)) {
			if (chunks.get(id).isServerLand())
				price = 0;
			
			if (econ.depositPlayer(seller.getName(), price).transactionSuccess()) {
				chunks.remove(id);
				seller.sendMessage(successColor + "Successfully sold this plot.");
				seller.sendMessage(String.format("You have been given %,.2f %s", price, econ.currencyNamePlural() + "."));
				seller.sendMessage(String.format("Your new balance is %,.2f %s", econ.getBalance(seller.getName()), econ.currencyNamePlural() + "."));
			}
			else
				seller.sendMessage(failColor + "Failed to sell this plot.");
		} else
			seller.sendMessage(failColor + "You don't own this plot.");
		
		return true;
	}
	
	public boolean landSell(final Player seller, String target, final String price) {
		final Chunk chunk = seller.getLocation().getChunk();
		final String id = ChunkID.get(chunk);
		final double askingPrice;
		try {
			askingPrice = Double.parseDouble(price);
		} catch (NumberFormatException e) {
			seller.sendMessage("Invalid number");
			return true;
		}

		if(chunks.containsKey(id) && chunks.get(id).isOwner(seller.getName())) {
			final Player buyer;
			if ((buyer = plugin.getServer().getPlayer(target)) != null) {
				if (buyer.getName().equals(seller.getName())) {
					seller.sendMessage(failColor + "You can't sell to yourself.");
					return true;
				}
				Conversation convo = new ConversationFactory(plugin).withFirstPrompt(new BooleanPrompt() {

					@Override
					public String getPromptText(ConversationContext context) {
						return String.format("%s wants to sell you the chunk at %s %d, %d for %,.2f", seller.getName(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), askingPrice);
					}

					@Override
					protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
						EndOfConvoWithMessagePrompt message;
						if (input) {
							if (econ.getBalance(buyer.getName()) < askingPrice) {
								seller.sendMessage(String.format(failColor + "%s has declined your request.", buyer.getName()));
								message = new EndOfConvoWithMessagePrompt(failColor + "You don't have enough money.");
							} else {
								econ.withdrawPlayer(buyer.getName(), askingPrice);
								econ.depositPlayer(seller.getName(), askingPrice);
								chunks.get(id).setOwner(buyer.getName());
								message = new EndOfConvoWithMessagePrompt(successColor + "You have purchased the plot!\n" +
										String.format("You have been charged %,.2f %s\n", price, econ.currencyNamePlural() + ".") +
										String.format("Your new balance is %,.2f %s\n", econ.getBalance(buyer.getName()), econ.currencyNamePlural() + "."));
								seller.sendMessage(successColor + "Successfully sold this plot.");
								seller.sendMessage(String.format("You have been given %,.2f %s", price, econ.currencyNamePlural() + "."));
								seller.sendMessage(String.format("Your new balance is %,.2f %s", econ.getBalance(seller.getName()), econ.currencyNamePlural() + "."));
							}
						} else {
							seller.sendMessage(String.format("%s has declined your request.", buyer.getName()));
							message = new EndOfConvoWithMessagePrompt("You have declined the request.");
						}

						return message;
					}
				}).withPrefix(new PluginNameConversationPrefix(plugin)).withModality(false).buildConversation(buyer);
				convo.begin();
				return true;
			} else {
				seller.sendMessage(failColor + "That player isn't online.");
				return true;
			}
		} else {
			seller.sendMessage(failColor + "You don't own this plot.");
			return true;
		}
	}

	// set toggle
	// list group membership
	

	//************************************
	//   group utilities 
	//************************************
	
	// list group
	// default list groups owned by player issuing command
	protected String groupList(String name, int page) {
		
		List<String> list = new ArrayList<String>();
		for (LandGroup group : groups.values()) {
			if (group.isOwner(name)) {
				list.add(group.getName());
			}
		}
		
		String header = formatHeader(String.format("%d groups owned by %s", list.size(), name));		
		return this.pageFormat(header, list, page);
	}
	
	protected String groupListByPlayer(Player callingPlayer, int page) {
		return groupList(callingPlayer.getName(), page);
	}	
	
	
	// list groups owned by specified playername
	protected String groupListByPlayerName(Player callingPlayer, String playerName, int page) {
		
		if (!callingPlayer.getName().equalsIgnoreCase(playerName) && !this.isAuth(callingPlayer, "AMR")) {
			return String.format(failColor + "You do not have permission to list groups owned by player [%s].", formatError(playerName));
		}

		return groupList(playerName, page);
		
	}
	
	// create group
	public boolean groupCreate(final Player callingPlayer, String groupName) {

		if (groups.containsKey(groupName.toUpperCase())) {
			callingPlayer.sendMessage(String.format(failColor + "The group [%s] already exists.",formatError(groupName)));
			return false;
		}

		groups.put(groupName.toUpperCase(), new LandGroup(groupName, callingPlayer.getName()));
		callingPlayer.sendMessage(String.format(successColor + "Group [%s] has been created!",formatName(groupName)));
		return true;
	
	}
	
	// disband group
	public boolean groupDisband(Player callingPlayer, String groupName) {
		
		// check exists
		if (!groups.containsKey(groupName.toUpperCase())) {
			callingPlayer.sendMessage(String.format(failColor + "Group [%s] does not exist.", formatError(groupName)));
			return false;
		}
		
		// check permission
		if (!groups.get(groupName.toUpperCase()).isOwner(callingPlayer) && !this.isAuth(callingPlayer, "AM")) {
			callingPlayer.sendMessage(String.format(failColor + "You do not have permission to disband group [%s].", formatError(groupName)));
			return false;
		}

		String gName = groups.get(groupName.toUpperCase()).getName();

		// disband group
		groups.remove(groupName.toUpperCase());
		
		callingPlayer.sendMessage(String.format(successColor + "Group [%s] has been disbanded.", formatName(gName)));
		return true;
			
	}	
	
	
	// group rename
	public boolean groupRename(Player callingPlayer, String oldGroupName, String newGroupName) {
		
		// check old group name exists
		if (!groups.containsKey(oldGroupName.toUpperCase())) {
			callingPlayer.sendMessage(String.format(failColor + "Group [%s] does not exist.", formatError(oldGroupName)));
			return false;
		}
		
		// make sure new group name doesn't exist
		if (groups.containsKey(newGroupName.toUpperCase())) {
			callingPlayer.sendMessage(String.format(failColor + "Group [%s] already exists.", formatError(newGroupName)));
			return false;			
		}
		
		LandGroup group = groups.get(oldGroupName.toUpperCase());
		
		// check permissions
		if (!group.isOwner(callingPlayer) && !this.isAuth(callingPlayer, "AM")) {
				callingPlayer.sendMessage(String.format(failColor + "You do not have permission to rename group [%s].", formatError(group.getName())));
				return false;
			}
		
		// rename group
		groups.remove(oldGroupName.toUpperCase());
		group.setName(newGroupName);
		groups.put(newGroupName.toUpperCase(), group);
		callingPlayer.sendMessage(String.format(successColor + "Group [%s] has been renamed to [%s].", formatName(oldGroupName), formatName(newGroupName)));
		return true;
		
		
	}
	
	// list chunks in a group
	protected String groupListLands(Player callingPlayer, String groupName, int page) {

		if (!groups.containsKey(groupName.toUpperCase()))
			return String.format(failColor + "Group [%s] does not exist.", formatError(groupName));
			
		LandGroup group = groups.get(groupName.toUpperCase());

		if (!group.isOwner(callingPlayer) && !this.isAuth(callingPlayer, "AMR")) 
			return String.format(failColor + "You do not have access to list the plots in group [%s].", formatError(group.getName()));

		
		List<String> list = group.getLands();
		String header = formatHeader(String.format("%d plots in group %s", list.size(), group.getName()));				
		return this.pageFormat(header, list, page);		
		
	}
	
	
	protected String groupListMembers(Player callingPlayer, String groupName, int page) {
		
		if (!groups.containsKey(groupName.toUpperCase()))
			return String.format(failColor + "Group [%s] does not exist.",formatError(groupName));
		
		LandGroup group = groups.get(groupName.toUpperCase());
		
		if (!group.isOwner(callingPlayer) && !this.isAuth(callingPlayer, "AMR"))
			return String.format(failColor + "You do not have access to list plots in group [%s].", formatError(group.getName()));
		
		List<String> list = group.getMembers();
		list.add("Owner: " + group.getOwner());
		
		String header = formatHeader(String.format("%d members in group %s", list.size(), group.getName()));							
		return this.pageFormat(header, list, page);		
		
	}	
	
	public boolean groupAddAllChunks(Player callingPlayer, String playerName, String groupName) {
		
		// group doesn't exist
		if (!groups.containsKey(groupName.toUpperCase())) {
			callingPlayer.sendMessage(String.format(failColor + "Group [%s] does not exist.", formatError(groupName)));
			return false;
		}

		LandGroup group = groups.get(groupName.toUpperCase());		
		
		// check perms
		if (!group.isOwner(playerName) && !this.isAuth(callingPlayer, "AM")) {
			callingPlayer.sendMessage(String.format(failColor + "You do not have access to add plots to group [%s].", formatError(group.getName())));
			return false;
		}
		
		int i = 0;
		
		for (Land chunk : chunks.values()) {
			if (chunk.isOwner(playerName) && !group.getLands().contains(chunk.getChunkID())) {
				group.addLand(chunk.getChunkID());
				i++;
			}
		}		

		callingPlayer.sendMessage(String.format(successColor + "Added %d plots to group %s.",i,formatName(group.getName())));
		return true;		
		
	}
	
	public boolean groupAddAllChunks(Player callingPlayer, String groupName) {

		return groupAddAllChunks(callingPlayer, callingPlayer.getName(), groupName);
		
	}
	
	// add chunk to group
	public boolean groupAddChunk(Player callingPlayer, String groupName) {
		
		// group doesn't exist
		if (!groups.containsKey(groupName.toUpperCase())) {
			callingPlayer.sendMessage(String.format(failColor + "Group [%s] does not exist.", formatError(groupName)));
			return false;
		}
			
		String id = ChunkID.get(callingPlayer);
		LandGroup group = groups.get(groupName.toUpperCase());
		
		// admins override this		
		if (!this.isAuth(callingPlayer, "A")) {
			
			// not group owner
			if (!group.isOwner(callingPlayer)) {
				callingPlayer.sendMessage(String.format(failColor + "You do not have permission to add plots to group [%s].", formatError(group.getName())));
				return false;
			
			// not plot owner
			} else if (chunks.containsKey(id) && !chunks.get(id).isOwner(callingPlayer)) {
				callingPlayer.sendMessage(String.format(failColor + "You do not have permission to add this plot to a group you do not own."));
				return false;										
			}
			
		} 

		// plot already member
		if (group.isPlotMember(id)) {
			callingPlayer.sendMessage(String.format(failColor + "Plot is already in group [%s].", formatError(group.getName())));
			return false;			
		} 
		
		
		// add the land
		group.addLand(id);
		callingPlayer.sendMessage(String.format(successColor + "Plot has been added to group [%s].",formatName(group.getName())));
		return true;
		
	}
	
	// remove chunk from group
	public boolean groupRemoveChunk(Player callingPlayer, String groupName) {
		
		if (!groups.containsKey(groupName.toUpperCase())) {
			callingPlayer.sendMessage(String.format(failColor + "Group [%s] does not exist.", formatError(groupName)));
			return true;			
		}

		String id = ChunkID.get(callingPlayer);
		LandGroup group = groups.get(groupName.toUpperCase());

		if (!group.isOwner(callingPlayer) && !this.isAuth(callingPlayer, "A")) {
			callingPlayer.sendMessage(String.format(failColor + "You do not have permission to remove plots from group [%s].", formatError(group.getName())));
			return false;
		}	
		
		if (!group.isPlotMember(id)) {
			callingPlayer.sendMessage(String.format(failColor + "Plot is not in group [%s].", formatError(group.getName())));
			return false;			
		} 


		group.removeLand(id);
		callingPlayer.sendMessage(String.format(successColor + "Plot has been removed from group [%s]!",formatName(group.getName())));
		return true;
			
		
	}
	
	public boolean groupChangeOwner(Player player, String groupName, String newOwnerName) {

		if (!groups.containsKey(groupName.toUpperCase())) {
			player.sendMessage(String.format(failColor + "Group [%s] does not exist.",formatError(groupName)));
			return false;
		}
		
		if (!this.isAuth(player, "A")) {
			player.sendMessage(failColor + "You do not have permission to change group owners.");
			return false;			
		}
		
		LandGroup group = groups.get(groupName.toUpperCase()); 
		String oldOwnerName = group.getOwner();
		group.setOwner(newOwnerName);
		
		player.sendMessage(String.format(successColor + "Group [%s] owner changed from [%s] to [%s].",formatName(group.getName()),formatName(oldOwnerName),formatName(newOwnerName)));
		return true;			
		
	}	
	
	// add player to group
	public boolean groupAddPlayer(Player callingPlayer, String groupName, String playerName) {
		
		if (!groups.containsKey(groupName.toUpperCase())) {
			callingPlayer.sendMessage(String.format(failColor + "Group [%s] does not exist.", formatError(groupName)));
			return false;			
		}

		LandGroup group = groups.get(groupName.toUpperCase());

		if (!group.isOwner(callingPlayer) && !this.isAuth(callingPlayer, "A")) {
			callingPlayer.sendMessage(String.format(failColor + "You do not have permission to add members to group [%s].", formatError(group.getName())));
			return false;
		}				
			
		if (group.isMember(playerName)) {
			callingPlayer.sendMessage(String.format(failColor + "[%s] is already a member of group [%s].", formatError(playerName), formatError(group.getName())));
			return false;	
		}
			

		group.addMember(playerName);
		callingPlayer.sendMessage(String.format(successColor + "[%s] has been added to group [%s].", formatName(playerName), formatName(group.getName())));
		return true;
		

	}
	
	
	// remove player from group
	public boolean groupRemovePlayer(Player callingPlayer, String groupName, String playerName) {
		
		if (!groups.containsKey(groupName.toUpperCase())) {
			callingPlayer.sendMessage(String.format(failColor + "Group [%s] does not exist.", formatError(groupName)));
			return false;		
		}

		LandGroup group = groups.get(groupName.toUpperCase());

		if (!group.isOwner(callingPlayer) && !this.isAuth(callingPlayer, "A")) {
			callingPlayer.sendMessage(String.format(failColor + "You do not have permission to remove members to group [%s].", formatError(group.getName())));
			return false;
		}				

		if (!group.isMember(playerName)) {
			callingPlayer.sendMessage(String.format(failColor + "[%s] is not a member of group [%s].", formatError(playerName), formatError(group.getName())));
			return false;	
		}
		
		group.removeMember(playerName);
		callingPlayer.sendMessage(String.format(successColor + "[%s] has been removed from group [%s].", formatName(playerName), formatName(group.getName())));					
		return true;

	}
	
	// sell group to player
	public boolean groupSell(final Player seller, String groupName, String target, final String price) {
	
		return false;
	}
	
	public String groupListToggles(Player callingPlayer, String groupName) {

		if (!groups.containsKey(groupName.toUpperCase())) {
			return String.format(failColor + "Group [%s] does not exist.", formatError(groupName));		
		}

		LandGroup group = groups.get(groupName.toUpperCase());

		if (!group.isOwner(callingPlayer) && !this.isAuth(callingPlayer, "A")) {
			return String.format(failColor + "You do not have permission to list toggles on group [%s].", formatError(group.getName()));
		}
		
		List<String> list = new ArrayList<String>();
		
		for (Toggle toggle : Toggle.values()) {
			list.add(String.format("%s: %b\n", toggle, group.getToggle(toggle)));
		}
		
		String header = formatHeader(String.format("Toggles for group"));			
		return this.listFormat(header, list);		
		
	}	
	
	// set toggle
	public boolean groupSetToggle(Player callingPlayer, String groupName, String toggleName, String toggleSetting) {
		
		if (!groups.containsKey(groupName.toUpperCase())) {
			callingPlayer.sendMessage(String.format(failColor + "Group [%s] does not exist.", formatError(groupName)));
			return false;		
		}

		LandGroup group = groups.get(groupName.toUpperCase());

		if (!group.isOwner(callingPlayer) && !this.isAuth(callingPlayer, "A")) {
			callingPlayer.sendMessage(String.format(failColor + "You do not have permission to set toggles on group [%s].", formatError(group.getName())));
			return false;
		}	
		
		Toggle groupToggle;
		
		try {
			groupToggle = Toggle.getByName(toggleName);
		} catch (Exception e) {
			callingPlayer.sendMessage(failColor + "That is not a valid toggle.");
			return true;
		}		
		
		boolean setValue;
		
		if (toggleSetting.equalsIgnoreCase("true") ||
			toggleSetting.equalsIgnoreCase("t") ||	
			toggleSetting.equalsIgnoreCase("on") ||	
			toggleSetting.equalsIgnoreCase("yes"))
			setValue = true;
		else if 
			(toggleSetting.equalsIgnoreCase("false") ||
			toggleSetting.equalsIgnoreCase("f") ||	
			toggleSetting.equalsIgnoreCase("off") ||	
			toggleSetting.equalsIgnoreCase("no"))
			setValue = false;
		else {
			callingPlayer.sendMessage(failColor + "That is not a valid setting.");
			return false;
		}
		
		group.setToggle(groupToggle, setValue);
		callingPlayer.sendMessage(String.format(successColor + "Set toggle [%s] to [%s] for group [%s].",formatName(toggleName), formatName(toggleSetting), formatName(group.getName())));
		return true;	
		
	}
	

	
	
	//************************************
	//   area utilities 
	//************************************
	
	// add area
	// remove area
	
	
	// misc utilities
	
	public boolean isAuth(Player player, String permSet) {
		
		if (
		   (permSet.contains("A") && player.hasPermission(LandOwnership.ADMIN_PERM)) ||
		   (permSet.contains("M") && player.hasPermission(LandOwnership.MANAGER_PERM)) ||
		   (permSet.contains("R") && player.hasPermission(LandOwnership.READER_PERM)) 
		   )
		   	return true;
		else
			return false;
	}	
	
	public String pageFormat(String header, List<String> list, int page) {
		
		StringBuilder builder = new StringBuilder();
		builder.append(header);
		
		page--;
		int pages = (int)Math.ceil(list.size() / (double)plugin.getListPageSize());
		
		if (pages <= 1 || page < 0)
			page = 0;
		
		for (int k = page * plugin.getListPageSize(), count = 0; k < list.size() && count < plugin.getListPageSize(); k++, count++)
			builder.append(list.get(k) + "\n");

		String pageProgress = String.format(" Page %d of %d", page + 1, pages);
		
		return String.format(builder.toString(), (pages > 1 ? pageProgress : ""));
		
	}
	
	public String listFormat(String header, List<String> list) {
		
		StringBuilder builder = new StringBuilder();
		builder.append(header);
		builder.append(list);
		
		return String.format(builder.toString());
		
	}	
	
	public String formatHeader(String header) {
		
		return String.format(successColor + "<==== "  + ChatColor.WHITE + header + successColor + " ====>\n" + ChatColor.WHITE);
		
	}
	

	public String formatName(String name) {
		return String.format(parmColor + name + successColor);
	}
	
	public String formatError(String name) {
		return String.format(parmColor + name + failColor);
	}
	
	
}