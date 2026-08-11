package com.powers.protection;

import com.powers.ImportedPackItems;
import com.powers.fx.PowerFx;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.testing.TestingOverrides;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;


/** Server-thread payment and presentation for Empyrean Jewel consent overrides. */
public final class ConsentOverrideRuntime {
	private static final ConsentPaymentLedger PAYMENTS = new ConsentPaymentLedger();

	private ConsentOverrideRuntime() {
	}

	public static boolean authorize(ServerPlayer caster, ServerPlayer target,
			ConsentKind kind, boolean ordinaryConsent) {
		boolean self = caster == target;
		boolean safeZone = !self && PowerProtection.isSafeZone(
				(ServerLevel) target.level(), target.position());
		boolean hasJewel = carriesEmpyreanJewel(caster);
		long tick = caster.level().getServer().getTickCount();
		boolean alreadyPaid = !PAYMENTS.requiresPayment(
				tick, caster.getUUID(), target.getUUID(), kind);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(caster);
		boolean enoughEnergy = alreadyPaid || TestingOverrides.energyDisabled(caster.getUUID())
				|| (long) data.energy() + com.powers.item.ArtifactEnergyReservoir.totalStored(caster)
				>= ConsentOverrideRules.OVERRIDE_ENERGY_SURCHARGE;
		ConsentOverrideRules.Decision decision = ConsentOverrideRules.decide(
				self, safeZone, ordinaryConsent, hasJewel, enoughEnergy);
		if (decision == ConsentOverrideRules.Decision.ALLOW_FREE) return true;
		if (decision != ConsentOverrideRules.Decision.ALLOW_OVERRIDE) {
			if (decision == ConsentOverrideRules.Decision.DENY_ENERGY) {
				PowerMessages.overlay(caster, Component.translatable("artifact.powers.empyrean.no_energy"));
			}
			return false;
		}
		if (alreadyPaid) return true;
		if (!data.consumeEnergy(ConsentOverrideRules.OVERRIDE_ENERGY_SURCHARGE)) return false;
		PAYMENTS.recordPayment(tick, caster.getUUID(), target.getUUID(), kind);
		PowersPackets.syncTo(caster);
		ServerLevel level = (ServerLevel) caster.level();
		PowerFx.clash(level, caster.getEyePosition(), target.getEyePosition(), 0x7D3FB2, 0xE8C96A);
		PowerFx.rune(level, target.position().add(0.0, 0.08, 0.0), 1.35,
				0xE8C96A, 18, tick * 0.07);
		PowerFx.sound(level, caster.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.9F, 0.62F);
		PowerMessages.overlay(caster, Component.translatable("artifact.powers.empyrean.override",
				target.getDisplayName(), kind.name().toLowerCase(java.util.Locale.ROOT)));
		PowerMessages.overlay(target, Component.translatable("artifact.powers.empyrean.overridden",
				caster.getDisplayName(), kind.name().toLowerCase(java.util.Locale.ROOT)));
		return true;
	}

	public static boolean carriesEmpyreanJewel(ServerPlayer player) {
		var jewel = ImportedPackItems.item("imported_artifact_emperyeanjewel");
		return jewel != null && player.getInventory().contains(stack -> stack.is(jewel));
	}

	public static void clear() {
		PAYMENTS.clear();
	}
}
