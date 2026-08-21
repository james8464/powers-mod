package com.powers.fx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Owns one intrusive node per revalidation key with constant-time membership mutation. */
public final class VisualScarRevalidationRing<K> {
	private final int capacity;
	private final Map<K, Node<K>> nodes = new HashMap<>();
	private Node<K> cursor;

	private VisualScarRevalidationRing(int capacity) {
		if (capacity < 1 || capacity > 2_048) {
			throw new IllegalArgumentException("capacity exceeds active-scar bounds");
		}
		this.capacity = capacity;
	}

	/** Creates a bounded ring and rejects duplicate or over-capacity initial membership. */
	public static <K> VisualScarRevalidationRing<K> of(Collection<K> keys, int capacity) {
		Objects.requireNonNull(keys, "keys");
		var ring = new VisualScarRevalidationRing<K>(capacity);
		for (K key : keys) {
			if (!ring.insert(key)) throw new IllegalArgumentException("duplicate or excessive key");
		}
		return ring;
	}

	/** Inserts one absent key at the tail while preserving the current inspection position. */
	public boolean insert(K key) {
		Objects.requireNonNull(key, "key");
		if (nodes.containsKey(key) || nodes.size() >= capacity) return false;
		Node<K> node = new Node<>(key);
		if (cursor == null) {
			node.next = node;
			node.previous = node;
			cursor = node;
		} else {
			Node<K> tail = cursor.previous;
			tail.next = node;
			node.previous = tail;
			node.next = cursor;
			cursor.previous = node;
		}
		nodes.put(key, node);
		return true;
	}

	/** Removes one present key in constant time and advances safely when it is current. */
	public boolean remove(K key) {
		Node<K> node = nodes.remove(key);
		if (node == null) return false;
		if (node.next == node) {
			cursor = null;
		} else {
			node.previous.next = node.next;
			node.next.previous = node.previous;
			if (cursor == node) cursor = node.next;
		}
		node.next = null;
		node.previous = null;
		return true;
	}

	/** Returns at most the requested distinct keys and advances the ring cursor exactly once per key. */
	public List<K> inspectNext(int maximum) {
		if (maximum < 0 || maximum > 64) throw new IllegalArgumentException("invalid revalidation work bound");
		int count = Math.min(maximum, nodes.size());
		List<K> result = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			result.add(cursor.key);
			cursor = cursor.next;
		}
		return List.copyOf(result);
	}

	/** Returns the exact logical membership count. */
	public int membershipSize() {
		return nodes.size();
	}

	/** Returns the exact allocated-node count, which must equal membership. */
	public int physicalNodeCount() {
		return nodes.size();
	}

	/** Verifies all bidirectional links and map identities without changing cursor state. */
	public boolean linksAreExact() {
		if (nodes.isEmpty()) return cursor == null;
		if (cursor == null) return false;
		Node<K> current = cursor;
		for (int count = 0; count < nodes.size(); count++) {
			if (current == null || current.next == null || current.previous == null
					|| current.next.previous != current || current.previous.next != current
					|| nodes.get(current.key) != current) return false;
			current = current.next;
		}
		return current == cursor;
	}

	private static final class Node<K> {
		private final K key;
		private Node<K> next;
		private Node<K> previous;

		private Node(K key) {
			this.key = key;
		}
	}
}
