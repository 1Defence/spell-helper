/*
 * Copyright (c) 2026, 1Defence https://github.com/1Defence
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.spellhelper;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Spell Helper",
	description = "Prevents misclicks on limited spells(xfer,potshare,heal other,telegrab)",
	tags = {"spells","alting","misclick","cast","helper","goats","potshare","xfer","energy transfer","heal other","ps","tg","telegrab"}
)
public class SpellHelperPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SpellHelperConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	enum CAST_TYPE {GENERIC,POTSHARE_BOOST,POTSHARE_RESTORE,NONE}

	final Set<String> HOLDABLE_SPELLS_GENERIC =
			Set.of(
					"Telekinetic Grab",
					"Energy Transfer",
					"Heal Other"
			);

	final Set<String> VALID_POTIONS_RESTORE =
			Set.of(
					"Energy potion",
					"Prayer potion",
					"Restore potion",
					"Super energy",
					"Super restore",
					"Sanfew serum",
					"Stamina potion"
			);

	final Set<String> VALID_POTIONS_BOOST =
			Set.of(
					"Attack potion",
					"Strength potion",
					"Defence potion",
					"Super attack",
					"Super strength",
					"Super defence",
					"Ranging potion",
					"Magic potion",
					"Bastion potion",
					"Battlemage potion",
					"Super combat potion"
			);

	final String POTSHARE_BOOST = "Boost Potion Share";
	final String POTSHARE_RESTORE = "Stat Restore Pot Share";

	private static final Pattern POTION_REGEX = Pattern.compile("\\s*\\(\\d+\\)\\s*$");

	CAST_TYPE castHeld = CAST_TYPE.NONE;

	int remainingAttempts = 0;
	final int MAX_ATTEMPTS = 3;

	boolean configRemoveMessage,configRemoveSound;

	//Don't want to cause unnecessary support traffic in the discord.
	String USER_CONFIRMATION_STRING = "i understand";

	@Override
	protected void startUp() throws Exception
	{
		castHeld = CAST_TYPE.NONE;
		remainingAttempts = 0;
		CacheConfigs();
	}

	@Override
	protected void shutDown() throws Exception
	{

	}

	@Provides
	SpellHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpellHelperConfig.class);
	}
	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(SpellHelperConfig.CONFIG_GROUP))
		{
			return;
		}

		CacheConfigs();
	}

	public void CacheConfigs(){
		configRemoveMessage = config.removeMessage().toLowerCase().equals(USER_CONFIRMATION_STRING);
		configRemoveSound = config.removeSound().toLowerCase().equals(USER_CONFIRMATION_STRING);
	}

	/**
	 * If attempting to cast a spell but you've misclicked or clicked on the wrong item for potshares sake, it will prevent the event
	 * Limited to 3 misclicks.
	 */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked)
	{
		MenuAction action = menuOptionClicked.getMenuAction();
		String option = menuOptionClicked.getMenuOption();
		String target = Text.removeTags(menuOptionClicked.getMenuTarget());

		if(!option.equals("Cast") && !option.equals("Cancel"))
			return;

		switch (action){
			case WIDGET_TARGET:
				//initial spell selection event, store the type if valid
				if(HOLDABLE_SPELLS_GENERIC.contains(target)){
					HoldSpell(CAST_TYPE.GENERIC);
				}else if(target.equals(POTSHARE_BOOST)){
					HoldSpell(CAST_TYPE.POTSHARE_BOOST);
				}else if(target.equals(POTSHARE_RESTORE)){
					HoldSpell(CAST_TYPE.POTSHARE_RESTORE);
				}
				break;
			case WIDGET_TARGET_ON_NPC:
			case WIDGET_TARGET_ON_PLAYER:
				if(castHeld == CAST_TYPE.GENERIC)
				{
					castHeld = CAST_TYPE.NONE;
				}
				break;
			case WIDGET_TARGET_ON_WIDGET:
				if(castHeld == CAST_TYPE.POTSHARE_BOOST)
				{
					String itemName = POTION_REGEX.matcher(target.split("-> ")[1]).replaceAll("");
					if(VALID_POTIONS_BOOST.contains(itemName)){
						//valid item, allow event
						castHeld = CAST_TYPE.NONE;
					}else if(DecrementAttempts()){
						//misclick likely, consumes event
						ConsumeEvent(menuOptionClicked);
					}else{
						//out of attempts, probably trying to cancel
						castHeld = CAST_TYPE.NONE;
					}
				}else if(castHeld == CAST_TYPE.POTSHARE_RESTORE)
				{
					String itemName = POTION_REGEX.matcher(target.split("-> ")[1]).replaceAll("");
					if(VALID_POTIONS_RESTORE.contains(itemName)){
						//valid item, allow event
						castHeld = CAST_TYPE.NONE;
					}else if(DecrementAttempts()){
						//misclick likely, consumes event
						ConsumeEvent(menuOptionClicked);
					}else{
						//out of attempts, probably trying to cancel
						castHeld = CAST_TYPE.NONE;
					}
				}
				break;
			case CANCEL:
				if(castHeld != CAST_TYPE.NONE){
					if(DecrementAttempts()){
						//likely misclick, consumes event
						ConsumeEvent(menuOptionClicked);
					}else{
						//out of attempts, probably trying to cancel
						castHeld = CAST_TYPE.NONE;
					}
				}
				break;
		}

	}

	/**
	 * Tracks what type of spell we are currently attempting to cast and sets up an allowance of misclicks
	 */
	void HoldSpell(CAST_TYPE castType){
		castHeld = castType;
		remainingAttempts = MAX_ATTEMPTS;
	}

	/**
	 * After 3 misclicks the event will stop consuming as failsafe
	 */
	boolean DecrementAttempts(){
		return (--remainingAttempts) > 0;
	}

	/**
	 * Consumes a misclick while giving the user visual/audial feedback indicating such
	 */
	void ConsumeEvent(MenuOptionClicked event){
		event.consume();

		if(!configRemoveMessage)
		{
			String message = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("[Spell Helper] You missed and your click was ignored click " + remainingAttempts + " more time" + (remainingAttempts > 1 ? "s" : "") + " to cancel")
					.build();
			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(message)
					.build());
		}

		if(!configRemoveSound)
		{
			client.playSoundEffect(1041);
		}
	}

}
