package com.samhren.vatautomation;

import com.lance5057.extradelight.ExtraDelightBlockEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(VatAutomation.MOD_ID)
public final class VatAutomation {
	public static final String MOD_ID = "vatautomation";

	public VatAutomation(IEventBus modBus) {
		modBus.addListener(VatAutomation::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		// The same handlers are exposed on every side: inserts are routed by content, not by face, so
		// funnels, belts and hoppers work wherever they are attached. The top is usually taken by the lid.
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ExtraDelightBlockEntities.VAT.get(),
				(vat, side) -> new VatItemHandler(vat));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ExtraDelightBlockEntities.VAT.get(),
				(vat, side) -> vat.getFluidTank());
	}
}
