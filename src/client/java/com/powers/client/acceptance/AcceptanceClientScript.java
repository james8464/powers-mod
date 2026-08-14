package com.powers.client.acceptance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Strict, deterministic instruction format for development multiplayer acceptance clients. */
public final class AcceptanceClientScript {
	public enum Operation {
		COMMAND, CHAT, ACTIVATE, SELECT, USE, ATTACK, GRIMOIRE, CRYSTAL, ARTIFACT,
		ARTIFACT_TELEPORT, TELEPORT, RESPAWN, CLOSE, LOCATOR, KEY, LOOK, CLEAN, SCREENSHOT
	}

	public record Step(int tick, Operation operation, String argument) {
		public Step {
			if (tick < 0) throw new IllegalArgumentException("negative acceptance tick");
			if (argument == null || argument.isBlank()) {
				throw new IllegalArgumentException("blank acceptance argument");
			}
		}
	}

	private AcceptanceClientScript() {
	}

	public static List<Step> parse(List<String> lines) {
		List<Step> result = new ArrayList<>();
		int previousTick = -1;
		for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
			String line = lines.get(lineNumber).strip();
			if (line.isEmpty() || line.startsWith("#")) continue;
			String[] cells = line.split("\\t", 3);
			if (cells.length != 3) throw malformed(lineNumber, "expected three tab-separated cells");
			int tick;
			try {
				tick = Integer.parseInt(cells[0]);
			} catch (NumberFormatException exception) {
				throw malformed(lineNumber, "tick is not an integer");
			}
			if (tick < previousTick) throw malformed(lineNumber, "ticks are out of order");
			Operation operation;
			try {
				operation = Operation.valueOf(cells[1].strip().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				throw malformed(lineNumber, "unknown operation");
			}
			String argument = cells[2].strip();
			validate(operation, argument, lineNumber);
			result.add(new Step(tick, operation, argument));
			previousTick = tick;
		}
		return List.copyOf(result);
	}

