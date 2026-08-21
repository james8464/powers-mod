package com.powers.fx;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Owns bounded nested round-robin queues across dimension, policy, owner, and impact. */
public final class VisualScarRequestQueue {
	private final int globalLimit;
	private final int ownerLimit;
	private final RingMap<String, PolicyLevel> dimensions = new RingMap<>();
	private final Map<UUID, Integer> ownerSizes = new HashMap<>();
	private int size;
	private int lastPollWork;

	public VisualScarRequestQueue(int globalLimit, int ownerLimit) {
		if (globalLimit < 1 || globalLimit > 2_048 || ownerLimit < 1 || ownerLimit > 128) {
			throw new IllegalArgumentException("request queue limits exceed hard ceilings");
		}
		this.globalLimit = globalLimit;
		this.ownerLimit = ownerLimit;
	}

	/** Adds one request into its exact hierarchy without allowing owner/accounting overflow. */
	public boolean offer(VisualScarLedgerRules.Request request) {
		Objects.requireNonNull(request, "request");
		if (size >= globalLimit || ownerSizes.getOrDefault(request.owner(), 0) >= ownerLimit) {
			return false;
		}
		PolicyLevel dimension = dimensions.getOrCreate(request.dimension(), PolicyLevel::new);
		OwnerLevel policy = dimension.policies.getOrCreate(request.providerPolicyId(), OwnerLevel::new);
		ImpactLevel owner = policy.owners.getOrCreate(request.owner(), ImpactLevel::new);
		ArrayDeque<VisualScarLedgerRules.Request> impact = owner.impacts.getOrCreate(
				request.impact(), ArrayDeque::new);
		impact.addLast(request);
		ownerSizes.merge(request.owner(), 1, Integer::sum);
		size++;
		return true;
	}

	/** Polls at most 64 requests with stable fairness at every hierarchy level. */
	public List<VisualScarLedgerRules.Request> poll(int maximum) {
		if (maximum < 0 || maximum > 64) throw new IllegalArgumentException("invalid request work bound");
		var result = new java.util.ArrayList<VisualScarLedgerRules.Request>(maximum);
		lastPollWork = 0;
		while (result.size() < maximum && !dimensions.isEmpty()) {
			RingMap.Node<String, PolicyLevel> dimensionNode = dimensions.next();
			RingMap.Node<Long, OwnerLevel> policyNode = dimensionNode.value.policies.next();
			RingMap.Node<UUID, ImpactLevel> ownerNode = policyNode.value.owners.next();
			RingMap.Node<VisualScarRules.Impact, ArrayDeque<VisualScarLedgerRules.Request>> impactNode =
					ownerNode.value.impacts.next();
			VisualScarLedgerRules.Request request = impactNode.value.removeFirst();
			result.add(request);
			lastPollWork++;
			size--;
			ownerSizes.compute(request.owner(), (owner, count) -> count == 1 ? null : count - 1);
			if (impactNode.value.isEmpty()) ownerNode.value.impacts.remove(impactNode.key);
			if (ownerNode.value.impacts.isEmpty()) policyNode.value.owners.remove(ownerNode.key);
			if (policyNode.value.owners.isEmpty()) dimensionNode.value.policies.remove(policyNode.key);
			if (dimensionNode.value.policies.isEmpty()) dimensions.remove(dimensionNode.key);
		}
		return List.copyOf(result);
	}

	public int lastPollWork() {
		return lastPollWork;
	}

	private static final class PolicyLevel {
		private final RingMap<Long, OwnerLevel> policies = new RingMap<>();
	}

	private static final class OwnerLevel {
		private final RingMap<UUID, ImpactLevel> owners = new RingMap<>();
	}

	private static final class ImpactLevel {
		private final RingMap<VisualScarRules.Impact,
				ArrayDeque<VisualScarLedgerRules.Request>> impacts = new RingMap<>();
	}

	/** Intrusive key ring with one physical node per present hierarchy member. */
	private static final class RingMap<K, V> {
		private final Map<K, Node<K, V>> nodes = new HashMap<>();
		private Node<K, V> cursor;

		private V getOrCreate(K key, Supplier<V> factory) {
			Node<K, V> current = nodes.get(key);
			if (current != null) return current.value;
			Node<K, V> created = new Node<>(key, factory.get());
			if (cursor == null) {
				created.next = created;
				created.previous = created;
				cursor = created;
			} else {
				Node<K, V> tail = cursor.previous;
				tail.next = created;
				created.previous = tail;
				created.next = cursor;
				cursor.previous = created;
			}
			nodes.put(key, created);
			return created.value;
		}

		private Node<K, V> next() {
			if (cursor == null) throw new IllegalStateException("empty fairness ring");
			Node<K, V> result = cursor;
			cursor = cursor.next;
			return result;
		}

		private void remove(K key) {
			Node<K, V> node = nodes.remove(key);
			if (node == null) return;
			if (node.next == node) {
				cursor = null;
			} else {
				node.previous.next = node.next;
				node.next.previous = node.previous;
				if (cursor == node) cursor = node.next;
			}
		}

		private boolean isEmpty() {
			return nodes.isEmpty();
		}

		private static final class Node<K, V> {
			private final K key;
			private final V value;
			private Node<K, V> next;
			private Node<K, V> previous;

			private Node(K key, V value) {
				this.key = key;
				this.value = value;
			}
		}
	}
}
