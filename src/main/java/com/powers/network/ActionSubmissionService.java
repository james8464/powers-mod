package com.powers.network;

import com.powers.magic.ActionRegistrySnapshot;
import com.powers.magic.ActionSubmissionValidation;
import com.powers.PowersMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Orders revision, canonical-key, and live-context checks before any submission side effect. */
public final class ActionSubmissionService {
	public record Request(long revision, String canonicalKey) { }
	public enum Result { ACCEPTED, RATE_LIMITED, REFRESHED }
	public record RefreshPayload(long revision, String surface) implements CustomPacketPayload {
		public static final Type<RefreshPayload> TYPE = new Type<>(PowersMod.id("action_refresh"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RefreshPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_LONG, RefreshPayload::revision,
						ByteBufCodecs.stringUtf8(16), RefreshPayload::surface, RefreshPayload::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	private ActionSubmissionService() {
	}

	static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(RefreshPayload.TYPE, RefreshPayload.STREAM_CODEC);
	}

	/** Invalidates a menu whose live owner disappeared, carrying the current authoritative revision. */
	public static void refresh(ServerPlayer player, String surface) {
		ServerPlayNetworking.send(player, new RefreshPayload(
				com.powers.magic.runtime.MagicRuntime.catalogue().snapshot().revision(), surface));
	}

	public static Result submit(ActionRegistrySnapshot snapshot, Request request,
			BooleanSupplier contextMatches, Runnable refresh, BooleanSupplier limiter, Runnable mutation) {
		Objects.requireNonNull(request, "request");
		Objects.requireNonNull(contextMatches, "contextMatches");
		Objects.requireNonNull(refresh, "refresh");
		Objects.requireNonNull(limiter, "limiter");
		Objects.requireNonNull(mutation, "mutation");
		if (ActionSubmissionValidation.validate(snapshot, request.revision(), request.canonicalKey())
				== ActionSubmissionValidation.REFRESH
				|| !contextMatches.getAsBoolean()) {
			refresh.run();
			return Result.REFRESHED;
		}
		if (!limiter.getAsBoolean()) return Result.RATE_LIMITED;
		mutation.run();
		return Result.ACCEPTED;
	}
}
