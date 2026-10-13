package com.powers.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BloodReadingTrendRulesTest {
	@Test
	void historyIsWindowedBoundedAndAmountOnly() {
		List<BloodReadingTrendRules.Sample> samples = new ArrayList<>();
		samples = BloodReadingTrendRules.record(samples, 1L,
				BloodReadingTrendRules.Kind.DAMAGE, 99.0F);
		for (int index = 0; index < 10; index++) {
			samples = BloodReadingTrendRules.record(samples, 700L + index,
					index % 2 == 0 ? BloodReadingTrendRules.Kind.DAMAGE
							: BloodReadingTrendRules.Kind.HEALING, index + 1.0F);
		}
		assertEquals(8, samples.size());
		assertEquals(0, samples.stream().filter(sample -> sample.tick() == 1L).count());
		var summary = BloodReadingTrendRules.summarize(samples, 710L);
		assertEquals(8, summary.sampleCount());
		assertEquals(24.0F, summary.recentDamage());
		assertEquals(28.0F, summary.recentHealing());
		assertEquals(BloodReadingTrendRules.Trend.VOLATILE, summary.trend());
	}

	@Test
	void trendDistinguishesWorseningRecoveryAndStable() {
		var worsening = List.of(new BloodReadingTrendRules.Sample(50L,
				BloodReadingTrendRules.Kind.DAMAGE, 8.0F));
		var recovering = List.of(new BloodReadingTrendRules.Sample(50L,
				BloodReadingTrendRules.Kind.HEALING, 8.0F));
		assertEquals(BloodReadingTrendRules.Trend.WORSENING,
				BloodReadingTrendRules.summarize(worsening, 50L).trend());
		assertEquals(BloodReadingTrendRules.Trend.RECOVERING,
				BloodReadingTrendRules.summarize(recovering, 50L).trend());
		assertEquals(BloodReadingTrendRules.Trend.STABLE,
				BloodReadingTrendRules.summarize(List.of(), 50L).trend());
	}

	@Test
	void finiteInputsCannotOverflowTheReportedTotals() {
		var samples = List.of(
				new BloodReadingTrendRules.Sample(5L, BloodReadingTrendRules.Kind.DAMAGE, Float.MAX_VALUE),
				new BloodReadingTrendRules.Sample(5L, BloodReadingTrendRules.Kind.DAMAGE, Float.MAX_VALUE));
		assertEquals(Float.MAX_VALUE,
				BloodReadingTrendRules.summarize(samples, 5L).recentDamage());
	}
}
