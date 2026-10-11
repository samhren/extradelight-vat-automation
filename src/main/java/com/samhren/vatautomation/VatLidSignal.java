package com.samhren.vatautomation;

import com.lance5057.extradelight.workstations.vat.VatBlockEntity;

/** Comparator output of the vat: full strength while the current stage needs the lid, otherwise off. */
public final class VatLidSignal {
	private VatLidSignal() {
	}

	public static int of(VatBlockEntity vat) {
		boolean running = vat.getCookTimeTotal() > 0 && vat.getStage() < vat.getStageTotal();
		return running && vat.isLidRequired() ? 15 : 0;
	}
}
