package com.powers.player;

import com.powers.PowersMod;
import com.powers.network.PowersPackets;
import com.powers.progression.RankGraph;
import com.powers.progression.RankGraphRegistry;
import com.powers.progression.RankNode;
import com.powers.progression.RankProgress;
import com.powers.progression.PowerScalingService;
import com.powers.progression.RankAttributeManager;
import com.powers.util.PowerMessages;
import com.powers.fx.PowerFx;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;

/**
 * Derives rank progression from advancements and applies player identity,
 * title, attribute, range, damage, and energy consequences.
 */
public final class SkillSystem {
	public static final String DARKNESS_TAG = "darkness";
	// ten earnable ranks in each progression, plus the level-0 state a player
	// sits at before they have finished a single rank
	public static final int MAX_LEVEL = 10;
	public static final int DARKNESS_MAX_LEVEL = 10;
	// the dark realm only opens to the darkness itself: tagged players who
	// have climbed to darkness rank 5; the dark crystal and riding along with
	// a teleporting player are the only ways around it
	public static final int DARKNESS_GATE_LEVEL = 5;

	private record Tier(String name, int color) {}

	// rank name and chat color per light-ladder level. index 0 is the state
	// before the first rank is earned, so index N is exactly rank N and the
	// top rank (10) is Origin - there is one name per level, not one fewer
	private static final Tier[] TIERS = {
			new Tier("Dormant", 0xFF555555),
			new Tier("Unawakened", 0xFF777777), new Tier("Spark", 0xFFFFB74D),
			new Tier("Awakened", 0xFFFFD54F), new Tier("Adept", 0xFF4FC3F7),
			new Tier("Weaver", 0xFF4DB6AC), new Tier("Arcanist", 0xFF9575CD),
			new Tier("Luminary", 0xFFFFEE58), new Tier("Voidcaller", 0xFF5C6BC0),
			new Tier("Ascendant", 0xFF26A69A), new Tier("Origin", 0xFFFF80AB)
	};

	// the darkness ladder's own names and colors, indexed the same way
	private static final Tier[] DARKNESS_TIERS = {
			new Tier("Unmarked", 0xFF3A3A3A),
			new Tier("Murk", 0xFF4B2E50), new Tier("Shade", 0xFF5B2C62),
			new Tier("Umbra", 0xFF352040), new Tier("Wraith", 0xFF3F1E34),
			new Tier("Revenant", 0xFF4F2A5B), new Tier("Dread", 0xFF512E55),
			new Tier("Soulblight", 0xFF462741), new Tier("Abyssal", 0xFF342239),
			new Tier("Voidwight", 0xFF432A40), new Tier("Nightfall", 0xFF200E20)
	};

	// the last name plate written for each player, so the rank prefix is only
	// re-sent when it actually changes instead of every single refresh
	private static final Map<UUID, Component> APPLIED_PREFIX = new HashMap<>();

	private SkillSystem() {
	}

	public static void refresh(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		data.reconcileAffinity(player);
		ServerAdvancementManager advancements = player.level().getServer().getAdvancements();
		// levels are earned through advancements; find the highest one done.
		// the stored level is the floor, so temporarily revoking the tree
		// while the other path is active can never demote anyone
		int highest = data.skillLevel();
		for (int level = 1; level <= MAX_LEVEL; level++) {
			if (isDone(player, advancements, skillId(level))) {
				highest = Math.max(highest, level);
			}
		}
		if (highest != data.skillLevel()) {
			data.setSkillLevel(player, highest);
			PowerMessages.sendImportant(player, "skill.powers.advanced", 3, rank(highest));
			PowersPackets.syncTo(player);
		}

		if (hasDarknessTag(player)) {
			int highestDarkness = data.darknessLevel();
			for (int level = 1; level <= DARKNESS_MAX_LEVEL; level++) {
				if (isDone(player, advancements, darknessId(level))) {
					highestDarkness = Math.max(highestDarkness, level);
				}
			}
			if (highestDarkness != data.darknessLevel()) {
				data.setDarknessLevel(player, highestDarkness);
				PowerMessages.sendImportant(player, "skill.powers.darkness_advanced", 3,
						darknessRank(highestDarkness));
				PowerFx.rankAwakening(player, true, highestDarkness);
				PowersPackets.syncTo(player);
			}
		}
		// Lazily migrates the old numeric ladder into the persistent maze.
		data.rankProgress(false);
		if (hasDarknessTag(player)) data.rankProgress(true);
		applyRank(player);
	}

