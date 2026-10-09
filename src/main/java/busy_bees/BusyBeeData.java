package busy_bees;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public interface BusyBeeData {

	boolean busyBees$hasPitcherPollen();

	void busyBees$setPitcherPollen(boolean pitcherPollen);

	boolean busyBees$isFounding();

	void busyBees$setFounding(boolean founding);

	@Nullable
	BlockPos busyBees$originHive();

	void busyBees$setOriginHive(@Nullable BlockPos origin);

	@Nullable
	BlockPos busyBees$foundingTarget();

	void busyBees$setFoundingTarget(@Nullable BlockPos target);

	@Nullable
	Direction busyBees$foundingFacing();

	void busyBees$setFoundingFacing(@Nullable Direction facing);

	int busyBees$foundingDwell();

	void busyBees$setFoundingDwell(int ticks);

	void busyBees$beginFounding(BlockPos origin);

	void busyBees$clearFounding();

	boolean busyBees$isRelocating();

	@Nullable
	BlockPos busyBees$relocateTarget();

	void busyBees$beginRelocate(BlockPos origin, BlockPos dest);

	void busyBees$clearRelocate();
}
