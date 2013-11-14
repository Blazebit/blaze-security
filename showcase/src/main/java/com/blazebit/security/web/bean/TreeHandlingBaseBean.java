package com.blazebit.security.web.bean;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.impl.model.EntityAction;
import com.blazebit.security.web.bean.model.TreeNodeModel;
import com.blazebit.security.web.bean.model.TreeNodeModel.Marking;
import com.blazebit.security.web.bean.model.TreeNodeModel.ResourceType;
import com.blazebit.security.web.service.api.ActionUtils;

public abstract class TreeHandlingBaseBean extends SecurityBaseBean {

    @Inject
    ActionUtils actionUtils;

    /**
     * helper to mark parent nodes when child nodes are marked
     * 
     * @param node
     */
    protected void propagateMarkingUpwards(DefaultTreeNode node) {
        if (node.getChildCount() > 0) {
            TreeNodeModel firstChild = ((TreeNodeModel) node.getChildren().get(0).getData());
            boolean foundDifferentMarking = false;
            Marking firstMarking = firstChild.getMarking();
            for (TreeNode child : node.getChildren()) {
                TreeNodeModel childNodeModel = ((TreeNodeModel) child.getData());
                if (!childNodeModel.getMarking().equals(firstMarking)) {
                    foundDifferentMarking = true;
                }

            }
            TreeNodeModel nodeModel = ((TreeNodeModel) node.getData());
            if (!foundDifferentMarking) {
                nodeModel.setMarking(firstMarking);
                nodeModel.setTooltip(firstChild.getTooltip());
            } else {
                nodeModel.setMarking(Marking.NONE);
            }
        }
    }

    protected TreeNode[] propagateSelectionUpwards(DefaultTreeNode node) {
        TreeNode[] selectedNodes = new TreeNode[] {};
        if (node.getChildCount() > 0) {
            boolean foundUnSelected = false;
            boolean foundSelectable = false;
            for (TreeNode child : node.getChildren()) {
                if (child.isSelectable()) {
                    foundSelectable = true;
                }
                if (!child.isSelected()) {
                    foundUnSelected = true;
                }
            }
            node.setSelectable(foundSelectable);
            if (!foundUnSelected) {
                if (!ArrayUtils.contains(selectedNodes, node)) {
                    selectedNodes = ArrayUtils.add(selectedNodes, node);
                }
                node.setSelected(true);
            }
        } else {
            if (node.isSelected()) {
                if (!ArrayUtils.contains(selectedNodes, node)) {
                    selectedNodes = ArrayUtils.add(selectedNodes, node);
                }
            }
        }
        return selectedNodes;
    }

    /**
     * helper to mark parent nodes when child nodes are marked
     * 
     * @param node
     */
    protected void propagateNodePropertiesTo(DefaultTreeNode node) {
        if (node.getChildCount() > 0) {
            TreeNodeModel firstChild = ((TreeNodeModel) node.getChildren().get(0).getData());
            boolean foundDifferentMarking = false;
            boolean foundUnSelected = false;
            boolean foundSelectable = false;
            Marking firstMarking = firstChild.getMarking();
            for (TreeNode child : node.getChildren()) {
                TreeNodeModel childNodeModel = ((TreeNodeModel) child.getData());
                if (child.isSelectable()) {
                    foundSelectable = true;
                }
                if (!child.isSelected()) {
                    foundUnSelected = true;
                }
                if (!childNodeModel.getMarking().equals(firstMarking)) {
                    foundDifferentMarking = true;
                }

            }
            node.setSelectable(foundSelectable);
            TreeNodeModel nodeModel = ((TreeNodeModel) node.getData());
            if (!foundDifferentMarking) {
                nodeModel.setMarking(firstMarking);
            } else {
                nodeModel.setMarking(Marking.NONE);
            }
            if (!foundUnSelected) {
                node.setSelected(true);
            }

        } else {
            // if entityNode has no children remove it
            TreeNodeModel treeNodeModel = (TreeNodeModel) node.getData();
            if (ResourceType.ENTITY.equals(treeNodeModel.getType()) || ResourceType.MODULE.equals(treeNodeModel.getType())) {
                node.setParent(null);
            } else {
                if (ResourceType.ACTION.equals(treeNodeModel.getType())) {
                    // if it a collection field action
                    if (actionUtils.getActionsForCollectionField().contains((EntityAction) treeNodeModel.getTarget())) {
                        node.setParent(null);
                    }
                }

            }
        }
    }

    protected List<TreeNode> sortTreeNodesByType(TreeNode[] selectedTreeNodes) {
        List<TreeNode> sortedSelectedNodes = Arrays.asList(selectedTreeNodes);
        Collections.sort(sortedSelectedNodes, new Comparator<TreeNode>() {

            @Override
            public int compare(TreeNode o1, TreeNode o2) {
                TreeNodeModel model1 = (TreeNodeModel) o1.getData();
                TreeNodeModel model2 = (TreeNodeModel) o2.getData();
                if (model1.getType().ordinal() == model2.getType().ordinal()) {
                    return 0;
                } else {
                    return model1.getType().ordinal() < model2.getType().ordinal() ? -1 : 1;
                }
            }

        });
        return sortedSelectedNodes;
    }

}