	public static String rank(int level) {
		return TIERS[clampLevel(level, MAX_LEVEL)].name();
	}

	public static int color(int level) {
		return TIERS[clampLevel(level, MAX_LEVEL)].color();
	}

	public static String darknessRank(int level) {
		return DARKNESS_TIERS[clampLevel(level, DARKNESS_MAX_LEVEL)].name();
	}

	public static int darknessColor(int level) {
		return DARKNESS_TIERS[clampLevel(level, DARKNESS_MAX_LEVEL)].color();
	}

	// levels run 0..max inclusive and there is one tier name for each
	private static int clampLevel(int level, int max) {
		return Math.max(0, Math.min(max, level));
	}

	public static Component prefix(ServerPlayer player) {
		boolean darkness = hasDarknessTag(player);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (darkness) {
			int level = data.darknessLevel();
			// hidden title: show the equivalent rank name from the normal ladder instead
			if (data.isDarknessPrefixHidden()) {
				return Component.literal("[" + rank(level) + "] ")
						.withStyle(style -> style.withColor(color(level)));
			}
			return Component.literal("[" + focusedTitle(data, true, darknessRank(level)) + "] ")
					.withStyle(style -> style.withColor(darknessColor(level)));
		}
		int level = data.skillLevel();
		return Component.literal("[" + focusedTitle(data, false, rank(level)) + "] ")
				.withStyle(style -> style.withColor(color(level)));
	}

	private static String focusedTitle(PlayerPowers.PlayerPowersData data, boolean darkness, String fallback) {
		RankProgress progress = data.rankProgress(darkness);
		RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
		RankNode node = graph.node(progress.focus());
		return node == null ? fallback : node.title();
	}

	public static boolean hasDarknessTag(ServerPlayer player) {
		return player.entityTags().contains(DARKNESS_TAG);
	}

	public static int effectiveLevel(ServerPlayer player) {
		return hasDarknessTag(player)
			? PlayerPowers.get(player).darknessLevel()
			: PlayerPowers.get(player).skillLevel();
	}

	public static boolean isDarkRealm(ResourceKey<Level> dimension) {
		return dimension.identifier().equals(PowersMod.id("dark_realm"));
	}

	// worthy of the dark realm: tagged players at darkness rank 5 or higher
	public static boolean canEnterDarkRealm(ServerPlayer player) {
		return hasDarknessTag(player) && PlayerPowers.get(player).darknessLevel() >= DARKNESS_GATE_LEVEL;
	}

	/**
	 * Shows exactly one advancement tree. Revoking the inactive journal entries
	 * is presentation-only: the numeric levels remain the authoritative floor,
	 * so an allegiance change never destroys earned progression.
	 */
	public static void syncPathVisibility(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		AdvancementPathRules.Selection selection = AdvancementPathRules.select(
				hasDarknessTag(player), data.skillLevel(), data.darknessLevel());
		if ("darkness_root".equals(selection.hiddenRoot())) {
			revokePath(player, PowersMod.id("darkness_root"), SkillSystem::darknessId, DARKNESS_MAX_LEVEL);
			awardPath(player, PowersMod.id("skill_root"), SkillSystem::skillId, selection.reachedLevel());
		} else {
			revokePath(player, PowersMod.id("skill_root"), SkillSystem::skillId, MAX_LEVEL);
			awardPath(player, PowersMod.id("darkness_root"), SkillSystem::darknessId, selection.reachedLevel());
		}
	}

	private static void revokePath(ServerPlayer player, Identifier rootId,
			IntFunction<Identifier> levelId, int maximumLevel) {
		ServerAdvancementManager advancements = player.level().getServer().getAdvancements();
		revoke(player, advancements.get(rootId));
		for (int level = 1; level <= maximumLevel; level++) {
			revoke(player, advancements.get(levelId.apply(level)));
		}
	}

