package com.blazebit.security.web.bean.model;

import java.util.HashMap;
import java.util.Map;

import com.blazebit.security.IdHolder;

public class EditModel {

    private IdHolder entity;
    private Map<String, FieldModel> fields = new HashMap<String, FieldModel>();
    private boolean selected;

    public EditModel(IdHolder selected) {
        this.entity = selected;
    }

    public EditModel() {
    }

    public IdHolder getEntity() {
        return entity;
    }

    public void setEntity(IdHolder entity) {
        this.entity = entity;
    }

    public Map<String, FieldModel> getFields() {
        return fields;
    }

    public void setFields(Map<String, FieldModel> fields) {
        this.fields = fields;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
