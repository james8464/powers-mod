package com.powers.power.abilities;

import com.powers.fx.GodlyPunishment;
import com.powers.fx.PowerFx;
import com.powers.power.travel.DestinationFailure;
import com.powers.util.PowerMessages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Presents the magical consequence of a rejected player-controlled journey. */
final class TravelFailurePresenter {
	private TravelFailurePresenter() {
	}

	/** Emits the bounded effect and concise feedback corresponding to a travel rejection. */
	static void report(ServerPlayer caster, LivingEntity subject, Vec3 target, DestinationFailure failure) {
		ServerLevel origin = (ServerLevel) subject.level();
		switch (failure) {
			case ANCHOR -> {
				if (subject instanceof ServerPlayer player) GodlyPunishment.chainBlock(origin, player);
				else PowerFx.rune(origin, subject.position().add(0, 1, 0), 1.8, 0xB36BFF, 24, 0.0);
				PowerMessages.send(caster, "ability.powers.anchored_teleport_blocked", 4);
			}
			case WARD -> {
				PowerFx.clash(origin, subject.position().add(0, 1, 0), target.add(0, 1, 0),
						0xFFD4FF, 0xB36BFF);
				subject.hurtServer(origin, subject.damageSources().magic(), 20.0f);
				if (subject instanceof ServerPlayer player) GodlyPunishment.strike(origin, player, 0xB36BFF, false);
				PowerMessages.send(caster, "amethyst.powers.teleport_repelled", 5);
			}
			case REALM_RESTRICTED -> {
				if (subject instanceof ServerPlayer player) GodlyPunishment.barrier(origin, player, 0x82CAFF);
				else PowerFx.rune(origin, subject.position().add(0, 1, 0), 2.0, 0x82CAFF, 24, 0.0);
				PowerMessages.send(caster, "ability.powers.no_entry", 4);
			}
			case OUT_OF_BOUNDS, UNLOADED_CHUNK -> PowerMessages.send(caster, "ability.powers.out_of_bounds", 3);
			case SAFE_ZONE -> PowerMessages.send(caster, "ability.powers.no_entry", 4);
			case COLLISION, HAZARD -> PowerMessages.send(caster, "ability.powers.solid_block", 3);
			case NONE -> { }
		}
	}
}
