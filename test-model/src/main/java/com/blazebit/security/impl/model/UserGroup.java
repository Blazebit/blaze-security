package com.blazebit.security.impl.model;

import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.blazebit.security.annotation.ResourceName;
import com.blazebit.security.model.Permission;
import com.blazebit.security.model.Role;
import com.blazebit.security.model.Subject;

/**
 * @author Christian Beikov
 */
@Entity
@ResourceName(name = "User Group", module = "Core")
public class UserGroup extends AbstractUserGroup {

	private static final long serialVersionUID = 1L;
	private Company company;
	private boolean selected;

	private Set<User> users = new HashSet<User>(0);
	private Set<UserGroup> userGroups = new HashSet<UserGroup>(0);
	// private Set<UserGroupPermission> permissions = new
	// HashSet<UserGroupPermission>(
	// 0);
	// private Set<UserGroupDataPermission> dataPermissions = new
	// HashSet<UserGroupDataPermission>(
	// 0);

	private UserGroup parent;

	@ManyToOne
	@JoinColumn(name = "parent_group", nullable = true)
	public UserGroup getParent() {
		return this.parent;
	}

	public void setParent(UserGroup parent) {
		this.parent = parent;
	}

	public UserGroup() {
	}

	public UserGroup(String name) {
		super(name);
	}

	@ManyToMany
	public Set<User> getUsers() {
		return this.users;
	}

	public void setUsers(Set<User> users) {
		this.users = users;
	}

	@OneToMany(mappedBy = "parent")
	public Set<UserGroup> getUserGroups() {
		return this.userGroups;
	}

	public void setUserGroups(Set<UserGroup> userGroups) {
		this.userGroups = userGroups;
	}

	// @OneToMany(mappedBy = "id.subject")
	// public Set<UserGroupPermission> getPermissions() {
	// return this.permissions;
	// }
	//
	// public void setPermissions(Set<UserGroupPermission> permissions) {
	// this.permissions = permissions;
	// }
	//
	// @OneToMany(mappedBy = "id.subject")
	// public Set<UserGroupDataPermission> getDataPermissions() {
	// return this.dataPermissions;
	// }
	//
	// public void setDataPermissions(Set<UserGroupDataPermission>
	// dataPermissions) {
	// this.dataPermissions = dataPermissions;
	// }
	//
	// @Transient
	// @Override
	// public Set<Permission> getAllPermissions() {
	// Set<Permission> allPermissions = new HashSet<Permission>();
	// allPermissions.addAll(this.permissions);
	// allPermissions.addAll(this.dataPermissions);
	// return allPermissions;
	// }

	@ManyToOne
	@JoinColumn(name = "company")
	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transient
	public Collection<Subject> getSubjects() {
		return (Collection<Subject>) (Collection<?>) users;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transient
	public Collection<Role> getRoles() {
		return (Collection<Role>) (Collection<?>) userGroups;
	}

	@Transient
	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

}
