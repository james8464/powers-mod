package com.powers.client.screen;

import com.powers.knowledge.KnowledgeAnswer;
import com.powers.knowledge.KnowledgeHistory;
import com.powers.network.KnowledgePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Searchable two-page journal for server-verified Minecraft and POWERS knowledge. */
public final class KnowledgeBookScreen extends Screen {
	private static final KnowledgeHistory HISTORY = new KnowledgeHistory(32);
	private static String pendingQuestion = "";
	private static KnowledgeAnswer latest;

	private EditBox question;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private boolean waiting;

	public KnowledgeBookScreen() {
		super(Component.translatable("screen.powers.knowledge.title"));
	}

	/** Accepts the server answer even if latency outlives the current screen instance. */
	public static void accept(KnowledgePackets.AnswerPayload payload) {
		latest = new KnowledgeAnswer(payload.entryId(), payload.answer(), payload.confidence(),
				payload.sources(), payload.registryIds());
		HISTORY.record(pendingQuestion, latest);
		if (net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof KnowledgeBookScreen screen) {
			screen.waiting = false;
		}
	}

	public static void reset() {
		HISTORY.clear();
		pendingQuestion = "";
		latest = null;
	}

	@Override
	protected void init() {
		panelWidth = Math.min(420, Math.max(300, width - 32));
		panelHeight = Math.min(238, Math.max(190, height - 28));
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;
		question = addRenderableWidget(new EditBox(font, left + 18, top + 32,
				panelWidth - 104, 20, Component.translatable("screen.powers.knowledge.question")));
		question.setMaxLength(256);
		question.setHint(Component.translatable("screen.powers.knowledge.hint"));
		addRenderableWidget(Button.builder(Component.translatable("screen.powers.knowledge.ask"),
				ignored -> ask()).bounds(left + panelWidth - 80, top + 32, 62, 20).build());
		addRenderableWidget(Button.builder(Component.literal("◀"), ignored -> show(HISTORY.previous()))
				.bounds(left + 18, top + panelHeight - 28, 30, 18).build());
		addRenderableWidget(Button.builder(Component.literal("▶"), ignored -> show(HISTORY.next()))
				.bounds(left + 52, top + panelHeight - 28, 30, 18).build());
		setInitialFocus(question);
	}

	private void ask() {
		String value = question.getValue().strip();
		if (value.isEmpty() || waiting) return;
		pendingQuestion = value;
		waiting = true;
		ClientPlayNetworking.send(new KnowledgePackets.AskPayload(value));
	}

	private void show(KnowledgeHistory.Entry entry) {
		if (entry == null) return;
		question.setValue(entry.question());
		latest = entry.answer();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
			ask();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFFF1D9A3);
		graphics.fill(left + panelWidth / 2 - 2, top + 7,
				left + panelWidth / 2 + 2, top + panelHeight - 7, 0xFF9A7049);
		graphics.outline(left, top, panelWidth, panelHeight, 0xFF4A2918);
		graphics.outline(left + 6, top + 6, panelWidth - 12, panelHeight - 12, 0xFFB98A52);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, top + 12, 0xFF3B2418);
		int answerTop = top + 62;
		int textWidth = panelWidth - 36;
		if (waiting) {
			graphics.centeredText(font, Component.translatable("screen.powers.knowledge.consulting"),
					width / 2, answerTop + 24, 0xFF6C4E37);
			return;
		}
		KnowledgeAnswer answer = latest;
		if (answer == null) {
			graphics.textWithWordWrap(font,
					Component.translatable("screen.powers.knowledge.welcome"),
					left + 18, answerTop, textWidth, 0xFF3B2418, false);
			return;
		}
		List<FormattedCharSequence> lines = font.split(Component.literal(answer.answer()), textWidth);
		int maxLines = Math.max(2, (panelHeight - 112) / 10);
		for (int index = 0; index < Math.min(maxLines, lines.size()); index++) {
			graphics.text(font, lines.get(index), left + 18, answerTop + index * 10, 0xFF302019, false);
		}
		int metadataY = top + panelHeight - 47;
		graphics.text(font, Component.translatable("screen.powers.knowledge.confidence",
				Math.round(answer.confidence() * 100.0)), left + 96, metadataY, 0xFF765333, false);
		String sources = answer.sources().isEmpty() ? "offline index"
				: String.join(" · ", answer.sources());
		graphics.text(font, font.split(Component.translatable("screen.powers.knowledge.sources", sources),
				panelWidth - 114).getFirst(), left + 96, metadataY + 11, 0xFF765333, false);
	}
}
