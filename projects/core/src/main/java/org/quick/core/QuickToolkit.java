package org.quick.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
	public static class Builder {
		private final QuickEnvironment theEnvironment;
		private final URL theLocation;

		private String theName;
		private String theDescription;
		private Version theVersion;

		private List<URL> theClassPaths;
		private List<QuickToolkit> theDependencies;
		private Map<String, String> theClassMappings;
		private Map<String, String> theResourceLocations;
		private List<QuickPermission> thePermissions;

		private QuickToolkit theBuiltToolkit;

		private Builder(QuickEnvironment environment, URL location) {
			theEnvironment = environment;
			theLocation = location;
			theClassPaths = new ArrayList<>();
			theDependencies = new ArrayList<>();
			theClassMappings = new LinkedHashMap<>();
			theResourceLocations = new LinkedHashMap<>();
			thePermissions = new ArrayList<>();
		}

		/**
		 * @param name The name for this toolkit
		 * @return This builder, for chaining
		 */
		public Builder setName(String name) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theName = name;
			return this;
		}

		/**
		 * @param descrip The description for this toolkit
		 * @return This builder, for chaining
		 */
		public Builder setDescription(String descrip) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theDescription = descrip;
			return this;
		}

		/**
		 * @param version The version for this toolkit
		 * @return This builder, for chaining
		 */
		public Builder setVersion(Version version) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theVersion = version;
			return this;
		}

		/**
		 * @param classPath The class path to add to this toolkit
		 * @return This builder, for chaining
		 */
		public Builder addClassPath(URL classPath) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theClassPaths.add(classPath);
			return this;
		}

		/**
		 * Adds a toolkit dependency for this toolkit
		 *
		 * @param toolkit The toolkit that this toolkit requires to perform its functionality
		 * @return This builder, for chaining
		 */
		public Builder addDependency(QuickToolkit toolkit) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theDependencies.add(toolkit);
			return this;
		}

		/**
		 * Maps a class to a tag name for this toolkit
		 *
		 * @param tagName The tag name to map the class to
		 * @param className The fully-qualified java class name to map the tag to
		 * @return This builder, for chaining
		 */
		public Builder map(String tagName, String className) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theClassMappings.put(tagName, className);
			return this;
		}

		/**
		 * Maps a resource to a tag name for this toolkit
		 *
		 * @param tagName The tag name to map the resource to
		 * @param resourceLocation The location of the resource to map the tag to
		 * @return This builder, for chaining
		 */
		public Builder mapResource(String tagName, String resourceLocation) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theResourceLocations.put(tagName, resourceLocation);
			return this;
		}

		/**
		 * Adds a permission requirement to this toolkit
		 *
		 * @param perm The permission that this toolkit requires or requests
		 * @return This builder, for chaining
		 */
		public Builder addPermission(QuickPermission perm) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			thePermissions.add(perm);
			return this;
		}

		/** @return The built toolkit */
		public QuickToolkit build() {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The toolkit may only be built once");
			if(theName == null)
				throw new IllegalStateException("No name set");
			if(theDescription == null)
				throw new IllegalStateException("No description set");
			if(theVersion == null)
				throw new IllegalStateException("No version set");

			theBuiltToolkit = new QuickToolkit(theEnvironment, theLocation, theName, theDescription, theVersion, theClassPaths,
				theClassMappings, theResourceLocations, theDependencies, thePermissions);
			return theBuiltToolkit;
		}

		/**
		 * @param styleSheet The style sheet to use with this toolkit
		 * @return This builder, for chaining
		 */
		public Builder addStyleSheet(StyleSheet styleSheet) {
			if(theBuiltToolkit == null)
				throw new IllegalStateException("Styles must be added after the toolkit is built");
			if(!theLocation.equals(QuickEnvironment.CORE_TOOLKIT) && !(styleSheet instanceof ToolkitStyleSheet)) {
				for (StyleAttribute<?> attr : styleSheet.attributes()) {
					if(attr.getDomain().getClass().getClassLoader() == theBuiltToolkit)
						continue;
					for (StyleConditionValue<?> sev : styleSheet.getStyleExpressions(attr)) {
						if (sev.getCondition().getType().getClassLoader() == theBuiltToolkit)
							continue;
						if (checkRole(sev.getCondition().getRole(), sev.getCondition().getParent()))
							continue;
						String msg = "Toolkit " + theLocation + ": Style sheet";
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
			theBuiltToolkit.theStyleDependencyController.add(styleSheet);
			return this;
		}

		private boolean checkRole(AttachPoint<?> role, StyleCondition parent) {
			if(role==null)
				return true;
			if (role.template.getDefiner().getClassLoader() != theBuiltToolkit)
				return false;
			return checkRole(parent.getRole(), parent.getParent());
		}
	}
}
