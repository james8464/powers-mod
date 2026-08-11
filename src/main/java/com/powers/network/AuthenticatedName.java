package com.powers.network;

import java.text.Normalizer;
import java.util.Locale;

/** Bounded canonical form for exact target names, formatting and common spoofing included. */
final class AuthenticatedName {
	private static final int MAX_CODE_POINTS = 128;

	private AuthenticatedName() {
	}

	static String canonical(String raw) {
		if (raw == null || raw.codePointCount(0, raw.length()) > MAX_CODE_POINTS) return "";
		String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC)
				.toLowerCase(Locale.ROOT);
		StringBuilder result = new StringBuilder(normalized.length());
		boolean formattingCode = false;
		for (int offset = 0; offset < normalized.length();) {
			int codePoint = normalized.codePointAt(offset);
			offset += Character.charCount(codePoint);
			if (formattingCode) {
				formattingCode = false;
				continue;
			}
			if (codePoint == '§') {
				formattingCode = true;
				continue;
			}
			int type = Character.getType(codePoint);
			if (type == Character.CONTROL || type == Character.FORMAT
					|| type == Character.SURROGATE || type == Character.PRIVATE_USE
					|| type == Character.UNASSIGNED) continue;
			result.appendCodePoint(confusableSkeleton(codePoint));
		}
		return result.toString().strip();
	}

	/** Small conservative skeleton covering lookalikes used in Minecraft-name spoofing. */
	private static int confusableSkeleton(int codePoint) {
		return switch (codePoint) {
			case 'а', 'α' -> 'a';
			case 'в', 'β' -> 'b';
			case 'с', 'ϲ' -> 'c';
			case 'е', 'ε' -> 'e';
			case 'і', 'ι' -> 'i';
			case 'ј' -> 'j';
			case 'к', 'κ' -> 'k';
			case 'м', 'μ' -> 'm';
			case 'н', 'η' -> 'h';
			case 'о', 'ο' -> 'o';
			case 'р', 'ρ' -> 'p';
			case 'ѕ' -> 's';
			case 'т', 'τ' -> 't';
			case 'у', 'υ' -> 'y';
			case 'х', 'χ' -> 'x';
			case 'ᴠ' -> 'v';
			default -> codePoint;
		};
	}
}
