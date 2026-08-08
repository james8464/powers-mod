package com.powers.player;

import com.powers.progression.RankGraph;
import com.powers.progression.RankGraphRegistry;
import com.powers.progression.RankProgress;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static com.powers.player.PlayerPowerAttachments.DARK_RANK_FOCUS;
import static com.powers.player.PlayerPowerAttachments.DARK_RANK_NODES;
import static com.powers.player.PlayerPowerAttachments.RANK_FOCUS;
import static com.powers.player.PlayerPowerAttachments.RANK_NODES;

/** Encapsulates persistence and legacy migration for the two rank mazes. */
final class PlayerRankState {
	private PlayerRankState() {
	}

	static RankProgress progress(AttachmentTarget target, boolean darkness, int legacyLevel) {
		AttachmentType<List<String>> nodesType = darkness ? DARK_RANK_NODES : RANK_NODES;
		AttachmentType<String> focusType = darkness ? DARK_RANK_FOCUS : RANK_FOCUS;
		List<String> completed = target.getAttachedOrElse(nodesType, List.of());
		RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
		if (!completed.isEmpty()) {
			return new RankProgress(new LinkedHashSet<>(completed), target.getAttachedOrElse(focusType, ""));
		}

		RankProgress migrated = RankProgress.migrateLegacy(graph, legacyLevel);
		store(target, darkness, migrated);
		return migrated;
	}

	static boolean unlock(AttachmentTarget target, boolean darkness, int earnedDepth, String nodeId) {
		RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
		RankProgress progress = progress(target, darkness, earnedDepth);
		if (!graph.unlockable(progress.completed(), earnedDepth).contains(nodeId)) return false;
		LinkedHashSet<String> updated = new LinkedHashSet<>(progress.completed());
		updated.add(nodeId);
		store(target, darkness, new RankProgress(updated, nodeId));
		return true;
	}

	static boolean focus(AttachmentTarget target, boolean darkness, int legacyLevel, String nodeId) {
		RankProgress progress = progress(target, darkness, legacyLevel);
		if (!progress.completed().contains(nodeId)) return false;
		target.setAttached(darkness ? DARK_RANK_FOCUS : RANK_FOCUS, nodeId);
		return true;
	}

	static void resetToLegacyPath(AttachmentTarget target, boolean darkness, int legacyLevel) {
		RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
		store(target, darkness, RankProgress.migrateLegacy(graph, legacyLevel));
	}

	private static void store(AttachmentTarget target, boolean darkness, RankProgress progress) {
		target.setAttached(darkness ? DARK_RANK_NODES : RANK_NODES, new ArrayList<>(progress.completed()));
		target.setAttached(darkness ? DARK_RANK_FOCUS : RANK_FOCUS, progress.focus());
	}
}
