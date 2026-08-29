package com.powers.power.state;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/** Synchronous, atomic, read-back-verified persistence for clock ownership authority. */
final class TimeStopJournalStore {
	enum WriteBoundary {
		AFTER_RENAME,
		AFTER_DIRECTORY_SYNC,
		AFTER_READBACK
	}

	@FunctionalInterface
	interface FaultInjector {
		void reach(WriteBoundary boundary) throws IOException;
	}

	private final Path target;
	private final FaultInjector faultInjector;

	TimeStopJournalStore(Path target) {
		this(target, ignored -> { });
	}

	TimeStopJournalStore(Path target, FaultInjector faultInjector) {
		this.target = target.toAbsolutePath().normalize();
		this.faultInjector = faultInjector;
	}

	static TimeStopJournalStore forServer(MinecraftServer server) {
		Path dataRoot = server.getWorldPath(LevelResource.DATA).toAbsolutePath().normalize();
		Path target = TimeStopSavedData.TYPE.id().withSuffix(".dat").resolveAgainst(dataRoot)
				.toAbsolutePath().normalize();
		if (!target.startsWith(dataRoot)) {
			throw new IllegalStateException("Time Stop journal escaped the world data directory");
		}
		return new TimeStopJournalStore(target);
	}

	TimeStopSavedData.Snapshot read() throws IOException {
		if (!Files.isRegularFile(target)) return TimeStopSavedData.emptySnapshot();
		try {
			CompoundTag root = NbtIo.readCompressed(target, NbtAccounter.unlimitedHeap());
			Tag encoded = root.get("data");
			if (encoded == null) throw new IOException("Time Stop journal has no data payload");
			DataResult<TimeStopSavedData> decoded = TimeStopSavedData.CODEC.parse(
					NbtOps.INSTANCE, encoded);
			return decoded.getOrThrow().snapshot();
		} catch (RuntimeException failure) {
			throw new IOException("Time Stop journal could not be decoded", failure);
		}
	}

	void writeVerified(TimeStopSavedData.Snapshot snapshot) throws IOException {
		Path parent = target.getParent();
		if (parent == null) throw new IOException("Time Stop journal has no parent directory");
		Files.createDirectories(parent);
		byte[] previous = Files.isRegularFile(target) ? Files.readAllBytes(target) : null;
		Path temporary = Files.createTempFile(parent, ".time-stop-ownership-", ".tmp");
		boolean installed = false;
		try {
			Files.write(temporary, encode(snapshot), StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.WRITE, StandardOpenOption.SYNC);
			try (FileChannel file = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
				file.force(true);
			}
			Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
					StandardCopyOption.REPLACE_EXISTING);
			installed = true;
			faultInjector.reach(WriteBoundary.AFTER_RENAME);
			syncDirectory(parent);
			faultInjector.reach(WriteBoundary.AFTER_DIRECTORY_SYNC);
			if (!snapshot.equals(read())) {
				throw new IOException("Time Stop journal read-back did not match the requested state");
			}
			faultInjector.reach(WriteBoundary.AFTER_READBACK);
		} catch (IOException | RuntimeException failure) {
			if (installed) {
				try {
					restore(parent, previous);
				} catch (IOException rollbackFailure) {
					failure.addSuppressed(rollbackFailure);
				}
			}
			throw failure;
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private void restore(Path parent, byte[] previous) throws IOException {
		if (previous == null) {
			Files.deleteIfExists(target);
			syncDirectory(parent);
			if (Files.exists(target)) throw new IOException("Time Stop journal rollback left a target");
			return;
		}
		Path rollback = Files.createTempFile(parent, ".time-stop-rollback-", ".tmp");
		try {
			Files.write(rollback, previous, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.WRITE, StandardOpenOption.SYNC);
			Files.move(rollback, target, StandardCopyOption.ATOMIC_MOVE,
					StandardCopyOption.REPLACE_EXISTING);
			syncDirectory(parent);
			if (!Arrays.equals(previous, Files.readAllBytes(target))) {
				throw new IOException("Time Stop journal rollback did not restore prior bytes");
			}
		} finally {
			Files.deleteIfExists(rollback);
		}
	}

	private static void syncDirectory(Path parent) throws IOException {
		try (FileChannel directory = FileChannel.open(parent, StandardOpenOption.READ)) {
			directory.force(true);
		}
	}

	private static byte[] encode(TimeStopSavedData.Snapshot snapshot) throws IOException {
		try {
			Tag encoded = TimeStopSavedData.CODEC.encodeStart(
					NbtOps.INSTANCE, new TimeStopSavedData(snapshot)).getOrThrow();
			CompoundTag root = new CompoundTag();
			root.put("data", encoded);
			NbtUtils.addCurrentDataVersion(root);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			NbtIo.writeCompressed(root, output);
			return output.toByteArray();
		} catch (RuntimeException failure) {
			throw new IOException("Time Stop journal could not be encoded", failure);
		}
	}
}
