package com.powers.api.v1;

import com.powers.PowersMod;
import com.powers.magic.ActionTargetContract;
import com.powers.magic.MagicActionDefinition;
import com.powers.magic.MagicActionId;
import com.powers.magic.MagicAspect;
import com.powers.magic.MagicDelivery;
import com.powers.magic.MagicIntent;
import com.powers.magic.MagicOrigin;
import com.powers.magic.MagicSignificance;
import com.powers.magic.MagicSignature;
import com.powers.magic.runtime.MagicPresenceHandle;
import com.powers.magic.runtime.MagicCastContext;
import com.powers.magic.runtime.PresenceAnchor;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.magic.InteractionContext;
import com.powers.player.PlayerPowers;
import com.powers.power.MagicUseGate;
import com.powers.protection.PowerProtection;
import com.powers.protection.PowerProtectionAdapters;
import com.powers.testing.TestingOverrides;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Production server-epoch owner behind {@link PowersApiV1}; extension code should use the interface. */
public final class PowersApiRuntime implements PowersApiV1 {
	private static final class AuthorizedCastContext implements CastContext {
		private final PowersApiRuntime runtime;
		private final ServerPlayer actor;
		private final String actionId;
		private final String extensionId;
		private final long epoch;
		private boolean consumed;

		private AuthorizedCastContext(PowersApiRuntime runtime, ServerPlayer actor, String actionId,
				String extensionId, long epoch) {
			this.runtime = runtime; this.actor = actor; this.actionId = actionId;
			this.extensionId = extensionId; this.epoch = epoch;
		}

		@Override public ServerPlayer actor() { return actor; }
		@Override public String actionId() { return actionId; }
		@Override public CastSource source() { return CastSource.EXTENSION; }
	}
	private record Candidate(String id, PowersExtension extension) { }
	private record OwnedPresence(String extensionId, MagicPresenceHandle handle, long expiresAt) { }
	private static final int MAX_EXTENSIONS = 256;
	private static final int MAX_ACTIONS_PER_EXTENSION = 64;
	private static final int MAX_ACTIONS_PER_EPOCH = 512;
	private static final int MAX_PROTECTIONS_PER_EXTENSION = 16;
	private static final int MAX_PROTECTIONS_PER_EPOCH = 256;
	private static final int MAX_HOOKS_PER_EXTENSION = 16;
	private static final int MAX_HOOKS_PER_EPOCH = 256;
	private static final int MAX_PRESENCES_PER_EXTENSION = 128;
	private static final int MAX_PRESENCES_PER_EPOCH = 1_024;
	private static final int MAX_PRESENCE_WORK_PER_TICK = 64;
	private static final int MAX_PRESENCE_WORK_PER_PLAYER_TICK = 4;
	private static final PowersApiRuntime GLOBAL = new PowersApiRuntime();
	private final Set<String> extensions = new LinkedHashSet<>();
	private final Map<MagicActionId, String> actions = new LinkedHashMap<>();
	private final Set<String> protections = new LinkedHashSet<>();
	private final List<LifecycleHook> hooks = new ArrayList<>();
	private final Map<PresenceHandle, OwnedPresence> presences = new LinkedHashMap<>();
	private final Map<String, Integer> presenceCounts = new LinkedHashMap<>();
	private final TreeMap<Long, Set<PresenceHandle>> presenceExpiries = new TreeMap<>();
	private final Map<java.util.UUID, Integer> presenceWork = new LinkedHashMap<>();
	private MinecraftServer server;
	private Thread ownerThread;
	private boolean registrationOpen;
	private boolean started;
	private long epoch;
	private long presenceWorkTick = Long.MIN_VALUE;
	private int presenceWorkTotal;
	private String activeExtension;
	private int activeActionCount;
	private int activeProtectionCount;
	private int activeHookCount;
	private boolean activeLimitExceeded;

	PowersApiRuntime() { }

	public static PowersApiRuntime global() { return GLOBAL; }
	public PowersApiV1 api() { return this; }

	/** Discovers Fabric entrypoints in stable identity order and opens one registration epoch. */
	public void startServer(MinecraftServer server) {
		startServer(server, FabricLoader.getInstance().getEntrypoints("powers:v1", PowersExtension.class));
	}

