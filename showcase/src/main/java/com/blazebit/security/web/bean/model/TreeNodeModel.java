/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author cuszk
 */
public class TreeNodeModel {

    public enum ResourceType {

        ENTITY,
        ACTION,
        FIELD,

        USERGROUP,
        USER,
        MODULE,
    }

    public enum Marking {
        NONE,
        OBJECT("color:blue"),
        REMOVED("color:red"),
        NEW("color:green"),
        SELECTED("color:gray");

        private String style;

        Marking() {

        }

        Marking(String style) {
            this.style = style;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }

    }

    /**
     * name to be displayed in the tree
     */
    private String name;
    /**
     * decides what kind of content is in this node
     */
    private ResourceType type;
    /**
     * a treenode can represent multiple resources. more precisely multiple object resources, but we store every type of
     * referenced resource in this collection
     */
    // private Set<EntityObjectField> objectInstances = new HashSet<EntityObjectField>();
    //
    private Set<TreeNodeModel> instances = new HashSet<TreeNodeModel>();
    /**
     * the content of the actual node
     */
    private Object target;
    /**
     * coloring
     */
    private Marking marking;
    /**
     * tooltip
     */
    private String tooltip;

    private boolean selected;

    public TreeNodeModel(String name, ResourceType type, Object target) {
        this.name = name;
        this.type = type;
        this.target = target;
        this.marking = Marking.NONE;
    }

    public TreeNodeModel(String name, ResourceType type, Object target, boolean selected) {
        this.name = name;
        this.type = type;
        this.target = target;
        this.marking = Marking.NONE;
        this.selected = selected;
    }

    public TreeNodeModel(String name, ResourceType type, Object target, Marking marking) {
        this.name = name;
        this.type = type;
        this.target = target;
        this.marking = marking;
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

    public Marking getMarking() {
        return marking;
    }

    public void setMarking(Marking marking) {
        this.marking = marking;
    }

    

    @Override
    public String toString() {
        return "TreeNodeModel [name=" + name + ", type=" + type + ", target=" + target + ", marking=" + marking + ", tooltip=" + tooltip + ", selected=" + selected + "]";
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((marking == null) ? 0 : marking.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        result = prime * result + ((tooltip == null) ? 0 : tooltip.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        TreeNodeModel other = (TreeNodeModel) obj;
        if (marking != other.marking)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (target == null) {
            if (other.target != null)
                return false;
        } else if (!target.equals(other.target))
            return false;
        if (tooltip == null) {
            if (other.tooltip != null)
                return false;
        } else if (!tooltip.equals(other.tooltip))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    // public Set<EntityObjectField> getObjectInstances() {
    // return objectInstances;
    // }
    //
    // public void setObjectInstances(Set<EntityObjectField> objectInstances) {
    // this.objectInstances = objectInstances;
    // }
    
    public Set<TreeNodeModel> getInstances() {
        return instances;
    }

    public List<TreeNodeModel> getNodeInstances() {
        return new ArrayList<TreeNodeModel>(instances);
    }

    public void setInstances(Set<TreeNodeModel> instances) {
        this.instances = instances;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
