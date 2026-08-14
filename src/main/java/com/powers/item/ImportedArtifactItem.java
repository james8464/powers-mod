package com.powers.item;

import com.powers.PowersDataComponents;
import com.powers.PowersMod;
import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.mind.PersistentDimensionDiagnostics;
import com.powers.player.PlayerPowers;
import com.powers.power.MagicUseGate;
import com.powers.power.AmethystDampening;
import com.powers.power.abilities.DelayedTravelRules;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.power.travel.TravelKind;
import com.powers.power.travel.TravelCohort;
import com.powers.protection.PowerProtection;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.network.RelicPackets;
import com.powers.power.artifact.ArtifactDeathWardManager;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.PowerMessages;
import com.powers.testing.TestingOverrides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Gives imported relic families bounded server-authoritative actions. */
public final class ImportedArtifactItem extends Item {
	private static final int USE_COOLDOWN_TICKS = 100;
	private static final Map<UUID, MiniportalRequest> PENDING_MINIPORTALS = new HashMap<>();
	private record MiniportalRequest(ResourceKey<Level> origin, ResourceKey<Level> destination,
			Vec3 position, int charges, int inventorySlot) { }
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
			case ENERGY_RESERVOIR -> RelicPackets.openReservoir(player, stack);
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

	private static boolean primeRitual(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		RitualDaggerRules.Preview preview = RitualDaggerRules.preview(
				player.getHealth(), data.energy(), data.energyCapacity());
		if (!preview.allowed()) return false;
		// A ritual payment is not an incoming attack: armor, spawn invulnerability,
		// forcefields, and other mods' damage cancellation must not turn it into free energy.
		player.setHealth(preview.resultingHealth());
		data.refundEnergy(preview.energyGain());
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
						area, 64, candidate -> candidate.isAlive()
								&& (candidate instanceof com.powers.entity.DarknessCreature
								|| candidate instanceof com.powers.entity.RadiantSentinel))) {
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
		String fallback = player.level().dimension().identifier().getPath() + " "
				+ anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ();
		String requested = context.getItemInHand().has(DataComponents.CUSTOM_NAME)
				? context.getItemInHand().getHoverName().getString() : fallback;
		context.getItemInHand().set(PowersDataComponents.TRAVEL_ANCHOR,
				new TravelAnchorData(player.level().dimension().identifier(),
						anchor.getX(), anchor.getY(), anchor.getZ(), requested));
		PowerFx.rune((ServerLevel) player.level(), Vec3.atBottomCenterOf(anchor), 1.4,
				0xC99C58, 20, 0.0);
		return explain(player, "item.powers.relic.anchor_bound");
	}

	private static boolean openMiniportal(ServerPlayer player, ItemStack device) {
		java.util.List<TravelAnchorData> anchors = new java.util.ArrayList<>();
		for (ItemStack candidate : player.getInventory().getNonEquipmentItems()) {
			anchors.add(candidate.get(PowersDataComponents.TRAVEL_ANCHOR));
		}
		TravelAnchorData anchor = MiniportalRules.firstAnchor(anchors);
		if (anchor == null) return explain(player, "item.powers.relic.no_anchor");
		int charges = MiniportalRules.charges(
				device.get(PowersDataComponents.MINIPORTAL_CHARGES));
		ServerLevel destination = player.level().getServer().getLevel(
				ResourceKey.create(Registries.DIMENSION, anchor.dimension()));
		if (destination == null) {
			PersistentDimensionDiagnostics.record("miniportal", anchor.dimension().toString());
			return explain(player, "item.powers.relic.anchor_unreachable");
		}
		boolean sameDimension = anchor.dimension().equals(
				player.level().dimension().identifier());
		if (!sameDimension) return explain(player, "item.powers.relic.same_dimension");
		if (!MiniportalRules.mayTravel(charges, true)) {
			return explain(player, "item.powers.relic.miniportal_empty");
		}
		BlockPos requested = anchor.position();
		Vec3 position = Vec3.atBottomCenterOf(requested);
		ServerLevel origin = (ServerLevel) player.level();
		if (!SafeDestinationResolver.validatePreload(player, destination, position,
				TravelKind.POWER).allowed()) return false;
		int inventorySlot = inventorySlot(player, device);
		if (inventorySlot < 0) return false;
		UUID ownerId = player.getUUID();
		PENDING_MINIPORTALS.put(ownerId, new MiniportalRequest(origin.dimension(),
				destination.dimension(), position, charges, inventorySlot));
		boolean accepted = TravelChunkLoader.request(ownerId, destination, requested, "miniportal",
				ImportedArtifactItem::completeMiniportal, ImportedArtifactItem::failMiniportal);
		if (!accepted) PENDING_MINIPORTALS.remove(ownerId);
		return accepted;
	}

	private static int inventorySlot(ServerPlayer player, ItemStack expected) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot) == expected) return slot;
		}
		return -1;
	}

	private static void completeMiniportal(MinecraftServer server, UUID ownerId) {
		MiniportalRequest request = PENDING_MINIPORTALS.remove(ownerId);
		ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
		ServerLevel origin = request == null ? null : server.getLevel(request.origin());
		ServerLevel destination = request == null ? null : server.getLevel(request.destination());
		if (player == null || origin == null || destination == null) return;
		ItemStack device = player.getInventory().getItem(request.inventorySlot());
		boolean stillMiniportal = device.getItem() instanceof ImportedArtifactItem artifact
				&& artifact.texture.equals("device_miniportal");
		AmethystDampening.update(player);
		int currentCharges = MiniportalRules.charges(
				device.get(PowersDataComponents.MINIPORTAL_CHARGES));
		if (!MiniportalRules.mayCommit(true, player.isAlive() && !player.isRemoved(),
				player.level() == origin, stillMiniportal, request.charges(), currentCharges)
				|| !DelayedTravelRules.travellerMayContinue(true,
				player.isAlive() && !player.isRemoved(), player.isAlive() && !player.isRemoved(),
				player.level() == origin, player.level() == origin,
				AmethystDampening.isDampened(player), AmethystDampening.isDampened(player))
				|| !SafeDestinationResolver.validateExact(player, destination, request.position(),
						TravelKind.POWER).allowed()) {
			explain(player, "item.powers.relic.anchor_unreachable");
			return;
		}
		TravelCohort.Snapshot cohort = TravelCohort.capture(origin, player, player);
		player.teleport(new TeleportTransition(destination, request.position(), Vec3.ZERO,
				player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		TravelCohort.move(cohort, destination, request.position());
		int remaining = MiniportalRules.afterSuccessfulTravel(request.charges());
		device.set(PowersDataComponents.MINIPORTAL_CHARGES, remaining);
		MiniportalRules.applyVisual(device, remaining);
		PowerFx.spiral(destination, request.position(), 1.0, 3.0, 0xC99C58, 24, 0.0);
	}

	private static void failMiniportal(MinecraftServer server, UUID ownerId) {
		PENDING_MINIPORTALS.remove(ownerId);
		ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
		if (player != null) explain(player, "item.powers.relic.anchor_unreachable");
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
