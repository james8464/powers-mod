package com.powers;

import com.powers.force.LivingForceBlock;
import com.powers.force.LivingForceKind;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * The blocks of the light and dark realms. darkness is pitch black and
 * pure light blazes at full brightness, and both are completely
 * unbreakable, exactly like bedrock.
 */
public final class PowersBlocks {
	private static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "ingredients"));

	public static final ResourceKey<Block> DARKNESS_KEY = key("darkness");
	// utterly black, unbreakable block of the dark realm
	public static final Block DARKNESS = register(DARKNESS_KEY,
			properties -> new LivingForceBlock(LivingForceKind.DARKNESS, properties), BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.strength(-1.0F, 3600000.0F)
			.noLootTable()
			.sound(SoundType.STONE)
			.randomTicks());

	public static final ResourceKey<Block> PURE_LIGHT_KEY = key("pure_light");
	// unbreakable block of the light realm that shines at full brightness
	public static final Block PURE_LIGHT = register(PURE_LIGHT_KEY,
			properties -> new LivingForceBlock(LivingForceKind.PURE_LIGHT, properties), BlockBehaviour.Properties.of()
			.mapColor(MapColor.QUARTZ)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.strength(-1.0F, 3600000.0F)
			.noLootTable()
			.sound(SoundType.STONE)
			.randomTicks()
			.lightLevel((state) -> 15));

	public static final ResourceKey<Block> LIGHT_MEMORY_OBELISK_KEY = key("light_memory_obelisk");
	public static final Block LIGHT_MEMORY_OBELISK = register(LIGHT_MEMORY_OBELISK_KEY,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.QUARTZ)
					.strength(-1.0F, 3600000.0F)
					.noLootTable()
					.noOcclusion()
					.sound(SoundType.AMETHYST)
					.lightLevel(state -> 15));

	public static final ResourceKey<Block> DARK_MEMORY_OBELISK_KEY = key("dark_memory_obelisk");
	public static final Block DARK_MEMORY_OBELISK = register(DARK_MEMORY_OBELISK_KEY,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLACK)
					.strength(-1.0F, 3600000.0F)
					.noLootTable()
					.noOcclusion()
					.sound(SoundType.SCULK)
					.lightLevel(state -> 4));

	public static final ResourceKey<Block> AMETHYST_WARD_KEY = key("amethyst_ward");
	// redstone-powered ward; while powered it dampens powers, and glows brighter to show it's on
	public static final Block AMETHYST_WARD = register(AMETHYST_WARD_KEY,
			AmethystWardBlock::new, BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_PURPLE)
					.instrument(NoteBlockInstrument.BASEDRUM)
					.strength(5.0F, 1200.0F)
					.sound(SoundType.AMETHYST)
					.lightLevel(state -> AmethystWardBlock.isPowered(state) ? 10 : 2));

	private PowersBlocks() {
	}

	private static ResourceKey<Block> key(String name) {
		return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(PowersMod.MOD_ID, name));
	}

	private static Block register(ResourceKey<Block> blockKey, BlockBehaviour.Properties properties) {
		return register(blockKey, Block::new, properties);
	}

	private static Block register(ResourceKey<Block> blockKey,
			java.util.function.Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties) {
		Block block = factory.apply(properties.setId(blockKey));
		Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
		ModItemIds.register(ModItemIds.create(blockKey.identifier().getPath()),
				(p) -> new BlockItem(block, p), new Item.Properties().stacksTo(64));
		return block;
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(INGREDIENTS_TAB)
				.register((creativeTab) -> {
					creativeTab.accept(DARKNESS);
					creativeTab.accept(PURE_LIGHT);
					creativeTab.accept(AMETHYST_WARD);
					creativeTab.accept(LIGHT_MEMORY_OBELISK);
					creativeTab.accept(DARK_MEMORY_OBELISK);
				});
	}
}
