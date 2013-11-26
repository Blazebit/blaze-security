package com.blazebit.security.web.bean.model;

import java.util.HashMap;
import java.util.Map;

import com.blazebit.security.IdHolder;
import com.blazebit.security.web.bean.model.FieldModel.Type;

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

    public Map<String, FieldModel> getCollectionFields() {
        Map<String, FieldModel> ret = new HashMap<String, FieldModel>();
        for (String field : fields.keySet()) {
            if (Type.COLLECTION.equals(fields.get(field).getType())) {
                ret.put(field, fields.get(field));
            }
        }
        return ret;
    }

    public Map<String, FieldModel> getPrimitiveFields() {
        Map<String, FieldModel> ret = new HashMap<String, FieldModel>();
        for (String field : fields.keySet()) {
            if (Type.PRIMITIVE.equals(fields.get(field).getType())) {
                ret.put(field, fields.get(field));
            }
        }
        return ret;
    }

}
