package com.powers.companion;

import com.powers.knowledge.KnowledgeAnswer;
import com.powers.knowledge.KnowledgeService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/** Owns Shadow conversation persistence and private/global message delivery. */
final class ShadowCompanionMessaging {
	private ShadowCompanionMessaging() {
	}

	static void answer(ServerPlayer owner, String question) {
		KnowledgeService.answerAsync(owner, question).thenAccept(answer ->
				owner.level().getServer().execute(() -> {
					if (owner.connection == null || owner.isRemoved()
							|| !PrivateCompanionManager.requested(owner.getUUID())) return;
					replyAndRemember(owner, question, spokenAnswer(answer));
				}));
	}

	static void rememberFailure(ServerPlayer owner, String reason) {
		ShadowCompanionStore.update(owner, state -> state.withMemory(
				state.memory().rememberFailure(reason)));
	}

	static void replyAndRemember(ServerPlayer owner, String request, String line) {
		ShadowCompanionStore.update(owner, state -> state.withMemory(
				state.memory().remember(request, line)));
		reply(owner, line);
	}

	static void reply(ServerPlayer owner, String line) {
		List<UUID> online = owner.level().getServer().getPlayerList().getPlayers().stream()
				.map(ServerPlayer::getUUID).toList();
		for (UUID id : PrivateCompanionRules.recipients(owner.getUUID(), online,
				PrivateCompanionManager.isRevealed(owner.getUUID()))) {
			ServerPlayer recipient = owner.level().getServer().getPlayerList().getPlayer(id);
			if (recipient != null) sendReply(recipient, owner, line);
		}
	}

	static void replyPrivate(ServerPlayer owner, String line) {
		sendReply(owner, owner, line);
	}

	static void broadcastAddress(ServerPlayer owner, String line) {
		Component message = Component.literal("<" + owner.getScoreboardName() + "> shadow, " + line)
				.withStyle(ChatFormatting.GRAY);
		owner.level().getServer().getPlayerList().broadcastSystemMessage(message, false);
	}

	private static void sendReply(ServerPlayer recipient, ServerPlayer owner, String line) {
		Component prefix = Component.literal("Shadow of " + owner.getScoreboardName() + ": ")
				.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
		recipient.sendSystemMessage(prefix.copy().append(
				Component.literal(line).withStyle(ChatFormatting.GRAY)));
	}

	private static String spokenAnswer(KnowledgeAnswer answer) {
		String text = answer.answer().strip();
		return text.isEmpty() ? "That truth has not yet left a trace I can verify." : text;
	}
}
