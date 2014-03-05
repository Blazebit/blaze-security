package com.blazebit.security.model.sample;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Embeddable;

@Embeddable
public class CarrierContactEntryId implements Serializable{

	private Integer carrierId;
	private Integer contactId;

	public CarrierContactEntryId() {
	}

	public CarrierContactEntryId(Integer carrierId, Integer contactId) {
		super();
		this.carrierId = carrierId;
		this.contactId = contactId;
	}

	@Basic
	public Integer getCarrierId() {
		return carrierId;
	}

	public void setCarrierId(Integer carrierId) {
		this.carrierId = carrierId;
	}

	@Basic
	public Integer getContactId() {
		return contactId;
	}

	public void setContactId(Integer contactId) {
		this.contactId = contactId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((carrierId == null) ? 0 : carrierId.hashCode());
		result = prime * result
				+ ((contactId == null) ? 0 : contactId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CarrierContactEntryId other = (CarrierContactEntryId) obj;
		if (carrierId == null) {
			if (other.carrierId != null)
				return false;
		} else if (!carrierId.equals(other.carrierId))
			return false;
		if (contactId == null) {
			if (other.contactId != null)
				return false;
		} else if (!contactId.equals(other.contactId))
			return false;
		return true;
	}

}
