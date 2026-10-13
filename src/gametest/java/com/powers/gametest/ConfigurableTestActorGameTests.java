package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.entity.PlayerLikeTarget;
import com.powers.entity.TestActorProfile;
import com.powers.item.artifact.ArtifactAlignment;

import com.powers.magic.participant.MagicParticipant;
import com.powers.magic.participant.MagicConsentAuthority;
import com.powers.magic.participant.MagicParticipants;
import com.powers.player.SkillSystem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Live proof that a test actor exposes bounded player-compatible controls. */
public final class ConfigurableTestActorGameTests {
	@GameTest(maxTicks = 20)
	public void configuredActorAppliesEverySupportedControl(GameTestHelper helper) {
		var actor = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 1, 2));
		actor.configure(TestActorProfile.of(10, ArtifactAlignment.DARKNESS, false,
				true, false, "fireball"));
		helper.assertTrue(actor.profile().rank() == 10 && !actor.profile().consents()
				&& actor.profile().armored() && !actor.profile().mobile()
				&& actor.profile().scriptedAction().equals("fireball"),
				"Actor did not retain its configured player-compatible profile");
		helper.assertTrue(actor.entityTags().contains(SkillSystem.DARKNESS_TAG)
				&& actor.isNoAi() && actor.getArmorValue() >= 20.0F,
				"Actor did not apply alignment, movement, or armour controls");
		helper.assertFalse(PlayerLikeTarget.alwaysConsents(actor),
				"Actor consent setting was ignored by player-compatible targeting");
		var participant = MagicParticipants.resolve(actor).orElseThrow();
		helper.assertTrue(participant.alignment() == MagicParticipant.Alignment.DARKNESS
				&& participant.consentAuthority() == MagicConsentAuthority.NONE,
				"Actor profile did not reach the participant policy used by protections");
		helper.succeed();
	}
}
