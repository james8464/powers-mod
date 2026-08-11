package com.powers.companion;

import com.powers.PowersItems;
import com.powers.fx.PowerFx;
import com.powers.testing.TestingOverrides;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns bounded ordinary manifestations and the interruptible Dark Crystal rite. */
public final class ShadowConjurationManager {
	public record Outcome(boolean accepted, boolean pending, String reason,
			int energyCost, int count) { }

	public static final class Reservation {
		private final int energy;
		private boolean resolved;
		private boolean committed;

		public Reservation(int energy) {
			this.energy = Math.max(0, energy);
		}

		public boolean commit() {
			if (resolved) return false;
			resolved = true;
			committed = true;
			return true;
		}

		public int refund() {
			if (resolved) return 0;
			resolved = true;
			return energy;
		}

		public boolean committed() {
			return committed;
		}
	}

	private record Rite(UUID bodyId, long startedAt, Reservation reservation) { }
	private static final Map<UUID, Rite> RITES = new HashMap<>();
	private static long completedConjurations;

	private ShadowConjurationManager() {
	}

	public static Outcome begin(ServerPlayer owner, ShadowCompanionEntity shadow,
			Item item, int requestedCount) {
		if (ShadowMagicState.actionsSuppressed(shadow)) {
			return new Outcome(false, false, "amethyst_suppressed", 0, 0);
		}
		boolean testing = TestingOverrides.energyDisabled(owner.getUUID());
		ShadowConjurationFacts facts = ShadowConjurationPolicy.facts(item, requestedCount,
				shadow.energy(), testing);
		ShadowConjurationRules.Decision decision = ShadowConjurationRules.evaluate(facts);
		if (!decision.allowed()) return new Outcome(false, false, decision.reason(), 0, 0);
		if (decision.rite()) return beginRite(owner, shadow, decision);

		shadow.setEnergy(shadow.energy() - decision.cost());
		ItemStack created = new ItemStack(item, decision.boundedCount());
		if (!owner.addItem(created) && !created.isEmpty()) owner.drop(created, false);
		ServerLevel level = (ServerLevel) shadow.level();
		PowerFx.rune(level, shadow.position().add(0.0, 0.1, 0.0), 1.0,
				0x49204F, 18, level.getGameTime() * 0.08);
		PowerFx.burst(level, owner.getEyePosition(), ParticleTypes.REVERSE_PORTAL,
				12, 0.35, 0.01);
		PowerFx.sound(level, shadow.position(), SoundEvents.SCULK_CATALYST_BLOOM, 0.65F, 0.65F);
		completedConjurations++;
		return new Outcome(true, false, "conjured", decision.cost(), decision.boundedCount());
	}

	private static Outcome beginRite(ServerPlayer owner, ShadowCompanionEntity shadow,
			ShadowConjurationRules.Decision decision) {
		UUID ownerId = owner.getUUID();
		if (RITES.containsKey(ownerId)) return new Outcome(false, true, "rite_already_active", 0, 0);
		if (owner.getInventory().contains(new ItemStack(PowersItems.DARK_CRYSTAL))) {
			return new Outcome(false, false, "dark_crystal_already_carried", 0, 0);
		}
		Reservation reservation = new Reservation(decision.cost());
		shadow.setEnergy(shadow.energy() - decision.cost());
		RITES.put(ownerId, new Rite(shadow.getUUID(), owner.level().getGameTime(), reservation));
		return new Outcome(true, true, "dark_crystal_rite_started", decision.cost(), 1);
	}

	public static Outcome tick(ServerPlayer owner, ShadowCompanionEntity shadow) {
		Rite rite = RITES.get(owner.getUUID());
		if (rite == null) return new Outcome(false, false, "no_rite", 0, 0);
		boolean bodyLost = !shadow.isAlive() || !shadow.getUUID().equals(rite.bodyId());
		boolean dimensionMismatch = owner.level() != shadow.level();
		if (interrupts(bodyLost, !owner.isAlive(), dimensionMismatch,
				shadow.energy() > 0 || ShadowMagicState.actionsSuppressed(shadow))) {
			return interrupt(owner, shadow, "rite_interrupted");
		}
		ServerLevel level = (ServerLevel) shadow.level();
		long elapsed = level.getGameTime() - rite.startedAt();
		if (elapsed % 20L == 0L) {
			double radius = 1.5 + 4.5 * Math.min(1.0,
					elapsed / (double) ShadowConjurationRules.DARK_CRYSTAL_CHANNEL_TICKS);
			PowerFx.rune(level, shadow.position(), radius, 0x2A0637, 32,
					elapsed * 0.025);
			PowerFx.burst(level, shadow.getEyePosition(), ParticleTypes.REVERSE_PORTAL,
					Math.min(28, 8 + (int) (elapsed / 80L)), radius * 0.18, 0.01);
		}
		if (!riteComplete(rite.startedAt(), level.getGameTime())) {
			return new Outcome(true, true, "channeling", 0, 0);
		}
		if (owner.getInventory().contains(new ItemStack(PowersItems.DARK_CRYSTAL))) {
			return interrupt(owner, shadow, "duplicate_prevented");
		}
		RITES.remove(owner.getUUID());
		rite.reservation().commit();
		ItemStack crystal = new ItemStack(PowersItems.DARK_CRYSTAL);
		if (!owner.addItem(crystal) && !crystal.isEmpty()) owner.drop(crystal, false);
		PowerFx.rune(level, shadow.position(), 7.0, 0x130018, 64, 0.0);
		PowerFx.burst(level, shadow.getEyePosition(), ParticleTypes.REVERSE_PORTAL, 48, 1.4, 0.03);
		PowerFx.sound(level, shadow.position(), SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 0.45F);
		completedConjurations++;
		return new Outcome(true, false, "dark_crystal_conjured", 0, 1);
	}

	public static Outcome interrupt(ServerPlayer owner, ShadowCompanionEntity shadow, String reason) {
		Rite rite = RITES.remove(owner.getUUID());
		if (rite == null) return new Outcome(false, false, "no_rite", 0, 0);
		int refund = rite.reservation().refund();
		shadow.setEnergy(shadow.energy() + refund);
		return new Outcome(false, false, reason, -refund, 0);
	}

	public static boolean active(UUID owner) {
		return RITES.containsKey(owner);
	}

	public static int activeCount() {
		return RITES.size();
	}

	public static long completedCount() {
		return completedConjurations;
	}

	/** Death consumes the committed effort; there is no body to receive a refund. */
	public static void abandon(UUID owner) {
		RITES.remove(owner);
	}

	public static boolean riteComplete(long startedAt, long now) {
		return now - startedAt >= ShadowConjurationRules.DARK_CRYSTAL_CHANNEL_TICKS;
	}

	public static boolean interrupts(boolean bodyLost, boolean ownerDead,
			boolean dimensionMismatch, boolean energyChanged) {
		return bodyLost || ownerDead || dimensionMismatch || energyChanged;
	}

	public static void clear() {
		RITES.clear();
		completedConjurations = 0L;
	}
}
