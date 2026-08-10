package com.powers.player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static com.powers.player.PlayerPowerAttachments.SKILL_DEEDS;

/** Copy-on-write typed adapter for normal mastery counters. */
final class SkillDeedStore {
	private SkillDeedStore() {
	}

	static Map<SkillDeed, Integer> read(AttachmentTarget target) {
		EnumMap<SkillDeed, Integer> result = new EnumMap<>(SkillDeed.class);
		for (var entry : target.getAttachedOrElse(SKILL_DEEDS, Map.of()).entrySet()) {
			SkillDeed deed = SkillDeed.fromKey(entry.getKey());
			if (deed != null && entry.getValue() > 0) result.put(deed, entry.getValue());
		}
		return Map.copyOf(result);
	}

	static Map<SkillDeed, Integer> increment(AttachmentTarget target, SkillDeed deed) {
		Map<String, Integer> updated = new HashMap<>(target.getAttachedOrElse(SKILL_DEEDS, Map.of()));
		updated.compute(deed.key(), (key, current) -> current == null || current < 0 ? 1
				: current == Integer.MAX_VALUE ? Integer.MAX_VALUE : current + 1);
		target.setAttached(SKILL_DEEDS, updated);
		return read(target);
	}
}
