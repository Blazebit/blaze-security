/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blazebit.security.web.bean.model;

/**
 *
 * @author cuszk
 */
public class NodeModel {

    public enum ResourceType {

        ENTITY,
        ACTION,
        FIELD,
        
        USERGROUP,
    }
    private String name;
    private ResourceType type;
    private Object target;
    private boolean marked;

    public NodeModel(String name, ResourceType type, Object target) {
        this.name = name;
        this.type = type;
        this.target = target;
        this.marked = false;
    }

    public NodeModel(String name, ResourceType type, Object target, boolean marked) {
        this.name = name;
        this.type = type;
        this.target = target;
        this.marked = marked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    @Override
    public String toString() {
        return "ResourceModel [name=" + name + ", type=" + type + ", target=" + target + ", marked=" + marked + "]";
    }
    
    
}
