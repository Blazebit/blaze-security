package com.blazebit.security.web.bean.model;


public class RowModel<T> {

	private T entity;
	private boolean selected;

	private String fieldSummary;

	public RowModel(T entity) {
		this.entity = entity;
	}

	public RowModel(T entity, boolean selected) {
		this.entity = entity;
		this.selected = selected;
	}

	public RowModel(T entity, boolean selected, String fieldSummary) {
		this.entity = entity;
		this.selected = selected;
		this.fieldSummary = fieldSummary;
	}

	public RowModel(T entity, String fieldSummary) {
		this.entity = entity;
		this.fieldSummary = fieldSummary;
	}

	public T getEntity() {
		return entity;
	}

	public void setEntity(T entity) {
		this.entity = entity;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getFieldSummary() {
		return fieldSummary;
	}

	public void setFieldSummary(String fieldSummary) {
		this.fieldSummary = fieldSummary;
	}

}
