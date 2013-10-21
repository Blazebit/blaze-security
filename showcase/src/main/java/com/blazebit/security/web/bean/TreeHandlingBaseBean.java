package com.blazebit.security.web.bean;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.blazebit.security.impl.model.UserGroup;
import com.blazebit.security.web.bean.model.NodeModel;
import com.blazebit.security.web.bean.model.NodeModel.Marking;

public abstract class TreeHandlingBaseBean extends SecurityBaseBean {

    /**
     * helper
     * 
     * @param node
     */
    protected TreeNode[] propagateSelectionUp(DefaultTreeNode node, TreeNode[] selectedNodes) {
        if (node.getChildCount() > 0) {
            boolean foundOneUnselected = false;
            for (TreeNode entityChild : node.getChildren()) {
                if (!entityChild.isSelected()) {
                    foundOneUnselected = true;
                    break;
                }
            }
            if (!foundOneUnselected) {
                node.setSelected(true);
                selectedNodes = addNodeToSelectedNodes(node, selectedNodes);
            }
        }
        return selectedNodes;
    }

    /**
     * helper to mark parent nodes when child nodes are marked
     * 
     * @param node
     */
    protected TreeNode[] propagateSelectionAndMarkingUp(DefaultTreeNode node, TreeNode[] selectedNodes) {
        if (node.getChildCount() > 0) {
            NodeModel firstChild = ((NodeModel) node.getChildren().get(0).getData());
            boolean foundDifferentMarking = false;
            boolean foundUnSelected = false;
            boolean foundSelectable = false;
            Marking firstMarking = firstChild.getMarking();
            for (TreeNode child : node.getChildren()) {
                NodeModel childNodeModel = ((NodeModel) child.getData());
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
            NodeModel nodeModel = ((NodeModel) node.getData());
            if (!foundDifferentMarking) {
                nodeModel.setMarking(firstMarking);
            } else {
                nodeModel.setMarking(Marking.NONE);
            }
            if (!foundUnSelected) {
                if (selectedNodes != null) {
                    selectedNodes = addNodeToSelectedNodes(node, selectedNodes);
                }
                node.setSelected(true);
            }

        }
        return selectedNodes;
    }

    /**
     * helper to mark node as selected
     * 
     * @param node
     */
    protected TreeNode[] addNodeToSelectedNodes(DefaultTreeNode node, TreeNode[] selectedNodes) {
        if (selectedNodes.length == 0) {
            selectedNodes = new TreeNode[1];
            selectedNodes[0] = node;
        }
        selectedNodes[selectedNodes.length - 1] = node;
        return selectedNodes;
    }

    protected List<TreeNode> sortTreeNodesByType(TreeNode[] selectedTreeNodes) {
        List<TreeNode> sortedSelectedNodes = Arrays.asList(selectedTreeNodes);
        Collections.sort(sortedSelectedNodes, new Comparator<TreeNode>() {

            @Override
            public int compare(TreeNode o1, TreeNode o2) {
                NodeModel model1 = (NodeModel) o1.getData();
                NodeModel model2 = (NodeModel) o2.getData();
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
