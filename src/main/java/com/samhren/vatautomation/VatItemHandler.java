package com.samhren.vatautomation;

import com.lance5057.extradelight.workstations.vat.VatBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Automation-facing view of the vat.
 * <p>
 * Slot 0 is a virtual input that always looks empty: anything inserted into it is routed into the
 * vat's real ingredient, container or stage-ingredient slots by {@link VatRouter}. Slot 1 is the
 * vat's output slot and is the only thing that can be extracted, so pipes never pull ingredients back
 * out.
 */
public final class VatItemHandler implements IItemHandler {
	public static final int INPUT = 0;
	public static final int OUTPUT = 1;

	private final VatBlockEntity vat;

	public VatItemHandler(VatBlockEntity vat) {
		this.vat = vat;
	}

	@Override
	public int getSlots() {
		return 2;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return slot == OUTPUT ? vat.getItemHandler().getStackInSlot(VatBlockEntity.OUTPUT_SLOT) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		Level level = vat.getLevel();
		if (slot != INPUT || stack.isEmpty() || level == null || level.isClientSide()) {
			return stack;
		}
		return VatRouter.insert(vat, level, stack, simulate);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (slot != OUTPUT) {
			return ItemStack.EMPTY;
		}
		return vat.getItemHandler().extractItem(VatBlockEntity.OUTPUT_SLOT, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return slot == OUTPUT ? vat.getItemHandler().getSlotLimit(VatBlockEntity.OUTPUT_SLOT) : 64;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return slot == INPUT && insertItem(INPUT, stack, true).getCount() < stack.getCount();
	}
}
