package com.powers.quality;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class SourceAudit {
	private static final Pattern PUBLIC_TYPE = Pattern.compile(
			"(?m)^public\\s+(?:(?:final|abstract|sealed|non-sealed)\\s+)*(?:class|interface|record|enum)\\s+");
	private static final Pattern WILDCARD_IMPORT = Pattern.compile("(?m)^import\\s+[^;]*\\*;");
	private static final Pattern UNFINISHED = Pattern.compile("\\b(?:TODO|FIXME|XXX|HACK)\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern DEBUG_WRITE = Pattern.compile("System\\.(?:out|err)|\\.printStackTrace\\s*\\(");
	private static final Pattern MANIFEST_ROW = Pattern.compile("^\\| `([^`]+\\.java)` \\|");
	private static final Pattern MECHANICAL_NARRATION = Pattern.compile(
			"(?i)^(?:apply|build|calculate|call|check|clear|close|compute|create|get|handle|initialize|"
					+ "iterate|load|loop|open|process|register|remove|return|run|save|send|set|spawn|update)\\b");
	private static final Pattern INTENT_SIGNAL = Pattern.compile(
			"(?i)\\b(?:after|authority|before|because|bound(?:ed|s)?|cap|deliberately|deterministic|ensures?|"
					+ "exact(?:ly)?|finite|hard|immutable|instead|invariant|keeps?|limit|must|never|only|otherwise|"
					+ "own(?:ed|er|ership|s)?|preserves?|prevents?|rather|so|stable|transactional|unless|until|"
					+ "when|while|without)\\b");
	private static final Pattern UNSUPPORTED_CERTAINTY = Pattern.compile(
			"(?i)\\b(?:always works?|cannot fail|completely safe|fully safe|guaranteed to work|"
					+ "handles everything|should never happen)\\b");
	private static final Pattern TOP_LEVEL_TYPE = Pattern.compile(
			"(?m)^(?:public\\s+)?(?:(?:final|abstract|sealed|non-sealed)\\s+)*(?:class|interface|record|enum)\\s+");
	private static final Pattern METHOD_DECLARATION = Pattern.compile("^[^=;{}]+\\([^;{}]*\\)\\s*;?$");
	private static final int MAX_REVIEWED_LINES = 450;
	private static final int MIXED_RESPONSIBILITY_LINES = 350;

	private SourceAudit() {
	}

	static Result scan(Path projectRoot) throws IOException {
		Set<String> productionFiles = productionFiles(projectRoot);
		Set<String> manifestFiles = manifestFiles(projectRoot);
		Set<String> undocumented = new LinkedHashSet<>();
		Set<String> missingPackages = new LinkedHashSet<>();
		Set<String> unfinished = new LinkedHashSet<>();
		Set<String> genericComments = new LinkedHashSet<>();
		Set<String> misleadingComments = new LinkedHashSet<>();
		Set<String> undocumentedContracts = new LinkedHashSet<>();
		Set<String> debug = new LinkedHashSet<>();
		Set<String> wildcard = new LinkedHashSet<>();
		Set<String> oversized = new LinkedHashSet<>();
		Set<String> mixedResponsibility = new LinkedHashSet<>();

		for (String relative : productionFiles) {
			Path file = projectRoot.resolve(relative);
			String source = Files.readString(file);
			if (!relative.endsWith("package-info.java") && !hasDocumentedPublicType(source)) {
				undocumented.add(relative);
			}
			for (Comment comment : comments(source)) {
				String location = relative + ":" + comment.line();
				if (UNFINISHED.matcher(comment.text()).find()) unfinished.add(location);
				if (isGenericNarration(comment)) genericComments.add(location);
				if (UNSUPPORTED_CERTAINTY.matcher(comment.text()).find()) misleadingComments.add(location);
			}
			if (relative.contains("/api/")) undocumentedContracts.addAll(
					undocumentedPublicContracts(relative, source));
			if (DEBUG_WRITE.matcher(source).find()) debug.add(relative);
			if (WILDCARD_IMPORT.matcher(source).find()) wildcard.add(relative);
			long lines;
			try (Stream<String> stream = Files.lines(file)) {
				lines = stream.count();
			}
			if (lines > MAX_REVIEWED_LINES) oversized.add(relative + " (" + lines + ")");
			Matcher topLevelTypes = TOP_LEVEL_TYPE.matcher(source);
			int topLevelTypeCount = 0;
			while (topLevelTypes.find()) topLevelTypeCount++;
			if (lines > MIXED_RESPONSIBILITY_LINES && topLevelTypeCount > 1) {
				mixedResponsibility.add(relative + " (" + lines + " lines, "
						+ topLevelTypeCount + " top-level types)");
			}
		}

		for (String directory : sourceDirectories(productionFiles)) {
			String packageInfo = directory + "/package-info.java";
			if (!productionFiles.contains(packageInfo)) missingPackages.add(packageInfo);
		}
		return new Result(productionFiles, manifestFiles, undocumented, missingPackages,
				unfinished, genericComments, misleadingComments, undocumentedContracts,
				debug, wildcard, oversized, mixedResponsibility);
	}

	private static boolean isGenericNarration(Comment comment) {
		if (comment.javadoc()) return false;
		String text = comment.text().replaceAll("\\s+", " ").trim();
		int words = text.isEmpty() ? 0 : text.split("\\s+").length;
		if (words == 0 || INTENT_SIGNAL.matcher(text).find()) return false;
		return words <= 12 || (words <= 20 && MECHANICAL_NARRATION.matcher(text).find());
	}

	private static Set<String> undocumentedPublicContracts(String relative, String source) {
		Set<String> result = new LinkedHashSet<>();
		boolean publicInterface = Pattern.compile("(?m)^public\\s+(?:sealed\\s+)?interface\\s+")
				.matcher(source).find();
		String[] lines = source.split("\\R", -1);
		int offset = 0;
		String typeName = Path.of(relative).getFileName().toString().replaceFirst("\\.java$", "");
		for (int index = 0; index < lines.length; index++) {
			String trimmed = lines[index].trim();
			boolean explicitPublic = trimmed.startsWith("public ");
			boolean implicitInterfaceMethod = publicInterface && trimmed.endsWith(";") && trimmed.contains("(");
			String declarationHead = trimmed.replaceFirst("^@\\w+(?:\\([^)]*\\))?\\s+", "");
			int body = declarationHead.indexOf('{');
			if (body >= 0) declarationHead = declarationHead.substring(0, body).trim();
			boolean declaration = (explicitPublic || implicitInterfaceMethod)
					&& METHOD_DECLARATION.matcher(declarationHead).matches()
					&& !trimmed.contains(" interface ") && !trimmed.startsWith("public interface ")
					&& !trimmed.startsWith("public class ") && !trimmed.startsWith("public record ")
					&& !trimmed.startsWith("public enum ") && !trimmed.contains("@Override");
			if (declaration) {
				Matcher name = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\(").matcher(trimmed);
				String methodName = null;
				while (name.find()) methodName = name.group(1);
				if (!typeName.equals(methodName) && !hasJavadocBefore(source, offset)) {
					result.add(relative + ":" + (index + 1));
				}
			}
			offset += lines[index].length() + 1;
		}
		return result;
	}

	private static boolean hasJavadocBefore(String source, int declarationOffset) {
		String prefix = source.substring(0, Math.min(declarationOffset, source.length()));
		int close = prefix.lastIndexOf("*/");
		if (close < 0) return false;
		int open = prefix.lastIndexOf("/**", close);
		if (open < 0) return false;
		String between = prefix.substring(close + 2)
				.replaceAll("(?m)^\\s*@[^\\n]+$", "")
				.trim();
		return between.isEmpty();
	}

	private static List<Comment> comments(String source) {
		List<Comment> result = new ArrayList<>();
		int line = 1;
		for (int index = 0; index < source.length();) {
			char current = source.charAt(index);
			if (current == '\n') { line++; index++; continue; }
			if (current == '"' || current == '\'') {
				char quote = current;
				boolean textBlock = quote == '"' && index + 2 < source.length()
						&& source.charAt(index + 1) == '"' && source.charAt(index + 2) == '"';
				index += textBlock ? 3 : 1;
				while (index < source.length()) {
					if (source.charAt(index) == '\n') line++;
					if (textBlock && index + 2 < source.length() && source.startsWith("\"\"\"", index)) {
						index += 3; break;
					}
					if (!textBlock && source.charAt(index) == '\\') { index = Math.min(source.length(), index + 2); continue; }
					if (!textBlock && source.charAt(index) == quote) { index++; break; }
					index++;
				}
				continue;
			}
			if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/') {
				int startLine = line;
				int lineStart = source.lastIndexOf('\n', Math.max(0, index - 1)) + 1;
				boolean leading = source.substring(lineStart, index).isBlank();
				int start = index + 2;
				int end = source.indexOf('\n', start);
				if (end < 0) end = source.length();
				result.add(new Comment(source.substring(start, end), startLine, false, leading));
				index = end;
				continue;
			}
			if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '*') {
				int startLine = line;
				boolean javadoc = index + 2 < source.length() && source.charAt(index + 2) == '*';
				int start = index + 2;
				int end = source.indexOf("*/", start);
				if (end < 0) end = source.length();
				String text = source.substring(start, end);
				line += (int) text.chars().filter(character -> character == '\n').count();
				result.add(new Comment(text, startLine, javadoc, false));
				index = Math.min(source.length(), end + 2);
				continue;
			}
			index++;
		}
		List<Comment> merged = new ArrayList<>();
		for (Comment comment : result) {
			if (!comment.javadoc() && !merged.isEmpty()) {
				Comment previous = merged.getLast();
				long previousLines = previous.text().chars().filter(character -> character == '\n').count();
				if (previous.leading() && comment.leading() && !previous.javadoc()
						&& comment.line() == previous.line() + previousLines + 1) {
					merged.set(merged.size() - 1, new Comment(previous.text() + "\n" + comment.text(),
							previous.line(), false, true));
					continue;
				}
			}
			merged.add(comment);
		}
		return merged;
	}

	private record Comment(String text, int line, boolean javadoc, boolean leading) { }

	private static Set<String> productionFiles(Path root) throws IOException {
		Set<String> result = new LinkedHashSet<>();
		for (String sourceRoot : List.of("src/main/java", "src/client/java")) {
			Path directory = root.resolve(sourceRoot);
			if (!Files.exists(directory)) continue;
			try (Stream<Path> paths = Files.walk(directory)) {
				paths.filter(path -> path.toString().endsWith(".java"))
						.map(root::relativize).map(SourceAudit::portable)
						.sorted().forEach(result::add);
			}
		}
		return Collections.unmodifiableSet(result);
	}

	private static Set<String> manifestFiles(Path root) throws IOException {
		Path manifest = root.resolve("docs/quality/code-audit.md");
		if (!Files.exists(manifest)) return Set.of();
		Set<String> result = new LinkedHashSet<>();
		for (String line : Files.readAllLines(manifest)) {
			Matcher matcher = MANIFEST_ROW.matcher(line);
			if (matcher.find()) result.add(matcher.group(1));
		}
		return Collections.unmodifiableSet(result);
	}

	private static Set<String> sourceDirectories(Set<String> files) {
		Set<String> result = new LinkedHashSet<>();
		for (String file : files) {
			if (file.endsWith("package-info.java")) continue;
			int slash = file.lastIndexOf('/');
			if (slash >= 0) result.add(file.substring(0, slash));
		}
		return result;
	}

	private static boolean hasDocumentedPublicType(String source) {
		Matcher matcher = PUBLIC_TYPE.matcher(source);
		if (!matcher.find()) return true;
		int declaration = matcher.start();
		int close = source.lastIndexOf("*/", declaration);
		int open = close < 0 ? -1 : source.lastIndexOf("/**", close);
		if (open < 0 || close < open) return false;
		String between = source.substring(close + 2, declaration)
				.replaceAll("(?m)^\\s*@[^\\n]+$", "");
		return between.isBlank();
	}

	private static String portable(Path path) {
		return path.toString().replace('\\', '/');
	}

	record Result(Set<String> productionFiles, Set<String> manifestFiles,
			Set<String> undocumentedPublicTypes, Set<String> missingPackageDocumentation,
			Set<String> unfinishedMarkers, Set<String> genericComments,
			Set<String> misleadingComments, Set<String> undocumentedPublicContracts,
			Set<String> debugWrites, Set<String> wildcardImports,
			Set<String> oversizedFiles, Set<String> mixedResponsibilityFiles) {
		String summary() {
			List<String> sections = new ArrayList<>();
			append(sections, "manifest missing/extra", symmetricDifference(productionFiles, manifestFiles));
			append(sections, "undocumented public types", undocumentedPublicTypes);
			append(sections, "missing package docs", missingPackageDocumentation);
			append(sections, "unfinished markers", unfinishedMarkers);
			append(sections, "generic narration comments", genericComments);
			append(sections, "unsupported certainty comments", misleadingComments);
			append(sections, "undocumented public API contracts", undocumentedPublicContracts);
			append(sections, "debug writes", debugWrites);
			append(sections, "wildcard imports", wildcardImports);
			append(sections, "oversized responsibility files", oversizedFiles);
			append(sections, "mixed-responsibility files", mixedResponsibilityFiles);
			return String.join(System.lineSeparator(), sections);
		}

		private static void append(List<String> sections, String name, Set<String> values) {
			if (!values.isEmpty()) sections.add(name + ": " + String.join(", ", values));
		}

		private static Set<String> symmetricDifference(Set<String> first, Set<String> second) {
			Set<String> difference = new LinkedHashSet<>(first);
			difference.addAll(second);
			Set<String> shared = new LinkedHashSet<>(first);
			shared.retainAll(second);
			difference.removeAll(shared);
			return difference;
		}
	}
}
