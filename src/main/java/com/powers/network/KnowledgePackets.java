package com.powers.network;

import com.powers.PowersMod;
import com.powers.knowledge.KnowledgeAnswer;
import com.powers.knowledge.KnowledgeService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.List;

/** Bounded question/answer protocol for the server-authoritative Knowledge Book. */
public final class KnowledgePackets {
	private static final StreamCodec<io.netty.buffer.ByteBuf, String> QUESTION_CODEC =
			ByteBufCodecs.stringUtf8(256);
	private static final StreamCodec<io.netty.buffer.ByteBuf, String> ANSWER_CODEC =
			ByteBufCodecs.stringUtf8(8_192);
	private static final StreamCodec<io.netty.buffer.ByteBuf, String> SOURCE_CODEC =
			ByteBufCodecs.stringUtf8(256);
	private static final StreamCodec<io.netty.buffer.ByteBuf, List<String>> SOURCE_LIST_CODEC =
			SOURCE_CODEC.apply(ByteBufCodecs.list(16));

	public record OpenPayload() implements CustomPacketPayload {
		public static final Type<OpenPayload> TYPE = new Type<>(PowersMod.id("open_knowledge_book"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> STREAM_CODEC =
				StreamCodec.unit(new OpenPayload());
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record AskPayload(String question) implements CustomPacketPayload {
		public static final Type<AskPayload> TYPE = new Type<>(PowersMod.id("ask_knowledge_book"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AskPayload> STREAM_CODEC =
				StreamCodec.composite(QUESTION_CODEC, AskPayload::question, AskPayload::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record AnswerPayload(String entryId, String answer, double confidence,
			List<String> sources, List<String> registryIds) implements CustomPacketPayload {
		public static final Type<AnswerPayload> TYPE = new Type<>(PowersMod.id("knowledge_answer"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AnswerPayload> STREAM_CODEC =
				StreamCodec.composite(
						SOURCE_CODEC, AnswerPayload::entryId,
						ANSWER_CODEC, AnswerPayload::answer,
						ByteBufCodecs.DOUBLE, AnswerPayload::confidence,
						SOURCE_LIST_CODEC, AnswerPayload::sources,
						SOURCE_LIST_CODEC, AnswerPayload::registryIds,
						AnswerPayload::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

		public AnswerPayload(KnowledgeAnswer answer) {
			this(answer.entryId(), answer.answer(), answer.confidence(), answer.sources(), answer.registryIds());
		}
	}

	private KnowledgePackets() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(OpenPayload.TYPE, OpenPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AskPayload.TYPE, AskPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(AnswerPayload.TYPE, AnswerPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AskPayload.TYPE, (payload, context) ->
				context.server().execute(() -> answer(context.player(), payload.question())));
	}

	public static void open(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, OpenPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenPayload());
		}
	}

	private static void answer(ServerPlayer player, String question) {
		if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.KNOWLEDGE)
				|| question == null || question.isBlank() || question.length() > 256
				|| !(player.getMainHandItem().is(Items.KNOWLEDGE_BOOK)
						|| player.getOffhandItem().is(Items.KNOWLEDGE_BOOK))) return;
		KnowledgeService.answerAsync(player, question).thenAccept(answer ->
				player.level().getServer().execute(() -> {
					if (!player.isRemoved() && player.connection != null
							&& ServerPlayNetworking.canSend(player, AnswerPayload.TYPE)) {
						ServerPlayNetworking.send(player, new AnswerPayload(answer));
					}
				}));
	}
}
