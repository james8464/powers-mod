package com.powers.power;

import com.powers.PowerStatusEffects;
import com.powers.PowersEffects;
import com.powers.AmethystWardBlock;
import com.powers.PowersBlocks;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.config.PowersConfigLoader;
import com.powers.util.PowerMessages;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The rules for amethyst's anti-power field: what counts as amethyst
 * (items, blocks, powered wards) and the poisoning effect and sting a
 * suppressed player gets.
 *
 * <p>What counts comes from the {@code #powers:amethyst} block and item tags
 * rather than a substring match on the registry id, so an unrelated mod's
 * "amethyst_hoe" no longer silently switches everyone's powers off - and a
 * server owner can retune the list from a datapack.
 *
 * <p>Powered wards are tracked in a per-dimension index maintained by
 * {@link AmethystWardBlock} instead of being hunted for. Scanning a 20-block
 * radius meant walking ~69,000 positions per player per second, which dwarfed
 * everything else the mod did each tick.
 */
public final class AmethystDampening {
	// how close amethyst blocks need to be to suppress powers
	private static final int RADIUS = 6;
	private static final int MAX_SUPPRESSED_WARDS_PER_DIMENSION = 4_096;
	private static final int MAX_SUPPRESSION_TICKS = 72_000;

	/** Blocks that shut powers down when a player stands near them. */
	public static final TagKey<Block> AMETHYST_BLOCKS =
			TagKey.create(Registries.BLOCK, PowersMod.id("amethyst"));
	/** Items that shut powers down when a player carries them. */
	public static final TagKey<Item> AMETHYST_ITEMS =
			TagKey.create(Registries.ITEM, PowersMod.id("amethyst"));

	// every currently powered ward, per dimension. kept up to date by the block
	// itself, so the lookup below is a walk over a handful of positions rather
	// than a brute-force sweep of the volume around each player
	private static final Map<ResourceKey<Level>, AmethystWardIndex> POWERED_WARDS = new HashMap<>();
	private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> SUPPRESSED_WARDS = new HashMap<>();
	private static final NaturalAmethystIndex NATURAL_AMETHYST = new NaturalAmethystIndex();

	private AmethystDampening() {
	}

	/** rechecks the field around the player and applies or clears the amethyst poisoning effect, returning whether they're suppressed */
	public static boolean update(ServerPlayer player) {
		return update((LivingEntity) player);
	}

	/** Player-like entities share location/ward suppression; only real players carry inventories. */
	public static boolean update(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) return false;
		boolean dampened = entity instanceof ServerPlayer player && hasAmethystItem(player)
				|| naturalAmethystNearby(level, entity.blockPosition())
				|| findPoweredWard(level, entity.blockPosition()).isPresent();
		if (dampened) {
			// 30 ticks is plenty because this effect gets refreshed on every update
			entity.addEffect(PowerStatusEffects.hidden(
					PowersEffects.AMETHYST_POISONING, 30, 0, true, true));
		} else if (entity.hasEffect(PowersEffects.AMETHYST_POISONING)) {
			entity.removeEffect(PowersEffects.AMETHYST_POISONING);
		}
		return dampened;
	}

	/** Records a ward that has just come under redstone power. */
	public static void addPoweredWard(ServerLevel level, BlockPos pos) {
		POWERED_WARDS.computeIfAbsent(level.dimension(), key -> new AmethystWardIndex()).add(pos);
	}

	/** Forgets a ward that has lost power, been broken, or been pushed away. */
	public static void removePoweredWard(ServerLevel level, BlockPos pos) {
		AmethystWardIndex wards = POWERED_WARDS.get(level.dimension());
		if (wards == null) {
			return;
		}
		wards.remove(pos);
		if (wards.size() == 0) {
			POWERED_WARDS.remove(level.dimension());
		}
	}

	/** Drops the ward index on shutdown; it is rebuilt from block updates on load. */
	public static void clearAll() {
		POWERED_WARDS.values().forEach(AmethystWardIndex::clear);
		POWERED_WARDS.clear();
		SUPPRESSED_WARDS.clear();
		NATURAL_AMETHYST.clear();
	}

	/** A completed ward-breaking ritual grounds one powered ward temporarily. */
	public static void suppressWard(ServerLevel level, BlockPos pos, int durationTicks) {
		long now = level.getGameTime();
		Map<BlockPos, Long> suppressed = SUPPRESSED_WARDS.computeIfAbsent(
				level.dimension(), key -> new java.util.LinkedHashMap<>());
		suppressed.entrySet().removeIf(entry -> entry.getValue() <= now);
		if (!suppressed.containsKey(pos) && suppressed.size() >= MAX_SUPPRESSED_WARDS_PER_DIMENSION) {
			suppressed.remove(suppressed.keySet().iterator().next());
		}
		suppressed.put(pos.immutable(), now + Math.clamp(durationTicks, 1, MAX_SUPPRESSION_TICKS));
	}

	/**
	 * Finds a redstone-powered ward within range, if any. Entries are verified
	 * against the world as they are read, so a ward removed while its chunk was
	 * unloaded (worldedit, another mod, a datapack) drops out of the index on
	 * first use instead of projecting a phantom field forever.
	 */
	public static Optional<BlockPos> findPoweredWard(ServerLevel level, BlockPos center) {
		AmethystWardIndex wards = POWERED_WARDS.get(level.dimension());
		if (wards == null || wards.size() == 0) {
			return Optional.empty();
		}
		int wardRadius = PowersConfigLoader.get().wardRadius();
		for (BlockPos pos : wards.nearby(center, wardRadius)) {
			Map<BlockPos, Long> suppressed = SUPPRESSED_WARDS.get(level.dimension());
			if (suppressed != null) {
				long expiry = suppressed.getOrDefault(pos, 0L);
				if (expiry > level.getGameTime()) continue;
				if (expiry != 0L) {
					suppressed.remove(pos);
					if (suppressed.isEmpty()) SUPPRESSED_WARDS.remove(level.dimension());
				}
			}
			if (LoadedChunks.contains(level, pos)) {
				BlockState state = level.getBlockState(pos);
				if (!state.is(PowersBlocks.AMETHYST_WARD) || !AmethystWardBlock.isPowered(state)) {
					wards.removeStale(pos);
					continue;
				}
			}
			return Optional.of(pos);
		}
		return Optional.empty();
	}

	/** whether the entity is currently under the amethyst poisoning effect */
	public static boolean isDampened(LivingEntity entity) {
		return entity != null && entity.hasEffect(PowersEffects.AMETHYST_POISONING);
	}

	/** Location-only suppression used by non-player tactical entities and rituals. */
	public static boolean isDampenedAt(ServerLevel level, BlockPos position) {
		return level != null && position != null
				&& (naturalAmethystNearby(level, position)
				|| findPoweredWard(level, position).isPresent());
	}

	private static boolean naturalAmethystNearby(ServerLevel level, BlockPos center) {
		return NATURAL_AMETHYST.nearby(level, center, RADIUS,
				position -> !isSuppressedWard(level, position));
	}

	/** A ward-breaking ritual masks the ward in both indexes for the same bounded lease. */
	private static boolean isSuppressedWard(ServerLevel level, BlockPos position) {
		Map<BlockPos, Long> suppressed = SUPPRESSED_WARDS.get(level.dimension());
		if (suppressed == null) return false;
		long expiry = suppressed.getOrDefault(position, 0L);
		if (expiry <= level.getGameTime()) {
			if (expiry != 0L) suppressed.remove(position);
			if (suppressed.isEmpty()) SUPPRESSED_WARDS.remove(level.dimension());
			return false;
		}
		if (level.getBlockState(position).is(PowersBlocks.AMETHYST_WARD)) return true;
		suppressed.remove(position);
		if (suppressed.isEmpty()) SUPPRESSED_WARDS.remove(level.dimension());
		return false;
	}

	/** Narrow block-change hook used only to maintain already materialized section indexes. */
	public static void blockChanged(ServerLevel level, BlockPos position, BlockState state) {
		NATURAL_AMETHYST.blockChanged(level, position, state);
	}

	/** Chunk lifecycle invalidation prevents stale natural-amethyst entries after external rewrites. */
	public static void invalidateChunk(ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
		NATURAL_AMETHYST.invalidateChunk(level, chunk);
	}

	public static NaturalAmethystIndex.Diagnostics naturalIndexDiagnostics() {
		return NATURAL_AMETHYST.diagnostics();
	}

	/** Per-dimension natural-amethyst counters for operator diagnostics. */
	public static Map<String, NaturalAmethystIndex.Diagnostics> naturalIndexDiagnosticsByDimension() {
		return NATURAL_AMETHYST.diagnosticsByDimension();
	}

	/** Combined per-dimension ward counters for operator diagnostics. */
	public static WardDiagnostics wardIndexDiagnostics() {
		long queries = 0L, candidates = 0L, misses = 0L, stale = 0L, memory = 0L;
		int entries = 0, chunks = 0;
		for (AmethystWardIndex index : POWERED_WARDS.values()) {
			AmethystWardIndex.Diagnostics value = index.diagnostics();
			queries += value.queries();
			candidates += value.candidates();
			misses += value.misses();
			stale += value.staleRemovals();
			entries += value.entries();
			chunks += value.chunks();
			memory += value.estimatedBytes();
		}
		return new WardDiagnostics(queries, candidates, misses, stale, entries, chunks, memory);
	}

	/** Per-dimension powered-ward counters without scanning loaded blocks. */
	public static Map<String, WardDiagnostics> wardIndexDiagnosticsByDimension() {
		Map<String, WardDiagnostics> result = new java.util.TreeMap<>();
		for (Map.Entry<ResourceKey<Level>, AmethystWardIndex> entry : POWERED_WARDS.entrySet()) {
			AmethystWardIndex.Diagnostics value = entry.getValue().diagnostics();
			result.put(entry.getKey().identifier().toString(), new WardDiagnostics(value.queries(),
					value.candidates(), value.misses(), value.staleRemovals(), value.entries(),
					value.chunks(), value.estimatedBytes()));
		}
		return java.util.Collections.unmodifiableMap(result);
	}

	public record WardDiagnostics(long queries, long candidates, long misses, long staleRemovals,
			int entries, int chunks, long estimatedBytes) { }

	/**
	 * The sting for using powers while suppressed: 2.5 magic damage,
	 * violet spark bursts, and a message that the power was blocked
	 */
	public static void punish(ServerPlayer player) {
		punish((LivingEntity) player);
		PowerMessages.send(player, "amethyst.powers.suppressed", 6);
	}

	/** Non-player magic users receive the same damage and semantic feedback without chat. */
	public static void punish(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) return;
		Vec3 pos = entity.position().add(0, 1, 0);
		// don't deal damage to a player who's already down. the source carries no
		// attacker on purpose: this is the amethyst answering back, so the
		// dampening shield must not cancel it
		if (entity.isAlive()) {
			entity.hurtServer(level, entity.damageSources().magic(), 2.5f);
		}
		PowerFx.burst(level, pos, ParticleTypes.ELECTRIC_SPARK, 16, 0.5, 0.1);
		PowerFx.coloredBurst(level, pos, 0xB36BFF, 22, 0.7);
		PowerFx.burst(level, pos, com.powers.PowersParticles.GLYPH, 10, 0.4, 0.2);
		PowerFx.sound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.1f);
	}

	// any amethyst item suppresses powers, from the main inventory, offhand, or armor
	private static boolean hasAmethystItem(ServerPlayer player) {
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (isAmethystItem(stack)) return true;
		}
		if (isAmethystItem(player.getOffhandItem())) return true;
		if (isAmethystItem(player.getItemBySlot(EquipmentSlot.HEAD))) return true;
		if (isAmethystItem(player.getItemBySlot(EquipmentSlot.CHEST))) return true;
		if (isAmethystItem(player.getItemBySlot(EquipmentSlot.LEGS))) return true;
		if (isAmethystItem(player.getItemBySlot(EquipmentSlot.FEET))) return true;
		return false;
	}

	// membership of the #powers:amethyst item tag, so the set is data-driven
	private static boolean isAmethystItem(ItemStack stack) {
		return !stack.isEmpty() && stack.is(AMETHYST_ITEMS);
	}

}
