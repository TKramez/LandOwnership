package com.amity.landownership;

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
	private String plotName;
	
	public Land(Player player, Chunk chunk) {
		this(player.getName(), chunk);
	}
	
	public Land(String name, Chunk chunk) {
		owner = name;
		world = chunk.getWorld().getName();
		x = chunk.getX();
		z = chunk.getZ();
		resetToggles();
		plotName = "";
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
		return String.format("ID: [%s,%d,%d], Members: [%d]", world, x, z, members.size());
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
	
	public String getPlotName() {
		if (plotName == "")
			return "Unnamed";
		else
			return plotName;
	}
	
	public void setPlotName(String name)	{
		plotName = name;
	}

	public boolean isOwner(Player player) {
		return (isServerLand() && player.hasPermission(LandOwnership.ADMIN_PERM)) || isOwner(player.getName());
	}
	
	public boolean isOwner(String name) {
		return name.equalsIgnoreCase(owner);
	}

	public boolean isMember(Player player) {
		return isMember(player.getName()) || isOwner(player);
	}
	
	public boolean isMember(String name) {
		return members.contains(name);
	}
	
	public List<String> getMembers() {
		return members;
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