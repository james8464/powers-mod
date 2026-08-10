package com.powers;

import com.powers.item.ShadowSwordItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The fantasy weapon set (nongko's Fantasy Weapons textures and models),
 * registered as own items in the POWERS namespace with no recipes.
 */
public final class PowersWeapons {
	private static final ResourceKey<CreativeModeTab> COMBAT_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB,
					Identifier.fromNamespaceAndPath("minecraft", "combat"));

	public static final Map<String, Item> WEAPONS = new LinkedHashMap<>();

	private enum Kind { SWORD, PICKAXE, SHOVEL }

	private record WeaponDef(String id, String displayName, ToolMaterial material, Kind kind, float damage, float speed) {
	}

	// the whole roster: id, display name, material, kind, damage, swing speed
	private static final WeaponDef[] DEFS = {
			new WeaponDef("amethyst_greatblade", "Amethyst Greatblade", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("amethyst_greatpick", "Amethyst Greatpick", ToolMaterial.DIAMOND, Kind.PICKAXE, 5f, 1.2f),
			new WeaponDef("amethyst_greatshovel", "Amethyst Greatshovel", ToolMaterial.DIAMOND, Kind.SHOVEL, 5.5f, 1f),
			new WeaponDef("ancient_greatslab", "Ancient Greatslab", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("azure_dagger", "Azure Dagger", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("azure_greataxe", "Azure Greataxe", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("azure_greatsword", "Azure Greatsword", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("azure_pickaxe", "Azure Pickaxe", ToolMaterial.DIAMOND, Kind.PICKAXE, 5f, 1.2f),
			new WeaponDef("azure_sabre", "Azure Sabre", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("azure_scythe", "Azure Scythe", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("azure_shovel", "Azure Shovel", ToolMaterial.DIAMOND, Kind.SHOVEL, 5.5f, 1f),
			new WeaponDef("berserkers_cleaver", "Berserker\'s Cleaver", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("berserkers_greataxe", "Berserker\'s Greataxe", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("black_iron_clobberer", "Black Iron Clobberer", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("black_iron_greataxe", "Black Iron Greataxe", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("black_iron_greatsword", "Black Iron Greatsword", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("calamity_blade", "Calamity Blade", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("claymore", "Claymore", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("crescent_greataxe", "Crescent Greataxe", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("crimson_cleaver", "Crimson Cleaver", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("crystal_frostblade", "Crystal Frostblade", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("crystal_frostscythe", "Crystal Frostscythe", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("demonic_sword", "Demonic Sword", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("demons_blood_blade", "Demon\'s Blood Blade", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("demons_blood_pick", "Demon\'s Blood Pick", ToolMaterial.NETHERITE, Kind.PICKAXE, 6f, 1.2f),
			new WeaponDef("demons_blood_shovel", "Demon\'s Blood Shovel", ToolMaterial.NETHERITE, Kind.SHOVEL, 6.5f, 1f),
			new WeaponDef("demonslayers_greatsword", "Demonslayer\'s Greatsword", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("dragon_sword", "Dragon Sword", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("emerald_greatcleaver", "Emerald Greatcleaver", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("ethereal_frostblade", "Ethereal Frostblade", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("flamberge", "Flamberge", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("gilded_phoenix_greataxe", "Gilded Phoenix Greataxe", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("gloomsteel_greataxe", "Gloomsteel Greataxe", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("gloomsteel_katana", "Gloomsteel Katana", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("gloomsteel_knife", "Gloomsteel Knife", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("grand_claymore", "Grand Claymore", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("heavenly_partisan", "Heavenly Partisan", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("iron_battle_axe", "Iron Battle Axe", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("iron_broadsword", "Iron Broadsword", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("iron_dagger", "Iron Dagger", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("iron_greataxe", "Iron Greataxe", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("iron_halberd", "Iron Halberd", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("iron_hay_sickle", "Iron Scythe", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("iron_mace", "Iron Mace", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("iron_polearm", "Iron Polearm", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("iron_sai", "Iron Sai", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("lycanbane", "Shadow Sword", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("moonlight", "Moonlight", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("nature_sword", "Nature Sword", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("nocturne", "Nocturne", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("oculus", "Oculus", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("phantomguard_greatsword", "Phantomguard Greatsword", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("phantomguard_partisan", "Phantomguard Partisan", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("piercer", "Piercer", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("ravenous_blade", "Ravenous Blade", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("revenants_darkscepter", "Revenant\'s Darkscepter", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("revenants_gravecleaver", "Revenant\'s Gravecleaver", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("revenants_gravescepter", "Revenant\'s Gravescepter", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("runic_piercer", "Runic Piercer", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("sacrificial_cleaver", "Sacrificial Cleaver", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("skeleton_axe", "Skeleton Axe", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("solstice", "Solstice", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("spider_sword", "Spider Sword", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("talonbrand", "Talonbrand", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("talonpick", "Talonpick", ToolMaterial.DIAMOND, Kind.PICKAXE, 5f, 1.2f),
			new WeaponDef("talonshovel", "Talonshovel", ToolMaterial.DIAMOND, Kind.SHOVEL, 5.5f, 1f),
			new WeaponDef("treacherous_axe", "Treacherous Axe", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("treacherous_bludgeon", "Treacherous Bludgeon", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("treacherous_cleaver", "Treacherous Cleaver", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("uchigatana", "Uchigatana", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("vaelith", "Vaelith", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("valhakyra", "Valhakyra", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("vengeance_blade", "Vengeance Blade", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("vesper", "Vesper", ToolMaterial.DIAMOND, Kind.SWORD, 7f, 1.6f),
			new WeaponDef("vindicator", "Vindicator", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("viridian_greataxe", "Viridian Greataxe", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("viridian_pickaxe", "Viridian Pickaxe", ToolMaterial.IRON, Kind.PICKAXE, 4f, 1.2f),
			new WeaponDef("viridian_shovel", "Viridian Shovel", ToolMaterial.IRON, Kind.SHOVEL, 4.5f, 1f),
			new WeaponDef("void_oculus", "Void Oculus", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("windreaper", "Windreaper", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("winterthorn", "Winterthorn", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
			new WeaponDef("wooden_bludgeon", "Wooden Bludgeon", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("wooden_tonfa", "Wooden Tonfa", ToolMaterial.IRON, Kind.SWORD, 6f, 1.6f),
			new WeaponDef("zenith", "Zenith", ToolMaterial.NETHERITE, Kind.SWORD, 8f, 1.6f),
	};

	private PowersWeapons() {
	}

	public static void initialize() {
		for (WeaponDef def : DEFS) {
			WEAPONS.put(def.id(), register(def));
		}
		CreativeModeTabEvents.modifyOutputEvent(COMBAT_TAB)
				.register(creativeTab -> WEAPONS.values().forEach(creativeTab::accept));
	}

	private static Item register(WeaponDef def) {
		Item.Properties props = new Item.Properties();
		switch (def.kind()) {
			case SWORD -> props.sword(def.material(), def.damage(), def.speed());
			case PICKAXE -> props.pickaxe(def.material(), def.damage(), def.speed());
			case SHOVEL -> props.shovel(def.material(), def.damage(), def.speed());
		}
		if (def.id().equals("lycanbane")) {
			props.component(DataComponents.LORE, new ItemLore(java.util.List.of(
					Component.translatable("item.powers.shadow_sword.lore")
							.withStyle(ChatFormatting.DARK_PURPLE),
					Component.translatable("item.powers.shadow_sword.controls")
							.withStyle(ChatFormatting.GRAY))));
		}
		return ModItemIds.register(ModItemIds.create(def.id()),
				def.id().equals("lycanbane") ? ShadowSwordItem::new : Item::new, props);
	}
}
