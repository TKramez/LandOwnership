package com.tkramez.landownership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

import net.milkbowl.vault.economy.Economy;

public class LandUtils {
	
	private LandOwnership plugin;
	private Economy econ;
	private HashMap<String, Land> chunks;
	
	public LandUtils(LandOwnership pl) {
		plugin = pl;
		econ = plugin.getEcon();
		chunks = plugin.getChunks();
	}
	
	protected String ListLands(String name, int page) {
		page--;
		StringBuilder builder = new StringBuilder(ChatColor.GREEN + "-----List %d Plots%s----\n" + ChatColor.WHITE);
		List<String> list = new ArrayList<String>();
		for (Land chunk : chunks.values()) {
			if (chunk.isOwner(name)) {
				list.add(chunk.toString());
			}
		}
		
		int pages = (int)Math.ceil(list.size() / (double)plugin.getListPageSize());
		
		if (pages <= 1 || page < 0)
			page = 0;
		
		for (int k = page * plugin.getListPageSize(), count = 0; k < list.size() && count < plugin.getListPageSize(); k++, count++)
			builder.append(list.get(k) + "\n");
		builder.setLength(builder.length() - 1);
		String pageProgress = String.format(" Page %d of %d", page + 1, pages);
		
		return String.format(builder.toString(), list.size(), (pages > 1 ? pageProgress : ""));
	}
	
	protected String listLands(Player player, int page) {
		return ListLands(player.getName(), page);
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

	public boolean addPlayer(Player player, String name) {
		String id = ChunkID.get(player);
		
		if (chunks.containsKey(id) && chunks.get(id).isOwner(player.getName())) {
			chunks.get(id).addMember(name);
			player.sendMessage("Player added.");
		} else
			player.sendMessage("You don't own this plot.");
		
		return true;
	}
	
	public boolean removePlayer(Player player, String name) {
		String id = ChunkID.get(player);
		
		if (chunks.containsKey(id) && chunks.get(id).isOwner(player.getName())) {
			chunks.get(id).removeMember(name);
			player.sendMessage("Player removed.");
		} else
			player.sendMessage("You don't own this plot.");
		
		return true;
	}
	
	public String buildMap(Player player) {
		StringBuilder builder = new StringBuilder(ChatColor.RED + "--Map--\n" + ChatColor.WHITE);
		int currentX = player.getLocation().getChunk().getX(), currentZ = player.getLocation().getChunk().getZ();
		for (int k = 0, x = currentX - 3; k < 7; k++, x++) {
			for (int j = 0, z = currentZ - 3; j < 7; j++, z++) {
				String id = ChunkID.get(player.getWorld().getChunkAt(x, z));
				boolean isCurrent = x == currentX && z == currentZ;
				ChatColor color = ChatColor.GRAY;
				char letter = 'X';
				if (isCurrent)
					color = ChatColor.WHITE;
				else if (chunks.containsKey(id)) {
					if (chunks.get(id).isServerLand())
						color = ChatColor.GOLD;
					else if (chunks.get(id).isOwner(player))
						color = ChatColor.GREEN;
					else
						color = ChatColor.RED;
					letter = chunks.get(id).getOwner().charAt(0);
				}
				
				builder.append(color).append(letter);
			}
			builder.append("\n");
		}
		
		return builder.toString();
	}
	
	public boolean claimForServer(Player player) {
		String id = ChunkID.get(player);
		
		if (chunks.containsKey(id)) {
			player.sendMessage(ChatColor.RED + "This chunk is already owned.");
		} else {
			if (player.hasPermission(LandOwnership.ADMIN_PERM)) {
				player.sendMessage("This land has been claimed for the server.");
				chunks.put(id, new Land("Server", player.getLocation().getChunk()));
			} else {
				player.sendMessage(ChatColor.RED + "You don't have permission to use that command.");
			}
		}
		
		return true;
	}

	public boolean purchase(Player player, double price) {
		if (econ.getBalance(player.getName()) >= price) {
			Chunk chunk = player.getLocation().getChunk();
			if (!chunks.containsKey(ChunkID.get(chunk))) {
				if (econ.withdrawPlayer(player.getName(), price).transactionSuccess()) {
					chunks.put(ChunkID.get(chunk), new Land(player, chunk));
					player.sendMessage("This chunk is now yours!");
					markChunk(chunk);
					return true;
				} else {
					player.sendMessage("Could not withdraw money from your account.");
					return true;
				}
			} else {
				player.sendMessage("This chunk is already owned.");
				return true;
			}
		} else {
			player.sendMessage("You don't have enough money to purchase this plot.");
			return true;
		}
	}
	
	public boolean sell(Player seller, double price) {
		String id = ChunkID.get(seller);
		
		if (chunks.containsKey(id) && chunks.get(id).isOwner(seller)) {
			if (econ.depositPlayer(seller.getName(), price).transactionSuccess()) {
				chunks.remove(id);
				seller.sendMessage("Successfully sold this plot.");
			}
			else
				seller.sendMessage("Failed to sell this plot.");
		} else
			seller.sendMessage("You don't own this plot.");
		
		return true;
	}
	
	public boolean sell(final Player seller, String target, String price) {
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
					seller.sendMessage("You can't sell to yourself.");
					return true;
				}
				Conversation convo = new ConversationFactory(plugin).withFirstPrompt(new BooleanPrompt() {

					@Override
					public String getPromptText(ConversationContext context) {
						return String.format("%s wants to sell you the chunk at %s %d, %d for %.2f", seller.getName(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), askingPrice);
					}

					@Override
					protected Prompt acceptValidatedInput(ConversationContext context, boolean input) {
						EndOfConvoWithMessagePrompt message;
						if (input) {
							if (econ.getBalance(buyer.getName()) < askingPrice) {
								seller.sendMessage(String.format("%s has declined your request.", buyer.getName()));
								message = new EndOfConvoWithMessagePrompt("You don't have enough money.");
							} else {
								econ.withdrawPlayer(buyer.getName(), askingPrice);
								econ.depositPlayer(seller.getName(), askingPrice);
								chunks.get(id).setOwner(buyer.getName());
								message = new EndOfConvoWithMessagePrompt("You have accepted the request.");
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
				seller.sendMessage("That player isn't online.");
				return true;
			}
		} else {
			seller.sendMessage("You don't own this plot.");
			return true;
		}
	}
}