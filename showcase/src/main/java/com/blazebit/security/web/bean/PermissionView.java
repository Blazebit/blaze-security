/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blazebit.security.web.bean;

import com.blazebit.security.web.bean.model.PermissionModel;
import java.util.List;
import org.primefaces.model.TreeNode;

/**
 *
 * @author cuszk
 */
public interface PermissionView {

    public List<PermissionModel> getPermissions();

    public String getPermissionHeader();

    public TreeNode getPermissionViewRoot();
    
    public boolean isShowPermissionTreeView();
    
    public void setShowPermissionTreeView(boolean set);
}
