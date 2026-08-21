package com.powers.client.screen;
import com.powers.client.AbilityGlyphRenderer;
import com.powers.cooldown.CooldownPresentation;
import com.powers.item.artifact.ArtifactActionDefinition;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactCatalogueRules;
import com.powers.item.artifact.ArtifactCatalogueTab;
import com.powers.item.artifact.ArtifactCatalogueViewModel;
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
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
/** Fixed-widget virtual catalogue and server-authoritative eight-slot loadout editor. */
public final class ArtifactCatalogueScreen extends Screen {
	private ArtifactMenuState state;
	private ArtifactCatalogueRules.Layout layout;
	private ArtifactCatalogueViewModel model;
	private List<String> favourites;
	private ArtifactActionDefinition selected;
	private ArtifactCatalogueTab tab = ArtifactCatalogueTab.FAVOURITES;
	private final List<ArtifactActionButton> actionButtons = new ArrayList<>();
	private final List<Button> tabButtons = new ArrayList<>();
	private final List<Button> favouriteButtons = new ArrayList<>();
	private EditBox searchBox;
	private CycleButton<Integer> sizeMorphControl;
	private CycleButton<Integer> gravityControl;
	private Button selectButton;
	private String query = "";
	private int sizeMorphOption;
	private int gravityOption;
	private int actionWidgetAllocations;
	private boolean lastBindNonOptimistic = true;
	ArtifactCatalogueScreen(ArtifactMenuState state) {
		super(Component.translatable("screen.powers.artifact.catalogue.title"));
		this.state = state;
		this.sizeMorphOption = state.sizeMorphOption();
		this.gravityOption = gravityOption(state);
		this.favourites = state.favourites();
	}
	@Override
	protected void init() {
		layout = ArtifactCatalogueRules.layout(width, height);
		String preservedSelection = model == null ? state.selectedKey() : model.selectedKey();
		ArtifactCatalogueTab preservedTab = model == null ? tab : model.tab();
		String preservedQuery = model == null ? query : model.query();
		model = new ArtifactCatalogueViewModel(state.revision(), state.actions(), favourites,
				state.recents(), preservedSelection, action -> state.actionName(action).getString(),
				layout.columns(), layout.rows());
		model.setFilter(preservedTab, preservedQuery);
		tab = preservedTab;
		query = preservedQuery;
		selected = model.selected();
		int left = layout.panelX();
		int top = layout.panelY();
		EditBox previousSearch = searchBox;
		searchBox = addRenderableWidget(new EditBox(font, left + 8, top + 32,
				layout.panelWidth() - 16, 18, previousSearch,
				Component.translatable("screen.powers.artifact.catalogue.search")));
		if (previousSearch == null) searchBox.setValue(query);
		searchBox.setMaxLength(48);
		searchBox.setHint(Component.translatable("screen.powers.artifact.catalogue.search_hint"));
		tabButtons.clear();
		int tabY = top + 54;
		int tabWidth = Math.max(24, (layout.panelWidth() - 28) / ArtifactCatalogueTab.values().length);
		for (int index = 0; index < ArtifactCatalogueTab.values().length; index++) {
			ArtifactCatalogueTab value = ArtifactCatalogueTab.values()[index];
			Button button = addRenderableWidget(Button.builder(tabLabel(value), ignored -> setTab(value))
					.bounds(left + 8 + index * (tabWidth + 3), tabY, tabWidth, 18).build());
			tabButtons.add(button);
		}
		actionButtons.clear();
		actionWidgetAllocations = 0;
		int contentLeft = left + 8;
		int contentTop = top + 76;
		int columnGap = 4;
		int columnWidth = (layout.panelWidth() - 16
				- columnGap * (layout.columns() - 1)) / layout.columns();
		for (int slot = 0; slot < model.poolSize(); slot++) {
			int column = ArtifactCatalogueRules.columnForSlot(slot, layout.rows());
			int row = ArtifactCatalogueRules.rowForSlot(slot, layout.rows());
			ArtifactActionButton button = new ArtifactActionButton(
					contentLeft + column * (columnWidth + columnGap), contentTop + row * 24,
					columnWidth, slot);
			actionButtons.add(addRenderableWidget(button));
			actionWidgetAllocations++;
		}
		int favouritesY = top + layout.panelHeight() - 20;
		addSelectionControls(left, favouritesY - 21);
		favouriteButtons.clear();
		int favouriteWidth = (layout.panelWidth() - 30) / ArtifactFavouriteRules.SLOT_COUNT;
		for (int slot = 0; slot < ArtifactFavouriteRules.SLOT_COUNT; slot++) {
			int favouriteSlot = slot;
			Button button = addRenderableWidget(Button.builder(
					Component.literal(Integer.toString(slot + 1)), ignored -> assign(favouriteSlot))
					.bounds(left + 8 + slot * (favouriteWidth + 2), favouritesY,
							favouriteWidth, 18).build());
			favouriteButtons.add(button);
		}
		refreshWidgets();
		setInitialFocus(searchBox);
	}
	@Override
	public void tick() {
		if (searchBox != null && !searchBox.getValue().equals(query)) {
			query = searchBox.getValue();
			model.setFilter(tab, query);
			refreshWidgets();
			}
		}
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
			double verticalAmount) {
		if (verticalAmount != 0.0 && insidePanel(mouseX, mouseY)) {
			model.scrollRows(verticalAmount > 0.0 ? -1 : 1);
			refreshActionWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
	@Override
	public boolean keyPressed(KeyEvent event) {
		int delta = switch (event.key()) {
			case GLFW.GLFW_KEY_UP -> -1;
			case GLFW.GLFW_KEY_DOWN -> 1;
			case GLFW.GLFW_KEY_LEFT -> -layout.rows();
			case GLFW.GLFW_KEY_RIGHT -> layout.rows();
			case GLFW.GLFW_KEY_PAGE_UP -> -model.poolSize();
			case GLFW.GLFW_KEY_PAGE_DOWN -> model.poolSize();
			default -> 0;
		};
		boolean horizontalEditing = searchBox != null && searchBox.isFocused()
				&& (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT);
		if (delta != 0 && !horizontalEditing) {
			model.moveSelection(delta);
			selected = model.selected();
			refreshWidgets();
			focusSelectedButton();
			return true;
		}
		return super.keyPressed(event);
	}
	/** Applies a newer server-authored menu without losing a still-valid local catalogue context. */
	public boolean acceptRefresh(ArtifactMenuState next) {
		if (next == null || next.alignment() != state.alignment()) return false;
		state = next;
		favourites = next.favourites();
		sizeMorphOption = next.sizeMorphOption();
		gravityOption = gravityOption(next);
		if (model != null) {
			model.refresh(next.revision(), next.actions(), favourites, next.recents(),
					next.selectedKey(), action -> next.actionName(action).getString());
			selected = model.selected();
			refreshWidgets();
		}
		return true;
	}

	private void setTab(ArtifactCatalogueTab value) {
		tab = value;
		model.setFilter(tab, query);
		refreshWidgets();
	}

	private void addSelectionControls(int left, int y) {
		int panelWidth = layout.panelWidth();
		sizeMorphControl = addRenderableWidget(CycleButton.<Integer>builder(
				option -> Component.literal(SizeMorphRules.scale(option) + "×"), () -> sizeMorphOption)
				.withValues(IntStream.range(0, SizeMorphRules.scales().size()).boxed().toList())
				.displayOnlyValue().create(left + 8, y, panelWidth / 2 - 12, 18,
						Component.translatable("screen.powers.shadow_sword.variant"),
						(button, option) -> sizeMorphOption = option));
		gravityControl = addRenderableWidget(CycleButton.<Integer>builder(
				option -> Component.translatable(switch (option) {
					case 0 -> "ability.powers.gravity_displacement.mode.pull";
					case 1 -> "ability.powers.gravity_displacement.mode.orbit";
					default -> "ability.powers.gravity_displacement.mode.repel";
				}), () -> gravityOption).withValues(List.of(0, 1, 2)).displayOnlyValue()
				.create(left + 8, y, panelWidth / 2 - 12, 18,
						Component.translatable("screen.powers.shadow_sword.variant"),
						(button, option) -> gravityOption = option));
		selectButton = addRenderableWidget(Button.builder(
				Component.translatable("screen.powers.artifact.catalogue.select"), ignored -> choose())
				.bounds(left + panelWidth / 2 + 4, y, panelWidth / 2 - 12, 18).build());
	}

	private void selectSlot(int slot) {
		model.selectVisible(slot);
		selected = model.selected();
		refreshWidgets();
	}

	private void assign(int slot) {
		ArtifactCatalogueViewModel.BindingIntent binding = model.bind(slot);
		if (binding == null || selected == null || state.locked(selected)) return;
		List<String> beforeSend = favourites;
		ClientPlayNetworking.send(new ShadowSwordPackets.BindFavouritePayload(
				state.revision(), state.alignment().serializedName(), binding.slot(), binding.actionKey()));
		lastBindNonOptimistic = favourites.equals(beforeSend);
	}

	private void choose() {
		if (selected == null || state.locked(selected)) return;
		int option = switch (selected.abilityId()) {
			case "size_shift" -> sizeMorphOption;
			case "gravity_displacement" -> gravityOption;
			default -> -1;
		};
		ClientPlayNetworking.send(new ShadowSwordPackets.SelectPayload(
				state.revision(), state.alignment().serializedName(), selected.key(), option));
		minecraft.gui.setScreen(null);
	}

	private void refreshWidgets() {
		refreshActionWidgets();
		for (int index = 0; index < tabButtons.size(); index++) {
			tabButtons.get(index).active = !query.isBlank()
					|| ArtifactCatalogueTab.values()[index] != tab;
		}
		boolean selectable = selected != null && !state.locked(selected);
		if (selectButton != null) selectButton.active = selectable;
		if (sizeMorphControl != null) sizeMorphControl.visible = selected != null
				&& selected.abilityId().equals("size_shift");
		if (gravityControl != null) gravityControl.visible = selected != null
				&& selected.abilityId().equals("gravity_displacement");
		for (int slot = 0; slot < favouriteButtons.size(); slot++) {
			ArtifactActionDefinition favourite = state.action(favourites.get(slot));
			Button button = favouriteButtons.get(slot);
			button.active = selectable;
			button.setTooltip(Tooltip.create(favourite == null
					? Component.translatable("screen.powers.artifact.catalogue.empty_favourite")
					: Component.translatable("screen.powers.artifact.catalogue.favourite",
							slot + 1, state.actionName(favourite))));
		}
	}

	private void refreshActionWidgets() {
		if (model == null) return;
		List<ArtifactActionDefinition> visible = model.visible();
		for (int slot = 0; slot < actionButtons.size(); slot++) {
			actionButtons.get(slot).bind(slot < visible.size() ? visible.get(slot) : null);
		}
		if (getFocused() instanceof ArtifactActionButton focused && (!focused.visible || focused.action == null
				|| !focused.action.key().equals(model.selectedKey()))) {
			setFocused(searchBox);
		}
	}

	private void focusSelectedButton() {
		for (ArtifactActionButton button : actionButtons) {
			if (button.visible && button.action != null
					&& button.action.key().equals(model.selectedKey())) {
				setFocused(button);
				return;
			}
		}
		setFocused(searchBox);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(new ShadowSwordScreen(state.withFavourites(favourites)));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.fill(layout.panelX(), layout.panelY(),
				layout.panelX() + layout.panelWidth(), layout.panelY() + layout.panelHeight(),
				state.alignment() == ArtifactAlignment.DARKNESS ? 0xF0140E1A : 0xF0191720);
		graphics.outline(layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight(), accent());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, layout.panelY() + 6, accent());
		graphics.text(font, Component.translatable("screen.powers.artifact.catalogue.summary",
				Component.translatable("screen.powers.artifact.alignment."
						+ state.alignment().serializedName()), state.rank(), state.energy(), model.filteredCount()),
				layout.panelX() + 8, layout.panelY() + 20, 0xFFBDB5C7, false);
		graphics.text(font, Component.translatable("screen.powers.artifact.catalogue.favourites"),
				layout.panelX() + 8, layout.panelY() + layout.panelHeight() - 53, 0xFFD8D1DF, false);
		if (model.filteredCount() > 0) {
			int first = model.firstVisibleIndex() + 1;
			int last = model.firstVisibleIndex() + model.visible().size();
			graphics.text(font, Component.translatable("screen.powers.artifact.catalogue.position",
					first, last, model.filteredCount()), layout.panelX() + layout.panelWidth() - 112,
					layout.panelY() + 20, 0xFFBDB5C7, false);
		}
	}

	int actionWidgetCount() {
		return actionButtons.size();
	}

	int actionWidgetAllocations() {
		return actionWidgetAllocations;
	}

	int filteredCount() {
		return model == null ? 0 : model.filteredCount();
	}

	String firstActionLabel() {
		return actionButtons.isEmpty() || !actionButtons.getFirst().visible
				? "" : actionButtons.getFirst().getMessage().getString();
	}

	void verificationQuery(String value, ArtifactCatalogueTab filter) {
		query = value == null ? "" : value;
		if (searchBox != null) searchBox.setValue(query);
		setTab(filter);
	}

	void verificationSearchValue(String value) {
		if (searchBox != null) searchBox.setValue(value == null ? "" : value);
	}

	String selectedKey() {
		return model == null ? state.selectedKey() : model.selectedKey();
	}

	String focusedActionKey() {
		return getFocused() instanceof ArtifactActionButton button && button.action != null
				? button.action.key() : "";
	}

	String focusedNarrationText() {
		return getFocused() instanceof ArtifactActionButton button ? button.getMessage().getString() : "";
	}

	boolean hiddenActionHasFocus() {
		return getFocused() instanceof ArtifactActionButton button && !button.visible;
	}

	String favouriteKey(int slot) {
		return slot >= 0 && slot < favourites.size() ? favourites.get(slot) : "";
	}

	int firstVisibleIndex() {
		return model == null ? 0 : model.firstVisibleIndex();
	}

	boolean lastBindNonOptimistic() {
		return lastBindNonOptimistic;
	}

	boolean noCategoryTabSelected() {
		return !tabButtons.isEmpty() && tabButtons.stream().allMatch(button -> button.active);
	}

	String selectedCategoryTab() {
		for (int index = 0; index < tabButtons.size(); index++) {
			if (!tabButtons.get(index).active) return ArtifactCatalogueTab.values()[index].name();
		}
		return "";
	}

	void verificationKey(int key) {
		keyPressed(new KeyEvent(key, 0, 0));
	}

	ArtifactMenuState verificationState() {
		return state;
	}

	private boolean insidePanel(double x, double y) {
		return x >= layout.panelX() && x < layout.panelX() + layout.panelWidth()
				&& y >= layout.panelY() + 65 && y < layout.panelY() + layout.panelHeight() - 42;
	}

	private int accent() {
		return state.alignment() == ArtifactAlignment.DARKNESS ? 0xFFC89AD4 : 0xFFFFE7A0;
	}

	private Component rowLabel(ArtifactActionDefinition action) {
		String stateText = state.locked(action)
				? Component.translatable("screen.powers.artifact.row.locked", action.requiredRank()).getString()
				: state.active(action) ? Component.translatable("screen.powers.artifact.row.active").getString()
				: state.cooldown(action) > 0 ? Component.translatable("screen.powers.artifact.row.cooldown",
						CooldownPresentation.wholeSeconds(state.cooldown(action))).getString()
				: Component.translatable("screen.powers.artifact.row.ready").getString();
		return Component.empty().append("    ").append(state.actionName(action)).append("  ")
				.append(Component.translatable("screen.powers.artifact.row.cost", state.cost(action)))
				.append("  ").append(stateText);
	}

	private static Component tabLabel(ArtifactCatalogueTab value) {
		return Component.translatable("screen.powers.artifact.catalogue.tab."
				+ value.name().toLowerCase(Locale.ROOT));
	}

	private static int gravityOption(ArtifactMenuState state) {
		ArtifactActionDefinition gravity = state.actions().stream()
				.filter(action -> action.abilityId().equals("gravity_displacement"))
				.findFirst().orElse(null);
		return gravity == null ? 1 : com.powers.item.artifact.ArtifactMenuRules
				.normalizeGravityOption(state.variant(gravity));
	}

	private final class ArtifactActionButton extends Button {
		private ArtifactActionDefinition action;

		private ArtifactActionButton(int x, int y, int width, int slot) {
			super(x, y, width, 20, Component.empty(), ignored -> selectSlot(slot), DEFAULT_NARRATION);
		}

		private void bind(ArtifactActionDefinition next) {
			action = next;
			visible = next != null;
			active = next != null;
			if (next != null) {
				setMessage(rowLabel(next));
				setTooltip(Tooltip.create(state.tooltip(next)));
			} else {
				setMessage(Component.empty());
				setTooltip(null);
			}
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			if (action == null) return;
			int fill = action.key().equals(model.selectedKey()) ? 0xE04C3B59
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
