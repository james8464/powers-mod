package com.powers.entity;

import com.powers.companion.LoreDialogueContext;
import com.powers.companion.LoreDialogueEngine;
import com.powers.companion.DialogueProviderRuntime;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/** Range-limited boss dialogue with identity-checked asynchronous delivery. */
final class FirstVesselDialogue {
	private FirstVesselDialogue() { }

	static void announce(FirstVessel vessel, ServerLevel level, String key) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(vessel) <= 128.0 * 128.0) {
				PowerMessages.overlay(player, Component.translatable(key));
			}
		}
	}

	static void announceLore(FirstVessel subject, ServerLevel level,
			LoreDialogueEngine dialogue, LoreDialogueContext context) {
		String fallback = dialogue.line(subject.getUUID(), context, true);
		UUID vesselId = subject.getUUID();
		var dimension = level.dimension();
		long callbackEpoch = com.powers.util.ServerCallbackGate.capture(level.getServer());
		DialogueProviderRuntime.request(vesselId, context, true, fallback).thenAccept(line ->
				com.powers.util.ServerCallbackGate.execute(callbackEpoch, server -> {
					ServerLevel currentLevel = server.getLevel(dimension);
					if (currentLevel != null && currentLevel.getEntity(vesselId) instanceof FirstVessel vessel) {
						sendLoreIfRelevant(vessel, currentLevel, line);
					}
				}));
	}

	private static void sendLoreIfRelevant(FirstVessel vessel, ServerLevel level, String line) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(vessel) <= 128.0 * 128.0) {
				PowerMessages.overlay(player, Component.literal(line));
			}
		}
	}

}
