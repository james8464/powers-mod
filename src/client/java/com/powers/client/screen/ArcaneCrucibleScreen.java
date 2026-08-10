package com.powers.client.screen;

import com.powers.forge.ArcaneCrucibleMenu;
import com.powers.forge.CrucibleChoice;
import com.powers.forge.CrucibleOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Vanilla-scale two-slot Crucible surface with a server-validated choice selector. */
public final class ArcaneCrucibleScreen extends AbstractContainerScreen<ArcaneCrucibleMenu> {
	private Button previous;
	private Button next;
	private Button transmute;

	public ArcaneCrucibleScreen(ArcaneCrucibleMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
		titleLabelX = 8;
		titleLabelY = 6;
		inventoryLabelX = 8;
		inventoryLabelY = 72;
	}

	@Override
	protected void init() {
		super.init();
		previous = addRenderableWidget(Button.builder(Component.literal("‹"), button -> select(-1))
				.bounds(leftPos + 69, topPos + 34, 18, 20).build());
		next = addRenderableWidget(Button.builder(Component.literal("›"), button -> select(1))
				.bounds(leftPos + 89, topPos + 34, 18, 20).build());
		transmute = addRenderableWidget(Button.builder(
				Component.translatable("screen.powers.arcane_crucible.transmute"), button -> transmute())
				.bounds(leftPos + 55, topPos + 58, 66, 18).build());
		updateButtons();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		updateButtons();
	}

	private void select(int direction) {
		List<CrucibleChoice> choices = menu.choices();
		if (choices.isEmpty()) return;
		int selected = Math.floorMod(menu.selectedIndex() + direction, choices.size());
		menu.clickMenuButton(minecraft.player, 100 + selected);
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + selected);
	}

	private void transmute() {
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
	}

	private void updateButtons() {
		if (previous == null) return;
		boolean choices = !menu.choices().isEmpty();
		previous.active = choices && !menu.mutating();
		next.active = choices && !menu.mutating();
		transmute.active = choices && !menu.mutating();
		transmute.setMessage(Component.translatable(menu.mutating()
				? "screen.powers.arcane_crucible.weaving" : "screen.powers.arcane_crucible.transmute"));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE16121C);
		graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 19, 0xFF34283E);
		graphics.fill(leftPos + 7, topPos + 23, leftPos + imageWidth - 7, topPos + 79, 0xFF211927);
		drawSlot(graphics, leftPos + 43, topPos + 36, 0xFF685A70);
		drawSlot(graphics, leftPos + 115, topPos + 36, 0xFF685A70);
		graphics.fill(leftPos + 62, topPos + 44, leftPos + 68, topPos + 46, 0xFF76569B);
		graphics.fill(leftPos + 108, topPos + 44, leftPos + 114, topPos + 46, 0xFF76569B);

		List<CrucibleChoice> choices = menu.choices();
		if (!choices.isEmpty()) {
			CrucibleChoice choice = choices.get(menu.selectedIndex());
			Component label = choice.operation() == CrucibleOperation.CONVERT
					? new ItemStack(BuiltInRegistries.ITEM.getValue(choice.targetItem())).getHoverName()
					: Component.translatable("screen.powers.arcane_crucible.choice." + choice.id());
			graphics.centeredText(font, label, leftPos + 88, topPos + 24, 0xFFE6D5FF);
			if (choice.targetItem() != null) {
				graphics.fakeItem(new ItemStack(BuiltInRegistries.ITEM.getValue(choice.targetItem())),
						leftPos + 80, topPos + 36);
			}
		} else {
			graphics.centeredText(font, Component.translatable("screen.powers.arcane_crucible.empty"),
					leftPos + 88, topPos + 25, 0xFF817788);
		}
	}

	private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x - 2, y - 2, x + 20, y, color);
		graphics.fill(x - 2, y + 18, x + 20, y + 20, color);
		graphics.fill(x - 2, y, x, y + 18, color);
		graphics.fill(x + 18, y, x + 20, y + 18, color);
	}
}
