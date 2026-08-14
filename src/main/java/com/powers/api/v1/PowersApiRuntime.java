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
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.protection.PowerProtectionAdapters;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Production server-epoch owner behind {@link PowersApiV1}; extension code should use the interface. */
public final class PowersApiRuntime implements PowersApiV1 {
	private record AuthorizedCastContext(ServerPlayer actor, String actionId,
			CastSource source) implements CastContext { }
	private record Candidate(String id, PowersExtension extension) { }
	private static final int MAX_EXTENSIONS = 256;
	private static final PowersApiRuntime GLOBAL = new PowersApiRuntime();
	private final Set<String> extensions = new LinkedHashSet<>();
	private final Set<MagicActionId> actions = new LinkedHashSet<>();
	private final Set<String> protections = new LinkedHashSet<>();
	private final List<LifecycleHook> hooks = new ArrayList<>();
	private final Map<PresenceHandle, MagicPresenceHandle> presences = new LinkedHashMap<>();
	private MinecraftServer server;
	private Thread ownerThread;
	private boolean registrationOpen;

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
		registrationOpen = true;
		List<Candidate> candidates = new ArrayList<>();
		for (PowersExtension extension : discovered == null ? List.<PowersExtension>of() : discovered) {
			if (extension == null) continue;
			try { candidates.add(new Candidate(extension.id(), extension)); }
			catch (RuntimeException | LinkageError failure) {
				PowersMod.LOGGER.error("POWERS API extension identity failed", failure);
			}
		}
		candidates.stream().sorted(Comparator.comparing(Candidate::id,
				Comparator.nullsLast(Comparator.naturalOrder()))).limit(MAX_EXTENSIONS)
				.forEach(candidate -> load(candidate.id(), candidate.extension()));
		registrationOpen = false;
		emit(ApiLifecycleEvent.SERVER_STARTED);
	}

	private void load(String id, PowersExtension extension) {
		if (id == null || !id.matches("[a-z0-9_.-]{1,64}") || !extensions.add(id)) return;
		Set<MagicActionId> priorActions = Set.copyOf(actions);
		Set<String> priorProtections = Set.copyOf(protections);
		int priorHooks = hooks.size();
		try { extension.register(this); }
		catch (RuntimeException | LinkageError failure) {
			PowersMod.LOGGER.error("POWERS API extension {} rejected", id, failure);
			rollback(priorActions, priorProtections, priorHooks);
			extensions.remove(id);
		}
	}

	private void rollback(Set<MagicActionId> priorActions, Set<String> priorProtections, int priorHooks) {
		for (MagicActionId id : Set.copyOf(actions)) if (!priorActions.contains(id)) {
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
		actions.add(id);
		return RegistrationResult.ACCEPTED;
	}

	@Override public RegistrationResult registerProtectionService(ProtectionService service) {
		checkThread();
		if (!registrationOpen) return RegistrationResult.LATE;
		if (service == null) return RegistrationResult.INVALID;
		if (!service.id().matches("[a-z0-9_.-]{1,64}")) return RegistrationResult.INVALID;
		boolean accepted = PowerProtectionAdapters.register(service.id(), service.priority(), query ->
				service.decision().allows(new ProtectionRequest(query.action().name(), query.level(),
						query.position(), query.actor(), query.target())));
		if (!accepted) return RegistrationResult.DUPLICATE;
		protections.add(service.id());
		return RegistrationResult.ACCEPTED;
	}

	@Override public RegistrationResult registerLifecycleHook(LifecycleHook hook) {
		checkThread();
		if (!registrationOpen) return RegistrationResult.LATE;
		if (hook == null) return RegistrationResult.INVALID;
		hooks.add(hook);
		return RegistrationResult.ACCEPTED;
	}

	@Override public CastContext castContext(ServerPlayer actor, String registeredActionId) {
		checkThread();
		if (actor == null || actor.level().getServer() != server || registeredActionId == null
				|| !actions.contains(new MagicActionId(registeredActionId))) {
			throw new IllegalArgumentException("Cast context requires a live actor and registered extension action");
		}
		return new AuthorizedCastContext(actor, registeredActionId, CastSource.EXTENSION);
	}

	@Override public PresenceHandle registerPresence(PhysicalPresence presence) {
		checkThread();
		MagicActionId action = presence == null ? null : new MagicActionId(presence.actionId());
		if (registrationOpen || presence == null || presence.level() == null
				|| presence.level().getServer() != server || !actions.contains(action)) {
			throw new IllegalStateException("Presence requires a started server and registered action");
		}
		MagicPresenceHandle internal = PhysicalMagicPresences.registerFixed(
				action, presence.owner(), presence.level(),
				new Vec3(presence.x(), presence.y(), presence.z()), presence.radius(), presence.expiresAt(),
				MagicPresenceHandle.Kind.valueOf(presence.kind().name()));
		PresenceHandle handle = new PresenceHandle(internal.presenceId().value());
		presences.put(handle, internal);
		return handle;
	}

	@Override public boolean removePresence(PresenceHandle handle) {
		checkThread();
		MagicPresenceHandle internal = presences.remove(handle);
		if (internal == null) return false;
		PhysicalMagicPresences.remove(internal);
		return true;
	}

	/** Emits stop hooks, removes all external state, and closes the server epoch. */
	public void stopServer() {
		if (ownerThread == null) return;
		checkThread();
		emit(ApiLifecycleEvent.SERVER_STOPPING);
		for (MagicPresenceHandle handle : presences.values()) PhysicalMagicPresences.remove(handle);
		for (String id : protections) PowerProtectionAdapters.unregister(id);
		for (MagicActionId id : actions) MagicRuntime.catalogue().unregisterExternal(id);
		presences.clear(); protections.clear(); actions.clear(); hooks.clear(); extensions.clear();
		registrationOpen = false; server = null; ownerThread = null;
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

	List<String> extensionIds() { return List.copyOf(extensions); }
}
