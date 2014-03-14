package com.tkramez.landownership;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class LandOwnershipListener implements Listener {

	private final LandOwnership plugin;
	private Logger log;
	private HashMap<String, Land> chunks;
	private HashMap<String, String> currentChunkOwner = new HashMap<String, String>();
	
	public LandOwnershipListener(LandOwnership pl) {
		plugin = pl;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		chunks = plugin.getChunks();
		
		log = plugin.getLogger();
		
		log.info("LandOwnershipListener enabled.");
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		String id = ChunkID.get(event.getBlock());
		if (chunks.containsKey(id) && !chunks.get(id).isMember(player)) {
			player.sendMessage(ChatColor.RED + "You can't build here.");
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteractEvent(PlayerInteractEvent event) {
		switch (event.getAction()) {
		case RIGHT_CLICK_BLOCK:
			Player player = event.getPlayer();
			String id = ChunkID.get(event.getClickedBlock());
			if (chunks.containsKey(id)) {
				if (!chunks.get(id).isMember(player) && !chunks.get(id).getToggle(Toggle.Public)) {
					player.sendMessage(ChatColor.RED + "You can't use that.");
					event.setCancelled(true);
				}
			}
			break;
			default:
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		String id = ChunkID.get(player);
		
		if (chunks.containsKey(id) && !chunks.get(id).isMember(player)) {
			player.sendMessage(ChatColor.RED + "You can't break here.");
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		onPlayerUseBucket(event);
	}
	
	@EventHandler
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		onPlayerUseBucket(event);
	}
	
	private void onPlayerUseBucket(PlayerBucketEvent event) {
		Player player = event.getPlayer();
		String id = ChunkID.get(event.getBlockClicked());
		
		if (chunks.containsKey(id) && !chunks.get(id).isMember(player)) {
			player.sendMessage(ChatColor.RED + "You can't do that here!");
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onExplosions(EntityExplodeEvent event) {
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();
			String id = ChunkID.get(block);
			if (chunks.containsKey(id)) {
				switch (event.getEntityType()) {
				case CREEPER:
					if (!chunks.get(id).getToggle(Toggle.CreeperExplosions))
						iter.remove();
					break;
				case GHAST:
					if (!chunks.get(id).getToggle(Toggle.GhastExplosions))
						iter.remove();
					break;
				case MINECART_TNT:
				case PRIMED_TNT:
					if (!chunks.get(id).getToggle(Toggle.TntExplosions))
						iter.remove();
					break;
				case WITHER:
					if (!chunks.get(id).getToggle(Toggle.WitherExplosions))
						iter.remove();
					break;
				default:
				}
			}
		}
	}
	
	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		String id = ChunkID.get(event.getLocation());
		if (chunks.containsKey(id) && !chunks.get(id).getToggle(Toggle.MobSpawning))
			event.setCancelled(true);
	}
	
	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		String id = ChunkID.get(event.getBlock());
		
		if (chunks.containsKey(id) && !chunks.get(id).getToggle(Toggle.EndermanPickup)) {
			if (event.getEntityType() == EntityType.ENDERMAN)
				event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
			Player defender = (Player) event.getEntity();
			Player attacker = (Player) event.getDamager();
			
			String id1 = ChunkID.get(defender);
			String id2 = ChunkID.get(attacker);
			
			if ((chunks.containsKey(id1) && !chunks.get(id1).getToggle(Toggle.Pvp)) || (chunks.containsKey(id2) && !chunks.get(id2).getToggle(Toggle.Pvp))) {
				String message = ChatColor.RED + "PVP is disabled in this plot.";
				defender.sendMessage(message);
				attacker.sendMessage(message);
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onBlockFromTo(BlockFromToEvent event) {
		if (event.getBlock().getType() == Material.STATIONARY_WATER || event.getBlock().getType() == Material.STATIONARY_LAVA) {
			Toggle toggle = event.getBlock().getType() == Material.STATIONARY_WATER ? Toggle.WaterFlow : Toggle.LavaFlow;
			String id1 = ChunkID.get(event.getBlock());
			String id2 = ChunkID.get(event.getToBlock());
			
			if ((chunks.containsKey(id1) && !chunks.get(id1).getToggle(toggle)) || (chunks.containsKey(id2) && !chunks.get(id2).getToggle(toggle)))
				event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onBlockSpread(BlockSpreadEvent event) {
		if (event.getSource().getType() == Material.FIRE) {
			String id = ChunkID.get(event.getBlock());
			
			if (chunks.containsKey(id) && !chunks.get(id).getToggle(Toggle.FireSpread))
				event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.getFrom().getChunk() != event.getTo().getChunk()) {
			Player player = event.getPlayer();
			String id = ChunkID.get(event.getTo());
			String ownerName = "Wild";
			if (!currentChunkOwner.containsKey(player.getName()))
				currentChunkOwner.put(player.getName(), ownerName);
			
			if (chunks.containsKey(id))
				ownerName = chunks.get(id).getOwner();
			
			if (!ownerName.equals(currentChunkOwner.get(player.getName()))) {
				currentChunkOwner.put(player.getName(), ownerName);
				if (ownerName.equals("Wild"))
					player.sendMessage("You have crossed into the wild!");
				else if (chunks.get(id).isServerLand())
					player.sendMessage("You have crossed into server land!");
				else
					player.sendMessage(String.format("You have crossed into land owned by %s!", ownerName));
			}
		}
	}
}