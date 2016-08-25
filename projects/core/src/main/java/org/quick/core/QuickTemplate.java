package org.quick.core;

import java.lang.reflect.ParameterizedType;
import java.util.*;

import org.observe.DefaultSettableValue;
import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.SettableValue;
import org.observe.collect.ObservableList;
import org.quick.core.layout.SizeGuide;
import org.quick.core.mgr.ElementList;
import org.quick.core.model.QuickAppModel;
import org.quick.core.model.QuickBehavior;
import org.quick.core.model.QuickModelConfig;
import org.quick.core.parser.QuickContent;
import org.quick.core.parser.QuickParseException;
import org.quick.core.parser.SimpleParseEnv;
import org.quick.core.parser.WidgetStructure;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.ImmutableStyle;
import org.quick.core.style.QuickStyle;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleAttributes;
import org.quick.core.tags.ModelAttribute;
import org.quick.core.tags.Template;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

/**
 * Allows complex widgets to be created more easily by addressing a template Quick file with widget definitions that are reproduced in each
 * instance of the widget. The template file also may contain attach point definitions, allowing the widget's content to be specified from
 * the XML invoking the widget.
 */
public abstract class QuickTemplate extends QuickElement {
	/**
	 * An attach point under a template widget
	 *
	 * @param <E> The type of element that belongs in the attach point's place
	 */
	public static class AttachPoint<E extends QuickElement> {
		/** The template structure that this attach point belongs to */
		public final TemplateStructure template;

		/** The widget structure that defined this attach point */
		public final QuickContent source;

		/** The name of the attach point */
		public final String name;

		/** The type of element that may occupy the attach point */
		public final Class<E> type;

		/**
		 * Whether the attach point may be specified externally
		 *
		 * @see TemplateStructure#EXTERNAL
		 */
		public final boolean external;

		/**
		 * Whether the attach point is required to be specified externally
		 *
		 * @see TemplateStructure#REQUIRED
		 */
		public final boolean required;

		/**
		 * Whether the attach point may be specified more than once
		 *
		 * @see TemplateStructure#MULTIPLE
		 */
		public final boolean multiple;

		/**
		 * Whether the attach point is the default attach point for its template structure
		 *
		 * @see TemplateStructure#DEFAULT
		 */
		public final boolean isDefault;

		/**
		 * Whether the attach point specifies an implementation
		 *
		 * @see TemplateStructure#IMPLEMENTATION
		 */
		public final boolean implementation;

		/**
		 * Whether the element or set of elements at the attach point can be changed generically. This only affects the mutability of the
		 * attach point as accessed from the list returned from {@link QuickTemplate#initChildren(List)}.
		 *
		 * @see TemplateStructure#MUTABLE
		 */
		public final boolean mutable;

