package com.powers.fx;

/** Supplies finite face bases and camera-relative transforms for scar motif primitives. */
public final class VisualScarGeometry {
	private VisualScarGeometry() {
	}

	/** Returns a right-handed surface basis whose U cross V vector equals the outward face normal. */
	public static Basis basis(VisualScarRules.Face face) {
		return switch (face) {
			case UP -> new Basis(vec(1, 0, 0), vec(0, 0, -1), vec(0, 1, 0));
			case DOWN -> new Basis(vec(1, 0, 0), vec(0, 0, 1), vec(0, -1, 0));
			case NORTH -> new Basis(vec(1, 0, 0), vec(0, -1, 0), vec(0, 0, -1));
			case SOUTH -> new Basis(vec(1, 0, 0), vec(0, 1, 0), vec(0, 0, 1));
			case WEST -> new Basis(vec(0, 0, 1), vec(0, 1, 0), vec(-1, 0, 0));
			case EAST -> new Basis(vec(0, 0, 1), vec(0, -1, 0), vec(1, 0, 0));
		};
	}

	/** Converts one bounded local surface point to finite camera-relative float coordinates. */
	public static VisualScarMotifGeometry.Vertex vertex(Basis basis, double localU, double localV,
			double worldX, double worldY, double worldZ,
			double cameraX, double cameraY, double cameraZ, double outward, int rgba) {
		double x = worldX - cameraX + basis.u().x() * localU
				+ basis.v().x() * localV + basis.normal().x() * outward;
		double y = worldY - cameraY + basis.u().y() * localU
				+ basis.v().y() * localV + basis.normal().y() * outward;
		double z = worldZ - cameraZ + basis.u().z() * localU
				+ basis.v().z() * localV + basis.normal().z() * outward;
		return new VisualScarMotifGeometry.Vertex((float) x, (float) y, (float) z, rgba);
	}

	private static Vec vec(double x, double y, double z) {
		return new Vec(x, y, z);
	}

	public record Vec(double x, double y, double z) {
	}

	public record Basis(Vec u, Vec v, Vec normal) {
	}
}
