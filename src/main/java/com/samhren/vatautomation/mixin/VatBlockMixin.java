package com.samhren.vatautomation.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.lance5057.extradelight.workstations.vat.VatBlock;
import com.lance5057.extradelight.workstations.vat.VatBlockEntity;
import com.samhren.vatautomation.VatLidSignal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gives the vat a comparator output so a piston can put the lid on and take it off. The vat entity
 * calls setChanged every tick, which already notifies comparators.
 */
@Mixin(VatBlock.class)
public abstract class VatBlockMixin extends Block {
	private VatBlockMixin(Properties properties) {
		super(properties);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof VatBlockEntity vat ? VatLidSignal.of(vat) : 0;
	}
}
