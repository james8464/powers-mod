package com.powers.item;

import java.util.Locale;

/** Authored combat identity shared by visually related fantasy weapons. */
public enum FantasyWeaponArchetype {
	FROST(7.2F, 1.45F, 60, 0xB8F4FF),
	SWIFT(5.4F, 2.25F, 20, 0x81E6FF),
	REAPER(7.8F, 1.35F, 50, 0x745090),
	CRUSHER(9.2F, 0.90F, 70, 0xC7A77A),
	BERSERKER(8.8F, 1.10F, 50, 0xD43131),
	ARCANE(6.6F, 1.55F, 40, 0xA35CFF),
	VITAL(6.4F, 1.60F, 45, 0x5FE06C),
	RADIANT(7.6F, 1.40F, 55, 0xFFF0A8),
	ABYSSAL(8.0F, 1.25F, 55, 0x3A0D4A),
	GUARDIAN(7.0F, 1.30F, 60, 0x7BA9D9),
	HUNTER(6.8F, 1.70F, 35, 0xD69B45),
	PIERCER(6.2F, 1.85F, 30, 0xE2E8F0);

	private final float damage;
	private final float speed;
	private final int procCooldownTicks;
	private final int color;

	FantasyWeaponArchetype(float damage, float speed, int procCooldownTicks, int color) {
		this.damage = damage;
		this.speed = speed;
		this.procCooldownTicks = procCooldownTicks;
		this.color = color;
	}

	public float damage() { return damage; }
	public float speed() { return speed; }
	public int procCooldownTicks() { return procCooldownTicks; }
	public int color() { return color; }

	public String loreKey() {
		return "item.powers.weapon_archetype." + name().toLowerCase(Locale.ROOT);
	}

	public static FantasyWeaponArchetype from(String id) {
		String name = id.toLowerCase(Locale.ROOT);
		if (has(name, "frost", "winter", "amethyst", "crystal")) return FROST;
		if (has(name, "dagger", "knife", "sai", "katana", "sabre", "tonfa", "uchigatana")) return SWIFT;
		if (has(name, "scythe", "sickle", "reaper")) return REAPER;
		if (has(name, "clobber", "bludgeon", "mace", "slab", "shovel")) return CRUSHER;
		if (has(name, "cleaver", "axe")) return BERSERKER;
		if (has(name, "oculus", "runic", "scepter")) return ARCANE;
		if (has(name, "nature", "viridian", "emerald")) return VITAL;
		if (has(name, "heaven", "solstice", "moon", "phoenix", "zenith", "valhakyra")) return RADIANT;
		if (has(name, "demon", "gloom", "nocturne", "void", "revenant", "calamity",
				"blood", "ravenous", "sacrificial")) return ABYSSAL;
		if (has(name, "greatsword", "claymore", "broadsword", "partisan", "halberd", "polearm")) {
			return GUARDIAN;
		}
		if (has(name, "pick", "piercer", "talon")) return PIERCER;
		return HUNTER;
	}

	private static boolean has(String value, String... fragments) {
		for (String fragment : fragments) if (value.contains(fragment)) return true;
		return false;
	}
}
