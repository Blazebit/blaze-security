/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package com.blazebit.security.web.bean.model;

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
        BLUE("color:blue"),
        RED("color:red"),
        GREEN("color:green");

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

    private String name;
    private ResourceType type;
    private Object target;
    private Marking marking;
    private String tooltip;

    public TreeNodeModel(String name, ResourceType type, Object target) {
        this.name = name;
        this.type = type;
        this.target = target;
        this.marking = Marking.NONE;
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
        return "ResourceModel [name=" + name + ", type=" + type + ", target=" + target + ", marking=" + marking + "]";
    }

    public boolean isMarked() {
        return Marking.RED.equals(this.getMarking()) || Marking.GREEN.equals(this.getMarking());
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

}
