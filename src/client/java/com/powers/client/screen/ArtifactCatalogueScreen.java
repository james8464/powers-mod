package com.powers.client.screen;

import com.powers.client.AbilityGlyphRenderer;
import com.powers.item.artifact.ArtifactActionCategory;
import com.powers.item.artifact.ArtifactActionDefinition;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactCatalogueRules;
import com.powers.item.artifact.ArtifactFavouriteRules;
import com.powers.network.ShadowSwordPackets;
import com.powers.power.abilities.SizeMorphRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.IntStream;

/** Responsive searchable catalogue and eight-slot quick-wheel editor. */
public final class ArtifactCatalogueScreen extends Screen {
	private final ArtifactMenuState state;
	private ArtifactCatalogueRules.Layout layout;
	private List<String> favourites;
	private ArtifactActionDefinition selected;
	private ArtifactActionCategory category;
	private EditBox searchBox;
	private String query = "";
	private int page;
	private int sizeMorphOption;

	ArtifactCatalogueScreen(ArtifactMenuState state) {
		super(Component.translatable("screen.powers.artifact.catalogue.title"));
		this.state = state;
		this.sizeMorphOption = state.sizeMorphOption();
		this.favourites = state.favourites();
	}

	@Override
	protected void init() {
		layout = ArtifactCatalogueRules.layout(width, height);
		if (selected == null) {
			selected = state.action(state.selectedKey());
			if (selected == null) selected = state.actions().getFirst();
		}
		int left = layout.panelX();
		int top = layout.panelY();
		EditBox previousSearch = searchBox;
		searchBox = addRenderableWidget(new EditBox(font, left + 8, top + 25,
				layout.panelWidth() - 82, 18, previousSearch,
				Component.translatable("screen.powers.artifact.catalogue.search")));
		if (previousSearch == null) searchBox.setValue(query);
		searchBox.setMaxLength(48);
		searchBox.setHint(Component.translatable("screen.powers.artifact.catalogue.search_hint"));

		addRenderableWidget(Button.builder(Component.literal("◀"), ignored -> {
			page--;
			rebuildWidgets();
		}).bounds(left + layout.panelWidth() - 68, top + 25, 28, 18).build());
		addRenderableWidget(Button.builder(Component.literal("▶"), ignored -> {
			page++;
			rebuildWidgets();
		}).bounds(left + layout.panelWidth() - 36, top + 25, 28, 18).build());

		int tabY = top + 47;
		int tabWidth = (layout.panelWidth() - 16 - 9) / 4;
		addTab(null, left + 8, tabY, tabWidth,
				Component.translatable("screen.powers.artifact.catalogue.tab.all"));
		for (int index = 0; index < ArtifactActionCategory.values().length; index++) {
			ArtifactActionCategory value = ArtifactActionCategory.values()[index];
			addTab(value, left + 8 + (index + 1) * (tabWidth + 3), tabY, tabWidth,
					ArtifactMenuState.sourceName(value));
		}

		List<ArtifactActionDefinition> filtered = filtered();
		page = Math.clamp(page, 0,
				ArtifactCatalogueRules.pageCount(filtered.size(), layout.pageSize()) - 1);
		List<ArtifactActionDefinition> visible = ArtifactCatalogueRules.page(
				filtered, page, layout.pageSize());
		int contentLeft = left + 8;
		int contentTop = top + 69;
		int columnGap = 4;
		int columnWidth = (layout.panelWidth() - 16
				- columnGap * (layout.columns() - 1)) / layout.columns();
		for (int index = 0; index < visible.size(); index++) {
			ArtifactActionDefinition action = visible.get(index);
			int column = index / layout.rows();
			int row = index % layout.rows();
			ArtifactActionButton button = new ArtifactActionButton(
					contentLeft + column * (columnWidth + columnGap), contentTop + row * 24,
					columnWidth, action);
			button.setTooltip(Tooltip.create(state.tooltip(action)));
			addRenderableWidget(button);
		}

		int favouritesY = top + layout.panelHeight() - 20;
		addSelectionControls(left, favouritesY - 21);
		int favouriteWidth = (layout.panelWidth() - 16 - 14) / 8;
		for (int slot = 0; slot < 8; slot++) {
			int favouriteSlot = slot;
			ArtifactActionDefinition favourite = state.action(favourites.get(slot));
			Button button = addRenderableWidget(Button.builder(
					Component.literal(Integer.toString(slot + 1)), ignored -> assign(favouriteSlot))
					.bounds(left + 8 + slot * (favouriteWidth + 2), favouritesY,
							favouriteWidth, 18).build());
			button.active = selected != null && !state.locked(selected);
			button.setTooltip(Tooltip.create(favourite == null
					? Component.translatable("screen.powers.artifact.catalogue.empty_favourite")
					: Component.translatable("screen.powers.artifact.catalogue.favourite",
							slot + 1, state.actionName(favourite))));
		}
		setInitialFocus(searchBox);
	}

	@Override
	public void tick() {
		if (searchBox != null && !searchBox.getValue().equals(query)) {
			query = searchBox.getValue();
			page = 0;
			rebuildWidgets();
		}
	}