		AttachPoint(TemplateStructure temp, QuickContent src, String aName, Class<E> aType, boolean ext, boolean req,
			boolean mult, boolean def, boolean impl, boolean isMutable) {
			template = temp;
			source = src;
			name = aName;
			type = aType;
			external = ext;
			required = req;
			multiple = mult;
			isDefault = def;
			implementation = impl;
			mutable = isMutable;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** Represents the structure of a templated widget */
	public static class TemplateStructure implements Iterable<AttachPoint<?>> {
		/** The prefix for template-related attributes in the template file */
		public static final String TEMPLATE_PREFIX = "template-";

		/**
		 * The attribute specifying that an element is an attach point definition. The value of the attribute is the name of the attach
		 * point.
		 */
		public static final String ATTACH_POINT = TEMPLATE_PREFIX + "attach-point";

		/** The name of an element that is an attach point if the type of the attach point is not specified */
		public static final String GENERIC_ELEMENT = TEMPLATE_PREFIX + "element";

		/** The name of an element that is an attach point if the type of the attach point is a text element */
		public static final String GENERIC_TEXT = TEMPLATE_PREFIX + "text";

		/**
		 * The attribute specifying whether an attach point may be specified externally (from the XML invoking the templated widget) or not.
		 * Internal-only attach points may simply be used as binding points, preventing a templated widget implementation from having to
		 * search and find a widget. Default is true.
		 */
		public static final String EXTERNAL = TEMPLATE_PREFIX + "external";

		/**
		 * The attribute specifying whether an attach point MUST be specified externally (from the XML invoking the templated widget).
		 * Default is false.
		 */
		public static final String REQUIRED = TEMPLATE_PREFIX + "required";

		/** The attribute specifying whether multiple widgets may be specified for the attach point. Default is false. */
		public static final String MULTIPLE = TEMPLATE_PREFIX + "multiple";

		/**
		 * The attribute specifying an attach point as the default attach point for the widget. Content in a templated widget's invocation
		 * from XML that does not specify a {@link #role} will be added at the default attach point. At most one default attach point may be
		 * specified for a widget.
		 */
		public static final String DEFAULT = TEMPLATE_PREFIX + "default";

		/**
		 * The attribute specifying that the element defining the attach point also defines a widget that will be placed at the attach point
		 * unless overridden externally. Attach points that specify {@link #MULTIPLE} may not be implementations. Default is false except
		 * for {@link #EXTERNAL internal-only} attach points, which <b>MUST</b> be implementations.
		 */
		public static final String IMPLEMENTATION = TEMPLATE_PREFIX + "implementation";

		/**
		 * The attribute specifying that the element or elements occupying the attach point may be modified dynamically. Default is true.
		 */
		public static final String MUTABLE = TEMPLATE_PREFIX + "mutable";

		/** Specifies behaviors for the templated element only */
		public static final String BEHAVIOR = TEMPLATE_PREFIX + "behavior";

		/** The cache key to use to retrieve instances of {@link TemplateStructure} */
		public static QuickCache.CacheItemType<Class<? extends QuickTemplate>, TemplateStructure, QuickException> TEMPLATE_STRUCTURE_CACHE_TYPE;

		static {
			TEMPLATE_STRUCTURE_CACHE_TYPE = new QuickCache.CacheItemType<Class<? extends QuickTemplate>, QuickTemplate.TemplateStructure, QuickException>() {
				@Override
				public TemplateStructure generate(QuickEnvironment env, Class<? extends QuickTemplate> key) throws QuickException {
					return genTemplateStructure(env, key);
				}

				@Override
				public int size(TemplateStructure value) {
					return 0; // TODO
				}
			};
		}

		/** The property type for the role attribute */
		public static final QuickPropertyType<AttachPoint<?>> roleType = QuickPropertyType.build("role", new TypeToken<AttachPoint<?>>(){})
			.build();

		/** The role attribute, defining how children added to a template are used */
		public static class RoleAttribute extends QuickAttribute<AttachPoint<?>> {
			private final TemplateStructure theTemplate;

			private RoleAttribute(TemplateStructure templateStruct) {
				super("role", roleType, new PropertyValidator<AttachPoint<?>>() {
					@Override
					public boolean isValid(AttachPoint<?> value) {
						return value.template == templateStruct;
					}

					@Override
					public void assertValid(AttachPoint<?> value) throws QuickException {
						if (value.template != templateStruct)
							throw new QuickException(
								"Attach point " + value.name + " from template " + value.template.getDefiner().getName()
									+ " cannot be assigned to a role in template " + templateStruct.getDefiner().getName());
					}
				}, Arrays.asList(str -> ObservableValue.constant(TypeToken.of(AttachPoint.class), templateStruct.getAttachPoint(str))));
				theTemplate = templateStruct;
			}

			/** @return The template structure that this role attribute assigns roles for */
			public TemplateStructure getTemplate() {
				return theTemplate;
			}
		}

		/** The attribute in a child of a template instance which marks the child as replacing an attach point from the definition */
		public final RoleAttribute role = new RoleAttribute(this);

		private final Class<? extends QuickTemplate> theDefiner;
		private final TemplateStructure theSuperStructure;
		private WidgetStructure theWidgetStructure;
		private Map<String, Class<?>> theModelAttributes;
		private AttachPoint<?> theDefaultAttachPoint;
		private Map<String, AttachPoint<?>> theAttachPoints;
		private Map<AttachPoint<?>, QuickContent> theAttachPointWidgets;
		private Map<String, QuickModelConfig> theModels;
		private Class<? extends QuickLayout> theLayoutClass;
		private List<Class<? extends QuickBehavior<?>>> theBehaviors;

		/**
		 * @param definer The templated class that defines the template structure
		 * @param superStructure The parent template structure
		 */
		public TemplateStructure(Class<? extends QuickTemplate> definer, TemplateStructure superStructure) {
			theDefiner = definer;
			theSuperStructure = superStructure;
			theBehaviors = Collections.emptyList();
		}

		/** @param widgetStructure The widget structure specified in the template Quick file */
		private void setWidgetStructure(WidgetStructure widgetStructure) {
			if (theWidgetStructure != null)
				throw new IllegalArgumentException("Widget structure for a template may only be set once");
			theWidgetStructure = widgetStructure;
		}

		/** @param attaches The map of attach points to the widget structure where the attach points point to */
		private void addAttaches(Map<AttachPoint<?>, QuickContent> attaches) {
			Map<String, AttachPoint<?>> attachPoints = new java.util.LinkedHashMap<>(attaches.size());
			AttachPoint<?> defAP = null;
			for (AttachPoint<?> ap : attaches.keySet()) {
				attachPoints.put(ap.name, ap);
				if (ap.isDefault)
					defAP = ap;
			}
			theDefaultAttachPoint = defAP;
			theAttachPoints = Collections.unmodifiableMap(attachPoints);
			theAttachPointWidgets = Collections.unmodifiableMap(attaches);
		}

		private void setModelAttributes(Map<String, Class<?>> atts) {
			theModelAttributes = Collections.unmodifiableMap(atts);
		}

		private void setModels(Map<String, QuickModelConfig> models) {
			theModels = Collections.unmodifiableMap(models);
		}

		private void setBehaviors(List<Class<? extends QuickBehavior<?>>> behaviors) {
			theBehaviors = Collections.unmodifiableList(behaviors);
		}

		/** @return The templated class that defines this template structure */
		public Class<? extends QuickTemplate> getDefiner() {
			return theDefiner;
		}

		/** @return The parent template structure that this structure builds on */
		public TemplateStructure getSuperStructure() {
			return theSuperStructure;
		}

		/** @return The widget structure specified in the template Quick file for this template */
		public WidgetStructure getWidgetStructure() {
			return theWidgetStructure;
		}

		/** @return All attach points in this template structure */
		public Collection<AttachPoint<?>> getAttachPoints() {
			return theAttachPoints.values();
		}

		/**
		 * @param name The name of the attach point to get, or null to get the default attach point
		 * @return The attach point definition with the given name, or the default attach point if name==null, or null if no attach point
		 *         with the given name exists or name==null and this template structure has no default attach point
		 */
		public AttachPoint<?> getAttachPoint(String name) {
			if (name == null)
				return theDefaultAttachPoint;
			return theAttachPoints.get(name);
		}

		/** @return This template structure's default attach point, or null if it does not have one */
		public AttachPoint<?> getDefaultAttachPoint() {
			return theDefaultAttachPoint;
		}

		/**
		 * @param content A piece of this template's {@link #getWidgetStructure() widget structure}
		 * @return The attach point that the content represents
		 */
		public AttachPoint<?> getAttachPoint(QuickContent content) {
			for (Map.Entry<AttachPoint<?>, QuickContent> attach : theAttachPointWidgets.entrySet()) {
				if (attach.getValue() == content)
					return attach.getKey();
			}
			return null;
		}

		/**
		 * @param child The child to get the role for
		 * @return The attach point whose role the child is in, or null if the child is not in a role in this template structure
		 */
		public AttachPoint<?> getRole(QuickElement child) {
			AttachPoint<?> ret = child.atts().get(role);
			if (ret == null)
				ret = theDefaultAttachPoint;
			return ret;
		}

		/**
		 * @param attachPoint The attach point to get the widget structure of
		 * @return The widget structure associated with the given attach point
		 */
		public QuickContent getWidgetStructure(AttachPoint<?> attachPoint) {
			return theAttachPointWidgets.get(attachPoint);
		}

		/** @return The model attributes specified in this template's annotation */
		public Map<String, Class<?>> getModelAttributes() {
			return theModelAttributes;
		}

		/** @return The behaviors that will be installed to instances of this template */
		public List<Class<? extends QuickBehavior<?>>> getBehaviors() {
			return theBehaviors;
		}

		@Override
		public java.util.Iterator<AttachPoint<?>> iterator() {
			return Collections.unmodifiableList(new ArrayList<>(theAttachPoints.values())).listIterator();
		}

		/** @return The type of layout to use for the templated widget */
		public Class<? extends QuickLayout> getLayoutClass() {
			return theLayoutClass;
		}

		/** @return The names of all the models configured in this template */
		public Set<String> getModels() {
			return theModels.keySet();
		}

		/**
		 * @param modelName The name of the model to get
		 * @return The config for the model configured with the given name in this template
		 */
		public QuickModelConfig getModel(String modelName) {
			return theModels.get(modelName);
		}

		@Override
		public String toString() {
			return "Template structure for " + theDefiner.getName();
		}

		/**
		 * Gets a template structure for a template type
		 *
		 * @param env The Quick environment to get the structure within
		 * @param templateType The template type to get the structure for
		 * @return The template structure for the given templated type
		 * @throws QuickException If an error occurs generating the structure
		 */
		public static TemplateStructure getTemplateStructure(QuickEnvironment env, Class<? extends QuickTemplate> templateType)
			throws QuickException {
			return env.getCache().getAndWait(env, TEMPLATE_STRUCTURE_CACHE_TYPE, templateType, true);
		}

		/**
		 * Generates a template structure for a template type
		 *
		 * @param env The Quick environment to generate the structure within
		 * @param templateType The template type to generate the structure for
		 * @return The template structure for the given templated type
		 * @throws QuickException If an error occurs generating the structure
		 */
		private static TemplateStructure genTemplateStructure(QuickEnvironment env, Class<? extends QuickTemplate> templateType)
			throws QuickException {
			if (!QuickTemplate.class.isAssignableFrom(templateType))
				throw new QuickException(
					"Only extensions of " + QuickTemplate.class.getName() + " may have template structures: " + templateType.getName());
			if (templateType == QuickTemplate.class)
				return null;
			Class<? extends QuickTemplate> superType = (Class<? extends QuickTemplate>) templateType.getSuperclass();
			TemplateStructure superStructure = null;
			while (superType != QuickTemplate.class) {
				if (superType.getAnnotation(Template.class) != null) {
					superStructure = getTemplateStructure(env, superType);
					break;
				}
				superType = (Class<? extends QuickTemplate>) superType.getSuperclass();
			}
			Template template = templateType.getAnnotation(Template.class);
			if (template == null) {
				if (superStructure != null)
					return superStructure;
				throw new QuickException("Concrete implementations of " + QuickTemplate.class.getName() + " like " + templateType.getName()
					+ " must be tagged with @" + Template.class.getName() + " or extend a class that does");
			}
			Map<String, Class<?>> modelAtts = new LinkedHashMap<>();
			for (ModelAttribute att : template.attributes()) {
				if (modelAtts.put(att.name(), att.type()) != null)
					throw new QuickException("Multiple model attributes named " + att.name() + " on template " + templateType.getName());
				TemplateStructure superS = superStructure;
				while (superS != null) {
					if (superS.getModel(att.name()) != null)
						throw new QuickException("Model attribute " + att.name() + " on template " + superS.getDefiner().getName()
							+ " cannot be overridden by template " + templateType.getName());
				}
			}

			java.net.URL location;
			try {
				location = QuickUtils.resolveURL(templateType.getResource(templateType.getSimpleName() + ".class"), template.location());
			} catch (QuickException e) {
				throw new QuickException(
					"Could not resolve template path " + template.location() + " for templated widget " + templateType.getName(), e);
			}
			org.quick.core.parser.QuickDocumentStructure docStruct;
			try (java.io.Reader templateReader = new java.io.BufferedReader(new java.io.InputStreamReader(location.openStream()))) {
				QuickClassView classView = new QuickClassView(env, null, (QuickToolkit) templateType.getClassLoader());
				classView.addNamespace("this", (QuickToolkit) templateType.getClassLoader());
				org.quick.core.mgr.MutatingMessageCenter msg = new org.quick.core.mgr.MutatingMessageCenter(env.msg(),
					"Template " + templateType.getName() + ": ", "template", templateType);
				docStruct = env.getDocumentParser().parseDocument(location, templateReader, classView, msg);
			} catch (java.io.IOException e) {
				throw new QuickException(
					"Could not read template resource " + template.location() + " for templated widget " + templateType.getName(), e);
			} catch (QuickParseException e) {
				throw new QuickException(
					"Could not parse template resource " + template.location() + " for templated widget " + templateType.getName(), e);
			}
			if (docStruct.getHead().getTitle() != null)
				env.msg()
					.warn("title specified but ignored in template xml \"" + location + "\" for template class " + templateType.getName());
			if (!docStruct.getHead().getStyleSheets().isEmpty())
				env.msg().warn(docStruct.getHead().getStyleSheets().size() + " style sheet "
						+ (docStruct.getHead().getStyleSheets().size() == 1 ? "" : "s") + " specified but ignored in template xml \""
						+ location + "\" for template class " + templateType.getName());
			LinkedHashMap<String, QuickModelConfig> models = new LinkedHashMap<>();
			for (String modelName : docStruct.getHead().getModelConfigs().keySet())
				models.put(modelName, docStruct.getHead().getModelConfigs().get(modelName));
			if (docStruct.getContent().getChildren().isEmpty())
				throw new QuickException("No contents specified in body section of template XML \"" + location + "\" for template class "
					+ templateType.getName());
			/* This block enforced only one widget at the root of a templated element.  Don't remember why I thought this was a good idea.
			if(docStruct.getContent().getChildren().size() > 1)
				throw new QuickException("More than one content element (" + docStruct.getContent().getChildren().size()
					+ ") specified in body section of template XML \"" + location + "\" for template class " + templateType.getName());
			*/
			for (QuickContent content : docStruct.getContent().getChildren()) {
				if (!(content instanceof WidgetStructure))
					throw new QuickException("Non-widget contents specified in body section of template XML \"" + location
						+ "\" for template class " + templateType.getName());
			}

			WidgetStructure content = docStruct.getContent();
			String layout = content.getAttributes().remove(LayoutContainer.LAYOUT_ATTR.getName());
			TemplateStructure templateStruct = new TemplateStructure(templateType, superStructure);
			templateStruct.setModelAttributes(modelAtts);
			templateStruct.setModels(models);
			String behaviorStr = content.getAttributes().remove(BEHAVIOR);
			if (behaviorStr != null) {
				String[] split = behaviorStr.split("\\s*,\\s*");
				ArrayList<Class<? extends QuickBehavior<?>>> behaviors = new ArrayList<>();
				for (String bStr : split) {
					Class<? extends QuickBehavior<?>> bClass;
					if (bStr.indexOf('.') >= 0)
						try {
							bClass = (Class<? extends QuickBehavior<?>>) QuickBehavior.class
								.asSubclass(templateType.getClassLoader().loadClass(bStr));
						} catch (ClassNotFoundException e) {
							throw new QuickException(
								"Behavior class " + bStr + " not findable from class loader of template type " + templateType.getName());
						}
					else
						try {
							bClass = (Class<? extends QuickBehavior<?>>) docStruct.getContent().getClassView().loadMappedClass(bStr,
								QuickBehavior.class);
						} catch (QuickException e) {
							throw new QuickException("Behavior class " + bStr + " not found for template type " + templateType.getName(),
								e);
						}
					Class<?> behaviorTarget = getBehaviorTarget(bClass);
					if (!behaviorTarget.isAssignableFrom(templateType)) {
						throw new QuickException("Behavior " + bClass.getName() + " targets instances of " + behaviorTarget.getName()
							+ ". It cannot be installed on " + templateType.getName() + " instances");
					}
					behaviors.add(bClass);
				}
				templateStruct.setBehaviors(behaviors);
			}
			if (layout != null) {
				QuickToolkit tk;
				if (templateType.getClassLoader() instanceof QuickToolkit)
					tk = (QuickToolkit) templateType.getClassLoader();
				else
					tk = env.getCoreToolkit();
				try {
					templateStruct.theLayoutClass = new QuickClassView(env, null, tk).loadMappedClass(layout, QuickLayout.class);
				} catch (QuickException e) {
					env.msg().warn(LayoutContainer.LAYOUT_ATTR.getName() + " value \"" + layout + "\" on template body for template class "
						+ templateType.getName() + " cannot be loaded", e);
				}
			}
			Map<AttachPoint<?>, QuickContent> attaches = new HashMap<>();
			try {
				content = (WidgetStructure) pullAttachPoints(templateStruct, content, null, attaches);
			} catch (QuickException e) {
				throw new QuickException("Error in template resource " + template.location() + " for templated widget "
					+ templateType.getName() + ": " + e.getMessage(), e);
			}
			templateStruct.setWidgetStructure(content);
			List<String> defaults = new ArrayList<>();
			for (AttachPoint<?> ap : attaches.keySet())
				if (ap.isDefault)
					defaults.add(ap.name);
			if (defaults.size() > 1)
				throw new QuickException("More than one default attach point " + defaults + " present in template resource "
					+ template.location() + " for templated widget " + templateType.getName());
			templateStruct.addAttaches(attaches);
			return templateStruct;
		}

		private static Class<?> getBehaviorTarget(Class<? extends QuickBehavior<?>> behaviorClass) throws QuickException {
			ParameterizedType targetType = getBehaviorTargetType(behaviorClass);
			if (targetType.getActualTypeArguments()[0] instanceof Class)
				return (Class<?>) targetType.getActualTypeArguments()[0];
			else
				throw new QuickException(QuickBehavior.class + " target type " + targetType.getActualTypeArguments()[0]
					+ " cannot be resolved." + " Define the behavior's target explicitly.");
		}

		private static ParameterizedType getBehaviorTargetType(java.lang.reflect.Type type) {
			if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() instanceof Class
				&& QuickBehavior.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()))
				return (ParameterizedType) type;
			Class<?> clazz;
			if (type instanceof Class)
				clazz = (Class<?>) type;
			else if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() instanceof Class)
				clazz = (Class<?>) ((ParameterizedType) type).getRawType();
			else
				return null;
			for (java.lang.reflect.Type intf : clazz.getGenericInterfaces()) {
				ParameterizedType ret = getBehaviorTargetType(intf);
				if (ret != null)
					return ret;
			}
			return null;
		}

