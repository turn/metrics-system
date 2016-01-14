package com.turn.metrics.system;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of gauges for properties exposed by RuntimeMXBean, mainly start time and uptime.
 *
 * @see java.lang.management.RuntimeMXBean
 *
 * @author Adam Lugowski
 */
@SuppressWarnings("unused")
public class RuntimeMetricSet implements MetricSet {
	private final java.lang.management.RuntimeMXBean runtimeMXBean;

	public RuntimeMetricSet() {
		runtimeMXBean = ManagementFactory.getRuntimeMXBean();
	}

	@Override
	public Map<String, Metric> getMetrics() {
		final Map<String, Metric> gauges = new HashMap<>();

		gauges.put("starttime_ms", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return runtimeMXBean.getStartTime();
			}
		});

		gauges.put("uptime_ms", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return runtimeMXBean.getUptime();
			}
		});

		return Collections.unmodifiableMap(gauges);
	}
}
