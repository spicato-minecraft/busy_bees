package busy_bees;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

public interface BusyBeeData {

	boolean busyBees$hasPitcherPollen();

	void busyBees$setPitcherPollen(boolean pitcherPollen);

	boolean busyBees$isFounding();

	void busyBees$setFounding(boolean founding);

	@Nullable
	BlockPos busyBees$originHive();

	void busyBees$setOriginHive(@Nullable BlockPos origin);
}
