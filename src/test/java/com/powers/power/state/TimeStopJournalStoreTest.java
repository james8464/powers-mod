package com.powers.power.state;

import com.powers.time.ControlTick;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeStopJournalStoreTest {
	@TempDir Path temporary;

	@Test
	void atomicJournalAcknowledgesExactActiveAndClearedSnapshots() throws IOException {
		SharedConstants.tryDetectVersion();
		TimeStopJournalStore store = new TimeStopJournalStore(
				temporary.resolve("data/powers/time_stop_ownership.dat"));
		TimeStopSavedData data = new TimeStopSavedData();
		data.activate(TimeStopLeaseRules.create(17L,
				UUID.fromString("00000000-0000-0000-0000-000000000001"),
				TimeStopLeaseSource.CRYSTAL, ControlTick.at(2_000L), 1_200L, null));

		store.writeVerified(data.snapshot());
		assertEquals(data.snapshot(), store.read());
		store.writeVerified(TimeStopSavedData.emptySnapshot());
		assertEquals(TimeStopSavedData.emptySnapshot(), store.read());
	}

	@Test
	void malformedOrUnwritableJournalNeverReturnsAcknowledgedSuccess() throws IOException {
		Path target = temporary.resolve("data/powers/time_stop_ownership.dat");
		TimeStopJournalStore store = new TimeStopJournalStore(target);
		Files.createDirectories(target.getParent());
		Files.write(target, new byte[] {1, 2, 3, 4});
		assertThrows(IOException.class, store::read);

		Path blocker = temporary.resolve("not-a-directory");
		Files.writeString(blocker, "blocked");
		TimeStopJournalStore blocked = new TimeStopJournalStore(blocker.resolve("journal.dat"));
		assertThrows(IOException.class,
				() -> blocked.writeVerified(TimeStopSavedData.emptySnapshot()));
	}

	@Test
	void postRenameFailuresRestoreThePreviouslyAcknowledgedJournal() throws IOException {
		SharedConstants.tryDetectVersion();
		Path target = temporary.resolve("transaction/data/powers/time_stop_ownership.dat");
		TimeStopJournalStore stable = new TimeStopJournalStore(target);
		stable.writeVerified(TimeStopSavedData.emptySnapshot());
		TimeStopSavedData data = new TimeStopSavedData();
		data.activate(TimeStopLeaseRules.create(19L,
				UUID.fromString("00000000-0000-0000-0000-000000000001"),
				TimeStopLeaseSource.CRYSTAL, ControlTick.at(4_000L), 1_200L, null));

		for (TimeStopJournalStore.WriteBoundary boundary : TimeStopJournalStore.WriteBoundary.values()) {
			TimeStopJournalStore failing = new TimeStopJournalStore(target, reached -> {
				if (reached == boundary) throw new IOException("injected " + boundary);
			});

			assertThrows(IOException.class, () -> failing.writeVerified(data.snapshot()), boundary.name());
			assertEquals(TimeStopSavedData.emptySnapshot(), stable.read(), boundary.name());
		}
	}
}
