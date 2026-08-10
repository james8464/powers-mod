package com.powers.player;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
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
		ItemStack guide = create();
		if (!player.addItem(guide)) player.drop(guide, false);
		data.markGuideReceived();
	}

	/** Builds a resolved vanilla written book; no custom screen or packet is required. */
	public static ItemStack create() {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
				Filterable.passThrough(TITLE), "The Archivist", 0, pages(), true));
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
						+ "Sanctuary, Kinetic Ward, anchors, living Light, and living Darkness all collide differently."));
	}

	private static Filterable<Component> page(String text) {
		return Filterable.passThrough(Component.literal(text).withStyle(ChatFormatting.DARK_PURPLE));
	}
}
