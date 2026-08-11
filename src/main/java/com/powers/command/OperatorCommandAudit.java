package com.powers.command;

import com.mojang.brigadier.context.CommandContext;
import com.powers.audit.OperatorAudit;
import com.powers.audit.OperatorAuditAction;
import com.powers.audit.OperatorAuditResult;
import com.powers.config.ConfigValidationReport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/** Bridges command sources to structured audits and immediate config validation feedback. */
final class OperatorCommandAudit {
	private OperatorCommandAudit() {
	}

	static void record(CommandSourceStack source, OperatorAuditAction action,
			OperatorAuditResult result, String subject, String detail) {
		OperatorAudit.record(action, result, source.getDisplayName().getString(), subject, detail);
	}

	static void sendConfigReport(CommandContext<CommandSourceStack> context,
			ConfigValidationReport report) {
		context.getSource().sendSuccess(() -> Component.literal(
				"Reloaded POWERS configuration; validation " + report.summary() + "."), true);
		for (String line : report.operatorLines()) {
			context.getSource().sendSuccess(() -> Component.literal("Config adjusted: " + line), false);
		}
	}
}
