package com.turn.metrics.system;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;


/**
 * A set of gauges for basic OS metrics as exposed by {@link OperatingSystemMXBean} and its Sun-specific sub interfaces.
 *
 * The Sun-specific {@code com.sun.management} versions of the interface provide more information, but might not be
 * available on all systems. The extra information from these versions are fetched opportunistically by calling the
 * functions via reflection. This means we don't have to import an interface that might not be available (the import
 * would then throw).
 *
 * @see java.lang.management.OperatingSystemMXBean
 * @see com.sun.management.OperatingSystemMXBean
 * @see com.sun.management.UnixOperatingSystemMXBean
 *
 * @author Adam Lugowski
 */
@SuppressWarnings("unused")
public class OperatingSystemMetricSet implements MetricSet {

	private final OperatingSystemMXBean os;

	public OperatingSystemMetricSet() {
		os = ManagementFactory.getOperatingSystemMXBean();
	}

	@Override
	public Map<String, Metric> getMetrics() {
		final Map<String, Metric> gauges = new HashMap<>();

		gauges.put("load.average", new Gauge<Double>() {
			@Override
			public Double getValue() {
				return os.getSystemLoadAverage();
			}
		});

		gauges.put("cpu.num_available", new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return os.getAvailableProcessors();
			}
		});

		// If available, add metrics from com.sun.management.OperatingSystemMXBean
		addDoubleIfExists(gauges, "cpu.usage", "getSystemCpuLoad");
		addDoubleIfExists(gauges, "cpu.process.usage", "getProcessCpuLoad");

		addLongIfExists(gauges, "cpu.process.ns", "getProcessCpuTime()");

		addLongIfExists(gauges, "mem.committed", "getCommittedVirtualMemorySize");
		addLongIfExists(gauges, "mem.free", "getFreePhysicalMemorySize");
		addLongIfExists(gauges, "mem.size", "getTotalPhysicalMemorySize");

		addLongIfExists(gauges, "swap.free", "getFreeSwapSpaceSize");
		addLongIfExists(gauges, "swap.size", "getTotalSwapSpaceSize");

		// If available, add metrics from com.sun.management.UnixOperatingSystemMXBean
		addLongIfExists(gauges, "file.descriptors.max", "getMaxFileDescriptorCount");
		addLongIfExists(gauges, "file.descriptors.open", "getOpenFileDescriptorCount");

		return Collections.unmodifiableMap(gauges);
	}

	/**
	 * Add a gauge to the metric set based on a reflection-based method call, if {@code this.os} has a method with that
	 * name. Assumes return value is {@code long}.
	 *
	 * @param gauges          Metric set.
	 * @param metricName      Name of metric to add gauge under.
	 * @param beanFuncName    Name of method to make gauge around, if {@code os} has it.
	 */
	private void addLongIfExists(Map<String, Metric> gauges, final String metricName, final String beanFuncName) {
		long val = invokeLong(beanFuncName, -1);
		if (val != -1) {
			// method appears to exist, add gauge
			gauges.put(metricName, new Gauge<Long>() {
				@Override
				public Long getValue() {
					return invokeLong(beanFuncName, -1);
				}
			});
		}
	}

	/**
	 * Add a gauge to the metric set based on a reflection-based method call, if {@code this.os} has a method with that
	 * name. Assumes return value is {@code double}.
	 *
	 * @param gauges          Metric set.
	 * @param metricName      Name of metric to add gauge under.
	 * @param beanFuncName    Name of method to make gauge around, if {@code os} has it.
	 */
	private void addDoubleIfExists(Map<String, Metric> gauges, final String metricName, final String beanFuncName) {
		double val = invokeDouble(beanFuncName, -1);
		if (val != -1) {
			// method appears to exist, add gauge
			gauges.put(metricName, new Gauge<Double>() {
				@Override
				public Double getValue() {
					return invokeDouble(beanFuncName, -1.);
				}
			});
		}
	}

	/**
	 * Invoke a method that returns {@code long} on {@code this.os} via reflection.
	 *
	 * @param name   name of method
	 * @param dflt   default value to return in case of error
	 * @return       value of {@code os.name()} or {@code dflt} on error.
	 */
	private long invokeLong(String name, long dflt) {
		try {
			final Method method = os.getClass().getDeclaredMethod(name);
			method.setAccessible(true);
			return (Long) method.invoke(os);
		} catch (NoSuchMethodException e) {
			return dflt;
		} catch (IllegalAccessException e) {
			return dflt;
		} catch (InvocationTargetException e) {
			return dflt;
		}
	}

	/**
	 * Invoke a method that returns {@code double} on {@code this.os} via reflection.
	 *
	 * @param name   name of method
	 * @param dflt   default value to return in case of error
	 * @return       value of {@code os.name()} or {@code dflt} on error.
	 */
	private double invokeDouble(String name, double dflt) {
		try {
			final Method method = os.getClass().getDeclaredMethod(name);
			method.setAccessible(true);
			return (Double) method.invoke(os);
		} catch (NoSuchMethodException e) {
			return dflt;
		} catch (IllegalAccessException e) {
			return dflt;
		} catch (InvocationTargetException e) {
			return dflt;
		}
	}
}
