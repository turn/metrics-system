package com.turn.metrics.system;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * A set of gauges for basic properties of file systems visible to the JVM.
 * </p>
 * <p>
 * For each FS, gauges for the following properties are reported:
 * </p>
 * <ul>
 * <li>{@code total_bytes} - total number of bytes    </li>
 * <li>{@code used_bytes} - number of bytes used      </li>
 * <li>{@code used_pc} - percentage used              </li>
 * <li>{@code free_bytes} - number of bytes free      </li>
 * <li>{@code free_pc} - percentage free              </li>
 * </ul>
 * <p>Note that filesystem names are highly implementation-specific. Override {@link #getFSName(FileStore)} with an
 * implementation that works on your platform, for example {@link #getFSNameByToStringPath(FileStore)}
 * </p>
 * @author Adam Lugowski
 */
@SuppressWarnings("unused")
public class FileSystemMetricSet implements MetricSet {

	private final FileSystem fileSystems;
	private final boolean skipZeroAvail;

	/**
	 * Get metrics about default filesystems as returned by {@link FileSystems#getDefault()},
	 * skipping those with no usable space.
	 */
	public FileSystemMetricSet() {
		this.fileSystems = FileSystems.getDefault();
		this.skipZeroAvail = true;
	}

	/**
	 *
	 * @param fs                the Filesystem to use
	 * @param skipZeroAvail     Flag about whether or not to include {@link FileStore}s that show no usable space
	 */
	public FileSystemMetricSet(FileSystem fs, boolean skipZeroAvail) {
		this.fileSystems = fs;
		this.skipZeroAvail = skipZeroAvail;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		final Map<String, Metric> gauges = new HashMap<>();

		for (final FileStore store: fileSystems.getFileStores()) {
			try {
				if (skipZeroAvail && store.getUsableSpace() == 0) {
					continue;
				}
			} catch (IOException e) {
				// threw IOException when tried to get available space. FS is probably not visible anymore, so exclude.
				continue;
			}

			String fsName = getFSName(store);

			// Add gauge for total disk space in bytes
			gauges.put("fs." + fsName + ".total_bytes", new Gauge<Long>() {
				@Override
				public Long getValue() {
					try {
						return store.getTotalSpace();
					} catch (IOException e) {
						return -1L;
					}
				}
			});

			// Add gauge for used disk space in bytes
			gauges.put("fs." + fsName + ".used_bytes", new Gauge<Long>() {
				@Override
				public Long getValue() {
					try {
						return store.getTotalSpace() - store.getUnallocatedSpace();
					} catch (IOException e) {
						return -1L;
					}
				}
			});

			// Add gauge for free disk space in bytes
			gauges.put("fs." + fsName + ".free_bytes", new Gauge<Long>() {
				@Override
				public Long getValue() {
					try {
						return store.getUsableSpace();
					} catch (IOException e) {
						return -1L;
					}
				}
			});

			// Add gauge for used disk space percentage
			gauges.put("fs." + fsName + ".used_pc", new Gauge<Double>() {
				@Override
				public Double getValue() {
					try {
						return (1. - ((double)store.getUnallocatedSpace() / (double)store.getTotalSpace())) * 100.;
					} catch (IOException e) {
						return -1.;
					}
				}
			});

			// Add gauge for free disk space percentage
			gauges.put("fs." + fsName + ".free_pc", new Gauge<Double>() {
				@Override
				public Double getValue() {
					try {
						return ((double)store.getUnallocatedSpace() / (double)store.getTotalSpace()) * 100.;
					} catch (IOException e) {
						return -1.;
					}
				}
			});
		}

		return Collections.unmodifiableMap(gauges);
	}

	/**
	 * FileStore names are highly implementation specific. Override this function to provide a naming scheme for
	 * your platform. Some basic potential implementations are provided.
	 *
	 * @param store       the {@link FileStore} to get name of
	 * @return            A name for {@code store} usable in a metric name
	 */
	public String getFSName(FileStore store) {
		return getFSNameByName(store);
	}

	/**
	 * FileStore names are highly implementation specific. This function simply escapes the name returned by name().
	 *
	 * @param store       the {@link FileStore} to get name of
	 * @return            an escaped version of {@link FileStore#name()}
	 */
	public String getFSNameByName(FileStore store) {
		return store.name().replaceAll("[^a-zA-Z0-9_]", "_");
	}

	/**
	 * FileStore names are highly implementation specific. This function attempts to extract a name based on the mount
	 * point. The mount point is inferred from toString. This is done in a best-effort way, as there is no way
	 * to actually get the mount point.
	 * <p>
	 * This function is for systems where {@link FileStore#toString()} returns a string like
	 * {@code / (/dev/disk0s2)} or {@code /mnt/volume1 (/dev/disk0s3)}
	 *</p>
	 * <p>
	 * In other words, Unix-like OSes. The root filesystem, {@code /}, is named "root", all others are named as their
	 * mount point path escaped with underscores.
	 * </p>
	 * @param store       the {@link FileStore} to get name of
	 * @return            A name based on the mount location as extracted from {@link FileStore#toString()}
	 */
	public String getFSNameByToStringPath(FileStore store) {
		String path;
		try {
			// Mount point path is the first word from toString()
			path = store.toString().split(" ", 2)[0];
		} catch (Exception e) {
			path = store.toString();
		}

		if (path.equals("/")) {
			return "root";
		}

		// slice off the leading '/'
		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}

		return path.replaceAll("[^a-zA-Z0-9_]", "_");
	}
}
