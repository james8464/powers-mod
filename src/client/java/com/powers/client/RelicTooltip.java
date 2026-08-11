package com.powers.client;

import com.powers.PowersDataComponents;
import com.powers.item.ArtifactEnergyModifiers;
import com.powers.item.ArtifactEnergyReservoir;
import com.powers.item.ArtifactRole;
import com.powers.item.ImportedArtifactItem;
import com.powers.item.MiniportalRules;
import com.powers.item.RitualDaggerRules;
import com.powers.item.TravelAnchorData;
import com.powers.power.PowerEnergy;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Adds live, bounded relic state without guessing server-owned balances. */
final class RelicTooltip {
	private RelicTooltip() {
	}

	static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!(stack.getItem() instanceof ImportedArtifactItem relic)) return;
			if (relic.texture().equals("artifact_malignember")) malignember(lines);
			if (relic.texture().equals("artifact_ritualdagger")) ritual(lines);
			if (relic.role() == ArtifactRole.ENERGY_RESERVOIR) reservoir(stack, relic, lines);
			if (relic.texture().equals("device_miniportal")) miniportal(stack, lines);
		});
	}

	private static void malignember(java.util.List<Component> lines) {
		lines.add(Component.translatable("tooltip.powers.malignember.title")
				.withStyle(ChatFormatting.DARK_RED));
		ArtifactEnergyModifiers.eligibleActionIds().stream().sorted().forEach(action -> {
			ArtifactEnergyModifiers.Quote quote = ArtifactEnergyModifiers.quote(
					true, action, PowerEnergy.baseCost(action));
			lines.add(Component.translatable("tooltip.powers.malignember.action",
					Component.translatableWithFallback("ability.powers." + action,
							humanize(action)), quote.saved()).withStyle(ChatFormatting.GRAY));
		});
	}

	private static void ritual(java.util.List<Component> lines) {
		var player = Minecraft.getInstance().player;
		RitualDaggerRules.Preview preview = RitualDaggerRules.preview(
				player == null ? 0.0F : player.getHealth(), ClientPowerState.energy(),
				ClientPowerState.energyCapacity());
		lines.add(Component.translatable("tooltip.powers.ritual_dagger.rune")
				.withStyle(ChatFormatting.DARK_RED));
		lines.add(Component.translatable("tooltip.powers.ritual_dagger.preview",
				preview.healthCost(), preview.resultingHealth(), preview.energyGain(),
				RitualDaggerRules.SURVIVAL_FLOOR).withStyle(
				preview.allowed() ? ChatFormatting.GRAY : ChatFormatting.RED));
	}

	private static void reservoir(ItemStack stack, ImportedArtifactItem relic,
			java.util.List<Component> lines) {
		int stored = ArtifactEnergyReservoir.stored(stack);
		lines.add(Component.translatable("tooltip.powers.reservoir.balance",
				ClientPowerState.energy(), ClientPowerState.energyCapacity(), stored,
				ArtifactEnergyReservoir.capacity(relic.texture())).withStyle(ChatFormatting.LIGHT_PURPLE));
		lines.add(Component.translatable("tooltip.powers.reservoir.open")
				.withStyle(ChatFormatting.DARK_GRAY));
	}

	private static void miniportal(ItemStack stack, java.util.List<Component> lines) {
		int charges = MiniportalRules.charges(stack.get(PowersDataComponents.MINIPORTAL_CHARGES));
		lines.add(Component.translatable("tooltip.powers.miniportal.charges",
				charges, MiniportalRules.MAX_CHARGES).withStyle(
				charges > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		TravelAnchorData anchor = MiniportalRules.firstAnchor(
				player.getInventory().getNonEquipmentItems().stream()
				.map(candidate -> candidate.get(PowersDataComponents.TRAVEL_ANCHOR))
				.toList());
		if (anchor != null) {
			lines.add(Component.translatable("tooltip.powers.miniportal.anchor", anchor.name(),
					anchor.dimension().getPath(), anchor.x(), anchor.y(), anchor.z())
					.withStyle(ChatFormatting.GRAY));
		}
	}

	private static String humanize(String value) {
		return java.util.Arrays.stream(value.split("_"))
				.map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
				.collect(java.util.stream.Collectors.joining(" "));
	}
}
