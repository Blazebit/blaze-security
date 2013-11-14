package com.blazebit.security.web.bean.model;

public class FieldModel {

    public enum Type {
        PRIMITIVE,
        COLLECTION;
    }

    private String fieldName;
    private boolean selected;
    private Type type;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public FieldModel(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public FieldModel(String fieldName, Type type) {
        this.fieldName = fieldName;
        this.type=type;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
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
        FieldModel other = (FieldModel) obj;
        if (fieldName == null) {
            if (other.fieldName != null)
                return false;
        } else if (!fieldName.equals(other.fieldName))
            return false;
        return true;
    }
    
    

}
