package org.jsonplayback.player.hibernate.entities;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

@Embeddable
public class DetailACompComp {
	@ManyToOne
	@JoinColumns({
		@JoinColumn(name="DUMMY_COL_DetailACompComp_01"),
		@JoinColumn(name="DUMMY_COL_DetailACompComp_02")
	})
	private MasterBEnt masterB;

	public MasterBEnt getMasterB() {
		return masterB;
	}

	public void setMasterB(MasterBEnt masterB) {
		this.masterB = masterB;
	}
}