		private static QuickContent pullAttachPoints(TemplateStructure template, WidgetStructure structure, WidgetStructure parent,
			Map<AttachPoint<?>, QuickContent> attaches) throws QuickException {
			WidgetStructure ret;
			if (structure.getTagName().equals(TemplateStructure.GENERIC_TEXT)) {
				if (!structure.getChildren().isEmpty())
					throw new QuickException(structure.getTagName() + " elements may not contain children");
				for (Map.Entry<String, String> att : structure.getAttributes().entrySet()) {
					String attName = att.getKey();
					if (!attName.startsWith(TEMPLATE_PREFIX))
						throw new QuickException(structure.getTagName() + " elements may not have non-template attributes");
				}

				return new org.quick.core.parser.QuickText(parent, "", false);
			} else {
				ret = new WidgetStructure(parent, structure.getClassView(), structure.getNamespace(), structure.getTagName());
				for (Map.Entry<String, String> att : structure.getAttributes().entrySet()) {
					String attName = att.getKey();
					if (!attName.startsWith(TEMPLATE_PREFIX))
						ret.addAttribute(attName, att.getValue());
				}

			}
			for (QuickContent content : structure.getChildren()) {
				if (!(content instanceof WidgetStructure))
					continue;
				WidgetStructure child = (WidgetStructure) content;
				String name = child.getAttributes().get(ATTACH_POINT);
				if (name == null) {
					ret.addChild(pullAttachPoints(template, child, ret, attaches));
					continue;
				}
				if (attaches.containsKey(name))
					throw new QuickException("Duplicate attach points named \"" + name + "\"");
				Class<? extends QuickElement> type;
				if (child.getNamespace() == null && child.getTagName().equals(TemplateStructure.GENERIC_ELEMENT))
					type = QuickElement.class;
				else if (child.getNamespace() == null && child.getTagName().equals(TemplateStructure.GENERIC_TEXT))
					type = QuickTextElement.class;
				else
					try {
						type = child.getClassView().loadMappedClass(child.getNamespace(), child.getTagName(), QuickElement.class);
					} catch (QuickException e) {
						throw new QuickException(
							"Could not load element type \"" + (child.getNamespace() == null ? "" : (child.getNamespace() + ":"))
								+ child.getTagName() + "\" for attach point \"" + name + "\": " + e.getMessage(),
							e);
					}

				for (String attName : child.getAttributes().keySet()) {
					if (attName.startsWith(TEMPLATE_PREFIX) && !attName.equals(ATTACH_POINT) && !attName.equals(EXTERNAL)
						&& !attName.equals(IMPLEMENTATION) && !attName.equals(REQUIRED) && !attName.equals(MULTIPLE)
						&& !attName.equals(DEFAULT) && !attName.equals(MUTABLE))
						throw new QuickException("Template attribute " + attName + " not recognized");
				}

				boolean external = getBoolean(child, EXTERNAL, true, name); // Externally-specifiable by default
				boolean implementation = getBoolean(child, IMPLEMENTATION, !external, name);
				boolean multiple = getBoolean(child, MULTIPLE, false, name);
				boolean required = getBoolean(child, REQUIRED, !implementation && !multiple, name);
				boolean def = getBoolean(child, DEFAULT, false, name);
				boolean mutable = getBoolean(child, MUTABLE, true, name);
				if (!external && (required || multiple || def || !implementation)) {
					throw new QuickException(
						"Non-externally-specifiable attach points (" + name + ") may not be required, default, or allow multiples");
				}
				if (!external && !implementation)
					throw new QuickException("Non-externally-specifiable attach points (" + name + ") must be implementations");
				if (!mutable && !implementation)
					throw new QuickException("Immutable attach points (" + name + ") must be implementations");
				if (!external && multiple)
					throw new QuickException("Non-externally-specifiable attach points (" + name + ") may not allow multiples");
				if (!mutable && multiple)
					throw new QuickException("Immutable attach points (" + name + ") may not allow multiples");
				if (implementation && multiple)
					throw new QuickException("Attach points (" + name + ") that allow multiples cannot be implementations");

				Map<AttachPoint<?>, QuickContent> check = new HashMap<>();
				QuickContent replacement = pullAttachPoints(template, child, ret, check);
				ret.addChild(replacement);

				if (external) {
					if (!check.isEmpty())
						throw new QuickException(
							"Externally-specifiable attach points (" + name + ") may not contain attach points: " + check.keySet());
				} else {
					attaches.putAll(check);
				}
				attaches.put(
					new AttachPoint<>(template, replacement, name, type, external, required, multiple, def, implementation, mutable),
					replacement);
			}
			ret.seal();
			return ret;
		}

