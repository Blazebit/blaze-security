package com.blazebit.security.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.blazebit.security.entity.EntityResourceType;

/**
 * @author Christian Beikov
 */
@Entity
@EntityResourceType(name = "User Group", module = "Core")
public class UserGroup extends BaseEntity<Integer> implements Role {

	private static final long serialVersionUID = 1L;
    private String name;
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
        this.name = name;
	}

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic(optional = false)
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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
