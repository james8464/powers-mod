package com.powers.companion.combat;

/** One bounded credit window; learning never authorizes or executes an action. */
public final class BoundedCombatLearner {
	public static final double MAX_EXPLORATION = 0.05;
	private final ShadowLearningState state;
	private Credit credit;

	private record Credit(String context, String type, String action, long openedAt,
			double targetHealth, double ownerHealth, double shadowHealth) { }

	public BoundedCombatLearner(ShadowLearningState state) {
		this.state = state;
	}

	public boolean openCredit(String context, String type, String action, long tick,
			double targetHealth, double ownerHealth, double shadowHealth) {
		if (credit != null) return false;
		credit = new Credit(context, type, action, tick, targetHealth, ownerHealth, shadowHealth);
		return true;
	}

	public double completeCredit(long tick, double targetHealth,
			double ownerHealth, double shadowHealth) {
		if (credit == null) return 0.0;
		double targetProgress = credit.targetHealth() - Math.clamp(targetHealth, 0.0, 1.0);
		double ownerCost = Math.max(0.0, credit.ownerHealth() - ownerHealth);
		double shadowCost = Math.max(0.0, credit.shadowHealth() - shadowHealth);
		double reward = Math.clamp(targetProgress * 2.0 - ownerCost - shadowCost * 0.65, -1.0, 1.0);
		state.adjust(credit.context(), credit.type(), credit.action(), reward);
		credit = null;
		return reward;
	}

	public boolean activeCredit() { return credit != null; }

	public static boolean shouldExplore(double sample, boolean unsafe, int loadPressure) {
		return !unsafe && loadPressure <= 0 && Double.isFinite(sample)
				&& sample >= 0.0 && sample < MAX_EXPLORATION;
	}
}
