package com.powers.progression;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossContributionLedgerTest {
	@Test
	void damageSupportAndControlResolveInStableOrderExactlyOnce() {
		BossContributionLedger ledger = new BossContributionLedger();
		UUID encounter = UUID.fromString("00000000-0000-0000-0000-000000000099");
		UUID damage = UUID.fromString("00000000-0000-0000-0000-000000000003");
		UUID support = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID control = UUID.fromString("00000000-0000-0000-0000-000000000002");

		assertFalse(ledger.noteDamage(encounter, damage, 7.0, 100));
		assertTrue(ledger.noteDamage(encounter, damage, 1.0, 110));
		assertTrue(ledger.noteSupport(encounter, support, 6.0, 120));
		assertTrue(ledger.noteControl(encounter, control, 20, 130));

		assertEquals(List.of(support, control, damage),
				ledger.resolve(encounter, 200, player -> true));
		assertFalse(ledger.noteDamage(encounter, damage, 100.0, 201));
		assertEquals(List.of(), ledger.resolve(encounter, 201, player -> true));
	}

	@Test
	void inactiveStaleAndNoiseOnlyParticipantsNeverReceiveCredit() {
		BossContributionLedger ledger = new BossContributionLedger();
		UUID encounter = UUID.fromString("00000000-0000-0000-0000-000000000088");
		UUID active = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID inactive = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID stale = UUID.fromString("00000000-0000-0000-0000-000000000003");
		UUID noise = UUID.fromString("00000000-0000-0000-0000-000000000004");

		ledger.noteDamage(encounter, active, 8.0, 3_000);
		ledger.noteSupport(encounter, inactive, 6.0, 3_000);
		ledger.noteControl(encounter, stale, 20, 1_799);
		ledger.noteDamage(encounter, noise, 0.25, 3_000);

		assertEquals(List.of(active), ledger.resolve(encounter, 3_000,
				player -> !player.equals(inactive)));
		assertEquals(0, ledger.activeEncounters());
	}

	@Test
	void deterministicAwardListIsHardCapped() {
		BossContributionLedger ledger = new BossContributionLedger();
		UUID encounter = UUID.fromString("00000000-0000-0000-0000-000000000077");
		List<UUID> participants = IntStream.rangeClosed(1, 30)
				.mapToObj(index -> UUID.fromString(String.format(
						"00000000-0000-0000-0000-%012d", 31 - index)))
				.toList();
		participants.forEach(player -> ledger.noteDamage(encounter, player, 8.0, 100));

		List<UUID> expected = participants.stream().sorted()
				.limit(BossContributionRules.MAX_CONTRIBUTORS).toList();
		assertEquals(expected, ledger.resolve(encounter, 100, player -> true));
	}
}
