package com.powers.mixin;

import com.powers.diagnostics.CrashDiagnosticSection;
import com.powers.diagnostics.CrashReportDiagnostics;
import net.minecraft.SystemReport;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds bounded POWERS aggregates to server crash reports. */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerCrashReportMixin {
	@Inject(method = "fillSystemReport", at = @At("RETURN"))
	private void powers$appendDiagnostics(CallbackInfoReturnable<SystemReport> callback) {
		CrashDiagnosticSection section = CrashReportDiagnostics.capture((MinecraftServer) (Object) this);
		SystemReport report = callback.getReturnValue();
		report.setDetail("POWERS active sessions", section.activeSessions());
		report.setDetail("POWERS last typed failure", section.lastTypedFailure());
	}
}
