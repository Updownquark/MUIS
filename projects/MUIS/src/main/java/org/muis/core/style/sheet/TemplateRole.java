package org.muis.core.style.sheet;

import java.util.Collections;
import java.util.List;

import org.muis.core.MuisTemplate;
import org.muis.core.MuisTemplate.AttachPoint;

/**
 * Represents a path of attach points that a widget is located at. For example, a text-field widget may have a border pane in its template
 * whose attach point is "border". A border widget may have a block in its template whose attach point is "contents". So that "contents"
 * block within the "border" border pane of a text-field has a template path of "border.contents".
 */
public class TemplateRole {
	private final AttachPoint theRole;

	private final List<String> theParentGroups;

	private final Class<? extends MuisTemplate> theParentType;

	private final TemplateRole theParentRole;

	public TemplateRole(AttachPoint role, List<String> parentGroups, Class<? extends MuisTemplate> parentType, TemplateRole parentRole) {
		if(parentType != null && !role.template.getDefiner().isAssignableFrom(parentType))
			throw new IllegalStateException(parentType.getSimpleName() + " is not a subtype of "
				+ role.template.getDefiner().getSimpleName());
		theRole = role;
		theParentGroups = parentGroups == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(parentGroups);
		theParentType = parentType == null ? role.template.getDefiner() : parentType;
		theParentRole = parentRole;
	}

	public AttachPoint getRole() {
		return theRole;
	}

	public TemplateRole getParent() {
		return theParentRole;
	}

	public Class<? extends MuisTemplate> getParentType() {
		return theParentType;
	}

	public List<String> getParentGroups() {
		return theParentGroups;
	}

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
			for(int i = 0; i < theParentGroups.size(); i++) {
				if(i > 0)
					ret.append(", ");
				ret.append(theParentGroups.get(i));
			}
			ret.append(')');
		}
		ret.append('#').append(theRole.name);
		return ret.toString();
	}
}
