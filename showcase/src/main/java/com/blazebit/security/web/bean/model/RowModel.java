package com.blazebit.security.web.bean.model;

import com.blazebit.security.model.IdHolder;

public class RowModel {

    private IdHolder entity;
    private boolean selected;

    private String fieldSummary;

    public RowModel(IdHolder entity) {
        this.entity = entity;
    }

    public RowModel(IdHolder entity, boolean selected) {
        this.entity = entity;
        this.selected = selected;
    }

    public RowModel(IdHolder entity, boolean selected, String fieldSummary) {
        this.entity = entity;
        this.selected = selected;
        this.fieldSummary = fieldSummary;
    }

    public RowModel(IdHolder entity, String fieldSummary) {
        this.entity = entity;
        this.fieldSummary = fieldSummary;
    }

    public IdHolder getEntity() {
        return entity;
    }

    public void setEntity(IdHolder entity) {
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
