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
package com.blazebit.security.exception;

/**
 * Exception to be thrown when an error occurs during granting or revoking a
 * permission
 * 
 */
public class PermissionActionException extends RuntimeException {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	public PermissionActionException() {
	}

	public PermissionActionException(String message) {
		super(message);
	}

	public PermissionActionException(String message, Throwable cause) {
		super(message, cause);
	}

	public PermissionActionException(Throwable cause) {
		super(cause);
	}
}
