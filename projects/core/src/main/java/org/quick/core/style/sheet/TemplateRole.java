package org.quick.core.style.sheet;

import java.util.Collections;
import java.util.Set;

import org.quick.core.QuickTemplate;
import org.quick.core.QuickTemplate.AttachPoint;

/**
 * Represents a path of attach points that a widget is located at. For example, a text-field widget may have a border pane in its template
 * whose attach point is "border". A border widget may have a block in its template whose attach point is "contents". So that "contents"
 * block within the "border" border pane of a text-field has a template path of "border.contents".
 */
public class TemplateRole {
	private final AttachPoint theRole;

	private final Set<String> theParentGroups;

	private final Class<? extends QuickTemplate> theParentType;

	private final TemplateRole theParentRole;

	/**
	 * @param role The attach point role for the base of this path
	 * @param parentGroups The groups that the attach point parent belongs to
	 * @param parentType The type of the attach point parent
	 * @param parentRole The parent role for this path
	 */
	public TemplateRole(AttachPoint role, Set<String> parentGroups, Class<? extends QuickTemplate> parentType, TemplateRole parentRole) {
		if(parentType != null && !role.template.getDefiner().isAssignableFrom(parentType))
			throw new IllegalStateException(parentType.getSimpleName() + " is not a subtype of "
				+ role.template.getDefiner().getSimpleName());
		theRole = role;
		theParentGroups = parentGroups == null ? Collections.emptySet() : Collections.unmodifiableSet(parentGroups);
		theParentType = parentType == null ? role.template.getDefiner() : parentType;
		theParentRole = parentRole;
	}

	/** @return The attach point role that is the base of this path */
	public AttachPoint getRole() {
		return theRole;
	}

	/** @return This role's parent role, may be null */
	public TemplateRole getParent() {
		return theParentRole;
	}

	/** @return The parent's type */
	public Class<? extends QuickTemplate> getParentType() {
		return theParentType;
	}

	/** @return The names of the groups this role's parent belongs to */
	public Set<String> getParentGroups() {
		return theParentGroups;
	}

	/** @return The depth of this path */
	public int getDepth() {
		if(theParentRole == null)
			return 1;
		else
			return theParentRole.getDepth() + 1;
	}

	/**
	 * @param path The path to test
	 * @return Whether a widget with this template path also satisfies the given path
	 */
	public boolean containsPath(TemplateRole path) {
		if(!theRole.equals(path.theRole))
			return false;
		if(!theParentGroups.containsAll(path.theParentGroups))
			return false;
		if(!theParentType.isAssignableFrom(path.theParentType))
			return false;
		if(theParentRole != null) {
			if(path.theParentRole == null)
				return false;
			return theParentRole.containsPath(path.theParentRole);
		}
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof TemplateRole))
			return false;
		TemplateRole role = (TemplateRole) o;
		if(!java.util.Objects.equals(theParentRole, role.theParentRole))
			return false;
		if(!theParentType.equals(role.theParentType))
			return false;
		if(!theParentGroups.equals(role.theParentGroups))
			return false;
		return theRole.equals(role.theRole);
	}

	@Override
	public int hashCode() {
		int ret = 0;
		if(theParentRole != null)
			ret += theParentRole.hashCode();
		if(theParentType != theRole.template.getDefiner())
			ret = ret * 17 + theParentType.getName().hashCode();
		if(!theParentGroups.isEmpty())
			ret = ret * 13 + theParentGroups.hashCode();
		ret = ret * 7 + theRole.hashCode();
		return ret;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		if(theParentRole != null) {
			ret.append(theParentRole.toString());
		}
		if(theParentType != theRole.template.getDefiner()) {
			ret.append("[" + theParentType.getSimpleName() + "]");
		}
		if(theParentRole == null)
			ret.append(theRole.template.getDefiner().getSimpleName());
		if(!theParentGroups.isEmpty()) {
			ret.append('(');
			boolean first = true;
			for (String g : theParentGroups) {
				if (first) {
					ret.append(", ");
					first = false;
				}
				ret.append(g);
			}
			ret.append(')');
		}
		ret.append('#').append(theRole.name);
		return ret.toString();
	}
}
