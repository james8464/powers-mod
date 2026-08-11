package com.powers.magic.participant;

import com.powers.companion.ShadowCompanionEntity;
import com.powers.entity.PowerTestActor;
import com.powers.entity.TestActorPowerState;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/** Central resolver for real players, safe test actors, and owner-bound Shadow bodies. */
public final class MagicParticipants {
	public enum Kind { PLAYER, TEST_ACTOR, SHADOW, NONE }
	public record Policy(MagicConsentAuthority consent, MagicParticipant.Alignment alignment) { }
	public record CapabilityResolution(Optional<MagicParticipant> participant,
			com.powers.knowledge.MagicFailureReason failure) {
		public boolean supported() { return participant.isPresent(); }
	}

	private MagicParticipants() {
	}

	public static Policy policy(Kind kind) {
		return switch (kind) {
			case PLAYER -> new Policy(MagicConsentAuthority.PLAYER_SETTINGS,
					MagicParticipant.Alignment.NEUTRAL);
			case TEST_ACTOR -> new Policy(MagicConsentAuthority.ALWAYS_ALLOW_TESTS,
					MagicParticipant.Alignment.NEUTRAL);
			case SHADOW -> new Policy(MagicConsentAuthority.OWNER_DELEGATED,
					MagicParticipant.Alignment.DARKNESS);
			case NONE -> new Policy(MagicConsentAuthority.NONE,
					MagicParticipant.Alignment.NEUTRAL);
		};
	}

	public static Optional<MagicParticipant> resolve(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) return Optional.of(player(player));
		if (entity instanceof PowerTestActor actor) return Optional.of(testActor(actor));
		if (entity instanceof ShadowCompanionEntity shadow) return Optional.of(shadow(shadow));
		return Optional.empty();
	}

	/** Resolves a participant only after the requested target capability is explicitly supported. */
	public static CapabilityResolution resolve(LivingEntity entity, ParticipantCapability capability) {
		Kind kind = kind(entity);
		var support = ParticipantCapabilityContract.check(kind, capability);
		if (!support.supported()) return new CapabilityResolution(Optional.empty(), support.failure());
		return new CapabilityResolution(resolve(entity), com.powers.knowledge.MagicFailureReason.NONE);
	}

	public static Kind kind(LivingEntity entity) {
		if (entity instanceof ServerPlayer) return Kind.PLAYER;
		if (entity instanceof PowerTestActor) return Kind.TEST_ACTOR;
		if (entity instanceof ShadowCompanionEntity) return Kind.SHADOW;
		return Kind.NONE;
	}

	private static MagicParticipant player(ServerPlayer player) {
		return new MagicParticipant() {
			@Override public java.util.UUID id() { return player.getUUID(); }
			@Override public LivingEntity entity() { return player; }
			@Override public Alignment alignment() {
				return SkillSystem.hasDarknessTag(player) ? Alignment.DARKNESS : Alignment.RADIANT;
			}
			@Override public MagicConsentAuthority consentAuthority() {
				return MagicConsentAuthority.PLAYER_SETTINGS;
			}
			@Override public int energy() { return PlayerPowers.get(player).energy(); }
			@Override public int capacity() { return PlayerPowers.get(player).energyCapacity(); }
			@Override public void setEnergy(int value) {
				var powers = PlayerPowers.get(player);
				int target = Math.clamp(value, 0, powers.energyCapacity());
				if (target < powers.energy()) powers.drainEnergy(powers.energy() - target);
				else powers.refundEnergy(target - powers.energy());
			}
			@Override public boolean suppressed() { return AmethystDampening.isDampened(player); }
			@Override public Optional<ServerPlayer> consentOwner() { return Optional.of(player); }
		};
	}

	private static MagicParticipant testActor(PowerTestActor actor) {
		return new MagicParticipant() {
			@Override public java.util.UUID id() { return actor.getUUID(); }
			@Override public LivingEntity entity() { return actor; }
			@Override public Alignment alignment() { return Alignment.NEUTRAL; }
			@Override public MagicConsentAuthority consentAuthority() {
				return MagicConsentAuthority.ALWAYS_ALLOW_TESTS;
			}
			@Override public int energy() { return TestActorPowerState.energy(actor.getUUID()); }
			@Override public int capacity() { return TestActorPowerState.ENERGY_CAPACITY; }
			@Override public void setEnergy(int value) {
				TestActorPowerState.setEnergy(actor.getUUID(), value);
			}
			@Override public boolean suppressed() { return AmethystDampening.isDampened(actor); }
			@Override public Optional<ServerPlayer> consentOwner() { return Optional.empty(); }
		};
	}

	private static MagicParticipant shadow(ShadowCompanionEntity shadow) {
		return new MagicParticipant() {
			@Override public java.util.UUID id() { return shadow.getUUID(); }
			@Override public LivingEntity entity() { return shadow; }
			@Override public Alignment alignment() { return Alignment.DARKNESS; }
			@Override public MagicConsentAuthority consentAuthority() {
				return MagicConsentAuthority.OWNER_DELEGATED;
			}
			@Override public int energy() { return shadow.energy(); }
			@Override public int capacity() { return com.powers.companion.ShadowCompanionRules.MAX_ENERGY; }
			@Override public void setEnergy(int value) { shadow.setEnergy(value); }
			@Override public boolean suppressed() { return AmethystDampening.isDampened(shadow); }
			@Override public Optional<ServerPlayer> consentOwner() {
				if (shadow.ownerId() == null || !(shadow.level() instanceof net.minecraft.server.level.ServerLevel level)) {
					return Optional.empty();
				}
				return Optional.ofNullable(level.getServer().getPlayerList().getPlayer(shadow.ownerId()));
			}
		};
	}
}
