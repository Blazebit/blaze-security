package com.blazebit.security.entity;

/**
 * Helper class to contain the module name, the displayed resource name of an
 * entity and a test expression if ambigous entities are given
 * 
 * @author cuszk
 * 
 */
public class EntityResourceDefinition {

	public EntityResourceDefinition() {
	}

	public EntityResourceDefinition(String moduleName, String resourceName,
			String test) {
		this.moduleName = moduleName;
		this.resourceName = resourceName;
		this.testExpression = test;
	}

	public EntityResourceDefinition(String moduleName, String resourceName) {
		this.moduleName = moduleName;
		this.resourceName = resourceName;
	}

	private String moduleName;
	private String resourceName;
	private String testExpression;

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public String getTestExpression() {
		return testExpression;
	}

	public void setTestExpression(String testExpression) {
		this.testExpression = testExpression;
	}

	@Override
	public String toString() {
		return "ResourceDefinition [moduleName=" + moduleName
				+ ", resourceName=" + resourceName + "]";
	}

}