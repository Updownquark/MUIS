package org.wam.core;

import java.security.Permission;

public class WamSecurityPermission extends java.security.Permission
{
	public static enum PermissionType
	{
		setAttribute, setParent, addChild, removeChild, setBounds, setZ, fireEvent;
	}

	public final PermissionType type;

	public final WamDocument document;

	public final WamElement element;

	public final Object value;

	public WamSecurityPermission(PermissionType _type, WamDocument doc, WamElement el, Object val)
	{
		super(_type.name());
		type = _type;
		if(el != null)
			document = el.getDocument();
		else
			document = doc;
		element = el;
		value = val;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof WamSecurityPermission))
			return false;
		WamSecurityPermission perm = (WamSecurityPermission) obj;
		return perm.type == type && perm.document == document && perm.element == element
			&& (perm.value == null ? value == null : perm.value.equals(value));
	}

	@Override
	public String getActions()
	{
		return null;
	}

	@Override
	public int hashCode()
	{
		return type.hashCode() + document.hashCode() + (element == null ? 0 : element.hashCode());
	}

	@Override
	public boolean implies(Permission permission)
	{
		return false;
	}
}
