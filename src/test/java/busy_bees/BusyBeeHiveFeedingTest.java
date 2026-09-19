package busy_bees;

import net.minecraft.world.level.block.entity.BeehiveBlockEntity.Occupant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusyBeeHiveFeedingTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void twoFlowersBreedTwoStoredAdults() {
		List<Occupant> occupants = new ArrayList<>();
		occupants.add(Occupant.create(0));
		occupants.add(Occupant.create(0));
		assertEquals(BusyBeeHiveFeeding.FeedOutcome.FED, BusyBeeHiveFeeding.feedOccupants(occupants));
		assertTrue(BusyBeeHiveFeeding.isInLoveAdult(occupants.get(0)));
		assertEquals(BusyBeeHiveFeeding.FeedOutcome.BRED, BusyBeeHiveFeeding.feedOccupants(occupants));
		assertEquals(3, occupants.size());
		assertEquals(BusyBeeHiveFeeding.BREED_COOLDOWN_AGE, BusyBeeHiveFeeding.occupantAge(occupants.get(0)));
		assertEquals(BusyBeeHiveFeeding.BREED_COOLDOWN_AGE, BusyBeeHiveFeeding.occupantAge(occupants.get(1)));
		assertEquals(BusyBeeHiveFeeding.BABY_AGE, BusyBeeHiveFeeding.occupantAge(occupants.get(2)));
		assertEquals(BusyBeeHiveFeeding.FeedOutcome.AGED, BusyBeeHiveFeeding.feedOccupants(occupants));
		assertTrue(BusyBeeHiveFeeding.occupantAge(occupants.get(2)) > BusyBeeHiveFeeding.BABY_AGE);
	}

	@Test
	void alreadyLovedAdultCannotBeFedAgain() {
		List<Occupant> occupants = new ArrayList<>();
		occupants.add(Occupant.create(0));
		assertEquals(BusyBeeHiveFeeding.FeedOutcome.FED, BusyBeeHiveFeeding.feedOccupants(occupants));
		assertTrue(BusyBeeHiveFeeding.isInLoveAdult(occupants.get(0)));
		assertEquals(BusyBeeHiveFeeding.FeedOutcome.NONE, BusyBeeHiveFeeding.feedOccupants(occupants));
		assertEquals(BusyBeeHiveFeeding.LOVE_TICKS, BusyBeeHiveFeeding.occupantInLove(occupants.get(0)));
	}

	@Test
	void cooldownAdultsCannotBeFed() {
		List<Occupant> occupants = new ArrayList<>();
		occupants.add(Occupant.create(0));
		occupants.add(Occupant.create(0));
		BusyBeeHiveFeeding.feedOccupants(occupants);
		BusyBeeHiveFeeding.feedOccupants(occupants);
		assertEquals(BusyBeeHiveFeeding.FeedOutcome.NONE, BusyBeeHiveFeeding.feedOccupants(occupants.subList(0, 2)));
	}
}
