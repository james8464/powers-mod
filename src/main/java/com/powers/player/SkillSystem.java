package com.powers.player;

import com.powers.PowersMod;
import com.powers.network.PowersPackets;
import com.powers.progression.RankGraph;
import com.powers.progression.RankGraphRegistry;
import com.powers.progression.RankNode;
import com.powers.progression.RankProgress;
import com.powers.util.PowerMessages;
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
			PowerMessages.send(player, "skill.powers.advanced", 3, rank(highest));
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
				PowerMessages.send(player, "skill.powers.darkness_advanced", 3, darknessRank(highestDarkness));
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

	public static float damage(ServerPlayer player, float base) {
		// 5.25% more damage per rank, topping out at +52.5% on the last rank
		return base * (1.0f + effectiveLevel(player) * 0.0525f);
	}

	public static double range(ServerPlayer player, double base) {
		// 2.1% more range per rank, matching the old ladder's +21% cap
		return base * (1.0 + effectiveLevel(player) * 0.021);
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
	 * Keeps earned advancement journal entries visible. Switching tags or maze
	 * focus never revokes history; numeric levels remain the migration floor.
	 */
	public static void syncPathVisibility(ServerPlayer player) {
		boolean darkness = hasDarknessTag(player);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		awardPath(player, PowersMod.id("skill_root"), SkillSystem::skillId, data.skillLevel());
		if (darkness) {
			awardPath(player, PowersMod.id("darkness_root"), SkillSystem::darknessId, data.darknessLevel());
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

	private static boolean isDone(ServerPlayer player, ServerAdvancementManager advancements, Identifier id) {
		AdvancementHolder holder = advancements.get(id);
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
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
		Component plate = prefix(player).copy().append(player.getName());
		// setCustomName dirties the entity's tracked data and broadcasts a
		// metadata packet to everyone watching, so only write it on a change
		if (plate.equals(APPLIED_PREFIX.get(player.getUUID()))) {
			return;
		}
		APPLIED_PREFIX.put(player.getUUID(), plate);
		player.setCustomName(plate);
		player.setCustomNameVisible(true);
	}

	/** Forgets a player's cached name plate when they disconnect. */
	public static void clear(UUID player) {
		APPLIED_PREFIX.remove(player);
	}

	/** Drops every cached name plate on shutdown so nothing leaks across restarts. */
	public static void clearAll() {
		APPLIED_PREFIX.clear();
	}
}