	/** Starts an epoch from an explicit extension collection; used by deterministic compatibility tests. */
	void startServer(MinecraftServer server, List<PowersExtension> discovered) {
		if (ownerThread != null) stopServer();
		this.server = server;
		ownerThread = Thread.currentThread();
		epoch++;
		started = false;
		registrationOpen = true;
		List<Candidate> candidates = new ArrayList<>();
		int inspected = 0;
		for (PowersExtension extension : discovered == null ? List.<PowersExtension>of() : discovered) {
			if (inspected++ >= MAX_EXTENSIONS) break;
			if (extension == null) continue;
			try { candidates.add(new Candidate(extension.id(), extension)); }
			catch (RuntimeException | LinkageError failure) {
				PowersMod.LOGGER.error("POWERS API extension identity failed", failure);
			}
		}
		candidates.stream().sorted(Comparator.comparing(Candidate::id,
				Comparator.nullsLast(Comparator.naturalOrder())))
				.forEach(candidate -> load(candidate.id(), candidate.extension()));
		registrationOpen = false;
	}

	/** Emits hooks only from Fabric's actual SERVER_STARTED boundary. */
	public void serverStarted(MinecraftServer server) {
		checkThread();
		if (ownerThread == null || this.server != server || started) return;
		started = true;
		emit(ApiLifecycleEvent.SERVER_STARTED);
	}

	private void load(String id, PowersExtension extension) {
		if (id == null || !id.matches("[a-z0-9_.-]{1,64}") || !extensions.add(id)) return;
		Set<MagicActionId> priorActions = Set.copyOf(actions.keySet());
		Set<String> priorProtections = Set.copyOf(protections);
		int priorHooks = hooks.size();
		activeExtension = id;
		activeActionCount = 0; activeProtectionCount = 0; activeHookCount = 0;
		activeLimitExceeded = false;
		try {
			extension.register(this);
			if (activeLimitExceeded) throw new RegistrationLimitException();
		}
		catch (RuntimeException | LinkageError failure) {
			if (!(failure instanceof RegistrationLimitException)) {
				PowersMod.LOGGER.error("POWERS API extension {} rejected", id, failure);
			}
			rollback(priorActions, priorProtections, priorHooks);
			extensions.remove(id);
		} finally {
			activeExtension = null;
		}
	}

	private static final class RegistrationLimitException extends RuntimeException { }

	private void rollback(Set<MagicActionId> priorActions, Set<String> priorProtections, int priorHooks) {
		for (MagicActionId id : Set.copyOf(actions.keySet())) if (!priorActions.contains(id)) {
			MagicRuntime.catalogue().unregisterExternal(id); actions.remove(id);
		}
		for (String id : Set.copyOf(protections)) if (!priorProtections.contains(id)) {
			PowerProtectionAdapters.unregister(id); protections.remove(id);
		}
		while (hooks.size() > priorHooks) hooks.removeLast();
	}

	@Override public RegistrationResult registerAction(ActionRegistration action) {
		checkThread();
		if (!registrationOpen) return RegistrationResult.LATE;
		if (action == null) return RegistrationResult.INVALID;
		if (activeActionCount >= MAX_ACTIONS_PER_EXTENSION || actions.size() >= MAX_ACTIONS_PER_EPOCH) {
			activeLimitExceeded = true;
			return RegistrationResult.LIMIT;
		}
		MagicActionId id = new MagicActionId(action.id());
		var definition = new MagicActionDefinition(id, MagicOrigin.EXTENSION,
				action.aspects().stream().map(value -> MagicAspect.valueOf(value.name()))
						.collect(java.util.stream.Collectors.toUnmodifiableSet()),
				MagicDelivery.valueOf(action.delivery().name()), MagicIntent.valueOf(action.intent().name()),
				action.potency(), action.range(), action.durationTicks(), action.energyCost(),
				action.cooldownTicks(), action.residueTicks(), action.priority(),
				new MagicSignature(action.primaryRgb(), action.primaryRgb(), action.id().hashCode(),
						"extension_" + action.id(), "extension_magic"),
				MagicSignificance.STANDARD, true, ActionTargetContract.ANY_LIVING);
		if (!MagicRuntime.catalogue().registerExternal(definition)) return RegistrationResult.DUPLICATE;
		actions.put(id, activeExtension);
		activeActionCount++;
		return RegistrationResult.ACCEPTED;
	}

	@Override public RegistrationResult registerProtectionService(ProtectionService service) {
		checkThread();
		if (!registrationOpen) return RegistrationResult.LATE;
		if (service == null) return RegistrationResult.INVALID;
		if (!service.id().matches("[a-z0-9_.-]{1,64}")) return RegistrationResult.INVALID;
		if (activeProtectionCount >= MAX_PROTECTIONS_PER_EXTENSION
				|| protections.size() >= MAX_PROTECTIONS_PER_EPOCH) {
			activeLimitExceeded = true;
			return RegistrationResult.LIMIT;
		}
		boolean accepted = PowerProtectionAdapters.register(service.id(), service.priority(), query ->
				service.decision().allows(new ProtectionRequest(query.action().name(), query.level(),
						query.position(), query.actor(), query.target())));
		if (!accepted) return RegistrationResult.DUPLICATE;
		protections.add(service.id());
		activeProtectionCount++;
		return RegistrationResult.ACCEPTED;
	}

