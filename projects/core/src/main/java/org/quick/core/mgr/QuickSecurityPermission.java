/*package org.quick.core.mgr;

import java.security.Permission;

import org.quick.core.QuickDocument;
import org.quick.core.QuickElement;

public class QuickSecurityPermission extends java.security.Permission {
	public static enum PermissionType {
		setAttribute, setParent, addChild, removeChild, setBounds, setZ, fireEvent;
	}

	public final PermissionType type;

	public final QuickDocument document;

	public final QuickElement element;

	public final Object value;

	public QuickSecurityPermission(PermissionType _type, QuickDocument doc, QuickElement el, Object val) {
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
	public boolean equals(Object obj) {
		if(!(obj instanceof QuickSecurityPermission))
			return false;
		QuickSecurityPermission perm = (QuickSecurityPermission) obj;
		return perm.type == type && perm.document == document && perm.element == element
			&& (perm.value == null ? value == null : perm.value.equals(value));
	}

	@Override
	public String getActions() {
		return null;
	}

	@Override
	public int hashCode() {
		return type.hashCode() + document.hashCode() + (element == null ? 0 : element.hashCode());
	}

	@Override
	public boolean implies(Permission permission) {
		return false;
	}
}*/
