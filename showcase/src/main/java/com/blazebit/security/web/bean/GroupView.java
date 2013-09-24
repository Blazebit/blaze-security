/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import com.blazebit.security.web.bean.model.GroupModel;
import java.util.List;
import org.primefaces.model.TreeNode;

/**
 *
 * @author cuszk
 */
public interface GroupView {

    public List<GroupModel> getGroups();

    public String getGroupHeader();

    public TreeNode getGroupRoot();

    public boolean isShowGroupTreeView();
    
    public void setShowGroupTreeView(boolean set);
}
