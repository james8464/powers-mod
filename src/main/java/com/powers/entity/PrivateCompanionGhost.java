package com.powers.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Render shell for the owner-private darkness companion.
 *
 * <p>The server never adds this type to a world. The authorised owner's client
 * creates one local instance from private state packets, so the apparition has
 * no collision, AI, tracking, persistence, or visibility to other players.</p>
 */
public final class PrivateCompanionGhost extends AbstractPlayerLikeMob {
	public PrivateCompanionGhost(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setNoGravity(true);
		noPhysics = true;
		setSilent(true);
		setInvulnerable(true);
	}

	@Override
	protected void registerTargetGoals() {
		// Client-local render shell: it deliberately has no targets.
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		// Defensive invariant: a malformed /summon must never create a real mob.
		discard();
	}
}
