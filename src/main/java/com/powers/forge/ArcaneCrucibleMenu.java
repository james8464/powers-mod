package com.powers.forge;

import com.powers.PowersMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Vanilla-slot menu; button packets carry only a bounded choice index. */
public final class ArcaneCrucibleMenu extends AbstractContainerMenu {
	private final Container container;
	private final ContainerData data;
	private final ContainerLevelAccess access;
	private final ArcaneCrucibleBlockEntity crucible;
	private int selectedIndex;
	private int selectedVersion;

	public ArcaneCrucibleMenu(int containerId, Inventory inventory, BlockPos pos) {
		this(containerId, inventory, new SimpleContainer(2),
				ContainerLevelAccess.create(inventory.player.level(), pos), new SimpleContainerData(2), null);
	}

	public ArcaneCrucibleMenu(int containerId, Inventory inventory,
			ArcaneCrucibleBlockEntity crucible, ContainerData data) {
		this(containerId, inventory, crucible,
				ContainerLevelAccess.create(crucible.getLevel(), crucible.getBlockPos()), data, crucible);
	}

	private ArcaneCrucibleMenu(int containerId, Inventory inventory, Container container,
			ContainerLevelAccess access, ContainerData data, ArcaneCrucibleBlockEntity crucible) {
		super(PowersMenus.ARCANE_CRUCIBLE, containerId);
		checkContainerSize(container, 2);
		checkContainerDataCount(data, 2);
		this.container = container;
		this.data = data;
		this.access = access;
		this.crucible = crucible;
		this.selectedVersion = data.get(0);
		container.startOpen(inventory.player);
		addSlot(new CrucibleSlot(container, ArcaneCrucibleBlockEntity.WEAPON_SLOT, 44, 37, crucible));
		addSlot(new CrucibleSlot(container, ArcaneCrucibleBlockEntity.CATALYST_SLOT, 116, 37, crucible));
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	public List<CrucibleChoice> choices() {
		return CrucibleTransformationCatalogue.choices(
				container.getItem(ArcaneCrucibleBlockEntity.WEAPON_SLOT),
				container.getItem(ArcaneCrucibleBlockEntity.CATALYST_SLOT));
	}

	public int selectedIndex() {
		return Math.clamp(selectedIndex, 0, Math.max(0, choices().size() - 1));
	}

	public boolean mutating() {
		return data.get(1) != 0;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id >= 100 && id < 116) {
			int index = id - 100;
			if (index >= choices().size()) return false;
			selectedIndex = index;
			selectedVersion = data.get(0);
			return true;
		}
		if (id != 0 || crucible == null || !(player instanceof ServerPlayer serverPlayer)
				|| mutating()) return false;
		List<CrucibleChoice> current = choices();
		if (selectedIndex < 0 || selectedIndex >= current.size()) return false;
		return crucible.begin(serverPlayer, current.get(selectedIndex), selectedVersion);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = getSlot(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < 2) {
			if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
		} else if (CrucibleEligibility.isCatalyst(stack)) {
			if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
		} else if (CrucibleEligibility.isBaseWeapon(stack) || CrucibleEligibility.isConvertedWeapon(stack)) {
			if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
		} else return ItemStack.EMPTY;
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return access.evaluate((level, pos) -> Container.stillValidBlockEntity(
				level.getBlockEntity(pos), player), false);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
	}

	private static final class CrucibleSlot extends Slot {
		private final ArcaneCrucibleBlockEntity crucible;

		private CrucibleSlot(Container container, int slot, int x, int y,
				ArcaneCrucibleBlockEntity crucible) {
			super(container, slot, x, y);
			this.crucible = crucible;
		}

		@Override public int getMaxStackSize() { return 1; }
		@Override public boolean mayPlace(ItemStack stack) {
			return crucible == null || crucible.canPlaceItem(index, stack);
		}
		@Override public boolean mayPickup(Player player) {
			return crucible == null || !crucible.isMutating();
		}
	}
}
