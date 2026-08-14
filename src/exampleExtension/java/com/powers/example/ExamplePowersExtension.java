package com.powers.example;

import com.powers.api.v1.ActionAspect;
import com.powers.api.v1.ActionDelivery;
import com.powers.api.v1.ActionIntent;
import com.powers.api.v1.ActionRegistration;
import com.powers.api.v1.ApiLifecycleEvent;
import com.powers.api.v1.CastSource;
import com.powers.api.v1.PowersApiV1;
import com.powers.api.v1.PowersExtension;
import com.powers.api.v1.ProtectionService;

import java.util.Set;

/** Independently compiled compatibility example using only the public v1 package. */
public final class ExamplePowersExtension implements PowersExtension {
	public static final String ACTION_ID = "example_resonant_field";
	private static volatile boolean started;

	@Override public String id() { return "powers.example"; }

	@Override public void register(PowersApiV1 api) {
		api.registerAction(new ActionRegistration(ACTION_ID, CastSource.EXTENSION,
				Set.of(ActionAspect.FORCE, ActionAspect.LIGHT), ActionDelivery.FIELD,
				ActionIntent.SUPPORT, 7, 6.0, 40, 10, 20, 40, 12, 0x66CCFF));
		api.registerProtectionService(new ProtectionService("powers.example", 100,
				request -> request.position() == null || request.position().getX() != 13));
		api.registerLifecycleHook(event -> started = event == ApiLifecycleEvent.SERVER_STARTED);
	}

	public static boolean started() { return started; }
}
