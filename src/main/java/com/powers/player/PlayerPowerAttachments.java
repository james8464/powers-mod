package com.powers.player;

import com.mojang.serialization.Codec;
import com.powers.PowersMod;
import com.powers.mind.MindBodyState;
import com.powers.power.PowerEnergy;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Declares the stable persistent attachment schema used by {@link PlayerPowers}.
 * Save identifiers and codecs are centralized here so gameplay operations do
 * not also own serialization registration details.
 */
final class PlayerPowerAttachments {
	static final AttachmentType<List<String>> POWER_SLOTS = persistentStringList("power_slots");
	static final AttachmentType<List<String>> ACTIVE_TOGGLES = sessionStringList("active_toggles");
	static final AttachmentType<Integer> ENERGY = persistentInt("energy", PowerEnergy.BASE_MAX);
	static final AttachmentType<Integer> DARKNESS_ENERGY =
			persistentInt("darkness_energy", PowerEnergy.darknessMaxCapacity(0));
	static final AttachmentType<Integer> SKILL_LEVEL = persistentInt("skill_level", 0);
	static final AttachmentType<Integer> DARKNESS_LEVEL = persistentInt("darkness_level", 0);
	static final AttachmentType<List<String>> RANK_NODES = persistentStringList("rank_nodes");
	static final AttachmentType<List<String>> DARK_RANK_NODES = persistentStringList("dark_rank_nodes");
	static final AttachmentType<List<String>> REALM_MEMORIES = persistentStringList("realm_memories");
	static final AttachmentType<String> RANK_FOCUS = persistentString("rank_focus");
	static final AttachmentType<String> DARK_RANK_FOCUS = persistentString("dark_rank_focus");
	static final AttachmentType<Boolean> DARKNESS_PREFIX_HIDDEN = persistentBoolean("darkness_prefix_hidden");
	static final AttachmentType<Boolean> GUIDE_RECEIVED = persistentBoolean("guide_received");
	static final AttachmentType<Integer> SIZE_MORPH_OPTION = persistentInt("size_morph_option", 3);
	static final AttachmentType<String> SHADOW_SWORD_SELECTION = persistentString("shadow_sword_selection");
	static final AttachmentType<String> HEAVENLY_PARTISAN_SELECTION =
			persistentString("heavenly_partisan_selection");
	static final AttachmentType<List<String>> SHADOW_SWORD_FAVOURITES =
			persistentStringList("shadow_sword_favourites");
	static final AttachmentType<List<String>> HEAVENLY_PARTISAN_FAVOURITES =
			persistentStringList("heavenly_partisan_favourites");
	static final AttachmentType<Map<String, Long>> COOLDOWNS = persistentMap(
			"cooldowns", Codec.unboundedMap(Codec.STRING, Codec.LONG));
	static final AttachmentType<Map<String, Integer>> SPELL_SELECTIONS = persistentMap(
			"spell_selections", Codec.unboundedMap(Codec.STRING, Codec.INT));
	static final AttachmentType<Map<String, Integer>> CRYSTAL_SELECTIONS = persistentMap(
			"crystal_selections", Codec.unboundedMap(Codec.STRING, Codec.INT));
	static final AttachmentType<Map<String, Integer>> DARKNESS_DEEDS = persistentMap(
			"darkness_deeds", Codec.unboundedMap(Codec.STRING, Codec.INT));
	static final AttachmentType<Map<String, Integer>> SKILL_DEEDS = persistentMap(
			"skill_deeds", Codec.unboundedMap(Codec.STRING, Codec.INT));
	static final AttachmentType<PlayerPowers.AnchorState> DIMENSIONAL_ANCHOR = AttachmentRegistry.create(
			PowersMod.id("dimensional_anchor"),
			builder -> builder.persistent(PlayerPowers.AnchorState.CODEC).copyOnDeath());
	static final AttachmentType<MindBodyState> MIND_BODY = AttachmentRegistry.create(
			PowersMod.id("mind_body"), builder -> builder.persistent(MindBodyState.CODEC).copyOnDeath());
	static final AttachmentType<Integer> FLIGHT_SNAPSHOT = sessionInt("flight_snapshot", -1);
	static final AttachmentType<Boolean> TELEPORT_CONSENT = persistentBoolean("teleport_consent");
	static final AttachmentType<Boolean> LOCATOR_CONSENT = persistentBoolean("locator_consent");
	static final AttachmentType<Boolean> COMPANION_CONSENT = persistentBoolean("companion_consent");
	static final AttachmentType<Boolean> DREAMWALK_CONSENT = persistentBoolean("dreamwalk_consent");
	static final AttachmentType<Boolean> POSSESSION_CONSENT = persistentBoolean("possession_consent");
	static final AttachmentType<String> PREVIOUS_GAMEMODE = AttachmentRegistry.create(
			PowersMod.id("previous_gamemode"), builder -> builder.persistent(Codec.STRING).copyOnDeath());
	static final AttachmentType<LastDeathRecord> LAST_DEATH = AttachmentRegistry.create(
			PowersMod.id("last_death"), builder -> builder.persistent(LastDeathRecord.CODEC).copyOnDeath());

	private PlayerPowerAttachments() {
	}

	/** Forces this schema class to initialize while Fabric is still registering content. */
	static void initialize() {
		// Static field initialization above performs every AttachmentRegistry registration.
	}

	private static AttachmentType<List<String>> persistentStringList(String name) {
		return AttachmentRegistry.create(PowersMod.id(name), builder -> builder
				.initializer(ArrayList::new).persistent(Codec.STRING.listOf()).copyOnDeath());
	}

	/** Registers runtime state that must never survive reconnect or death. */
	private static AttachmentType<List<String>> sessionStringList(String name) {
		return AttachmentRegistry.create(PowersMod.id(name), builder -> builder.initializer(ArrayList::new));
	}

	private static AttachmentType<String> persistentString(String name) {
		return AttachmentRegistry.create(PowersMod.id(name), builder -> builder
				.initializer(() -> "").persistent(Codec.STRING).copyOnDeath());
	}

	private static AttachmentType<Integer> persistentInt(String name, int initialValue) {
		return AttachmentRegistry.create(PowersMod.id(name), builder -> builder
				.initializer(() -> initialValue).persistent(Codec.INT).copyOnDeath());
	}

	/** Registers a runtime scalar without a codec or death-copy policy. */
	private static AttachmentType<Integer> sessionInt(String name, int initialValue) {
		return AttachmentRegistry.create(PowersMod.id(name), builder -> builder.initializer(() -> initialValue));
	}

	private static AttachmentType<Boolean> persistentBoolean(String name) {
		return AttachmentRegistry.create(PowersMod.id(name), builder -> builder
				.initializer(() -> Boolean.FALSE).persistent(Codec.BOOL).copyOnDeath());
	}

	private static <T> AttachmentType<Map<String, T>> persistentMap(String name, Codec<Map<String, T>> codec) {
		return AttachmentRegistry.create(PowersMod.id(name), builder -> builder
				.initializer(HashMap::new).persistent(codec).copyOnDeath());
	}
}
