package com.samhren.vatautomation;

import com.lance5057.extradelight.workstations.FancyTank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Automation-facing view of the vat's tank.
 * <p>
 * ExtraDelight's FancyTank refuses any fill that does not fit completely, so pipes that push fixed-size
 * packets (Create pushes half the pump RPM in mB per transfer) stall below full. It also reports zero tanks
 * while empty, and its two-argument isFluidValid calls itself. This wrapper trims fills to the space left,
 * always reports the single tank the vat has, and validates through the tank's predicate instead.
 */
public final class VatFluidHandler implements IFluidHandler {
	private final FancyTank tank;

	public VatFluidHandler(FancyTank tank) {
		this.tank = tank;
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return tank == 0 ? this.tank.getFluid() : FluidStack.EMPTY;
	}

	@Override
	public int getTankCapacity(int tank) {
		return tank == 0 ? this.tank.getTankCapacity(0) : 0;
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		if (tank != 0 || !this.tank.isFluidValid(stack)) {
			return false;
		}
		FluidStack contained = this.tank.getFluid();
		return contained.isEmpty() || FluidStack.isSameFluidSameComponents(contained, stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		int accepted = Math.min(resource.getAmount(), tank.getVacancy());
		return accepted <= 0 ? 0 : tank.fill(resource.copyWithAmount(accepted), action);
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return tank.drain(resource, action);
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return tank.drain(maxDrain, action);
	}
}
