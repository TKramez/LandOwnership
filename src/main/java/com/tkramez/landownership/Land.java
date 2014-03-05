package com.tkramez.landownership;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class Land implements Serializable {

	private static final long serialVersionUID = 2903036802636156439L;
	
	private String owner;
	private final String world;
	private final int x, z;
	private List<String> members = new ArrayList<String>();
	private boolean isPublic = false, isPvpEnabled = false, areExplosionsEnabled = false;
	
	public Land(Player player, Chunk chunk) {
		this(player.getName(), chunk);
	}
	
	public Land(String name, Chunk chunk) {
		owner = name;
		world = chunk.getWorld().getName();
		x = chunk.getX();
		z = chunk.getZ();
	}
	
	public boolean isServerLand() {
		return owner.equalsIgnoreCase("server");
	}
	
	public String getChunkID() {
		return String.format("%s,%d,%d", world, x, z);
	}
	
	@Override
	public String toString() {
		return String.format("%s: %s %d, %d %d Members %s", owner, world, x, z, members.size(), isPublic ? "Public" : "Non-Public");
	}
	
	public String getOwner() {
		return owner;
	}
	
	public void setOwner(Player player) {
		setOwner(player.getName());
	}
	
	public void setOwner(String name) {
		owner = name;
		members.clear();
		isPublic = false;
		isPvpEnabled = false;
		areExplosionsEnabled = false;
	}
	
	public final boolean isPvpEnabled() {
		return isPvpEnabled;
	}

	public final void setPvpEnabled(boolean isPvpEnabled) {
		this.isPvpEnabled = isPvpEnabled;
	}

	public final boolean isAreExplosionsEnabled() {
		return areExplosionsEnabled;
	}

	public final void setAreExplosionsEnabled(boolean areExplosionsEnabled) {
		this.areExplosionsEnabled = areExplosionsEnabled;
	}

	public boolean isOwner(Player player) {
		return isOwner(player.getName());
	}
	
	public boolean isOwner(String name) {
		return name.equalsIgnoreCase(owner);
	}

	public boolean isMember(Player player) {
		return isMember(player.getName());
	}
	
	public boolean isMember(String name) {
		return isOwner(name) || members.contains(name);
	}
	
	public void addMember(Player player) {
		addMember(player.getName());
	}

	public void addMember(String name) {
		if (!members.contains(name))
			members.add(name);
	}
	
	public boolean removeMember(Player player) {
		return removeMember(player.getName());
	}

	public boolean removeMember(String name) {
		return members.remove(name);
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
}