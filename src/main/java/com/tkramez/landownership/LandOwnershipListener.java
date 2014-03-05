package com.tkramez.landownership;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class LandOwnershipListener implements Listener {

	private final LandOwnership plugin;
	private Logger log;
	private HashMap<String, Land> chunks;
	
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
		String id = ChunkID.get(event.getBlock().getChunk());
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
			String id = ChunkID.get(event.getClickedBlock().getChunk());
			if (chunks.containsKey(id)) {
				if (!chunks.get(id).isMember(player) && !chunks.get(id).isPublic()) {
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
		String id = ChunkID.get(player.getLocation().getChunk());
		
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
		String id = ChunkID.get(event.getBlockClicked().getChunk());
		
		if (chunks.containsKey(id) && !chunks.get(id).isMember(player)) {
			player.sendMessage(ChatColor.RED + "You can't do that here!");
			event.setCancelled(true);
		}
	}
}