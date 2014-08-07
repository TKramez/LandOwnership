package com.amity.landownership;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.bukkit.entity.Player;


public class LandGroup implements Serializable {
	
	
	private static final long serialVersionUID = -7379384219905138948L;

	private String groupName;
	private String owner;
	
	private List<String> memberChunks = new ArrayList<String>();
	private List<String> members = new ArrayList<String>();
	private EnumMap<Toggle, Boolean> toggles = new EnumMap<Toggle, Boolean>(Toggle.class);
	
	public LandGroup(String name, String groupOwner) {
		
		owner = groupOwner;
		groupName = name;
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
		
	public String getName() {
		return groupName;
	}
	
	public void setName(String name) {
		groupName = name;
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

	public boolean isServerGroup() {
		return owner.equalsIgnoreCase("server");
	}
	
	public boolean isOwner(Player player) {
		return (isServerGroup() && player.hasPermission(LandOwnership.ADMIN_PERM)) || isOwner(player.getName());
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
		
		if (members.contains(name)) {
			return members.remove(name);
		} 
		else return false;
		
	}	
	
	
	public boolean isPlotMember(String chunkID) {
		
		if (memberChunks.contains(chunkID))
			return true;
		else
			return false;
		
	}
	
	// add a chunk to the group
	public boolean addLand(String chunkID) {
		
		if (!memberChunks.contains(chunkID))
			memberChunks.add(chunkID);
		
		return true;
	}
	
	// remove a chunk from the group
	public boolean removeLand(String chunkID) {
		
		if (memberChunks.contains(chunkID))
			memberChunks.remove(chunkID);
		
		return true;
	}
	
	public List<String> getLands() {
		return memberChunks;
	}
	
	
}