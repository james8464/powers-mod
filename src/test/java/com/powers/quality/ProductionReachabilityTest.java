package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards against production-only placeholders that are never reached by the mod runtime. */
class ProductionReachabilityTest {
	private static final List<String> SOURCE_ROOTS = List.of("src/main/java", "src/client/java");
	private static final List<String> REFERENCE_ROOTS = List.of(
			"src/main/java", "src/client/java", "src/main/resources", "src/client/resources");

	@Test
	void everyTopLevelProductionTypeHasAProductionOrResourceConsumer() throws IOException {
		Path project = Path.of(System.getProperty("user.dir"));
		List<Path> referenceFiles = referenceFiles(project);
		List<String> orphans = new ArrayList<>();
		for (String rootName : SOURCE_ROOTS) {
			Path root = project.resolve(rootName);
			if (!Files.isDirectory(root)) continue;
			try (Stream<Path> files = Files.walk(root)) {
				for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
					String fileName = source.getFileName().toString();
					if (fileName.equals("package-info.java") || fileName.equals("module-info.java")) continue;
					String typeName = fileName.substring(0, fileName.length() - ".java".length());
					Pattern reference = Pattern.compile("\\b" + Pattern.quote(typeName) + "\\b");
					boolean consumed = false;
					for (Path candidate : referenceFiles) {
						if (candidate.equals(source)) continue;
						if (reference.matcher(Files.readString(candidate)).find()) {
							consumed = true;
							break;
						}
					}
					if (!consumed) orphans.add(project.relativize(source).toString().replace('\\', '/'));
				}
			}
		}
		assertTrue(orphans.isEmpty(), () -> "Production types without a runtime/resource consumer: "
				+ String.join(", ", orphans));
	}

	private static List<Path> referenceFiles(Path project) throws IOException {
		List<Path> files = new ArrayList<>();
		for (String rootName : REFERENCE_ROOTS) {
			Path root = project.resolve(rootName);
			if (!Files.isDirectory(root)) continue;
			try (Stream<Path> paths = Files.walk(root)) {
				paths.filter(Files::isRegularFile).filter(ProductionReachabilityTest::isTextReferenceFile)
						.forEach(files::add);
			}
		}
		for (String buildFile : List.of("build.gradle", "settings.gradle", "gradle.properties")) {
			Path path = project.resolve(buildFile);
			if (Files.isRegularFile(path)) files.add(path);
		}
		return List.copyOf(files);
	}

	private static boolean isTextReferenceFile(Path path) {
		String name = path.getFileName().toString();
		return name.endsWith(".java") || name.endsWith(".json") || name.endsWith(".properties")
				|| name.endsWith(".mcmeta") || name.endsWith(".accesswidener");
	}
}
