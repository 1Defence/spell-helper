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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("spellhelper")
public interface SpellHelperConfig extends Config
{
    String CONFIG_GROUP = "spellhelper";
    @ConfigItem(
            keyName = "removeMessage",
            name = "Remove Message(read)",
            description = "This plugin consumes up to 2 misclicks of valid spells (potshare,heal other,spec xfer,telegrab) to prevent unintentional user-confusion on spells not going through set this field to \"i understand\" without quotes to remove the game message",
            position = 0
    )
    default String removeMessage() { return ""; }

    @ConfigItem(
            keyName = "removeSound",
            name = "Remove Sound(read)",
            description = "This plugin consumes up to 2 misclicks of valid spells (potshare,heal other,spec xfer,telegrab) to prevent unintentional user-confusion on spells not going through set this field to \"i understand\" without quotes to remove the game sound",
            position = 0
    )
    default String removeSound() { return ""; }
}
