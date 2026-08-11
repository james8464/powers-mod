package com.powers.player;

import com.powers.PowersItems;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.spell.SpellCastingManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.List;

/** Creates and grants the one-time in-world controls and progression guide. */
public final class PlayerGuide {
	private static final String TITLE = "POWERS: First Awakening";
	private PlayerGuide() {
	}

	/** Gives or safely drops the guide exactly once for a persistent character. */
	public static void giveIfNeeded(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (data.hasReceivedGuide()) return;
		ItemStack guide = create(player);
		if (!player.addItem(guide)) player.drop(guide, false);
		data.markGuideReceived();
	}

	/** Builds a resolved vanilla written book; no custom screen or packet is required. */
	public static ItemStack create() {
		return create(null);
	}

	/** Builds the concise guide with bindings resolved from current saved selections. */
	public static ItemStack create(ServerPlayer player) {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
				Filterable.passThrough(TITLE), "The Archivist", 0,
				player == null ? pages() : pages(player), true));
		return book;
	}

	static String title() {
		return TITLE;
	}

	static List<Filterable<Component>> pages() {
		return List.of(
				page("FIRST AWAKENING\n\nYour three innate powers are bound to V, X, C. "
						+ "They consume the ten-symbol energy well above hunger. Sleep or wait to recover."),
				page("THE LABYRINTH\n\nPress B to open your title maze. Earn rank through the visible rites, "
						+ "then awaken connected titles. Rank changes only innate powers—not crystals, spells, or artifacts."),
				page("BOOKS & CRYSTALS\n\nSneak-use a grimoire to turn its page; use normally to cast. "
						+ "Sneak-use multi-form crystals to change mode. Amethyst suppresses and poisons magic."),
				page("MIND & REALMS\n\nRealm travel, projection, possession, and distant marking leave a vulnerable "
						+ "physical body. Harm to that body returns to you. Realm exit requirements cannot be bypassed."),
				page("THE TWO RELICS\n\nThe Shadow Sword answers Darkness; the Heavenly Partisan answers Light. "
						+ "Sneak-use either to open its combat wheel. The Shadow can be addressed with messages beginning “shadow, …”."),
				page("COUNTER-MAGIC\n\nForcefields absorb the complete hit that breaks them. Amethyst wards, "
						+ "Hearth forcefields, powered wards, anchors, living Light, and living Darkness all collide differently."));
	}

	private static List<Filterable<Component>> pages(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		List<String> spells = SpellCastingManager.registry().definitions().stream()
				.map(book -> book.spells().get(data.selectedSpell(book.key(), book.spells().size())).id())
				.toList();
		List<String> crystals = player.getInventory().getNonEquipmentItems().stream()
				.filter(PowersItems::isCrystal)
				.map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
				.distinct().limit(3).toList();
		List<String> artifactFavourites = new java.util.ArrayList<>(
				ArtifactSelectionState.favourites(player, ArtifactAlignment.DARKNESS));
		artifactFavourites.addAll(ArtifactSelectionState.favourites(player, ArtifactAlignment.LIGHT));
		java.util.ArrayList<Filterable<Component>> result = new java.util.ArrayList<>(pages());
		result.add(1, page(bindingDiagram(data.getSlotIds(), spells, crystals, artifactFavourites)));
		return List.copyOf(result);
	}

	static String bindingDiagram(List<String> powers, List<String> spells,
			List<String> crystals, List<String> artifactFavourites) {
		StringBuilder diagram = new StringBuilder("CURRENT BINDINGS\n\n");
		String[] keys = {"V", "X", "C"};
		for (int index = 0; index < Math.min(keys.length, powers.size()); index++) {
			append(diagram, keys[index], powers.get(index));
		}
		if (!spells.isEmpty()) append(diagram, "Spell", spells.getFirst());
		if (!crystals.isEmpty()) append(diagram, "Crystal", crystals.getFirst());
		for (int index = 0; index < Math.min(8, artifactFavourites.size()); index++) {
			if (!artifactFavourites.get(index).isBlank()) {
				append(diagram, "Wheel " + (index + 1), artifactFavourites.get(index));
			}
		}
		return diagram.toString();
	}

	private static void append(StringBuilder diagram, String binding, String action) {
		diagram.append(binding).append(" → ").append(humanize(action)).append('\n');
	}

	private static String humanize(String value) {
		if (value == null || value.isBlank()) return "Unbound";
		return java.util.Arrays.stream(value.replace('/', '_').split("_"))
				.filter(word -> !word.isBlank())
				.map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
				.collect(java.util.stream.Collectors.joining(" "));
	}

	private static Filterable<Component> page(String text) {
		return Filterable.passThrough(Component.literal(text).withStyle(ChatFormatting.DARK_PURPLE));
	}
}
