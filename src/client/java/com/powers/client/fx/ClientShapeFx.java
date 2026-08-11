package com.powers.client.fx;

import com.powers.fx.ShapeFxKind;
import com.powers.network.MagicFxPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/** Expands one bounded semantic ring, rune, or spiral entirely on the client. */
public final class ClientShapeFx {
	private ClientShapeFx() {
	}

	public static void handle(MagicFxPackets.ShapeFxPayload payload) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return;
		DustParticleOptions dust = new DustParticleOptions(payload.color(), 1.0F);
		int count = Math.max(1, (int) Math.round(payload.count() * FxAccessibility.effectScale(client)));
		for (int index = 0; index < count; index++) {
			if (payload.kind() == ShapeFxKind.RUNE) spawnRunePoint(client, payload, dust, index, count);
			else spawnSimplePoint(client, payload, dust, index, count);
		}
	}

	private static void spawnSimplePoint(Minecraft client, MagicFxPackets.ShapeFxPayload payload,
			ParticleOptions particle, int index, int count) {
		double progress = index / (double) Math.max(1, count - 1);
		double turns = payload.kind() == ShapeFxKind.SPIRAL ? 2.0 : 1.0;
		double angle = payload.phase() + progress * Math.PI * 2.0 * turns;
		double y = payload.kind() == ShapeFxKind.SPIRAL ? progress * payload.height() : 0.0;
		add(client, payload, particle, angle, payload.radius(), y);
	}

	private static void spawnRunePoint(Minecraft client, MagicFxPackets.ShapeFxPayload payload,
			DustParticleOptions dust, int index, int count) {
		int family = index % 5;
		double progress = index / (double) Math.max(1, count - 1);
		if (family < 2) {
			add(client, payload, dust, payload.phase() + progress * Math.PI * 5.0,
					payload.radius(), 0.0);
		} else if (family < 4) {
			add(client, payload, ParticleTypes.END_ROD,
					payload.phase() + progress * Math.PI * 5.0, payload.radius(), 0.15);
		} else {
			double angle = payload.phase() + Math.PI / 8.0 + progress * Math.PI * 10.0;
			add(client, payload, dust, angle, payload.radius() * 0.55,
					progress * payload.height());
		}
	}

	private static void add(Minecraft client, MagicFxPackets.ShapeFxPayload payload,
			ParticleOptions particle, double angle, double radius, double y) {
		client.level.addParticle(particle,
				payload.x() + Math.cos(angle) * radius,
				payload.y() + y,
				payload.z() + Math.sin(angle) * radius,
				0.0, 0.0, 0.0);
	}
}
