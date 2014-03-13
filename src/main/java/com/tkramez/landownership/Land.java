package com.tkramez.landownership;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class Land implements Serializable {

	private static final long serialVersionUID = 2903036802636156439L;
	
	private String owner;
	private final String world;
	private final int x, z;
	private List<String> members = new ArrayList<String>();
	private EnumMap<Toggle, Boolean> toggles = new EnumMap<Toggle, Boolean>(Toggle.class);
	
	public Land(Player player, Chunk chunk) {
		this(player.getName(), chunk);
	}
	
	public Land(String name, Chunk chunk) {
		owner = name;
		world = chunk.getWorld().getName();
		x = chunk.getX();
		z = chunk.getZ();
		resetToggles();
	}
	
	public void setToggle(Toggle toggle, boolean value) {
		if (toggles == null)
			resetToggles();
		
		toggles.put(toggle, value);
	}
	
	public boolean getToggle(Toggle toggle) {
		if (toggles == null)
			resetToggles();
		if (!toggles.containsKey(toggle))
			toggles.put(toggle, false);
		
		return toggles.get(toggle);
	}
	
	private void resetToggles() {
		if (toggles == null)
			toggles = new EnumMap<Toggle, Boolean>(Toggle.class);
		
		for (Toggle toggle : Toggle.values())
			toggles.put(toggle, false);
	}

	public boolean isServerLand() {
		return owner.equalsIgnoreCase("server");
	}
	
	public String getChunkID() {
		return String.format("%s,%d,%d", world, x, z);
	}
	
	@Override
	public String toString() {
		return String.format("%s: %s %d, %d %d Members", owner, world, x, z, members.size());
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
		
		resetToggles();
	}

	public boolean isOwner(Player player) {
		return isOwner(player.getName());
	}
	
	public boolean isOwner(String name) {
		return name.equalsIgnoreCase(owner);
	}

	public boolean isMember(Player player) {
		return isMember(player.getName()) || (isServerLand() && player.hasPermission(LandOwnership.ADMIN_PERM));
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
}