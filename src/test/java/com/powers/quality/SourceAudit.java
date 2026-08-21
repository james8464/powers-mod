package com.powers.quality;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

final class SourceAudit {
	private static final Pattern WILDCARD_IMPORT = Pattern.compile("(?m)^import\\s+[^;]*\\*;");
	private static final Pattern UNFINISHED = Pattern.compile("\\b(?:TODO|FIXME|XXX|HACK)\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern DEBUG_WRITE = Pattern.compile("System\\.(?:out|err)|\\.printStackTrace\\s*\\(");
	private static final Pattern MANIFEST_ROW = Pattern.compile("^\\| `([^`]+\\.java)` \\|");
	private static final Pattern MECHANICAL_NARRATION = Pattern.compile(
			"(?i)^(?:apply|build|calculate|call|check|clear|close|compute|create|get|handle|initialize|"
					+ "iterate|load|loop|open|process|register|remove|return|run|save|send|set|spawn|update)(?:\\s|$)");
	private static final Pattern STRONG_INTENT_SIGNAL = Pattern.compile(
			"(?i)\\b(?:after|authority|before|because|bound(?:ed|s)?|cap|deliberately|deterministic|ensures?|"
					+ "exact(?:ly)?|finite|hard|immutable|instead|invariant|keeps?|limit|must|never|otherwise|"
					+ "own(?:ed|er|ership|s)?|preserves?|prevents?|rather|stable|transactional|unless|until|"
					+ "while|without)\\b");
	private static final Pattern VAGUE_COMMENT = Pattern.compile(
			"(?i)^(?:first|second|value|logic|handler|setup)(?:\\s|$)");
	private static final Pattern CONTRACT_SIGNAL = Pattern.compile(
			"(?i)\\b(?:authorit(?:y|ative)|authori[sz](?:e|ed|es|ation)|valid(?:ate|ated|ates|ation)?|invalid|reject(?:s|ed|ion)?|"
					+ "require(?:s|d)?|epoch|lifecycle|start(?:s|ed)?|stop(?:s|ped)?|remov(?:e|es|ed|al)|"
					+ "expir(?:e|es|ed|y)|one-shot|once|outcome|returns?|reports?|registers?|issues?|commits?|"
					+ "exposes?|identifies|supplies|allows?|denies|throws?|creates?|clears?|preserves?|emits?)\\b");
	private static final Pattern UNSUPPORTED_CERTAINTY = Pattern.compile(
			"(?i)\\b(?:always works?|cannot fail|completely safe|fully safe|guaranteed to work|"
					+ "handles everything|should never happen)\\b");
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
			ParsedSource parsed = ParsedSource.parse(relative, source);
			if (!relative.endsWith("package-info.java")) {
				undocumented.addAll(parsed.undocumentedPublicTypes());
			}
			for (Comment comment : comments(source)) {
				addMatches(unfinished, relative, comment, UNFINISHED);
				int genericLine = genericNarrationLine(comment);
				if (genericLine >= 0) genericComments.add(relative + ":" + genericLine);
				addMatches(misleadingComments, relative, comment, UNSUPPORTED_CERTAINTY);
			}
			if (relative.contains("/api/")) undocumentedContracts.addAll(
					parsed.undocumentedPublicContracts());
			if (DEBUG_WRITE.matcher(source).find()) debug.add(relative);
			if (WILDCARD_IMPORT.matcher(source).find()) wildcard.add(relative);
			long lines;
			try (Stream<String> stream = Files.lines(file)) {
				lines = stream.count();
			}
			if (lines > MAX_REVIEWED_LINES) oversized.add(relative + " (" + lines + ")");
			int ownerCount = parsed.externallyVisibleOwnerCount();
			if (lines > MIXED_RESPONSIBILITY_LINES && ownerCount > 1) {
				mixedResponsibility.add(relative + " (" + lines + " lines, "
						+ ownerCount + " externally visible owners)");
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

	private static void addMatches(Set<String> findings, String relative, Comment comment, Pattern pattern) {
		Matcher matcher = pattern.matcher(comment.text());
		while (matcher.find()) findings.add(relative + ":" + comment.lineAt(matcher.start()));
	}

	private static int genericNarrationLine(Comment comment) {
		String[] lines = comment.text().split("\\R", -1);
		boolean paragraphHasIntent = STRONG_INTENT_SIGNAL.matcher(comment.text()).find();
		for (int index = 0; index < lines.length; index++) {
			String text = lines[index].replaceFirst("^\\s*\\*?\\s*", "").trim();
			if (text.isEmpty() || text.startsWith("@")) continue;
			int words = text.split("\\s+").length;
			boolean mechanical = MECHANICAL_NARRATION.matcher(text).find();
			boolean vague = VAGUE_COMMENT.matcher(text).find() && words <= 6;
			if ((mechanical && !paragraphHasIntent) || vague) {
				return comment.line() + index;
			}
		}
		return -1;
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
					if (textBlock && index + 2 < source.length() && source.startsWith("\"\"\"", index)
							&& !isEscaped(source, index)) {
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

	private static boolean isEscaped(String source, int offset) {
		int slashes = 0;
		for (int index = offset - 1; index >= 0 && source.charAt(index) == '\\'; index--) slashes++;
		return (slashes & 1) == 1;
	}

	private record Comment(String text, int line, boolean javadoc, boolean leading) {
		int lineAt(int offset) {
			return line + (int) text.substring(0, Math.max(0, offset)).chars()
					.filter(character -> character == '\n').count();
		}
	}

	private record ParsedSource(Set<String> undocumentedPublicTypes,
			Set<String> undocumentedPublicContracts,
			int externallyVisibleOwnerCount) {
		static ParsedSource parse(String relative, String source) throws IOException {
			var compiler = ToolProvider.getSystemJavaCompiler();
			JavaFileObject input = new SimpleJavaFileObject(
					URI.create("string:///" + relative.replace(" ", "%20")), JavaFileObject.Kind.SOURCE) {
				@Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
			};
			JavacTask task = (JavacTask) compiler.getTask(null, null, diagnostic -> { },
					List.of("-proc:none"), null, List.of(input));
			CompilationUnitTree unit = task.parse().iterator().next();
			DocTrees docs = DocTrees.instance(task);
			SourcePositions positions = docs.getSourcePositions();
			Set<String> types = new LinkedHashSet<>();
			Set<String> contracts = new LinkedHashSet<>();
			int[] owners = {0};
			new TreePathScanner<Void, Void>() {
				private final Deque<Boolean> publicSurface = new ArrayDeque<>();
				private final Deque<Boolean> interfaceScope = new ArrayDeque<>();

				@Override public Void visitClass(ClassTree tree, Void unused) {
					boolean topLevel = getCurrentPath().getParentPath().getLeaf() instanceof CompilationUnitTree;
					Set<Modifier> modifiers = tree.getModifiers().getFlags();
					boolean explicitlyPublic = modifiers.contains(Modifier.PUBLIC);
					boolean implicitlyPublic = !topLevel && !interfaceScope.isEmpty()
							&& interfaceScope.peek() && !modifiers.contains(Modifier.PRIVATE);
					boolean visible = topLevel
							? explicitlyPublic
							: !publicSurface.isEmpty() && publicSurface.peek()
									&& (explicitlyPublic || implicitlyPublic);
					if (topLevel && visible) {
						var doc = docs.getDocCommentTree(getCurrentPath());
						if (doc == null || doc.toString().isBlank()) {
							types.add(finding(relative, unit, positions, tree));
						}
					}
					if (visible && (topLevel || hasDeclaredBehaviour(tree))) owners[0]++;
					publicSurface.push(visible);
					interfaceScope.push(tree.getKind() == Tree.Kind.INTERFACE
							|| tree.getKind() == Tree.Kind.ANNOTATION_TYPE);
					super.visitClass(tree, unused);
					interfaceScope.pop();
					publicSurface.pop();
					return null;
				}

				@Override public Void visitMethod(MethodTree tree, Void unused) {
					boolean inherited = tree.getModifiers().getAnnotations().stream()
							.map(annotation -> annotation.getAnnotationType().toString())
							.anyMatch(annotation -> annotation.equals("Override")
									|| annotation.equals("java.lang.Override"));
					boolean interfaceMethod = !interfaceScope.isEmpty() && interfaceScope.peek()
							&& !tree.getModifiers().getFlags().contains(Modifier.PRIVATE);
					boolean publicMethod = tree.getModifiers().getFlags().contains(Modifier.PUBLIC);
					if (!publicSurface.isEmpty() && publicSurface.peek() && (publicMethod || interfaceMethod)
							&& !inherited) {
						var doc = docs.getDocCommentTree(getCurrentPath());
						String contract = doc == null ? "" : doc.toString();
						int contractWords = contract.isBlank() ? 0 : contract.trim().split("\\s+").length;
						if (contractWords < 4 || !CONTRACT_SIGNAL.matcher(contract).find()) {
							contracts.add(finding(relative, unit, positions, tree));
						}
					}
					return super.visitMethod(tree, unused);
				}
			}.scan(unit, null);
			return new ParsedSource(Collections.unmodifiableSet(types),
					Collections.unmodifiableSet(contracts), owners[0]);
		}

		private static String finding(String relative, CompilationUnitTree unit,
				SourcePositions positions, Tree tree) {
			long offset = positions.getStartPosition(unit, tree);
			long line = unit.getLineMap().getLineNumber(Math.max(0, offset));
			return relative + ":" + line;
		}

		private static boolean hasDeclaredBehaviour(ClassTree tree) {
			return (tree.getKind() == Tree.Kind.CLASS || tree.getKind() == Tree.Kind.INTERFACE)
					&& tree.getMembers().stream().anyMatch(member -> member instanceof MethodTree method
							&& method.getReturnType() != null);
		}
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
