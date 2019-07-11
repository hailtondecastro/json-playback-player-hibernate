package org.jsplayback.backend.hibernate.entities;

import java.util.Collection;

public class MasterBComp {
	private Collection<DetailAEnt> detailAEntCol;
	private MasterBCompComp masterBCompComp;

	public Collection<DetailAEnt> getDetailAEntCol() {
		return detailAEntCol;
	}

	public void setDetailAEntCol(Collection<DetailAEnt> detailAEntCol) {
		this.detailAEntCol = detailAEntCol;
	}

	public MasterBCompComp getMasterBCompComp() {
		return masterBCompComp;
	}

	public void setMasterBCompComp(MasterBCompComp masterBCompComp) {
		this.masterBCompComp = masterBCompComp;
	}
}
