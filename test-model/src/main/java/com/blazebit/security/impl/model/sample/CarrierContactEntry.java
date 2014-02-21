package com.blazebit.security.impl.model.sample;

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.blazebit.security.annotation.ResourceName;
import com.blazebit.security.model.BaseEntity;

@Entity
@ResourceName(name="carrier contact entry", module ="carrier")
public class CarrierContactEntry implements Serializable, BaseEntity<CarrierContactEntryId> {

	private CarrierContactEntryId id;
	private TestCarrier carrier;
	private Contact contact;

	@EmbeddedId
	public CarrierContactEntryId getId() {
		return id;
	}

	public void setId(CarrierContactEntryId id) {
		this.id = id;
	}

	@ManyToOne
	public TestCarrier getCarrier() {
		return carrier;
	}

	public void setCarrier(TestCarrier carrier) {
		this.carrier = carrier;
	}

	@ManyToOne
	public Contact getContact() {
		return contact;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
	}

}
