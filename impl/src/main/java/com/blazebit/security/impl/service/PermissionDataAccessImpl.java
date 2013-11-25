/*
 * Copyright 2013 Blazebit.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.blazebit.security.impl.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.blazebit.security.Action;
import com.blazebit.security.Permission;
import com.blazebit.security.PermissionDataAccess;
import com.blazebit.security.PermissionManager;
import com.blazebit.security.Resource;
import com.blazebit.security.Role;
import com.blazebit.security.Subject;

/**
 * 
 * @author cuszk
 * @TODO Add null checks etc
 */
@Stateless
public class PermissionDataAccessImpl extends PermissionCheckBase implements
		PermissionDataAccess {

	private static final Logger LOG = Logger
			.getLogger(PermissionDataAccessImpl.class.getName());

	@Inject
	private PermissionManager permissionManager;

	@Override
	public boolean isRevokable(Subject subject, Action action, Resource resource) {
		checkParameters(subject, action, resource);
		return !getRevokablePermissionsWhenRevoking(subject, action, resource)
				.isEmpty();
	}

	@Override
	public boolean isRevokable(List<Permission> permissions, Action action,
			Resource resource) {
		checkParameters(action, resource);
		return !getRevokablePermissionsWhenRevoking(permissions, action,
				resource).isEmpty();
	}

	@Override
	public Set<Permission> getRevokablePermissionsWhenRevoking(Subject subject,
			Action action, Resource resource) {
		checkParameters(subject, action, resource);
		// look up itself
		Permission permission = findPermission(subject, action, resource);
		if (permission != null) {
			// if exact permission found -> revoke that
			return new HashSet<Permission>(Arrays.asList(permission));
		} else {
			return getReplaceablePermissions(subject, action, resource);
		}
	}

	@Override
	public Set<Permission> getRevokablePermissionsWhenRevoking(
			List<Permission> permissions, Action action, Resource resource) {
		checkParameters(action, resource);
		// look up itself
		Permission permission = findPermission(permissions, action, resource);
		if (permission != null) {
			// if exact permission found -> revoke that
			return new HashSet<Permission>(Arrays.asList(permission));
		} else {
			return getReplaceablePermissions(permissions, action, resource);
		}
	}

	private Set<Permission> getReplaceablePermissions(Subject subject,
			Action action, Resource resource) {
		return getReplaceablePermissions(
				permissionManager.getPermissions(subject), action, resource);
	}

	private Set<Permission> getReplaceablePermissions(
			List<Permission> permissions, Action action, Resource resource) {
		Set<Permission> ret = new HashSet<Permission>();
		for (Permission permission : permissions) {
			if (permission.getResource().isReplaceableBy(resource)
					&& permission.getAction().implies(action)) {
				ret.add(permission);
			}
		}
		return ret;
	}

	@Override
	public boolean isRevokable(Role role, Action action, Resource resource) {
		checkParameters(role, action, resource);
		return !getRevokablePermissionsWhenRevoking(role, action, resource)
				.isEmpty();
	}

	@Override
	public Set<Permission> getRevokablePermissionsWhenRevoking(Role role,
			Action action, Resource resource) {
		checkParameters(role, action, resource);
		// look up itself
		Permission permission = findPermission(role, action, resource);
		if (permission != null) {
			// if exact permission found -> revoke that
			return new HashSet<Permission>(Arrays.asList(permission));
		} else {
			return getReplaceablePermissions(role, action, resource);
		}
	}

	private Set<Permission> getReplaceablePermissions(Role role, Action action,
			Resource resource) {
		Set<Permission> ret = new HashSet<Permission>();
		for (Permission rolePermission : permissionManager.getPermissions(role)) {
			if (rolePermission.getResource().isReplaceableBy(resource)
					&& rolePermission.getAction().implies(action)) {
				ret.add(rolePermission);
			}
		}
		return ret;
	}

	@Override
	public boolean isGrantable(Subject subject, Action action, Resource resource) {
		checkParameters(subject, action, resource);
		List<Permission> permissions = permissionManager
				.getPermissions(subject);
		return isGrantable(permissions, action, resource);
	}

	@Override
	public boolean isGrantable(List<Permission> permissions, Action action,
			Resource resource) {
		checkParameters(action, resource);
		if (!resource.isApplicable(action)) {
			LOG.warning("Action " + action + " cannot be applied to "
					+ resource);
			return false;
		}
		return getImpliedBy(permissions, action, resource).isEmpty();
		// Collection<Resource> connectedResources =
		// resource.connectedResources();
		// for (Resource connectedResource : connectedResources) {
		// if (findPermission(permissions, action, connectedResource) != null) {
		// LOG.warning("Overriding permission already exists");
		// return false;
		// }
		// }
		// return true;
	}

	@Override
	public Set<Permission> getImpliedBy(List<Permission> permissions,
			Action action, Resource resource) {
		Set<Permission> ret = new HashSet<Permission>();
		Collection<Resource> connectedResources = resource.connectedResources();
		for (Resource connectedResource : connectedResources) {
			Permission connectedPermission = findPermission(permissions,
					action, connectedResource);
			if (connectedPermission != null) {
				ret.add(connectedPermission);
			}
		}
		return ret;
	}

	@Override
	public Set<Permission> getRevokablePermissionsWhenGranting(
			List<Permission> permissions, Action action, Resource resource) {
		checkParameters(action, resource);
		return getReplaceablePermissions(permissions, action, resource);

	}

	// TODO rename
	@Override
	public Set<Permission> getRevokablePermissionsWhenGranting(Subject subject,
			Action action, Resource resource) {
		checkParameters(subject, action, resource);
		return getReplaceablePermissions(subject, action, resource);

	}

	@Override
	public boolean isGrantable(Role role, Action action, Resource resource) {
		checkParameters(role, action, resource);
		List<Permission> permissions = permissionManager.getPermissions(role);
		return isGrantable(permissions, action, resource);

	}

	@Override
	public Set<Permission> getRevokablePermissionsWhenGranting(Role role,
			Action action, Resource resource) {
		checkParameters(role, action, resource);
		return getReplaceablePermissions(role, action, resource);

	}

	@Override
	public Permission findPermission(Subject subject, Action action,
			Resource resource) {
		checkParameters(subject, action, resource);
		List<Permission> permissions = permissionManager
				.getPermissions(subject);
		return findPermission(permissions, action, resource);
	}

	@Override
	public Permission findPermission(List<Permission> permissions,
			Action action, Resource resource) {
		checkParameters(action, resource);
		for (Permission permission : permissions) {
			if (permission.getResource().equals(resource)
					&& permission.getAction().equals(action)) {
				return permission;
			}
		}
		return null;
	}

	@Override
	public Permission findPermission(Role role, Action action, Resource resource) {
		checkParameters(role, action, resource);
		List<Permission> permissions = permissionManager.getPermissions(role);
		return findPermission(permissions, action, resource);
	}
}