	@Override public RegistrationResult registerLifecycleHook(LifecycleHook hook) {
		checkThread();
		if (!registrationOpen) return RegistrationResult.LATE;
		if (hook == null) return RegistrationResult.INVALID;
		if (activeHookCount >= MAX_HOOKS_PER_EXTENSION || hooks.size() >= MAX_HOOKS_PER_EPOCH) {
			activeLimitExceeded = true;
			return RegistrationResult.LIMIT;
		}
		hooks.add(hook);
		activeHookCount++;
		return RegistrationResult.ACCEPTED;
	}

	@Override public CastContext castContext(ServerPlayer actor, String registeredActionId) {
		checkThread();
		MagicActionId action = registeredActionId == null ? null : new MagicActionId(registeredActionId);
		if (!started || !isLiveActor(actor) || action == null || !actions.containsKey(action)) {
			throw new IllegalArgumentException("Cast context requires a live actor and registered extension action");
		}
		return new AuthorizedCastContext(this, actor, registeredActionId, actions.get(action), epoch);
	}

	@Override public PresenceHandle registerPresence(CastContext context, PhysicalPresence presence) {
		checkThread();
		if (!(context instanceof AuthorizedCastContext authorized) || authorized.runtime != this
				|| authorized.epoch != epoch || authorized.consumed || !started
				|| !isLiveActor(authorized.actor)) {
			throw new IllegalArgumentException("Presence requires fresh server-authored cast authority");
		}
		authorized.consumed = true;
		MagicActionId action = new MagicActionId(authorized.actionId);
		MagicActionDefinition definition = MagicRuntime.catalogue().definition(action);
		if (presence == null || definition == null || !authorized.extensionId.equals(actions.get(action))
				|| presence.level() == null || presence.level() != authorized.actor.level()
				|| presence.level().getServer() != server) {
			throw new IllegalStateException("Presence does not match its authoritative cast");
		}
		Vec3 point = new Vec3(presence.x(), presence.y(), presence.z());
		long now = server.getTickCount();
		long maxLifetime = Math.max(1, Math.max(definition.baseDurationTicks(), definition.residueTicks()));
		double maxRange = definition.baseRange();
		if (presence.radius() > maxRange || authorized.actor.position().distanceToSqr(point) > maxRange * maxRange
				|| presence.expiresAt() <= now || presence.expiresAt() - now > maxLifetime) {
			throw new IllegalArgumentException("Presence exceeds registered action bounds");
		}
		if (authorized.actor.isSpectator() || !MagicUseGate.passes(authorized.actor, true, authorized.actionId)
				|| PowerProtection.isSafeZone(presence.level(), point)
				|| !PowerProtection.mayRitual(authorized.actor, presence.level(),
						net.minecraft.core.BlockPos.containing(point))) {
			throw new IllegalStateException("Presence denied by authoritative policy");
		}
		pruneExpiredPresences(now);
		if (presences.size() >= MAX_PRESENCES_PER_EPOCH
				|| presenceCounts.getOrDefault(authorized.extensionId, 0) >= MAX_PRESENCES_PER_EXTENSION) {
			throw new IllegalStateException("Presence work limit reached");
		}
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(authorized.actor);
		String cooldownKey = "powers_api:" + authorized.actionId;
		long previousCooldown = data.cooldownReadyAt(cooldownKey);
		if (!TestingOverrides.cooldownsDisabled(authorized.actor.getUUID()) && previousCooldown > now) {
			throw new IllegalStateException("Presence action is on cooldown");
		}
		if (!claimPresenceWork(authorized.actor.getUUID(), now)) {
			throw new IllegalStateException("Presence work limit reached");
		}
		MagicCastContext cast = new MagicCastContext(definition, authorized.actor.getUUID(),
				presence.level().dimension().identifier().toString(),
				PresenceAnchor.fixed(point.x, point.y, point.z), Math.max(1.0, presence.radius()), now,
				InteractionContext.DEFAULT);
		if (!MagicRuntime.global().previewCast(cast).adjustment().allowed()) {
			throw new IllegalStateException("Presence blocked by canonical magic collision policy");
		}
		boolean paid = TestingOverrides.energyDisabled(authorized.actor.getUUID())
				|| data.consumeEnergy(definition.baseEnergy());
		if (!paid) throw new IllegalStateException("Insufficient authoritative energy");
		MagicPresenceHandle internal;
		try {
			internal = PhysicalMagicPresences.registerFixed(action, authorized.actor.getUUID(), presence.level(),
					point, presence.radius(), presence.expiresAt(),
					MagicPresenceHandle.Kind.valueOf(presence.kind().name()));
		} catch (RuntimeException | LinkageError failure) {
			if (!TestingOverrides.energyDisabled(authorized.actor.getUUID())) {
				data.refundEnergy(definition.baseEnergy());
			}
			throw failure;
		}
		if (!TestingOverrides.cooldownsDisabled(authorized.actor.getUUID())) {
			data.setCooldown(cooldownKey, now + definition.baseCooldownTicks());
		}
		PresenceHandle handle = new PresenceHandle(internal.presenceId().value());
		presences.put(handle, new OwnedPresence(authorized.extensionId, internal, presence.expiresAt()));
		presenceCounts.merge(authorized.extensionId, 1, Integer::sum);
		presenceExpiries.computeIfAbsent(presence.expiresAt(), ignored -> new LinkedHashSet<>()).add(handle);
		return handle;
	}