	private static void awardPath(ServerPlayer player, Identifier rootId, IntFunction<Identifier> levelId,
			int reachedLevel) {
		ServerAdvancementManager advancements = player.level().getServer().getAdvancements();
		AdvancementHolder root = advancements.get(rootId);
		if (root == null) {
			return;
		}
		award(player, root);
		for (int level = 1; level <= reachedLevel; level++) {
			AdvancementHolder holder = advancements.get(levelId.apply(level));
			if (holder != null) {
				award(player, holder);
			}
		}
	}

	// grants every criterion of an advancement, skipping the work when it is already done
	private static void award(ServerPlayer player, AdvancementHolder holder) {
		PlayerAdvancements progressTracker = player.getAdvancements();
		if (progressTracker.getOrStartProgress(holder).isDone()) {
			return;
		}
		for (String criterion : holder.value().criteria().keySet()) {
			progressTracker.award(holder, criterion);
		}
	}

	/** Removes every awarded criterion so the whole inactive tree disappears. */
	private static void revoke(ServerPlayer player, AdvancementHolder holder) {
		if (holder == null) {
			return;
		}
		PlayerAdvancements progressTracker = player.getAdvancements();
		AdvancementProgress progress = progressTracker.getOrStartProgress(holder);
		for (String criterion : holder.value().criteria().keySet()) {
			if (progress.getCriterion(criterion) != null && progress.getCriterion(criterion).isDone()) {
				progressTracker.revoke(holder, criterion);
			}
		}
	}

	private static boolean isDone(ServerPlayer player, ServerAdvancementManager advancements, Identifier id) {
		AdvancementHolder holder = advancements.get(id);
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	/** Grants one deed-controlled darkness advancement after its counters pass. */
	public static void awardDarknessRite(ServerPlayer player, int level) {
		if (!hasDarknessTag(player) || level < 1 || level > DARKNESS_MAX_LEVEL) {
			return;
		}
		ServerAdvancementManager advancements = player.level().getServer().getAdvancements();
		AdvancementHolder root = advancements.get(PowersMod.id("darkness_root"));
		AdvancementHolder rite = advancements.get(darknessId(level));
		if (root != null) {
			award(player, root);
		}
		if (rite != null) {
			award(player, rite);
		}
	}

	/** Grants one tracker-controlled normal advancement after its mastery counters pass. */
	public static void awardSkillRite(ServerPlayer player, int level) {
		if (hasDarknessTag(player) || level < 1 || level > MAX_LEVEL) return;
		ServerAdvancementManager advancements = player.level().getServer().getAdvancements();
		AdvancementHolder root = advancements.get(PowersMod.id("skill_root"));
		AdvancementHolder rite = advancements.get(skillId(level));
		if (root != null) award(player, root);
		if (rite != null) award(player, rite);
	}

	private static Identifier skillId(int level) {
		return PowersMod.id("skill/level_" + String.format("%02d", level));
	}

	private static Identifier darknessId(int level) {
		return PowersMod.id("darkness/level_" + String.format("%02d", level));
	}

	/** Re-applies the visible name prefix, e.g. after hiding the darkness title. */
	public static void refreshPrefix(ServerPlayer player) {
		// drop the cached plate so the change is written even though the rank
		// number itself did not move
		APPLIED_PREFIX.remove(player.getUUID());
		applyRank(player);
	}

	private static void applyRank(ServerPlayer player) {
		RankAttributeManager.reconcile(player, PowerScalingService.profile(player));
		Component rankPrefix = prefix(player);
		// Tracked entity data reaches every observing client. Only dirty it when
		// the title changes; this avoids redundant metadata packets on refresh.
		if (rankPrefix.equals(APPLIED_PREFIX.get(player.getUUID()))) {
			return;
		}
		APPLIED_PREFIX.put(player.getUUID(), rankPrefix);
		((RankDisplayData) player).powers$setRankPrefix(rankPrefix);
	}

	/** Forgets a player's cached name plate when they disconnect. */
	public static void clear(UUID player) {
		APPLIED_PREFIX.remove(player);
		RankAttributeManager.forget(player);
	}

	/** Drops every cached name plate on shutdown so nothing leaks across restarts. */
	public static void clearAll() {
		APPLIED_PREFIX.clear();
		RankAttributeManager.clearAll();
	}
}
