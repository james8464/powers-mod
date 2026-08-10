package com.powers.client.screen;

import com.powers.PowersMod;
import com.powers.client.ClientPowerState;
import com.powers.network.RankPackets;
import com.powers.progression.RankGraph;
import com.powers.progression.RankGraphRegistry;
import com.powers.progression.RankMazeLayout;
import com.powers.progression.RankNode;
import com.powers.progression.RankPresentation;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Interactive, server-authoritative view of the player's non-exclusive title maze. */
public final class RankMazeScreen extends Screen {
	private static final int PANEL_WIDTH = 304;
	private static final int PANEL_HEIGHT = 232;
	private static final int GRAPH_WIDTH = 280;
	private static final int GRAPH_HEIGHT = 166;
	private static final Identifier LIGHT_PANEL =
			PowersMod.id("textures/gui/rank_maze/light_panel.png");
	private static final Identifier DARK_PANEL =
			PowersMod.id("textures/gui/rank_maze/dark_panel.png");

	private RankGraph graph;
	private RankMazeLayout layout;
	private final Map<String, RankMazeLayout.NodeBox> boxes = new HashMap<>();
	private Set<String> completed = Set.of();
	private Set<String> unlockable = Set.of();
	private RankNode selected;
	private Button unlockButton;
	private Button focusButton;

	public RankMazeScreen() {
		super(Component.translatable("screen.powers.rank.title"));
	}

	@Override
	protected void init() {
		boolean darkness = ClientPowerState.darkness();
		graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
		completed = Set.copyOf(ClientPowerState.rankNodes());
		unlockable = graph.unlockable(completed, ClientPowerState.rankDepth());
		layout = RankMazeLayout.arrange(graph, GRAPH_WIDTH, GRAPH_HEIGHT);
		boxes.clear();
		int left = panelX() + 12;
		int top = panelY() + 26;
		for (RankMazeLayout.NodeBox box : layout.nodes()) {
			boxes.put(box.id(), box);
			RankNode node = graph.node(box.id());
			if (node == null) continue;
			RankNodeButton button = new RankNodeButton(left + box.x(), top + box.y(), box.height(), node);
			button.setTooltip(Tooltip.create(Component.literal(node.title() + " — " + perkSummary(node))));
			addRenderableWidget(button);
		}

		unlockButton = addRenderableWidget(Button.builder(Component.translatable("screen.powers.rank.unlock"),
				button -> act(false)).bounds(panelX() + 72, panelY() + 207, 76, 18).build());
		focusButton = addRenderableWidget(Button.builder(Component.translatable("screen.powers.rank.focus"),
				button -> act(true)).bounds(panelX() + 156, panelY() + 207, 76, 18).build());
		select(focusedNode());
	}

	private RankNode focusedNode() {
		RankNode focused = graph.node(ClientPowerState.rankFocus());
		if (focused != null) return focused;
		return graph.nodes().stream().sorted(java.util.Comparator.comparingInt(RankNode::depth)
				.thenComparing(RankNode::id)).findFirst().orElseThrow();
	}

	private void select(RankNode node) {
		selected = node;
		if (unlockButton == null) return;
		unlockButton.active = unlockable.contains(node.id());
		focusButton.active = completed.contains(node.id()) && !node.id().equals(ClientPowerState.rankFocus());
	}

	private void act(boolean focus) {
		if (selected == null) return;
		ClientPlayNetworking.send(new RankPackets.RankActionPayload(selected.id(), focus));
		onClose();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int left = panelX();
		int top = panelY();
		Identifier panel = ClientPowerState.darkness() ? DARK_PANEL : LIGHT_PANEL;
		graphics.blit(RenderPipelines.GUI_TEXTURED, panel, left, top, 0, 0,
				PANEL_WIDTH, PANEL_HEIGHT, 512, 256, 512, 256);
		drawEdges(graphics, left + 12, top + 26);
	}

	private void drawEdges(GuiGraphicsExtractor graphics, int left, int top) {
		if (layout == null) return;
		for (RankMazeLayout.Edge edge : layout.edges()) {
			RankMazeLayout.NodeBox parent = boxes.get(edge.parent());
			RankMazeLayout.NodeBox child = boxes.get(edge.child());
			if (parent == null || child == null) continue;
			int color = completed.contains(edge.parent()) && completed.contains(edge.child())
					? 0xE8FFF0B0 : 0x804F5264;
			drawLine(graphics, left + parent.centerX(), top + parent.centerY(),
					left + child.centerX(), top + child.centerY(), color);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, panelY() + 9,
				ClientPowerState.darkness() ? 0xFFE8C8FF : 0xFFFFF0BE);
		if (selected != null) {
			String status = selected.id().equals(ClientPowerState.rankFocus())
					? Component.translatable("screen.powers.rank.focused").getString()
					: completed.contains(selected.id())
							? Component.translatable("screen.powers.rank.unlocked").getString()
							: unlockable.contains(selected.id())
									? Component.translatable("screen.powers.rank.available").getString()
									: Component.translatable("screen.powers.rank.locked").getString();
			graphics.centeredText(font, Component.literal(selected.title() + " — " + status),
					width / 2, panelY() + 180, 0xFFFFFFFF);
			String summary = font.plainSubstrByWidth(perkSummary(selected), PANEL_WIDTH - 18);
			graphics.centeredText(font, Component.literal(summary), width / 2,
					panelY() + 191, 0xFFC7C2D2);
		}
	}

	private String perkSummary(RankNode node) {
		return RankPresentation.summary(node);
	}

	private int panelX() {
		return Math.max(8, (width - PANEL_WIDTH) / 2);
	}

	private int panelY() {
		return Math.max(4, (height - PANEL_HEIGHT) / 2);
	}

	private static void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
		int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
		for (int step = 0; step <= steps; step++) {
			int x = x1 + (x2 - x1) * step / Math.max(1, steps);
			int y = y1 + (y2 - y1) * step / Math.max(1, steps);
			graphics.fill(x, y, x + 1, y + 1, color);
		}
	}

	/** Rune node button with status conveyed through both border shape and colour. */
	private final class RankNodeButton extends Button {
		private final RankNode node;

		private RankNodeButton(int x, int y, int height, RankNode node) {
			super(x, y, 42, height, Component.literal(node.title()), button -> select(node), DEFAULT_NARRATION);
			this.node = node;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			boolean earned = completed.contains(node.id());
			boolean available = unlockable.contains(node.id());
			boolean focused = node.id().equals(ClientPowerState.rankFocus());
			int fill = focused ? 0xE0AE7D24 : earned ? 0xD02A6370 : available ? 0xD04D3265 : 0xC4141720;
			int border = focused ? 0xFFFFFFA8 : earned ? 0xFF9EF2FF : available ? 0xFFD7A4FF : 0xFF575B66;
			if (isHoveredOrFocused()) border = 0xFFFFFFFF;
			graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), fill);
			graphics.outline(getX(), getY(), getWidth(), getHeight(), border);
			String shortTitle = font.plainSubstrByWidth(node.title(), 35);
			graphics.centeredText(font, Component.literal(shortTitle), getX() + getWidth() / 2,
					getY() + 1, earned || available ? 0xFFFFFFFF : 0xFF8D909A);
		}
	}
}
