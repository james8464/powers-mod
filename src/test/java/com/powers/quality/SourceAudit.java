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
	private static final Pattern UNFINISHED = Pattern.compile("\\b(?:TODO|FIXME|XXX|HACK)\\b");
	private static final Pattern DEBUG_WRITE = Pattern.compile("System\\.(?:out|err)|\\.printStackTrace\\s*\\(");
	private static final Pattern MANIFEST_ROW = Pattern.compile("^\\| `([^`]+\\.java)` \\|");
	private static final int MAX_REVIEWED_LINES = 450;

	private SourceAudit() {
	}

	static Result scan(Path projectRoot) throws IOException {
		Set<String> productionFiles = productionFiles(projectRoot);
		Set<String> manifestFiles = manifestFiles(projectRoot);
		Set<String> undocumented = new LinkedHashSet<>();
		Set<String> missingPackages = new LinkedHashSet<>();
		Set<String> unfinished = new LinkedHashSet<>();
		Set<String> debug = new LinkedHashSet<>();
		Set<String> wildcard = new LinkedHashSet<>();
		Set<String> oversized = new LinkedHashSet<>();

		for (String relative : productionFiles) {
			Path file = projectRoot.resolve(relative);
			String source = Files.readString(file);
			if (!relative.endsWith("package-info.java") && !hasDocumentedPublicType(source)) {
				undocumented.add(relative);
			}
			if (UNFINISHED.matcher(source).find()) unfinished.add(relative);
			if (DEBUG_WRITE.matcher(source).find()) debug.add(relative);
			if (WILDCARD_IMPORT.matcher(source).find()) wildcard.add(relative);
			long lines;
			try (Stream<String> stream = Files.lines(file)) {
				lines = stream.count();
			}
			if (lines > MAX_REVIEWED_LINES) oversized.add(relative + " (" + lines + ")");
		}

		for (String directory : sourceDirectories(productionFiles)) {
			String packageInfo = directory + "/package-info.java";
			if (!productionFiles.contains(packageInfo)) missingPackages.add(packageInfo);
		}
		return new Result(productionFiles, manifestFiles, undocumented, missingPackages,
				unfinished, debug, wildcard, oversized);
	}

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
			Set<String> unfinishedMarkers, Set<String> debugWrites,
			Set<String> wildcardImports, Set<String> oversizedFiles) {
		String summary() {
			List<String> sections = new ArrayList<>();
			append(sections, "manifest missing/extra", symmetricDifference(productionFiles, manifestFiles));
			append(sections, "undocumented public types", undocumentedPublicTypes);
			append(sections, "missing package docs", missingPackageDocumentation);
			append(sections, "unfinished markers", unfinishedMarkers);
			append(sections, "debug writes", debugWrites);
			append(sections, "wildcard imports", wildcardImports);
			append(sections, "oversized responsibility files", oversizedFiles);
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
