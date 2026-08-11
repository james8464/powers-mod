package com.powers.forge;

import com.powers.PowersBlockEntities;
import com.powers.PowersBlocks;
import com.powers.fx.PowerFx;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/** Persistent two-slot inventory and one-lock delayed transaction owner. */
public final class ArcaneCrucibleBlockEntity extends BaseContainerBlockEntity
		implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
	public static final int WEAPON_SLOT = 0;
	public static final int CATALYST_SLOT = 1;
	private static final int RITUAL_TICKS = 40;
	private static final int[] SLOTS = {WEAPON_SLOT, CATALYST_SLOT};

	private final CrucibleMutationGuard mutation = new CrucibleMutationGuard();
	private final ContainerData data = new ContainerData() {
		@Override public int get(int index) {
			return switch (index) {
				case 0 -> version;
				case 1 -> mutation.isLocked() ? 1 : 0;
				default -> 0;
			};
		}

		@Override public void set(int index, int value) {
			if (index == 0) version = Math.max(0, value);
		}

		@Override public int getCount() { return 2; }
	};
	private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
	private int version;
	private long releaseAt;
	private int pendingVersion;
	private String pendingChoice = "";

	public ArcaneCrucibleBlockEntity(BlockPos pos, BlockState state) {
		super(PowersBlockEntities.ARCANE_CRUCIBLE, pos, state);
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.powers.arcane_crucible");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new ArcaneCrucibleMenu(containerId, inventory, this, data);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
	}

	public ContainerData dataAccess() {
		return data;
	}

	public int version() {
		return version;
	}

	public boolean isMutating() {
		return mutation.isLocked();
	}

	public List<CrucibleChoice> choices() {
		return CrucibleTransformationCatalogue.choices(getItem(WEAPON_SLOT), getItem(CATALYST_SLOT));
	}

	/** Starts a two-second ritual without consuming either input. */
	public boolean begin(ServerPlayer player, CrucibleChoice choice, int expectedVersion) {
		if (level == null || level.isClientSide() || expectedVersion != version || !mutation.tryLock()) {
			return false;
		}
		CrucibleTransactionResult prepared = CrucibleTransactionEngine.prepare(
				getItem(WEAPON_SLOT), getItem(CATALYST_SLOT), choice);
		if (!prepared.success()) {
			mutation.unlock();
			PowerMessages.send(player, "container.powers.arcane_crucible.invalid", 4);
			return false;
		}
		pendingChoice = choice.id();
		pendingVersion = version;
		releaseAt = level.getGameTime() + RITUAL_TICKS;
		updateLit(true);
		PowerFx.sound((ServerLevel) level, center(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.1F, 0.65F);
		return true;
	}

	public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos,
			BlockState state, ArcaneCrucibleBlockEntity crucible) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		if (crucible.mutation.isLocked()) {
			long remaining = crucible.releaseAt - level.getGameTime();
			if (remaining > 0) {
				if (remaining % 5 == 0) crucible.ritualPulse(serverLevel, remaining);
				return;
			}
			crucible.commit(serverLevel);
		} else if (level.getGameTime() % 20 == 0) {
			crucible.updateLit(!crucible.choices().isEmpty());
		}
	}

	private void ritualPulse(ServerLevel level, long remaining) {
		double radius = 0.7 + (RITUAL_TICKS - remaining) / 80.0;
		PowerFx.rune(level, center(), radius, 0xA46DFF, 14,
				level.getGameTime() * 0.12);
		PowerFx.beam(level, center().add(0.0, 4.0, 0.0), center(),
				PowerFx.dust(0xA46DFF, 0.9F), 10);
	}

	private void commit(ServerLevel serverLevel) {
		try {
			CrucibleChoice choice = choices().stream()
					.filter(candidate -> candidate.id().equals(pendingChoice)).findFirst().orElse(null);
			if (choice == null || version != pendingVersion) return;
			CrucibleTransactionResult transaction = CrucibleTransactionEngine.prepare(
					getItem(WEAPON_SLOT), getItem(CATALYST_SLOT), choice);
			if (!transaction.success()) return;
			// Keep the output inside the same persistent inventory transaction.
			// Giving/dropping it here creates a second save boundary and can duplicate
			// or lose the result if the server stops between those two mutations.
			items.set(WEAPON_SLOT, transaction.result().copy());
			items.set(CATALYST_SLOT, transaction.catalystAfter());
			version++;
			super.setChanged();
			int color = choice.alignment() == com.powers.item.artifact.ArtifactAlignment.DARKNESS
					? 0x4B145D : 0xFFF2B2;
			PowerFx.rune(serverLevel, center(), transaction.levelUp() ? 3.0 : 2.0,
					color, transaction.levelUp() ? 48 : 32, 0.0);
			PowerFx.burst(serverLevel, center().add(0.0, 0.8, 0.0),
					ParticleTypes.ELECTRIC_SPARK, transaction.levelUp() ? 18 : 10, 0.55, 0.04);
			PowerFx.sound(serverLevel, center(), SoundEvents.LIGHTNING_BOLT_THUNDER,
					transaction.levelUp() ? 1.8F : 1.2F, 1.15F);
		} finally {
			pendingChoice = "";
			mutation.unlock();
			updateLit(!choices().isEmpty());
		}
	}

	private net.minecraft.world.phys.Vec3 center() {
		return net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(0.0, 0.45, 0.0);
	}

	private void updateLit(boolean lit) {
		if (level == null) return;
		BlockState state = getBlockState();
		if (state.is(PowersBlocks.ARCANE_CRUCIBLE) && state.getValue(ArcaneCrucibleBlock.LIT) != lit) {
			level.setBlock(worldPosition, state.setValue(ArcaneCrucibleBlock.LIT, lit), Block.UPDATE_ALL);
		}
	}

	@Override
	public void setChanged() {
		version++;
		super.setChanged();
		updateLit(mutation.isLocked() || !choices().isEmpty());
	}

	@Override public int getContainerSize() { return 2; }
	@Override public int getMaxStackSize() { return 1; }
	@Override public int[] getSlotsForFace(Direction side) { return SLOTS.clone(); }

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (mutation.isLocked()) return false;
		return slot == WEAPON_SLOT
				? CrucibleEligibility.isBaseWeapon(stack) || CrucibleEligibility.isConvertedWeapon(stack)
				: slot == CATALYST_SLOT && CrucibleEligibility.isCatalyst(stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return !mutation.isLocked();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(2, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		version = Math.max(0, input.getIntOr("PowersCrucibleVersion", 0));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("PowersCrucibleVersion", version);
	}
}
