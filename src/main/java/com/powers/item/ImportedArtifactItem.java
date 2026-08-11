package com.powers.item;

import com.powers.PowersDataComponents;
import com.powers.PowersMod;
import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.MagicUseGate;
import com.powers.power.AmethystDampening;
import com.powers.power.abilities.DelayedTravelRules;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.power.travel.TravelKind;
import com.powers.protection.PowerProtection;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.power.artifact.ArtifactDeathWardManager;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.PowerMessages;
import com.powers.testing.TestingOverrides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Gives imported relic families bounded server-authoritative actions. */
public final class ImportedArtifactItem extends Item {
	private static final int USE_COOLDOWN_TICKS = 100;
	private final String texture;
	private final ImportedArtifactKind kind;
	private final ArtifactRole role;

	public ImportedArtifactItem(Properties properties, String texture) {
		super(properties.stacksTo(ArtifactRoleCatalogue.role(texture)
				== ArtifactRole.ARCANE_ENERGY_DUST ? 16 : 1));
		this.texture = texture;
		this.kind = ImportedArtifactRules.kind(texture);
		this.role = ArtifactRoleCatalogue.role(texture);
	}

	@Override
	public InteractionResult use(Level level, net.minecraft.world.entity.player.Player user,
			InteractionHand hand) {
		if (level.isClientSide() || !(user instanceof ServerPlayer player)) {
			return InteractionResult.SUCCESS;
		}
		if (!MagicUseGate.passes(player, true)) return InteractionResult.FAIL;
		ItemStack stack = player.getItemInHand(hand);
		if (!TestingOverrides.cooldownsDisabled(player.getUUID())
				&& player.getCooldowns().isOnCooldown(stack)) return InteractionResult.SUCCESS;
		boolean success = switch (kind) {
			case ATTUNEMENT -> recharge(player, 24, 0xE6CF7B);
			case ENERGY_RESERVOIR -> transferReservoir(player, stack);
			case RITUAL_CATALYST -> primeRitual(player);
			case HEART_RELIC -> awakenHeart(player);
			case TRAVEL_RELIC -> texture.equals("device_miniportal")
					? openMiniportal(player, stack) : explain(player, "item.powers.relic.bind_first");
			case COMMAND_RELIC -> commandGuardians(player);
			case ARCANE_CATALYST -> explain(player, "item.powers.relic.crucible_catalyst");
			case LORE_RELIC -> explain(player, "item.powers.relic.lore_fragment");
			case TRANSMUTER -> explain(player, "item.powers.relic.target_block");
			case NONE -> false;
		};
		if (success && !TestingOverrides.cooldownsDisabled(player.getUUID())) {
			player.getCooldowns().addCooldown(stack, USE_COOLDOWN_TICKS);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().isClientSide()
				|| !(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
		if (!MagicUseGate.passes(player, true)) return InteractionResult.FAIL;
		ItemStack stack = context.getItemInHand();
		if (!TestingOverrides.cooldownsDisabled(player.getUUID())
				&& player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		boolean success = switch (kind) {
			case TRANSMUTER -> transmute(player, context);
			case TRAVEL_RELIC -> bindAnchor(player, context);
			default -> false;
		};
		if (!success) return kind == ImportedArtifactKind.TRANSMUTER
				|| kind == ImportedArtifactKind.TRAVEL_RELIC
				? InteractionResult.FAIL : super.useOn(context);
		if (!TestingOverrides.cooldownsDisabled(player.getUUID())) {
			player.getCooldowns().addCooldown(stack, USE_COOLDOWN_TICKS);
		}
		return InteractionResult.SUCCESS;
	}

	public String texture() {
		return texture;
	}

	public ImportedArtifactKind kind() {
		return kind;
	}

	public ArtifactRole role() {
		return role;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return texture.equals("device_miniportal");
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return texture.equals("device_miniportal") ? MiniportalRules.barWidth(
				MiniportalRules.charges(stack.get(PowersDataComponents.MINIPORTAL_CHARGES)))
				: super.getBarWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return texture.equals("device_miniportal") ? 0xC99CFF : super.getBarColor(stack);
	}

	private static boolean transferReservoir(ServerPlayer player, ItemStack stack) {
		ImportedArtifactItem relic = (ImportedArtifactItem) stack.getItem();
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		int stored = ArtifactEnergyReservoir.stored(stack);
		int transfer;
		if (player.isShiftKeyDown()) {
			transfer = Math.min(100, Math.min(data.energy(),
					ArtifactEnergyReservoir.capacity(relic.texture()) - stored));
			if (transfer <= 0) return explain(player, "item.powers.relic.reservoir_cannot_store");
			data.drainEnergy(transfer);
			ArtifactEnergyReservoir.setStored(stack, stored + transfer);
		} else {
			transfer = Math.min(100, Math.min(stored, data.energyCapacity() - data.energy()));
			if (transfer <= 0) return explain(player, "item.powers.relic.reservoir_cannot_release");
			ArtifactEnergyReservoir.setStored(stack, stored - transfer);
			data.refundEnergy(transfer);
		}
		com.powers.network.PowersPackets.syncTo(player);
		PowerFx.rune((ServerLevel) player.level(), player.position().add(0.0, 1.0, 0.0),
				1.2, 0x7B4BA3, 18, player.level().getGameTime() * 0.06);
		return true;
	}

	private static boolean primeRitual(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (player.getHealth() <= 6.0F || data.energy() >= data.energyCapacity()) return false;
		// A ritual payment is not an incoming attack: armor, spawn invulnerability,
		// forcefields, and other mods' damage cancellation must not turn it into free energy.
		player.setHealth(player.getHealth() - 4.0F);
		data.refundEnergy(80);
		com.powers.network.PowersPackets.syncTo(player);
		PowerFx.rune((ServerLevel) player.level(), player.position(), 1.8, 0x9D1735, 26, 0.0);
		return true;
	}

	private boolean awakenHeart(ServerPlayer player) {
		ImportedArtifactRules.HeartSpecialization specialization =
				ImportedArtifactRules.heartSpecialization(texture);
		if (specialization == ImportedArtifactRules.HeartSpecialization.BLOOD_WARD) {
			return ArtifactDeathWardManager.arm(player, ArtifactAlignment.DARKNESS);
		}
		if (specialization == ImportedArtifactRules.HeartSpecialization.CLOCKWORK) {
			player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), 8.0F));
			player.addEffect(PowerStatusEffects.hidden(
					MobEffects.ABSORPTION, 20 * 60, 1, true, true));
			PowerFx.rune((ServerLevel) player.level(), player.position(), 1.4,
					0xB58C61, 20, 0.0);
			return true;
		}
		float before = player.getHealth();
		float healing = specialization == ImportedArtifactRules.HeartSpecialization.WILDWOOD ? 12.0F
				: specialization == ImportedArtifactRules.HeartSpecialization.GHOUL ? 6.0F : 9.0F;
		player.heal(healing);
		if (specialization == ImportedArtifactRules.HeartSpecialization.GHOUL) {
			PlayerPowers.get(player).regenerateEnergy(30);
		}
		PowerFx.coloredBurst((ServerLevel) player.level(), player.position().add(0.0, 1.0, 0.0),
				specialization == ImportedArtifactRules.HeartSpecialization.GHOUL
						? 0x743551 : 0xB43A4C, 16, 0.5);
		return player.getHealth() > before
				|| specialization == ImportedArtifactRules.HeartSpecialization.GHOUL;
	}

