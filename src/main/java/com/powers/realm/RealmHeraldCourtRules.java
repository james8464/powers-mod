package com.powers.realm;

/** Deterministic, bounded pressure profile for a persistent Herald Court encounter. */
public final class RealmHeraldCourtRules {
	public static final int MAX_PARTICIPANTS = 8;
	public static final int MAX_SUMMONS = 6;
	public static final int MAX_HAZARD_RADIUS = 12;
	public static final int MAX_HAZARD_TAIL_TICKS = 20;
	public static final double PARTICIPANT_DAMAGE_STEP = 0.10;

	public enum Phase {
		VERDICT,
		ASCENDANT,
		LAST_COVENANT
	}

	public record Profile(Phase phase, int participants, float beamDamage, int beamCadenceTicks,
			int summonCap, int summonCadenceTicks, int summonLifetimeTicks,
			int hazardRadius, int hazardPulseTicks,
			int hazardDurationTicks, boolean blocksRecoveryExit) {
		public int temporalSummonCeiling() {
			return (summonLifetimeTicks + summonCadenceTicks - 1) / summonCadenceTicks;
		}
	}

	private RealmHeraldCourtRules() {
	}

	public static Phase phase(float healthRatio) {
		if (!Float.isFinite(healthRatio)) return Phase.LAST_COVENANT;
		if (healthRatio > 2.0F / 3.0F) return Phase.VERDICT;
		if (healthRatio > 1.0F / 3.0F) return Phase.ASCENDANT;
		return Phase.LAST_COVENANT;
	}

	public static Phase restoredPhase(int ordinal) {
		return Phase.values()[Math.clamp(ordinal, 0, Phase.values().length - 1)];
	}

	public static int normalizeParticipants(int participants) {
		return Math.clamp(participants, 1, MAX_PARTICIPANTS);
	}

	public static Profile profile(Phase phase, int participantCount) {
		int participants = normalizeParticipants(participantCount);
		float baseDamage = switch (phase) {
			case VERDICT -> 52.0F;
			case ASCENDANT -> 62.0F;
			case LAST_COVENANT -> 72.0F;
		};
		int phasePressure = phase.ordinal();
		float damage = (float) Math.min(96.0,
				baseDamage * (1.0 + PARTICIPANT_DAMAGE_STEP * (participants - 1)));
		int cadence = 80 - phasePressure * 10;
		int summonCap = Math.min(MAX_SUMMONS,
				1 + phasePressure + (participants - 1) / 2);
		int pulse = 60 - phasePressure * 10;
		return new Profile(phase, participants, damage, cadence, summonCap, 200,
				20 * (35 + phasePressure * 5), 7 + phasePressure * 2, pulse,
				pulse + MAX_HAZARD_TAIL_TICKS, false);
	}
}
