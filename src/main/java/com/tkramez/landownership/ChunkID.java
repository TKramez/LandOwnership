package com.tkramez.landownership;

import org.bukkit.Chunk;

public class ChunkID {

	public static String get(Chunk chunk) {
		return String.format("%s,%d,%d", chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}
}