	private static void validate(Operation operation, String argument, int lineNumber) {
		if (argument.isEmpty() || argument.length() > 512 || argument.chars()
				.anyMatch(character -> Character.isISOControl(character) && character != '\t')) {
			throw malformed(lineNumber, "invalid argument");
		}
		switch (operation) {
			case ACTIVATE -> parseBoundedInteger(argument, 0, 2, lineNumber, "slot");
			case SELECT -> {
				String[] values = argument.split(" ");
				if (values.length != 2) throw malformed(lineNumber, "selection needs slot and option");
				parseBoundedInteger(values[0], 0, 2, lineNumber, "slot");
				parseBoundedInteger(values[1], 0, 255, lineNumber, "option");
			}
			case SCREENSHOT -> {
				if (!argument.matches("[a-zA-Z0-9_-]{1,80}")) {
					throw malformed(lineNumber, "unsafe screenshot label");
				}
			}
			case RESPAWN -> {
				if (!argument.equals("now")) throw malformed(lineNumber, "respawn argument must be now");
			}
			case USE -> {
				if (!argument.equals("main")) throw malformed(lineNumber, "use argument must be main");
			}
			case ATTACK -> validateIdentifier(argument, lineNumber, "target");
			case GRIMOIRE -> validatePair(argument, 0, 15, lineNumber, "spell");
			case CRYSTAL -> parseBoundedInteger(argument, 0, 255, lineNumber, "mode");
			case ARTIFACT -> {
				String[] values = argument.split(" ");
				if (values.length != 3 || !(values[0].equals("light")
						|| values[0].equals("darkness"))) {
					throw malformed(lineNumber, "artifact needs alignment, action and option");
				}
				validateIdentifier(values[1], lineNumber, "action");
				parseBoundedInteger(values[2], -1, 255, lineNumber, "option");
			}
			case ARTIFACT_TELEPORT -> {
				String[] values = argument.split(" ");
				if (values.length != 6 || !(values[0].equals("light")
						|| values[0].equals("darkness"))) {
					throw malformed(lineNumber,
							"artifact teleport needs alignment, x, y, z, dimension and target");
				}
				parseFiniteDouble(values[1], lineNumber, "x");
				parseFiniteDouble(values[2], lineNumber, "y");
				parseFiniteDouble(values[3], lineNumber, "z");
				validateIdentifier(values[4], lineNumber, "dimension");
				validateIdentifier(values[5], lineNumber, "target");
			}
			case TELEPORT -> {
				String[] values = argument.split(" ");
				if (values.length != 5) {
					throw malformed(lineNumber, "teleport needs slot, x, y, z and dimension");
				}
				parseBoundedInteger(values[0], 0, 2, lineNumber, "slot");
				parseFiniteDouble(values[1], lineNumber, "x");
				parseFiniteDouble(values[2], lineNumber, "y");
				parseFiniteDouble(values[3], lineNumber, "z");
				validateIdentifier(values[4], lineNumber, "dimension");
			}
			case LOCATOR -> {
				if (argument.length() > 64) throw malformed(lineNumber, "locator input is too long");
			}
			case KEY -> {
				String[] values = argument.split(" ");
				if (values.length != 2 || !(values[1].equals("on") || values[1].equals("off"))
						|| !(values[0].equals("forward") || values[0].equals("back")
						|| values[0].equals("left") || values[0].equals("right")
						|| values[0].equals("jump") || values[0].equals("sneak")
						|| values[0].equals("sprint") || values[0].equals("advancements")
						|| values[0].equals("rank_maze"))) {
					throw malformed(lineNumber, "key needs a supported name and on/off state");
				}
			}
			case LOOK -> {
				String[] values = argument.split(" ");
				if (values.length != 2) throw malformed(lineNumber, "look needs yaw and pitch");
				parseBoundedDouble(values[0], -180.0, 180.0, lineNumber, "yaw");
				parseBoundedDouble(values[1], -90.0, 90.0, lineNumber, "pitch");
			}
			case CLOSE -> {
				if (!argument.equals("screen")) throw malformed(lineNumber,
						"close argument must be screen");
			}
			case CLEAN -> {
				if (!argument.equals("ui")) throw malformed(lineNumber,
						"clean argument must be ui");
			}
			case COMMAND, CHAT -> { }
		}
	}

	private static void validatePair(String argument, int minimum, int maximum,
			int lineNumber, String numericField) {
		String[] values = argument.split(" ");
		if (values.length != 2) throw malformed(lineNumber, "expected identifier and " + numericField);
		validateIdentifier(values[0], lineNumber, "identifier");
		parseBoundedInteger(values[1], minimum, maximum, lineNumber, numericField);
	}

	private static void validateIdentifier(String value, int lineNumber, String field) {
		if (!value.matches("[a-zA-Z0-9_.:/-]{1,64}")) {
			throw malformed(lineNumber, "unsafe " + field);
		}
	}

	private static int parseBoundedInteger(String value, int minimum, int maximum,
			int lineNumber, String field) {
		try {
			int parsed = Integer.parseInt(value);
			if (parsed < minimum || parsed > maximum) {
				throw malformed(lineNumber, field + " is out of range");
			}
			return parsed;
		} catch (NumberFormatException exception) {
			throw malformed(lineNumber, field + " is not an integer");
		}
	}

	private static double parseFiniteDouble(String value, int lineNumber, String field) {
		try {
			double parsed = Double.parseDouble(value);
			if (!Double.isFinite(parsed)) throw malformed(lineNumber, field + " is not finite");
			return parsed;
		} catch (NumberFormatException exception) {
			throw malformed(lineNumber, field + " is not a number");
		}
	}

	private static double parseBoundedDouble(String value, double minimum, double maximum,
			int lineNumber, String field) {
		double parsed = parseFiniteDouble(value, lineNumber, field);
		if (parsed < minimum || parsed > maximum) {
			throw malformed(lineNumber, field + " is out of range");
		}
		return parsed;
	}

	private static IllegalArgumentException malformed(int lineNumber, String reason) {
		return new IllegalArgumentException("Acceptance script line " + (lineNumber + 1) + ": " + reason);
	}
}
