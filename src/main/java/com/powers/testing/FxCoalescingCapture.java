package com.powers.testing;

import com.powers.network.FxPacketCoalescer;

import java.util.Locale;
import java.util.Objects;

/** Evaluates one connected-client FX capture against the PERF-005 reduction gate. */
public record FxCoalescingCapture(FxPacketCoalescer.TrafficSnapshot traffic,
		double packetReductionPercent, double byteReductionPercent, boolean passed) {
	private static final double MINIMUM_REDUCTION_PERCENT = 25.0;

	public static FxCoalescingCapture evaluate(FxPacketCoalescer.TrafficSnapshot traffic) {
		Objects.requireNonNull(traffic, "traffic");
		double packetReduction = traffic.packetReductionPercent();
		double byteReduction = traffic.byteReductionPercent();
		return new FxCoalescingCapture(traffic, packetReduction, byteReduction,
				traffic.attemptedPackets() > 0L
						&& packetReduction >= MINIMUM_REDUCTION_PERCENT
						&& byteReduction >= MINIMUM_REDUCTION_PERCENT);
	}

	/** Stable log marker consumed by the real-client evidence harness. */
	public String marker() {
		return String.format(Locale.ROOT,
				"POWERS_FX_CAPTURE passed=%s attemptedPackets=%d deliveredPackets=%d"
						+ " attemptedBytes=%d deliveredBytes=%d packetReduction=%.3f"
						+ " byteReduction=%.3f",
				passed, traffic.attemptedPackets(), traffic.deliveredPackets(),
				traffic.attemptedBytes(), traffic.deliveredBytes(),
				packetReductionPercent, byteReductionPercent);
	}
}
