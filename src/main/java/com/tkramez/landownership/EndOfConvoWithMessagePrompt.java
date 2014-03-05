package com.tkramez.landownership;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;

public class EndOfConvoWithMessagePrompt extends MessagePrompt {

	private String message;
	
	public EndOfConvoWithMessagePrompt(String message) {
		this.message = message;
	}
	
	@Override
	public String getPromptText(ConversationContext context) {
		return message;
	}

	@Override
	protected Prompt getNextPrompt(ConversationContext context) {
		return Prompt.END_OF_CONVERSATION;
	}

}