		private static boolean getBoolean(WidgetStructure child, String attName, boolean def, String attachPoint) throws QuickException {
			if (!child.getAttributes().containsKey(attName))
				return def;
			else if ("true".equals(child.getAttributes().get(attName)))
				return true;
			else if ("false".equals(child.getAttributes().get(attName)))
				return false;
			else
				throw new QuickException("Attach point \"" + attachPoint + "\" specifies illegal " + attName + " value \""
					+ child.getAttributes().get(attName) + "\"--may be true or false");
		}
	}

	/**
	 * Represents an attach point within a particular widget instance
	 *
	 * @param <E> The type of element that belongs in the attach point's place
	 */
	protected class AttachPointInstance<E extends QuickElement> {
		/** The attach point this instance is for */
		public final AttachPoint<E> attachPoint;

		private final ElementList<?> theParentChildren;

		private final SettableValue<E> theValue;

		private final QuickContainer<E> theContainer;

		AttachPointInstance(AttachPoint<E> ap, QuickTemplate template, ElementList<?> pc) {
			attachPoint = ap;
			theParentChildren = pc;
			if (attachPoint.multiple){
				theValue=null;
				theContainer = new AttachPointInstanceContainer<>(attachPoint, template, theParentChildren);
			} else{
				theValue = new DefaultSettableValue<E>() {
					private Observer<ObservableValueEvent<E>> theController = control(null);

					@Override
					public TypeToken<E> getType() {
						return TypeToken.of(attachPoint.type);
					}

					@Override
					public E get() {
						for (QuickElement el : theParentChildren)
							if (attachPoint.template.getRole(el) == attachPoint)
								return (E) el;
						return null;
					}

					@Override
					public <V extends E> E set(V value, Object cause) throws IllegalArgumentException {
						String err = isEnabled().get();
						if (err == null)
							err = isAcceptable(value);
						if (err != null)
							throw new IllegalArgumentException(err);
						E old = doReplace(value);
						theController.onNext(createChangeEvent(old, value, cause));
						return old;
					}

					@Override
					public <V extends E> String isAcceptable(V value) {
						return isAcceptable(value);
					}

					@Override
					public ObservableValue<String> isEnabled() {
						String msg = null;
						if (!attachPoint.mutable)
							msg = "Attach point " + attachPoint + " cannot be modified";
						return ObservableValue.constant(TypeToken.of(String.class), msg);
					}

					private E doReplace(E el) {
						if (el == null) {
							Iterator<? extends QuickElement> iter = theParentChildren.iterator();
							while (iter.hasNext()) {
								QuickElement ret = iter.next();
								if (attachPoint.template.getRole(ret) == attachPoint) {
									iter.remove();
									return (E) ret;
								}
							}
							return null;
						}

						// Scan the parent children for either the element occupying the attach point (and replace it) or
						// for the first element whose widget template occurs after the attach point declaration (and insert the element
						// before it).
						// If neither occurs, just add the element at the end
						HashSet<QuickElement> postAttachEls = new HashSet<>();
						HashSet<AttachPoint<?>> postAttachAPs = new HashSet<>();
						{
							boolean foundAttach = false;
							for (QuickContent sibling : attachPoint.source.getParent().getChildren()) {
								if (foundAttach) {
									if (sibling instanceof WidgetStructure
										&& ((WidgetStructure) sibling).getAttributes().containsKey(TemplateStructure.ATTACH_POINT)) {
										postAttachAPs.add(attachPoint.template.getAttachPoint(
											((WidgetStructure) sibling).getAttributes().get(TemplateStructure.ATTACH_POINT)));
									} else {
										QuickElement staticEl = theStaticContent.get(sibling);
										if (staticEl != null)
											postAttachEls.add(staticEl);
									}
								} else if (sibling == attachPoint.source)
									foundAttach = true;
							}
						}
						ListIterator<? extends QuickElement> iter = theParentChildren.listIterator();
						while (iter.hasNext()) {
							QuickElement ret = iter.next();
							if (attachPoint.template.getRole(ret) == attachPoint) {
								((ListIterator<QuickElement>) iter).set(el);
								return (E) ret;
							} else {
								boolean postAttach = postAttachEls.contains(ret);
								if (!postAttach && postAttachAPs.contains(ret.atts().get(attachPoint.template.role)))
									postAttach = true;
								if (postAttach) {
									iter.hasPrevious();
									iter.previous();
									((ListIterator<QuickElement>) iter).add(el);
									return null;
								}
							}
						}
						((ListIterator<QuickElement>) iter).add(el);
						return null;
					}
				};
				theContainer = null;
			}
		}

		/** @return The element occupying the attach point */
		public SettableValue<E> getValue() {
			if (attachPoint.multiple)
				throw new IllegalStateException("The " + attachPoint.name + " attach point allows multiple elements");
			return theValue;
		}

		/** @return A container for the elements occupying the multiple-enabled attach point */
		public QuickContainer<E> getContainer() {
			if (!attachPoint.multiple)
				throw new IllegalStateException("The " + attachPoint.name + " attach point does not allow multiple elements");
			return theContainer;
		}
	}

	private TemplateStructure theTemplateStructure;

	/**
	 * The {@link org.quick.core.mgr.AttributeManager#accept(Object, QuickAttribute) wanter} for the {@link TemplateStructure#role role}
	 * attribute on this element's content
	 */
	private final Object theRoleWanter;

	private final Map<AttachPoint<?>, AttachPointInstance<?>> theAttachPoints;

	private final Map<String, Object> theModels;

	private ExpressionContext theContext;

	private final Map<QuickContent, QuickElement> theStaticContent;

	// Valid during initialization only (prior to initChildren())--will be null after that

	private Map<AttachPoint<?>, List<QuickElement>> theAttachmentMappings;

	private Set<QuickElement> theUninitialized;

	private QuickLayout theLayout;

	private List<QuickBehavior<?>> theBehaviors;

	/** Creates a templated widget */
	public QuickTemplate() {
		theRoleWanter = new Object();
		theAttachPoints = new LinkedHashMap<>();
		theModels = new LinkedHashMap<>();
		theStaticContent = new HashMap<>();
		theAttachmentMappings = new HashMap<>();
		theUninitialized = new HashSet<>();
		theBehaviors = new ArrayList<>();

		life().runWhen(() -> {
			try {
				QuickEnvironment env = getDocument().getEnvironment();
				theTemplateStructure = TemplateStructure.getTemplateStructure(env, QuickTemplate.this.getClass());
				DefaultExpressionContext.Builder ctxBuilder = DefaultExpressionContext.build().withParent(super.getContext());
				initModels(theTemplateStructure, ctxBuilder);
			} catch (QuickException e) {
				msg().fatal("Could not generate template structure", e);
			}
		}, QuickConstants.CoreStage.INIT_SELF.toString(), 1);
		life().runWhen(() -> {
			for (Class<? extends QuickBehavior<?>> behaviorClass : theTemplateStructure.getBehaviors()) {
				QuickBehavior<?> behavior;
				try {
					behavior = behaviorClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					msg().error("Could not instantiate behavior " + behaviorClass.getName() + " for templated widget "
						+ theTemplateStructure.getDefiner().getName(), e);
					continue;
				}
				try {
					addBehavior(behavior);
				} catch (RuntimeException e) {
					msg().error("Failed to install behavior " + behaviorClass.getName() + " on templated widget " + getClass().getName(),
						e);
				}
			}
		}, QuickConstants.CoreStage.INIT_CHILDREN.toString(), 1);
	}

	/** @return This template widget's template structure */
	public TemplateStructure getTemplate() {
		return theTemplateStructure;
	}

	/**
	 * @param attach The attach point to get the container for
	 * @return The container of all elements occupying the attach point in this widget instance
	 * @throws IllegalArgumentException If the attach point is not recognized in this templated widget or does not support multiple elements
	 */
	protected <E extends QuickElement> QuickContainer<E> getContainer(AttachPoint<E> attach) throws IllegalArgumentException {
		AttachPointInstance<E> instance = (AttachPointInstance<E>) theAttachPoints.get(attach);
		if (instance == null)
			throw new IllegalArgumentException("Unrecognized attach point: " + attach + " in " + getClass().getName());
		return instance.getContainer();
	}

	/**
	 * @param attach The attach point to get the element at
	 * @return The element attached at the given attach point. May be null.
	 */
	protected <E extends QuickElement> SettableValue<E> getElement(AttachPoint<E> attach) {
		AttachPointInstance<E> instance = (AttachPointInstance<E>) theAttachPoints.get(attach);
		if (instance == null)
			throw new IllegalArgumentException("Unrecognized attach point: " + attach + " in " + getClass().getName());
		return instance.getValue();
	}

	private void initModels(TemplateStructure template, DefaultExpressionContext.Builder ctxBuilder) throws QuickException {
		if (template.getSuperStructure() != null)
			initModels(template.getSuperStructure(), ctxBuilder);

		Map<String, QuickAttribute<?>> atts = new HashMap<>();
		for (QuickAttribute<?> att : atts().attributes()) {
			Class<?> modelAttType = template.getModelAttributes().get(att.getName());
			if (modelAttType != null && modelAttType.isAssignableFrom(att.getType().getType().getRawType()))
				atts.put(att.getName(), att);
		}
		for (String att : template.getModelAttributes().keySet())
			if (!atts.containsKey(att))
				throw new QuickException("Template-declared attribute " + att + ", type " + template.getModelAttributes().get(att).getName()
					+ " has not been accepted in this template widget's attributes");
		for (String modelName : template.getModels()) {
			QuickModelConfig modelConfig = template.getModel(modelName);
			QuickAppModel model = org.quick.core.model.DefaultQuickModel.buildQuickModel(modelConfig,
				getDocument().getEnvironment().getPropertyParser(),
				new SimpleParseEnv(theTemplateStructure.getWidgetStructure().getClassView(), msg(), ctxBuilder.copy().build()));
			theModels.put(modelName, model);
			ctxBuilder.withValue(modelName, ObservableValue.constant(TypeToken.of(QuickAppModel.class), model));
		}
	}

	@Override
	public ExpressionContext getContext() {
		return theContext;
	}

	@Override
	public SizeGuide getWSizer() {
		if (theLayout != null)
			return theLayout.getWSizer(this, getChildren().toArray());
		else
			return super.getWSizer();
	}

	@Override
	public SizeGuide getHSizer() {
		if (theLayout != null)
			return theLayout.getHSizer(this, getChildren().toArray());
		else
			return super.getWSizer();
	}

	@Override
	public void doLayout() {
		if (theLayout != null)
			theLayout.layout(this, getChildren().toArray());
		super.doLayout();
	}

	/**
	 * @param <E> The type of element the behavior applies to
	 * @param behavior The behavior to install in this widget
	 */
	protected <E> void addBehavior(QuickBehavior<E> behavior) {
		behavior.install((E) this);
		theBehaviors.add(behavior);
	}

	/**
	 * @param <E> The type of element the behavior applies to
	 * @param behavior The behavior to uninstall from this widget
	 */
	protected <E> void removeBehavior(QuickBehavior<E> behavior) {
		if (!theBehaviors.remove(behavior))
			throw new IllegalArgumentException("This behavior is not installed on this element");
		behavior.uninstall((E) this);
	}

	/** @return All behaviors installed in this widget */
	protected List<QuickBehavior<?>> getBehaviors() {
		return Collections.unmodifiableList(theBehaviors);
	}

	@Override
	public ElementList<? extends QuickElement> initChildren(List<QuickElement> children) {
		if (theTemplateStructure == null)
			return getChildManager(); // Failed to parse template structure
		if (theAttachmentMappings == null)
			throw new IllegalArgumentException("initChildren() may only be called once on an element");

		/* Initialize this templated widget using theTemplateStructure from the top (direct extension of QuickTemplate2) down
		 * (to this templated class) */
		try {
			initTemplate(theTemplateStructure);
		} catch (QuickParseException e) {
			msg().fatal("Failed to implement widget structure for templated type " + QuickTemplate.this.getClass().getName(), e);
			return getChildManager();
		}

		initExternalChildren(children, theTemplateStructure);

		// Verify we've got all required attach points satisfied, etc.
		if (!verifyTemplateStructure(theTemplateStructure))
			return super.ch();

		initTemplateChildren(this, theTemplateStructure.getWidgetStructure());

		if (theLayout != null)
			theLayout.install(this, Observable.empty);

		// Don't need these anymore
		theAttachmentMappings = null;
		theUninitialized = null;

		return new AttachPointSetChildList();
	}

	private void initTemplate(TemplateStructure structure) throws QuickParseException {
		if (structure.getSuperStructure() != null)
			initTemplate(structure.getSuperStructure());

		if (structure.getLayoutClass() != null) {
			try {
				theLayout = structure.getLayoutClass().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				msg().error("Could not instantiate layout type " + structure.getLayoutClass().getName(), e);
			}
		}

		for (Map.Entry<String, String> att : structure.getWidgetStructure().getAttributes().entrySet()) {
			if (!atts().isSet(att.getKey())) {
				try {
					atts().set(att.getKey(), att.getValue(), this);
				} catch (QuickException e) {
					msg().error("Templated root attribute " + att.getKey() + "=" + att.getValue() + " failed for templated widget "
						+ theTemplateStructure.getDefiner().getName(), e);
				}
			} else if (att.getKey().equals("style")) {
				QuickStyle elStyle = atts().get(StyleAttributes.STYLE_ATTRIBUTE);
				QuickStyle templateStyle;
				ImmutableStyle.Builder builder = ImmutableStyle.build(msg());
				boolean mod = false;
				try {
					templateStyle = StyleAttributes.parseStyle(getDocument().getEnvironment().getPropertyParser(), this, att.getValue());
					for (StyleAttribute<?> styleAtt : templateStyle.attributes()) {
						if (!elStyle.isSet(styleAtt)) {
							mod = true;
							builder.set((StyleAttribute<Object>) styleAtt, (ObservableValue<Object>) templateStyle.get(styleAtt));
						}
					}
					if (mod) {
						for (StyleAttribute<?> styleAtt : elStyle.attributes())
							builder.set((StyleAttribute<Object>) styleAtt, (ObservableValue<Object>) elStyle.get(styleAtt));
						atts().set(StyleAttributes.STYLE_ATTRIBUTE, builder.build());
					}
				} catch (QuickException e) {
					msg().error("Could not parse style attribute of template", e);
				}
			}
		}

		for (QuickContent content : structure.getWidgetStructure().getChildren())
			createTemplateChild(structure, this, content, getDocument().getEnvironment().getContentCreator(), this);
	}

	private QuickElement createTemplateChild(TemplateStructure template, QuickElement parent, QuickContent child,
		org.quick.core.parser.QuickContentCreator creator, QuickParseEnv templateCtx) throws QuickParseException {
		QuickElement ret;
		AttachPoint<?> ap = template.getAttachPoint(child);
		List<QuickElement> mappings = null;
		if (ap != null) {
			mappings = theAttachmentMappings.get(ap);
			if (mappings == null) {
				mappings = new ArrayList<>();
				theAttachmentMappings.put(ap, mappings);
			}
		}
		if (child instanceof WidgetStructure) {
			WidgetStructure cw = (WidgetStructure) child;
			if (cw.getNamespace() == null && cw.getTagName().equals(TemplateStructure.GENERIC_ELEMENT))
				return null;
			if (ap != null) {
				if (!ap.implementation)
					return null;
				WidgetStructure apStruct = (WidgetStructure) child;
				WidgetStructure implStruct = new WidgetStructure(apStruct.getParent(), apStruct.getClassView(), apStruct.getNamespace(),
					apStruct.getTagName());
				for (Map.Entry<String, String> att : apStruct.getAttributes().entrySet()) {
					if (!att.getKey().startsWith(TemplateStructure.TEMPLATE_PREFIX))
						implStruct.addAttribute(att.getKey(), att.getValue());
				}
				for (QuickContent content : apStruct.getChildren())
					implStruct.addChild(content);
				implStruct.seal();
				ret = creator.getChild(parent, implStruct, false);
				ret.atts().accept(theRoleWanter, template.role);
				try {
					ret.atts().set(template.role, ap);
					ret.atts().set(TemplateStructure.IMPLEMENTATION, "true", templateCtx);
				} catch (QuickException e) {
					throw new IllegalStateException("Should not have thrown exception here", e);
				}
			} else
				ret = creator.getChild(parent, child, false);
		} else
			ret = creator.getChild(parent, child, false);
		if (ap != null)
			mappings.add(ret);
		else if (!theStaticContent.containsKey(child))
			theStaticContent.put(child, ret);

		theUninitialized.add(ret);

		if (child instanceof WidgetStructure)
			for (QuickContent sub : ((WidgetStructure) child).getChildren())
				createTemplateChild(template, ret, sub, creator, templateCtx);

		return ret;
	}

	private void initExternalChildren(List<QuickElement> children, TemplateStructure template) {
		AttachPoint<?>[] roles = new AttachPoint[children.size()];
		for (int c = 0; c < children.size(); c++) {
			QuickElement child = children.get(c);
			child.atts().accept(theRoleWanter, template.role);
			roles[c] = child.atts().get(template.role);
			if (roles[c] == null) {
				roles[c] = template.getDefaultAttachPoint();
				try {
					child.atts().set(template.role, roles[c]);
				} catch (QuickException e) {
					throw new IllegalArgumentException("Should not get error here", e);
				}
			}
			if (roles[c] == null) {
				msg().error("No role specified for child of templated widget " + template.getDefiner().getName(), "child", child);
				return;
			}
			if (!roles[c].external) {
				msg().error(
					"Role \"" + roles[c] + "\" is not specifiable externally for templated widget " + template.getDefiner().getName(),
					"child", child);
				return;
			}
			if (!roles[c].type.isInstance(child)) {
				msg().error(
					"Children fulfilling role \"" + roles[c] + "\" in templated widget " + template.getDefiner().getName()
						+ " must be of type " + roles[c].type.getName() + ", not " + child.getClass().getName(),
					"child", child);
				return;
			}
			List<QuickElement> attaches = theAttachmentMappings.get(roles[c]);
			if (attaches == null)
				throw new IllegalStateException(QuickTemplate.class.getSimpleName() + " implementation error: No attachment mappings for "
					+ template.getDefiner().getSimpleName() + "." + roles[c]);
			attaches.clear(); // The given children should replace in-place implementations
		}
		/*child.addListener(QuickConstants.Events.ATTRIBUTE_CHANGED, new org.quick.core.event.AttributeChangedListener<AttachPoint>(
			template.role) {
			private boolean theCallbackLock;

			@Override
			public void attributeChanged(AttributeChangedEvent<AttachPoint> event) {
				if(theCallbackLock)
					return;
				child.msg().error("The " + template.role.getName() + " attribute may not be changed");
				theCallbackLock = true;
				try {
					try {
						child.atts().set(template.role, role);
					} catch(QuickException e) {
						child.msg().error("Should not get an exception here", e);
					}
				} finally {
					theCallbackLock = false;
				}
			}
		});*/
		for (int c = 0; c < children.size(); c++) {
			QuickElement child = children.get(c);
			List<QuickElement> attaches = theAttachmentMappings.get(roles[c]);
			if (!roles[c].multiple && !attaches.isEmpty()) {
				msg().error("Multiple children fulfilling role \"" + roles[c] + "\" in templated widget " + template.getDefiner().getName(),
					"child", child);
				return;
			}
			QuickContent widgetStruct = template.getWidgetStructure(roles[c]);
			if (widgetStruct instanceof WidgetStructure) {
				for (Map.Entry<String, String> attr : ((WidgetStructure) widgetStruct).getAttributes().entrySet()) {
					if (attr.getKey().startsWith(TemplateStructure.TEMPLATE_PREFIX))
						continue;
					try {
						child.atts().set(attr.getKey(), attr.getValue(), child.getParent());
					} catch (QuickException e) {
						child.msg().error(
							"Template-specified attribute " + attr.getKey() + "=" + attr.getValue() + " is not supported by content", e);
					}
				}
			}
			attaches.add(child);
		}
	}

	private boolean verifyTemplateStructure(TemplateStructure struct) {
		boolean ret = true;
		if (struct.getSuperStructure() != null)
			ret &= verifyTemplateStructure(struct.getSuperStructure());
		for (AttachPoint<?> ap : struct)
			ret &= verifyAttachPoint(struct, ap);
		return ret;
	}

	private boolean verifyAttachPoint(TemplateStructure struct, AttachPoint<?> ap) {
		if (ap.required && theAttachmentMappings.get(ap).isEmpty()) {
			msg().error("No widget specified for role " + ap.name + " for template " + struct.getDefiner().getName());
			return false;
		}
		return true;
	}

	private void initTemplateChildren(QuickElement parent, WidgetStructure structure) {
		if (parent != this && !theUninitialized.contains(parent))
			return;
		List<QuickElement> ret = new ArrayList<>();
		List<AttachPoint<?>> attaches = new ArrayList<>();
		for (QuickContent childStruct : structure.getChildren()) {
			AttachPoint<?> ap = theTemplateStructure.getAttachPoint(childStruct);
			if (ap != null) {
				attaches.add(ap);
				for (QuickElement child : theAttachmentMappings.get(ap)) {
					ret.add(child);
					if (childStruct instanceof WidgetStructure)
						initTemplateChildren(child, (WidgetStructure) childStruct);
				}
			} else {
				QuickElement child = theStaticContent.get(childStruct);
				ret.add(child);
				if (childStruct instanceof WidgetStructure)
					initTemplateChildren(child, (WidgetStructure) childStruct);
			}
		}
		try {
			parent.atts().set(TemplateStructure.IMPLEMENTATION, null, this);
		} catch (QuickException e) {
			throw new IllegalStateException("Should not get error here", e);
		}

		ElementList<?> childList;
		if (parent == this)
			childList = super.initChildren(ret);
		else
			childList = parent.initChildren(ret);

		for (AttachPoint<?> attach : attaches)
			theAttachPoints.put(attach, new AttachPointInstance<>(attach, this, childList));
	}

	static String assertFits(AttachPoint<?> attach, QuickElement e) {
		if (e == null) {
			if (attach.required)
				return "Attach point " + attach + " is required--may not be set to null";
			else
				return null;
		}
		if (!attach.type.isInstance(e))
			return e.getClass().getName() + " may not be assigned to attach point " + attach + " (type " + attach.type.getName() + ")";
		return null;
	}

	ObservableList<? extends ObservableList<? extends QuickElement>> getAttachPointContentList(){
		ObservableList<AttachPoint<?>> aps = ObservableList.constant(new TypeToken<AttachPoint<?>>() {},
			new ArrayList<>(theTemplateStructure.getAttachPoints()));
		return aps.map(ap -> {
			ObservableList<? extends QuickElement> contents;
			if(!ap.external)
				contents = ObservableList.constant(TypeToken.of(QuickElement.class));
			else if(ap.multiple){
				contents = getContainer(ap).getContent();
			} else{
				ObservableValue<ObservableList<QuickElement>> el = getElement(ap)
					.mapV(e -> e == null ? ObservableList.constant(TypeToken.of(QuickElement.class))
						: ObservableList.constant(TypeToken.of(QuickElement.class), e));
				contents = ObservableList.flattenValue(el);
			}
			return contents;
		});
	}

	private class AttachPointSetChildList extends ObservableList.FlattenedObservableList<QuickElement> implements ElementList<QuickElement> {
		AttachPointSetChildList() {
			super(getAttachPointContentList());
		}

		@Override
		public QuickElement getParent(){
			return QuickTemplate.this;
		}

		@Override
		public boolean add(QuickElement e) {
			AttachPoint<?> role = e.atts().get(theTemplateStructure.role);
			if (role == null)
				role = theTemplateStructure.getDefaultAttachPoint();
			if (role == null) {
				throw new UnsupportedOperationException("Templated widget " + QuickTemplate.class.getName()
					+ " does not have a default attach point, and therefore does not support addition"
					+ " of children without a role assignment");
			}
			if (!role.external)
				throw new UnsupportedOperationException("The " + role.name + " attach point is not externally-exposed");
			if (!role.mutable)
				throw new UnsupportedOperationException("The " + role.name + " attach point is not mutable");
			if (!role.type.isInstance(e))
				throw new UnsupportedOperationException("The " + role.name + " attach point's elements must be of type "
					+ role.type.getName() + ", not " + e.getClass().getName());
			if (role.multiple)
				return getContainer((AttachPoint<QuickElement>) role).getContent().add(e);
			else {
				SettableValue<QuickElement> apVal = getElement((AttachPoint<QuickElement>) role);
				if (apVal.get() != null)
					throw new UnsupportedOperationException(
						"The " + role.name + " attach point only supports a single element and is already occupied");
				apVal.set(e, null);
				return true;
			}
		}

		@Override
		public boolean remove(Object o) {
			if (!(o instanceof QuickElement))
				return false;
			QuickElement e = (QuickElement) o;
			AttachPoint<?> role = e.atts().get(theTemplateStructure.role);
			if (role == null)
				role = theTemplateStructure.getDefaultAttachPoint();
			if (role == null)
				return false;
			if (!role.type.isInstance(e))
				return false;
			if (!role.external)
				throw new UnsupportedOperationException("The " + role.name + " attach point is not externally-exposed");
			if (!role.mutable)
				throw new UnsupportedOperationException("The " + role.name + " attach point is not mutable");
			if (role.multiple) {
				getContainer(role).getContent().remove(e);
				// TODO move this to AttachPointInstanceElementList
				org.quick.core.mgr.ElementList<? extends QuickElement> content = getContainer(role).getContent();
				if (role.required && content.size() == 1 && content.get(0).equals(e))
					throw new UnsupportedOperationException(
						"The " + role.name + " attach point is required and only has one element left in it");
				return content.remove(e);
			} else {
				SettableValue<QuickElement> apVal = getElement((AttachPoint<QuickElement>) role);
				if (!e.equals(apVal.get()))
					return false;
				if (role.required)
					throw new UnsupportedOperationException("The " + role.name + " attach point is required");
				apVal.set(null, null);
				return true;
			}
		}

		@Override
		public boolean addAll(int index, Collection<? extends QuickElement> c) {
			throw new UnsupportedOperationException("add at index unsupported");
		}

		@Override
		public void clear() {
			for (AttachPoint<?> ap : theTemplateStructure) {
				if (!ap.external)
					continue;
				if (ap.required)
					throw new UnsupportedOperationException("Template has required attach points--can't be cleared");
				if (!ap.mutable) {
					if ((ap.multiple && !getContainer(ap).getContent().isEmpty()) || (!ap.multiple && getElement(ap) != null))
						throw new UnsupportedOperationException("Template has non-empty, immutable attach points--can't be cleared");
				}
			}
			for (AttachPoint<?> ap : theTemplateStructure) {
				if (ap.multiple)
					getContainer(ap).getContent().clear();
				else
					getElement(ap).set(null, null);
			}
		}

		@Override
		public QuickElement set(int index, QuickElement element) {
			QuickElement e = get(index);
			AttachPoint<?> role = e.atts().get(theTemplateStructure.role);
			if (role == null)
				role = theTemplateStructure.getDefaultAttachPoint();
			if (role == null)
				throw new UnsupportedOperationException("Templated widget " + QuickTemplate.class.getName()
					+ " does not have a default attach point, and therefore does not support replacement"
					+ " of children without a role assignment");
			if (!role.external)
				throw new UnsupportedOperationException("The " + role.name + " attach point is not externally-exposed");
			if (!role.mutable)
				throw new UnsupportedOperationException("The " + role.name + " attach point is not mutable");
			if (!role.type.isInstance(e))
				throw new UnsupportedOperationException("The " + role.name + " attach point's elements must be of type "
					+ role.type.getName() + ", not " + e.getClass().getName());
			AttachPoint<?> elRole = element.atts().get(theTemplateStructure.role);
			if (elRole != null && elRole != role)
				throw new UnsupportedOperationException(
					"The role of the element to set (" + elRole + ") is not the same as the element being replaced (" + role + ")");
			if (role.multiple) {
				ElementList<QuickElement> contents = getContainer((AttachPoint<QuickElement>) role).getContent();
				int contentIdx = contents.indexOf(e);
				if (contentIdx < 0)
					throw new UnsupportedOperationException("The contents of the " + role.name
						+ " attach point does not include the element at index " + index + " in the contents list");
				contents.set(contentIdx, element);
			} else {
				SettableValue<QuickElement> apVal = getElement((AttachPoint<QuickElement>) role);
				if (apVal.get() != e)
					throw new UnsupportedOperationException(
						"The " + role.name + " attach point's value is not the element at index " + index);
				apVal.set(element, null);
			}
			return e;
		}

		@Override
		public void add(int index, QuickElement element) {
			throw new UnsupportedOperationException("add at index unsupported");
		}

		@Override
		public QuickElement remove(int index) {
			QuickElement el = get(index);
			remove(el);
			return el;
		}
	}

	private class AttachPointInstanceContainer<E extends QuickElement> implements QuickContainer<E> {
		private AttachPointInstanceElementList<?, E> theContent;

		AttachPointInstanceContainer(AttachPoint<E> ap, QuickTemplate template, ElementList<?> parentChildren) {
			theContent = new AttachPointInstanceElementList<>(ap, template, parentChildren);
		}

		@Override
		public ElementList<E> getContent() {
			return theContent;
		}
	}

	private class AttachPointInstanceElementList<E1 extends QuickElement, E2 extends QuickElement>
		extends ObservableList.DynamicFilteredList<E1, E2> implements ElementList<E2> {
		@SuppressWarnings("unused")
		private final AttachPoint<E2> theAttachPoint;

		AttachPointInstanceElementList(AttachPoint<E2> ap, QuickTemplate template, ElementList<E1> parentChildren) {
			super(parentChildren, TypeToken.of(ap.type), e -> {
				AttachPoint<?> elAp = e.atts().get(ap.template.role);
				FilterMapResult<E2> result = null;
				if (elAp == ap || (ap.isDefault && elAp == null))
					result = new FilterMapResult<>((E2) e, true);
				return result;
			}, e -> (E1) e);
			theAttachPoint = ap;
		}

		@Override
		public QuickElement getParent() {
			return QuickTemplate.this;
		}

		@Override
		public boolean addAll(int index, Collection<? extends E2> c) {
			throw new UnsupportedOperationException("Not implemented");
		}

		@Override
		public E2 set(int index, E2 element) {
			throw new UnsupportedOperationException("Not implemented");
		}

		@Override
		public void add(int index, E2 element) {
			throw new UnsupportedOperationException("Not implemented");
		}

		@Override
		public E2 remove(int index) {
			throw new UnsupportedOperationException("Not implemented");
		}

		@Override
		public boolean add(E2 e) {
			throw new UnsupportedOperationException("Not implemented");
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException("Not implemented");
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("Not implemented");
		}
	}
}
