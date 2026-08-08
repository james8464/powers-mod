package com.powers.progression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure deterministic rank-graph layout used by the textured maze screen. */
public record RankMazeLayout(List<NodeBox> nodes, List<Edge> edges, int width, int height) {
	private static final int NODE_WIDTH = 42;
	private static final int NODE_HEIGHT = 10;
	private static final int MARGIN = 12;

	public RankMazeLayout {
		nodes = List.copyOf(nodes);
		edges = List.copyOf(edges);
	}

	public static RankMazeLayout arrange(RankGraph graph, int width, int height) {
		int safeWidth = Math.max(120, width);
		int safeHeight = Math.max(120, height);
		Map<Integer, List<RankNode>> bands = new HashMap<>();
		for (RankNode node : graph.nodes()) bands.computeIfAbsent(node.depth(), ignored -> new ArrayList<>()).add(node);
		List<NodeBox> boxes = new ArrayList<>();
		Map<String, NodeBox> byId = new HashMap<>();
		int maxDepth = bands.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
		for (int depth = 0; depth <= maxDepth; depth++) {
			List<RankNode> band = bands.getOrDefault(depth, List.of()).stream()
					.sorted(Comparator.comparing(RankNode::branch).thenComparing(RankNode::id)).toList();
			int y = MARGIN + depth * Math.max(1,
					(safeHeight - MARGIN * 2 - NODE_HEIGHT) / Math.max(1, maxDepth));
			int spacing = (safeWidth - MARGIN * 2) / Math.max(1, band.size());
			for (int index = 0; index < band.size(); index++) {
				RankNode node = band.get(index);
				int x = MARGIN + index * spacing + (spacing - NODE_WIDTH) / 2;
				NodeBox box = new NodeBox(node.id(), Math.max(MARGIN, x), y, NODE_WIDTH, NODE_HEIGHT);
				boxes.add(box);
				byId.put(node.id(), box);
			}
		}
		List<Edge> edges = new ArrayList<>();
		for (RankNode node : graph.nodes()) {
			for (String parent : node.parents()) edges.add(new Edge(parent, node.id()));
		}
		return new RankMazeLayout(boxes, edges, safeWidth, safeHeight);
	}

	public record NodeBox(String id, int x, int y, int width, int height) {
		public boolean overlaps(NodeBox other) {
			return x < other.x + other.width && x + width > other.x
					&& y < other.y + other.height && y + height > other.y;
		}
		public int centerX() { return x + width / 2; }
		public int centerY() { return y + height / 2; }
	}

	public record Edge(String parent, String child) {
	}
}
