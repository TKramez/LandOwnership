package com.tkramez.landownership;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ChunkID {
	
	public static String get(Block block) {
		return get(block.getLocation());
	}
	
	public static String get(Player player) {
		return get(player.getLocation());
	}
	
	public static String get(Location loc) {
		return get(loc.getChunk());
	}

	public static String get(Chunk chunk) {
		return String.format("%s,%d,%d", chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}
}