	private void addTab(ArtifactActionCategory value, int x, int y, int width, Component label) {
		Button button = addRenderableWidget(Button.builder(label, ignored -> {
			category = value;
			page = 0;
			rebuildWidgets();
		}).bounds(x, y, width, 18).build());
		button.active = value != category;
	}

	private void addSelectionControls(int left, int y) {
		int panelWidth = layout.panelWidth();
		if (selected != null && selected.abilityId().equals("size_shift")) {
			addRenderableWidget(CycleButton.<Integer>builder(
					option -> Component.literal(SizeMorphRules.scale(option) + "×"), () -> sizeMorphOption)
					.withValues(IntStream.range(0, SizeMorphRules.scales().size()).boxed().toList())
					.displayOnlyValue().create(left + 8, y, panelWidth / 2 - 12, 18,
							Component.translatable("screen.powers.shadow_sword.variant"),
							(button, option) -> sizeMorphOption = option));
		}
		Button bind = addRenderableWidget(Button.builder(
				Component.translatable("screen.powers.artifact.catalogue.select"), ignored -> choose())
				.bounds(left + panelWidth / 2 + 4, y, panelWidth / 2 - 12, 18).build());
		bind.active = selected != null && !state.locked(selected);
	}

	private List<ArtifactActionDefinition> filtered() {
		return ArtifactCatalogueRules.filter(state.actions(), category, query,
				action -> state.actionName(action).getString());
	}

	private void select(ArtifactActionDefinition action) {
		selected = action;
		rebuildWidgets();
	}

	private void assign(int slot) {
		if (selected == null || state.locked(selected)) return;
		favourites = ArtifactFavouriteRules.assign(favourites, slot, selected.key());
		ClientPlayNetworking.send(new ShadowSwordPackets.BindFavouritePayload(
				state.alignment().serializedName(), slot, selected.key()));
		rebuildWidgets();
	}

	private void choose() {
		if (selected == null || state.locked(selected)) return;
		int option = selected.abilityId().equals("size_shift") ? sizeMorphOption : -1;
		ClientPlayNetworking.send(new ShadowSwordPackets.SelectPayload(
				state.alignment().serializedName(), selected.key(), option));
		minecraft.gui.setScreen(null);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(new ShadowSwordScreen(state.withFavourites(favourites)));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int accent = accent();
		graphics.fill(layout.panelX(), layout.panelY(),
				layout.panelX() + layout.panelWidth(), layout.panelY() + layout.panelHeight(),
				state.alignment() == ArtifactAlignment.DARKNESS ? 0xF0140E1A : 0xF0191720);
		graphics.outline(layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight(), accent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, layout.panelY() + 8, accent());
		graphics.text(font, Component.translatable("screen.powers.artifact.catalogue.summary",
				state.rank(), state.energy(), filtered().size()), layout.panelX() + 8,
				layout.panelY() + 14, 0xFFBDB5C7, false);
		graphics.text(font, Component.translatable("screen.powers.artifact.catalogue.favourites"),
				layout.panelX() + 8, layout.panelY() + layout.panelHeight() - 29,
				0xFFD8D1DF, false);
		int pages = ArtifactCatalogueRules.pageCount(filtered().size(), layout.pageSize());
		graphics.text(font, (page + 1) + "/" + pages,
				layout.panelX() + layout.panelWidth() - 62, layout.panelY() + 30,
				0xFFBDB5C7, false);
	}

	private int accent() {
		return state.alignment() == ArtifactAlignment.DARKNESS ? 0xFFC89AD4 : 0xFFFFE7A0;
	}

	private Component rowLabel(ArtifactActionDefinition action) {
		String stateText = state.locked(action)
				? Component.translatable("screen.powers.artifact.row.locked", action.requiredRank()).getString()
				: state.active(action) ? Component.translatable("screen.powers.artifact.row.active").getString()
				: state.cooldown(action) > 0 ? Component.translatable("screen.powers.artifact.row.cooldown",
						(state.cooldown(action) + 19) / 20).getString()
				: Component.translatable("screen.powers.artifact.row.ready").getString();
		return Component.empty().append("    ").append(state.actionName(action)).append("  ")
				.append(Component.translatable("screen.powers.artifact.row.cost", state.cost(action)))
				.append("  ").append(stateText);
	}

	private final class ArtifactActionButton extends Button {
		private final ArtifactActionDefinition action;

		private ArtifactActionButton(int x, int y, int width, ArtifactActionDefinition action) {
			super(x, y, width, 20, rowLabel(action), ignored -> select(action), DEFAULT_NARRATION);
			this.action = action;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			int fill = selected == action ? 0xE04C3B59
					: isHoveredOrFocused() ? 0xD0393045 : 0xC0221B28;
			graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), fill);
			graphics.outline(getX(), getY(), getWidth(), getHeight(),
					state.locked(action) ? 0xFF55505C : accent());
			AbilityGlyphRenderer.draw(graphics, action.abilityId(), getX() + 12, getY() + 10,
					state.locked(action) ? 0xFF68636D : accent());
			String label = font.plainSubstrByWidth(getMessage().getString(), getWidth() - 28);
			graphics.text(font, label, getX() + 23, getY() + 6,
					state.locked(action) ? 0xFF77727C : 0xFFF1EDF5, false);
		}
	}
}