	private static boolean recharge(ServerPlayer player, int energy, int color) {
		if (!PlayerPowers.get(player).regenerateEnergy(energy)) return false;
		PowerFx.rune((ServerLevel) player.level(), player.position(), 1.0, color, 16, 0.0);
		return true;
	}

	private static boolean commandGuardians(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		AABB area = player.getBoundingBox().inflate(24.0);
		int commanded = 0;
		for (com.powers.entity.AbstractPlayerLikeMob guardian :
				BoundedEntityCandidates.ofClass(level, com.powers.entity.AbstractPlayerLikeMob.class,
						area, 64, LivingEntity::isAlive)) {
			guardian.configureGuardian(player.getUUID(), 2_400, guardian.eliteGuardian());
			guardian.setTarget(null);
			guardian.heal(20.0F);
			commanded++;
		}
		PowerFx.ring(level, player.position(), 12.0, 0x78D7C6, 28, 0.0);
		PowerFx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME,
				0.8F, 0.72F);
		return commanded > 0 || explain(player, "item.powers.relic.no_guardians");
	}

	private static boolean bindAnchor(ServerPlayer player, UseOnContext context) {
		if (!(context.getItemInHand().getItem() instanceof ImportedArtifactItem relic)
				|| !relic.texture.contains("lodestone")) {
			return explain(player, "item.powers.relic.requires_lodestone");
		}
		BlockPos anchor = context.getClickedPos().relative(context.getClickedFace());
		context.getItemInHand().set(PowersDataComponents.TRAVEL_ANCHOR,
				new TravelAnchorData(player.level().dimension().identifier(),
						anchor.getX(), anchor.getY(), anchor.getZ()));
		PowerFx.rune((ServerLevel) player.level(), Vec3.atBottomCenterOf(anchor), 1.4,
				0xC99C58, 20, 0.0);
		return explain(player, "item.powers.relic.anchor_bound");
	}

	private static boolean openMiniportal(ServerPlayer player, ItemStack device) {
		TravelAnchorData anchor = null;
		for (ItemStack candidate : player.getInventory().getNonEquipmentItems()) {
			TravelAnchorData stored = candidate.get(PowersDataComponents.TRAVEL_ANCHOR);
			if (stored != null) {
				anchor = stored;
				break;
			}
		}
		if (anchor == null) return explain(player, "item.powers.relic.no_anchor");
		int charges = MiniportalRules.charges(
				device.get(PowersDataComponents.MINIPORTAL_CHARGES));
		boolean sameDimension = anchor.dimension().equals(
				player.level().dimension().identifier());
		if (!sameDimension) return explain(player, "item.powers.relic.same_dimension");
		if (!MiniportalRules.mayTravel(charges, true)) {
			return explain(player, "item.powers.relic.miniportal_empty");
		}
		ServerLevel destination = player.level().getServer().getLevel(
				ResourceKey.create(Registries.DIMENSION, anchor.dimension()));
		if (destination == null) return false;
		BlockPos requested = anchor.position();
		Vec3 position = Vec3.atBottomCenterOf(requested);
		ServerLevel origin = (ServerLevel) player.level();
		var server = origin.getServer();
		if (!SafeDestinationResolver.validatePreload(player, destination, position,
				TravelKind.POWER).allowed()) return false;
		return TravelChunkLoader.request(player.getUUID(), destination, requested, "miniportal", () -> {
			ServerPlayer current = server.getPlayerList().getPlayer(player.getUUID());
			if (current != null) AmethystDampening.update(current);
			int currentCharges = MiniportalRules.charges(
					device.get(PowersDataComponents.MINIPORTAL_CHARGES));
			if (!MiniportalRules.mayCommit(current == player,
					player.isAlive() && !player.isRemoved(), player.level() == origin,
					ownsExactStack(player, device), charges, currentCharges)
					|| !DelayedTravelRules.travellerMayContinue(current == player,
					player.isAlive() && !player.isRemoved(), player.isAlive() && !player.isRemoved(),
					player.level() == origin, player.level() == origin,
					AmethystDampening.isDampened(player), AmethystDampening.isDampened(player))
					|| !SafeDestinationResolver.validate(player, destination, position,
							TravelKind.POWER).allowed()) {
				if (current == player) explain(player, "item.powers.relic.anchor_unreachable");
				return;
			}
			player.teleport(new TeleportTransition(destination, position, Vec3.ZERO,
					player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			device.set(PowersDataComponents.MINIPORTAL_CHARGES,
					MiniportalRules.afterSuccessfulTravel(charges));
			PowerFx.spiral(destination, position, 1.0, 3.0, 0xC99C58, 24, 0.0);
		}, () -> {
			ServerPlayer current = server.getPlayerList().getPlayer(player.getUUID());
			if (current == player) explain(player, "item.powers.relic.anchor_unreachable");
		});
	}

	private static boolean ownsExactStack(ServerPlayer player, ItemStack expected) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot) == expected) return true;
		}
		return player.getMainHandItem() == expected || player.getOffhandItem() == expected;
	}

	private static boolean transmute(ServerPlayer player, UseOnContext context) {
		ServerLevel level = (ServerLevel) context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!PowerProtection.mayAffectBlock(player, level, pos)) return false;
		BlockState state = level.getBlockState(pos);
		BlockState replacement = state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
				? Blocks.IRON_ORE.defaultBlockState()
				: state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE)
						? Blocks.DEEPSLATE_IRON_ORE.defaultBlockState()
						: state.is(Blocks.NETHERRACK) ? Blocks.NETHER_QUARTZ_ORE.defaultBlockState()
						: state.is(Blocks.END_STONE) ? Blocks.AMETHYST_BLOCK.defaultBlockState() : null;
		if (replacement == null || !PlayerPowers.get(player).consumeEnergy(30)) return false;
		level.setBlockAndUpdate(pos, replacement);
		PowerFx.coloredBurst(level, Vec3.atCenterOf(pos), 0xD8B65C, 16, 0.45);
		return true;
	}

	private static boolean explain(ServerPlayer player, String translation) {
		PowerMessages.overlay(player, net.minecraft.network.chat.Component.translatable(translation));
		return true;
	}
}
