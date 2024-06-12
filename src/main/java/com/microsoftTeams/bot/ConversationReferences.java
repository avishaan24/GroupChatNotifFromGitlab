package com.microsoftTeams.bot;

import com.microsoft.bot.schema.ConversationReference;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Map of ConversationReference object the bot handling.
 *
 * @see NotifyController
 * @see GroupChatGitlabBot
 */
public class ConversationReferences extends ConcurrentHashMap<String, List<ConversationReference>> {
}
