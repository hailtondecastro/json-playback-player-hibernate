package org.jsonplayback.player.hibernate.entities;

import java.util.Collection;

public class MasterBCompComp {
	private Collection<DetailAEnt> detailAEntCol;

	public Collection<DetailAEnt> getDetailAEntCol() {
		return detailAEntCol;
	}

	public void setDetailAEntCol(Collection<DetailAEnt> detailAEntCol) {
		this.detailAEntCol = detailAEntCol;
	}
}
