package org.quick.core;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.observe.Observable;
import org.observe.collect.ObservableCollection;
import org.observe.util.TypeTokens;
import org.quick.core.QuickTemplate.AttachPoint;
import org.quick.core.parser.Version;
import org.quick.core.style.CompoundStyleSheet;
import org.quick.core.style.MutableStyleSheet;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleCondition;
import org.quick.core.style.StyleConditionValue;
import org.quick.core.style.StyleSheet;

/** Represents a toolkit that contains resources for use in a Quick document */
public class QuickToolkit extends QuickLibrary {
	/** A toolkit style sheet contains no values itself, but serves as a container to hold all style sheets referred to by the toolkit */
	public class ToolkitStyleSheet extends CompoundStyleSheet {
		ToolkitStyleSheet(ObservableCollection<StyleSheet> dependencies) {
			super(dependencies);
		}

		@Override
		public String toString() {
			return "Style sheet for toolkit " + QuickToolkit.this;
		}
	}

	private final QuickEnvironment theEnvironment;
	private final Observable<?> theDeath;

	private ToolkitStyleSheet theStyle;
	private ObservableCollection<StyleSheet> theStyleDependencyController;

	private QuickToolkit(QuickEnvironment env, URL uri, String name, String descrip, Version version, List<URL> cps,
		Map<String, String> classMap, Map<String, String> resMap, List<QuickToolkit> depends, List<QuickPermission> perms) {
		super(uri, name, descrip, version, cps, classMap, resMap, depends, perms);
		theEnvironment = env;
		theStyleDependencyController = ObservableCollection.create(TypeTokens.get().of(StyleSheet.class));
		theStyle = new ToolkitStyleSheet(theStyleDependencyController.flow().unmodifiable().collect());
		// TODO Assuming toolkits are immortal right now.
		// If we ever want to remove them from memory when they're unused, replace this with something that can be called by the environment
		theDeath = Observable.empty();
	}

	/** @return The environment that this toolkit is for */
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	/** @return The styles for this toolkit */
	public ToolkitStyleSheet getStyle() {
		return theStyle;
	}

	public QuickWidgetSet<?, ?> getImplementation() {
		return (QuickWidgetSet<?, ?>) super.getPriorityDependency();
	}

	public QuickToolkit satisfyWith(QuickWidgetSet<?, ?> implementation) {
		setPriorityDependency(implementation);
		return this;
	}

	/**
	 * Creates a toolkit builder
	 *
	 * @param env The environment that the toolkit is in
	 * @param location The location of the toolkit
	 * @return The builder
	 */
	public static Builder build(QuickEnvironment env, URL location) {
		return new Builder(env, location);
	}

	/** Builds a QuickToolkit */
	public static class Builder extends QuickLibrary.Builder<QuickToolkit, Builder> {
		private final QuickEnvironment theEnvironment;

		private Builder(QuickEnvironment environment, URL location) {
			super(location);
			theEnvironment = environment;
		}

		@Override
		public Builder addDependency(QuickLibrary lib) {
			if (!(lib instanceof QuickToolkit))
				throw new IllegalArgumentException("A toolkit cannot depend on a " + lib.getClass().getSimpleName());
			return super.addDependency(lib);
		}

		@Override
		public QuickToolkit createLibrary(URL location, String name, String description, Version version, List<URL> classPaths,
			Map<String, String> classMappings, Map<String, String> resourceLocations, List<QuickLibrary> dependencies,
			List<QuickPermission> permissions) {
			return new QuickToolkit(theEnvironment, location, name, description, version, classPaths, classMappings, resourceLocations,
				(List<QuickToolkit>) (List<?>) dependencies, permissions);
		}

		/**
		 * @param styleSheet The style sheet to use with this toolkit
		 * @return This builder, for chaining
		 */
		public Builder addStyleSheet(StyleSheet styleSheet) {
			if (getBuilt() == null)
				throw new IllegalStateException("Styles must be added after the toolkit is built");
			if (!getLocation().equals(QuickEnvironment.CORE_TOOLKIT) && !(styleSheet instanceof ToolkitStyleSheet)) {
				for (StyleAttribute<?> attr : styleSheet.attributes()) {
					if (attr.getDomain().getClass().getClassLoader() == getBuilt())
						continue;
					for (StyleConditionValue<?> sev : styleSheet.getStyleExpressions(attr)) {
						if (sev.getCondition().getType().getClassLoader() == getBuilt())
							continue;
						if (checkRole(sev.getCondition().getRole(), sev.getCondition().getParent()))
							continue;
						String msg = "Toolkit " + getLocation() + ": Style sheet";
						msg += " assigns styles for non-toolkit attributes to non-toolkit element types";
						if (!(styleSheet instanceof MutableStyleSheet))
							throw new IllegalStateException(msg);
						else {
							theEnvironment.msg().error(msg);
							((MutableStyleSheet) styleSheet).clear(attr, sev.getCondition());
						}
					}
				}
			}
			getBuilt().theStyleDependencyController.add(styleSheet);
			return this;
		}

		private boolean checkRole(AttachPoint<?> role, StyleCondition parent) {
			if(role==null)
				return true;
			if (role.template.getDefiner().getClassLoader() != getBuilt())
				return false;
			return checkRole(parent.getRole(), parent.getParent());
		}
	}
}
