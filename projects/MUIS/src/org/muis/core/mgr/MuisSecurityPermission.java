package org.muis.core.mgr;

import java.security.Permission;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;

public class MuisSecurityPermission extends java.security.Permission
{
	public static enum PermissionType
	{
		setAttribute, setParent, addChild, removeChild, setBounds, setZ, fireEvent;
	}

	public final PermissionType type;

	public final MuisDocument document;

	public final MuisElement element;

	public final Object value;

	public MuisSecurityPermission(PermissionType _type, MuisDocument doc, MuisElement el, Object val)
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
		if(!(obj instanceof MuisSecurityPermission))
			return false;
		MuisSecurityPermission perm = (MuisSecurityPermission) obj;
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
