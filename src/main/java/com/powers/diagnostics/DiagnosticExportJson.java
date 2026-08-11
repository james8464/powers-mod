package com.powers.diagnostics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Stable JSON encoding for the versioned aggregate diagnostic schema. */
public final class DiagnosticExportJson {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private DiagnosticExportJson() {
	}

	public static String toJson(DiagnosticExport export) {
		return GSON.toJson(export) + "\n";
	}
}