	@Override public boolean removePresence(PresenceHandle handle) {
		checkThread();
		if (server != null) pruneExpiredPresences(server.getTickCount());
		return releasePresence(handle);
	}

	/** Emits stop hooks, removes all external state, and closes the server epoch. */
	public void stopServer() {
		if (ownerThread == null) return;
		checkThread();
		emit(ApiLifecycleEvent.SERVER_STOPPING);
		for (OwnedPresence presence : presences.values()) PhysicalMagicPresences.remove(presence.handle());
		for (String id : protections) PowerProtectionAdapters.unregister(id);
		for (MagicActionId id : actions.keySet()) MagicRuntime.catalogue().unregisterExternal(id);
		presences.clear(); presenceCounts.clear(); presenceExpiries.clear(); presenceWork.clear();
		protections.clear(); actions.clear(); hooks.clear(); extensions.clear();
		registrationOpen = false; started = false; server = null; ownerThread = null;
	}

	private void emit(ApiLifecycleEvent event) {
		for (LifecycleHook hook : List.copyOf(hooks)) try { hook.onLifecycle(event); }
		catch (RuntimeException | LinkageError failure) {
			PowersMod.LOGGER.error("POWERS API lifecycle hook failed during {}", event, failure);
		}
	}

	private void checkThread() {
		if (ownerThread != null && (Thread.currentThread() != ownerThread
				|| server != null && !server.isSameThread())) {
			throw new IllegalStateException("POWERS API mutation must run on the bound server thread");
		}
	}

	private boolean isLiveActor(ServerPlayer actor) {
		return actor != null && server != null && actor.level().getServer() == server
				&& server.getPlayerList().getPlayer(actor.getUUID()) == actor
				&& actor.connection != null && !actor.hasDisconnected()
				&& actor.isAlive() && !actor.isRemoved();
	}

	private boolean claimPresenceWork(java.util.UUID actor, long tick) {
		if (presenceWorkTick != tick) {
			presenceWorkTick = tick;
			presenceWorkTotal = 0;
			presenceWork.clear();
		}
		int actorWork = presenceWork.getOrDefault(actor, 0);
		if (presenceWorkTotal >= MAX_PRESENCE_WORK_PER_TICK
				|| actorWork >= MAX_PRESENCE_WORK_PER_PLAYER_TICK) return false;
		presenceWorkTotal++;
		presenceWork.put(actor, actorWork + 1);
		return true;
	}

	/** Reclaims only elapsed active buckets; work is bounded by the live-presence caps, not history. */
	private void pruneExpiredPresences(long now) {
		for (PresenceHandle handle : presenceExpiries.headMap(now, true).values().stream()
				.flatMap(Collection::stream).toList()) {
			OwnedPresence owned = presences.get(handle);
			if (owned != null && owned.expiresAt() <= now) releasePresence(handle);
		}
		presenceExpiries.headMap(now, true).clear();
	}

	private boolean releasePresence(PresenceHandle handle) {
		OwnedPresence owned = presences.remove(handle);
		if (owned == null) return false;
		Set<PresenceHandle> bucket = presenceExpiries.get(owned.expiresAt());
		if (bucket != null) {
			bucket.remove(handle);
			if (bucket.isEmpty()) presenceExpiries.remove(owned.expiresAt());
		}
		presenceCounts.computeIfPresent(owned.extensionId(),
				(ignored, count) -> count <= 1 ? null : count - 1);
		PhysicalMagicPresences.remove(owned.handle());
		return true;
	}

	List<String> extensionIds() { return List.copyOf(extensions); }
}
