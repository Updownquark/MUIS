package org.muis.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import org.muis.core.eval.impl.ObservableEvaluator;
import org.muis.core.eval.impl.ObservableItemEvaluator;
import org.muis.core.event.ChildEvent;
import org.muis.core.layout.SizeGuide;
import org.muis.core.mgr.AbstractElementList;
import org.muis.core.mgr.ElementList;
import org.muis.core.mgr.MuisMessageCenter;
import org.muis.core.model.MuisAppModel;
import org.muis.core.model.MuisBehavior;
import org.muis.core.model.MuisValueReferenceParser;
import org.muis.core.parser.DefaultModelValueReferenceParser;
import org.muis.core.parser.MuisContent;
import org.muis.core.parser.MuisParseException;
import org.muis.core.parser.WidgetStructure;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.attach.StyleAttributeType;
import org.muis.core.tags.Template;
import org.muis.util.MuisUtils;
import org.observe.*;
import org.observe.Observer;

import prisms.lang.*;
import prisms.lang.EvaluationEnvironment.VariableImpl;
import prisms.lang.types.ParsedIdentifier;

/**
 * Allows complex widgets to be created more easily by addressing a template MUIS file with widget definitions that are reproduced in each
 * instance of the widget. The template file also may contain attach point definitions, allowing the widget's content to be specified from
 * the XML invoking the widget.
 */
public abstract class MuisTemplate extends MuisElement {
	/** An attach point under a template widget */
	public static class AttachPoint {
		/** The template structure that this attach point belongs to */
		public final TemplateStructure template;

		/** The widget structure that defined this attach point */
		public final MuisContent source;

		/** The name of the attach point */
		public final String name;

		/** The type of element that may occupy the attach point */
		public final Class<? extends MuisElement> type;

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
		 * attach point as accessed from the list returned from {@link MuisTemplate#initChildren(MuisElement[])}.
		 *
		 * @see TemplateStructure#MUTABLE
		 */
		public final boolean mutable;

		/** Whether the immutable implementation in this attach point exposes its attributes through the template widget */
		public final boolean exposeAtts;

		AttachPoint(TemplateStructure temp, MuisContent src, String aName, Class<? extends MuisElement> aType, boolean ext, boolean req,
			boolean mult, boolean def, boolean impl, boolean isMutable, boolean attsExposed) {
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
			exposeAtts = attsExposed;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** Represents the structure of a templated widget */
	public static class TemplateStructure implements Iterable<AttachPoint> {
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

		/** The attribute specifying that the element or elements occupying the attach point may be modified dynamically. Default is true. */
		public static final String MUTABLE = TEMPLATE_PREFIX + "mutable";

		/** Specifies behaviors for the templated element only */
		public static final String BEHAVIOR = TEMPLATE_PREFIX + "behavior";

		/**
		 * The attribute specifying that the element occupying the attach point should expose its attributes through the template widget,
		 * allowing them to be modified from application-level XML. This attribute may only be set on an {@link #MUTABLE immutable} attach
		 * point.
		 */
		public static final String EXPOSE_ATTRIBUTES = TEMPLATE_PREFIX + "expose-atts";

		/** The cache key to use to retrieve instances of {@link TemplateStructure} */
		public static MuisCache.CacheItemType<Class<? extends MuisTemplate>, TemplateStructure, MuisException> TEMPLATE_STRUCTURE_CACHE_TYPE;

		static {
			TEMPLATE_STRUCTURE_CACHE_TYPE = new MuisCache.CacheItemType<Class<? extends MuisTemplate>, MuisTemplate.TemplateStructure, MuisException>() {
				@Override
				public TemplateStructure generate(MuisEnvironment env, Class<? extends MuisTemplate> key) throws MuisException {
					return genTemplateStructure(env, key);
				}

				@Override
				public int size(TemplateStructure value) {
					return 0; // TODO
				}
			};
		}

		/** The attribute type of the templated role attribute */
		public static final class RoleAttributeType implements MuisProperty.PropertyType<AttachPoint> {
			private final TemplateStructure theTemplate;

			RoleAttributeType(TemplateStructure template) {
				theTemplate = template;
			}

			@Override
			public Type getType() {
				return new Type(AttachPoint.class);
			}

			@Override
			public org.observe.ObservableValue<AttachPoint> parse(MuisParseEnv env, String value) throws MuisException {
				AttachPoint ret = theTemplate.getAttachPoint(value);
				if(ret == null)
					throw new MuisException("No such attach point \"" + value + "\" in template " + theTemplate.getDefiner().getName());
				return org.observe.ObservableValue.constant(ret);
			}

			@Override
			public boolean canCast(Type type) {
				return type.canAssignTo(AttachPoint.class);
			}

			@Override
			public <V extends AttachPoint> V cast(Type type, Object value) {
				if(value instanceof AttachPoint)
					return (V) value;
				return null;
			}
		}

		/** The attribute in a child of a template instance which marks the child as replacing an attach point from the definition */
		public final MuisAttribute<AttachPoint> role = new MuisAttribute<>("role", new RoleAttributeType(this));

		private final Class<? extends MuisTemplate> theDefiner;

		private final TemplateStructure theSuperStructure;

		private WidgetStructure theWidgetStructure;

		private AttachPoint theDefaultAttachPoint;

		private Map<String, AttachPoint> theAttachPoints;

		private Map<AttachPoint, MuisContent> theAttachPointWidgets;

		private Map<String, MuisAppModel> theModels;

		private Class<? extends MuisLayout> theLayoutClass;

		private Class<? extends MuisBehavior<?>> [] theBehaviors;

		/**
		 * @param definer The templated class that defines the template structure
		 * @param superStructure The parent template structure
		 */
		public TemplateStructure(Class<? extends MuisTemplate> definer, TemplateStructure superStructure) {
			theDefiner = definer;
			theSuperStructure = superStructure;
			theBehaviors = new Class[0];
		}

		/** @param widgetStructure The widget structure specified in the template MUIS file */
		private void setWidgetStructure(WidgetStructure widgetStructure) {
			if(theWidgetStructure != null)
				throw new IllegalArgumentException("Widget structure for a template may only be set once");
			theWidgetStructure = widgetStructure;
		}

		/** @param attaches The map of attach points to the widget structure where the attach points point to */
		private void addAttaches(Map<AttachPoint, MuisContent> attaches) {
			Map<String, AttachPoint> attachPoints = new java.util.LinkedHashMap<>(attaches.size());
			AttachPoint defAP = null;
			for(AttachPoint ap : attaches.keySet()) {
				attachPoints.put(ap.name, ap);
				if(ap.isDefault)
					defAP = ap;
			}
			theDefaultAttachPoint = defAP;
			theAttachPoints = Collections.unmodifiableMap(attachPoints);
			theAttachPointWidgets = Collections.unmodifiableMap(attaches);
		}

		private void setModels(Map<String, MuisAppModel> models) {
			theModels = Collections.unmodifiableMap(models);
		}

		private void setBehaviors(Class<? extends MuisBehavior<?>> [] behaviors) {
			theBehaviors = behaviors;
		}

		/** @return The templated class that defines this template structure */
		public Class<? extends MuisTemplate> getDefiner() {
			return theDefiner;
		}

		/** @return The parent template structure that this structure builds on */
		public TemplateStructure getSuperStructure() {
			return theSuperStructure;
		}

		/** @return The widget structure specified in the template MUIS file for this template */
		public WidgetStructure getWidgetStructure() {
			return theWidgetStructure;
		}

		/**
		 * @param name The name of the attach point to get, or null to get the default attach point
		 * @return The attach point definition with the given name, or the default attach point if name==null, or null if no attach point
		 *         with the given name exists or name==null and this template structure has no default attach point
		 */
		public AttachPoint getAttachPoint(String name) {
			if(name == null)
				return theDefaultAttachPoint;
			return theAttachPoints.get(name);
		}

		/** @return This template structure's default attach point, or null if it does not have one */
		public AttachPoint getDefaultAttachPoint() {
			return theDefaultAttachPoint;
		}

		/**
		 * @param content A piece of this template's {@link #getWidgetStructure() widget structure}
		 * @return The attach point that the content represents
		 */
		public AttachPoint getAttachPoint(MuisContent content) {
			for(Map.Entry<AttachPoint, MuisContent> attach : theAttachPointWidgets.entrySet()) {
				if(attach.getValue() == content)
					return attach.getKey();
			}
			return null;
		}

		/**
		 * @param child The child to get the role for
		 * @return The attach point whose role the child is in, or null if the child is not in a role in this template structure
		 */
		public AttachPoint getRole(MuisElement child) {
			AttachPoint ret = child.atts().get(role);
			if(ret == null)
				ret = theDefaultAttachPoint;
			return ret;
		}

		/**
		 * @param attachPoint The attach point to get the widget structure of
		 * @return The widget structure associated with the given attach point
		 */
		public MuisContent getWidgetStructure(AttachPoint attachPoint) {
			return theAttachPointWidgets.get(attachPoint);
		}

		/** @return The behaviors that will be installed to instances of this template */
		public Class<? extends MuisBehavior<?>> [] getBehaviors() {
			return theBehaviors.clone();
		}

		@Override
		public java.util.Iterator<AttachPoint> iterator() {
			return Collections.unmodifiableList(new ArrayList<>(theAttachPoints.values())).listIterator();
		}

		/** @return The type of layout to use for the templated widget */
		public Class<? extends MuisLayout> getLayoutClass() {
			return theLayoutClass;
		}

		/** @return The names of all the models configured in this template */
		public String [] getModels() {
			return theModels.keySet().toArray(new String[theModels.size()]);
		}

		/**
		 * @param modelName The name of the model to get
		 * @param msg The message center for reporting errors
		 * @return A copy of the model configured with the given name in this template
		 */
		public MuisAppModel getModel(String modelName, MuisMessageCenter msg) {
			MuisAppModel template = theModels.get(modelName);
			if(template == null)
				return null;
			if(template instanceof org.muis.core.model.DefaultMuisModel) {
				org.muis.core.model.DefaultMuisModel ret = ((org.muis.core.model.DefaultMuisModel) template).clone();
				ret.seal();
				return ret;
			} else {
				boolean wrapped = template instanceof org.muis.core.model.MuisWrappingModel;
				Object toCopy;
				if(wrapped)
					toCopy = ((org.muis.core.model.MuisWrappingModel) template).getWrapped();
				else
					toCopy = template;

				boolean publicClone;
				try {
					publicClone = toCopy instanceof Cloneable
						&& (toCopy.getClass().getMethod("clone").getModifiers() & Modifier.PUBLIC) != 0;
				} catch(NoSuchMethodException | SecurityException e) {
					publicClone = false;
				}
				if(publicClone) {
					try {
						toCopy = toCopy.getClass().getMethod("clone").invoke(toCopy);
					} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
						| SecurityException e) {
						msg.error("Could not duplicate templated model of type " + toCopy.getClass().getName(), e);
						return null;
					}
				} else {
					try {
						toCopy = toCopy.getClass().newInstance();
					} catch(InstantiationException | IllegalAccessException e) {
						msg.error("Could not instantiate templated model of type " + toCopy.getClass().getName(), e);
						return null;
					}
				}
				if(wrapped)
					return new org.muis.core.model.MuisWrappingModel(toCopy, msg);
				else
					return (MuisAppModel) toCopy;
			}
		}

		@Override
		public String toString() {
			return "Template structure for " + theDefiner.getName();
		}

		/**
		 * Gets a template structure for a template type
		 *
		 * @param env The MUIS environment to get the structure within
		 * @param templateType The template type to get the structure for
		 * @return The template structure for the given templated type
		 * @throws MuisException If an error occurs generating the structure
		 */
		public static TemplateStructure getTemplateStructure(MuisEnvironment env, Class<? extends MuisTemplate> templateType)
			throws MuisException {
			return env.getCache().getAndWait(env, TEMPLATE_STRUCTURE_CACHE_TYPE, templateType, true);
		}

		/**
		 * Generates a template structure for a template type
		 *
		 * @param env The MUIS environment to generate the structure within
		 * @param templateType The template type to generate the structure for
		 * @return The template structure for the given templated type
		 * @throws MuisException If an error occurs generating the structure
		 */
		private static TemplateStructure genTemplateStructure(MuisEnvironment env, Class<? extends MuisTemplate> templateType)
			throws MuisException {
			if(!MuisTemplate.class.isAssignableFrom(templateType))
				throw new MuisException("Only extensions of " + MuisTemplate.class.getName() + " may have template structures: "
					+ templateType.getName());
			if(templateType == MuisTemplate.class)
				return null;
			Class<? extends MuisTemplate> superType = (Class<? extends MuisTemplate>) templateType.getSuperclass();
			TemplateStructure superStructure = null;
			while(superType != MuisTemplate.class) {
				if(superType.getAnnotation(Template.class) != null) {
					superStructure = getTemplateStructure(env, superType);
					break;
				}
				superType = (Class<? extends MuisTemplate>) superType.getSuperclass();
			}
			Template template = templateType.getAnnotation(Template.class);
			if(template == null) {
				if(superStructure != null)
					return superStructure;
				throw new MuisException("Concrete implementations of " + MuisTemplate.class.getName() + " like " + templateType.getName()
					+ " must be tagged with @" + Template.class.getName() + " or extend a class that does");
			}

			java.net.URL location;
			try {
				location = MuisUtils.resolveURL(templateType.getResource(templateType.getSimpleName() + ".class"), template.location());
			} catch(MuisException e) {
				throw new MuisException("Could not resolve template path " + template.location() + " for templated widget "
					+ templateType.getName(), e);
			}
			org.muis.core.parser.MuisDocumentStructure docStruct;
			try (java.io.Reader templateReader = new java.io.BufferedReader(new java.io.InputStreamReader(location.openStream()))) {
				MuisClassView classView = new MuisClassView(env, null, (MuisToolkit) templateType.getClassLoader());
				classView.addNamespace("this", (MuisToolkit) templateType.getClassLoader());
				org.muis.core.mgr.MutatingMessageCenter msg = new org.muis.core.mgr.MutatingMessageCenter(env.msg(), "Template "
					+ templateType.getName() + ": ", "template", templateType);
				docStruct = env.getParser().parseDocument(location, templateReader, classView, msg);
			} catch(java.io.IOException e) {
				throw new MuisException("Could not read template resource " + template.location() + " for templated widget "
					+ templateType.getName(), e);
			} catch(MuisParseException e) {
				throw new MuisException("Could not parse template resource " + template.location() + " for templated widget "
					+ templateType.getName(), e);
			}
			if(docStruct.getHead().getTitle() != null)
				env.msg().warn(
					"title specified but ignored in template xml \"" + location + "\" for template class " + templateType.getName());
			if(!docStruct.getHead().getStyleSheets().isEmpty())
				env.msg().warn(
					docStruct.getHead().getStyleSheets().size() + " style sheet "
						+ (docStruct.getHead().getStyleSheets().size() == 1 ? "" : "s") + " specified but ignored in template xml \""
						+ location + "\" for template class " + templateType.getName());
			LinkedHashMap<String, MuisAppModel> models = new LinkedHashMap<>();
			for(String modelName : docStruct.getHead().getModels())
				models.put(modelName, docStruct.getHead().getModel(modelName));
			if(docStruct.getContent().getChildren().isEmpty())
				throw new MuisException("No contents specified in body section of template XML \"" + location + "\" for template class "
					+ templateType.getName());
			/* This block enforced only one widget at the root of a templated element.  Don't remember why I thought this was a good idea.
			if(docStruct.getContent().getChildren().size() > 1)
				throw new MuisException("More than one content element (" + docStruct.getContent().getChildren().size()
					+ ") specified in body section of template XML \"" + location + "\" for template class " + templateType.getName());
			*/
			for(MuisContent content : docStruct.getContent().getChildren()) {
				if(!(content instanceof WidgetStructure))
					throw new MuisException("Non-widget contents specified in body section of template XML \"" + location
						+ "\" for template class " + templateType.getName());
			}

			WidgetStructure content = docStruct.getContent();
			String layout = content.getAttributes().remove(LayoutContainer.LAYOUT_ATTR.getName());
			TemplateStructure templateStruct = new TemplateStructure(templateType, superStructure);
			templateStruct.setModels(models);
			String behaviorStr = content.getAttributes().remove(BEHAVIOR);
			if(behaviorStr != null) {
				String [] split = behaviorStr.split("\\s*,\\s*");
				ArrayList<Class<? extends MuisBehavior<?>>> behaviors = new ArrayList<>();
				for(String bStr : split) {
					Class<? extends MuisBehavior<?>> bClass;
					if(bStr.indexOf('.') >= 0)
						try {
							bClass = (Class<? extends MuisBehavior<?>>) MuisBehavior.class.asSubclass(templateType.getClassLoader()
								.loadClass(bStr));
						} catch(ClassNotFoundException e) {
							throw new MuisException("Behavior class " + bStr + " not findable from class loader of template type "
								+ templateType.getName());
						}
					else
						try {
							bClass = (Class<? extends MuisBehavior<?>>) docStruct.getContent().getClassView()
								.loadMappedClass(bStr, MuisBehavior.class);
						} catch(MuisException e) {
							throw new MuisException("Behavior class " + bStr + " not found for template type " + templateType.getName(), e);
						}
					Class<?> behaviorTarget = getBehaviorTarget(bClass);
					if(!behaviorTarget.isAssignableFrom(templateType)) {
						throw new MuisException("Behavior " + bClass.getName() + " targets instances of " + behaviorTarget.getName()
							+ ". It cannot be installed on " + templateType.getName() + " instances");
					}
					behaviors.add(bClass);
				}
				templateStruct.setBehaviors(behaviors.toArray(new Class[behaviors.size()]));
			}
			if(layout != null) {
				MuisToolkit tk;
				if(templateType.getClassLoader() instanceof MuisToolkit)
					tk = (MuisToolkit) templateType.getClassLoader();
				else
					tk = env.getCoreToolkit();
				try {
					templateStruct.theLayoutClass = new MuisClassView(env, null, tk).loadMappedClass(layout, MuisLayout.class);
				} catch(MuisException e) {
					env.msg().warn(
						LayoutContainer.LAYOUT_ATTR.getName() + " value \"" + layout + "\" on template body for template class "
							+ templateType.getName() + " cannot be loaded", e);
				}
			}
			Map<AttachPoint, MuisContent> attaches = new HashMap<>();
			try {
				content = (WidgetStructure) pullAttachPoints(templateStruct, content, null, attaches);
			} catch(MuisException e) {
				throw new MuisException("Error in template resource " + template.location() + " for templated widget "
					+ templateType.getName() + ": " + e.getMessage(), e);
			}
			templateStruct.setWidgetStructure(content);
			List<String> defaults = new ArrayList<>();
			for(AttachPoint ap : attaches.keySet())
				if(ap.isDefault)
					defaults.add(ap.name);
			if(defaults.size() > 1)
				throw new MuisException("More than one default attach point " + defaults + " present in template resource "
					+ template.location() + " for templated widget " + templateType.getName());
			templateStruct.addAttaches(attaches);
			return templateStruct;
		}

		private static Class<?> getBehaviorTarget(Class<? extends MuisBehavior<?>> behaviorClass) throws MuisException {
			ParameterizedType targetType = getBehaviorTargetType(behaviorClass);
			if(targetType.getActualTypeArguments()[0] instanceof Class)
				return (Class<?>) targetType.getActualTypeArguments()[0];
			else
				throw new MuisException(MuisBehavior.class + " target type " + targetType.getActualTypeArguments()[0]
					+ " cannot be resolved." + " Define the behavior's target explicitly.");
		}

		private static ParameterizedType getBehaviorTargetType(java.lang.reflect.Type type) {
			if(type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() instanceof Class
				&& MuisBehavior.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()))
				return (ParameterizedType) type;
			Class<?> clazz;
			if(type instanceof Class)
				clazz = (Class<?>) type;
			else if(type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() instanceof Class)
				clazz = (Class<?>) ((ParameterizedType) type).getRawType();
			else
				return null;
			for(java.lang.reflect.Type intf : clazz.getGenericInterfaces()) {
				ParameterizedType ret = getBehaviorTargetType(intf);
				if(ret != null)
					return ret;
			}
			return null;
		}

		private static MuisContent pullAttachPoints(TemplateStructure template, WidgetStructure structure, WidgetStructure parent,
			Map<AttachPoint, MuisContent> attaches) throws MuisException {
			WidgetStructure ret;
			if(structure.getTagName().equals(TemplateStructure.GENERIC_TEXT)) {
				if(!structure.getChildren().isEmpty())
					throw new MuisException(structure.getTagName() + " elements may not contain children");
				for(Map.Entry<String, String> att : structure.getAttributes().entrySet()) {
					String attName = att.getKey();
					if(!attName.startsWith(TEMPLATE_PREFIX))
						throw new MuisException(structure.getTagName() + " elements may not have non-template attributes");
				}

				return new org.muis.core.parser.MuisText(parent, "", false);
			} else {
				ret = new WidgetStructure(parent, structure.getClassView(), structure.getNamespace(), structure.getTagName());
				for(Map.Entry<String, String> att : structure.getAttributes().entrySet()) {
					String attName = att.getKey();
					if(!attName.startsWith(TEMPLATE_PREFIX))
						ret.addAttribute(attName, att.getValue());
				}

			}
			for(MuisContent content : structure.getChildren()) {
				if(!(content instanceof WidgetStructure))
					continue;
				WidgetStructure child = (WidgetStructure) content;
				String name = child.getAttributes().get(ATTACH_POINT);
				if(name == null) {
					ret.addChild(pullAttachPoints(template, child, ret, attaches));
					continue;
				}
				if(attaches.containsKey(name))
					throw new MuisException("Duplicate attach points named \"" + name + "\"");
				Class<? extends MuisElement> type;
				if(child.getNamespace() == null && child.getTagName().equals(TemplateStructure.GENERIC_ELEMENT))
					type = MuisElement.class;
				else if(child.getNamespace() == null && child.getTagName().equals(TemplateStructure.GENERIC_TEXT))
					type = MuisTextElement.class;
				else
					try {
						type = child.getClassView().loadMappedClass(child.getNamespace(), child.getTagName(), MuisElement.class);
					} catch(MuisException e) {
						throw new MuisException("Could not load element type \""
							+ (child.getNamespace() == null ? "" : (child.getNamespace() + ":")) + child.getTagName()
							+ "\" for attach point \"" + name + "\": " + e.getMessage(), e);
					}

				for(String attName : child.getAttributes().keySet()) {
					if(attName.startsWith(TEMPLATE_PREFIX) && !attName.equals(ATTACH_POINT) && !attName.equals(EXTERNAL)
						&& !attName.equals(IMPLEMENTATION) && !attName.equals(REQUIRED) && !attName.equals(MULTIPLE)
						&& !attName.equals(DEFAULT) && !attName.equals(MUTABLE) && !attName.equals(EXPOSE_ATTRIBUTES))
						throw new MuisException("Template attribute " + attName + " not recognized");
				}

				boolean external = getBoolean(child, EXTERNAL, true, name); // Externally-specifiable by default
				boolean implementation = getBoolean(child, IMPLEMENTATION, !external, name);
				boolean multiple = getBoolean(child, MULTIPLE, false, name);
				boolean required = getBoolean(child, REQUIRED, !implementation && !multiple, name);
				boolean def = getBoolean(child, DEFAULT, false, name);
				boolean mutable = getBoolean(child, MUTABLE, true, name);
				boolean attsExposed = getBoolean(child, EXPOSE_ATTRIBUTES, false, name);
				if(!external && (required || multiple || def || !implementation)) {
					throw new MuisException("Non-externally-specifiable attach points (" + name
						+ ") may not be required, default, or allow multiples");
				}
				if(!external && !implementation)
					throw new MuisException("Non-externally-specifiable attach points (" + name + ") must be implementations");
				if(!mutable && !implementation)
					throw new MuisException("Immutable attach points (" + name + ") must be implementations");
				if(!external && multiple)
					throw new MuisException("Non-externally-specifiable attach points (" + name + ") may not allow multiples");
				if(!mutable && multiple)
					throw new MuisException("Immutable attach points (" + name + ") may not allow multiples");
				if(implementation && multiple)
					throw new MuisException("Attach points (" + name + ") that allow multiples cannot be implementations");
				if(attsExposed && mutable)
					throw new MuisException("Mutable attach points (" + name + ") may not expose attributes");

				Map<AttachPoint, MuisContent> check = new HashMap<>();
				MuisContent replacement = pullAttachPoints(template, child, ret, check);
				ret.addChild(replacement);

				if(external) {
					if(!check.isEmpty())
						throw new MuisException("Externally-specifiable attach points (" + name + ") may not contain attach points: "
							+ check.keySet());
				} else {
					attaches.putAll(check);
				}
				attaches.put(new AttachPoint(template, replacement, name, type, external, required, multiple, def, implementation, mutable,
					attsExposed), replacement);
			}
			ret.seal();
			return ret;
		}

		private static boolean getBoolean(WidgetStructure child, String attName, boolean def, String attachPoint) throws MuisException {
			if(!child.getAttributes().containsKey(attName))
				return def;
			else if("true".equals(child.getAttributes().get(attName)))
				return true;
			else if("false".equals(child.getAttributes().get(attName)))
				return false;
			else
				throw new MuisException("Attach point \"" + attachPoint + "\" specifies illegal " + attName + " value \""
					+ child.getAttributes().get(attName) + "\"--may be true or false");
		}
	}

	/** Represents an attach point within a particular widget instance */
	protected class AttachPointInstance {
		/** The attach point this instance is for */
		public final AttachPoint attachPoint;

		private final ElementList<?> theParentChildren;

		private final MuisContainer<MuisElement> theContainer;

		AttachPointInstance(AttachPoint ap, MuisTemplate template, ElementList<?> pc) {
			attachPoint = ap;
			theParentChildren = pc;
			if(attachPoint.multiple)
				theContainer = new AttachPointInstanceContainer(attachPoint, template, theParentChildren);
			else
				theContainer = null;
		}

		/** @return The element occupying the attach point */
		public MuisElement getValue() {
			if(attachPoint.multiple)
				throw new IllegalStateException("The " + attachPoint.name + " attach point allows multiple elements");
			for(MuisElement el : theParentChildren)
				if(attachPoint.template.getRole(el) == attachPoint)
					return el;
			return null;
		}

		/**
		 * @param el The element to set as the occupant of the attach point
		 * @return The element that was occupying the attach point before this call
		 * @throws IllegalArgumentException If the given widget cannot occupy the attach point
		 */
		public MuisElement setValue(MuisElement el) throws IllegalArgumentException {
			if(attachPoint.multiple)
				throw new IllegalStateException("The " + attachPoint.name + " attach point allows multiple elements");
			assertFits(attachPoint, el);
			if(el == null) {
				Iterator<? extends MuisElement> iter = theParentChildren.iterator();
				while(iter.hasNext()) {
					MuisElement ret = iter.next();
					if(attachPoint.template.getRole(ret) == attachPoint) {
						iter.remove();
						return ret;
					}
				}
				return null;
			}

			// Scan the parent children for either the element occupying the attach point (and replace it) or
			// for the first element whose widget template occurs after the attach point declaration (and insert the element before it).
			// If neither occurs, just add the element at the end
			HashSet<MuisElement> postAttachEls = new HashSet<>();
			HashSet<AttachPoint> postAttachAPs = new HashSet<>();
			{
				boolean foundAttach = false;
				for(MuisContent sibling : attachPoint.source.getParent().getChildren()) {
					if(foundAttach) {
						if(sibling instanceof WidgetStructure
							&& ((WidgetStructure) sibling).getAttributes().containsKey(TemplateStructure.ATTACH_POINT)) {
							postAttachAPs.add(attachPoint.template.getAttachPoint(((WidgetStructure) sibling).getAttributes().get(
								TemplateStructure.ATTACH_POINT)));
						} else {
							MuisElement staticEl = theStaticContent.get(sibling);
							if(staticEl != null)
								postAttachEls.add(staticEl);
						}
					} else if(sibling == attachPoint.source)
						foundAttach = true;
				}
			}
			ListIterator<? extends MuisElement> iter = theParentChildren.listIterator();
			while(iter.hasNext()) {
				MuisElement ret = iter.next();
				if(attachPoint.template.getRole(ret) == attachPoint) {
					((ListIterator<MuisElement>) iter).set(el);
					return ret;
				} else {
					boolean postAttach = postAttachEls.contains(ret);
					if(!postAttach && postAttachAPs.contains(ret.atts().get(attachPoint.template.role)))
						postAttach = true;
					if(postAttach) {
						iter.hasPrevious();
						iter.previous();
						((ListIterator<MuisElement>) iter).add(el);
						return null;
					}
				}
			}
			((ListIterator<MuisElement>) iter).add(el);
			return null;
		}

		/** @return A container for the elements occupying the multiple-enabled attach point */
		public MuisContainer<MuisElement> getContainer() {
			if(!attachPoint.multiple)
				throw new IllegalStateException("The " + attachPoint.name + " attach point does not allow multiple elements");
			return theContainer;
		}
	}

	/** A parse environment with access to a templated widget's fields */
	public static class TemplateParseEnv implements MuisParseEnv {
		private final MuisTemplate theTemplateWidget;
		private final TemplateStructure theTemplateStruct;

		private DefaultModelValueReferenceParser theModelParser;

		TemplateParseEnv(MuisTemplate template, TemplateStructure templateStruct) {
			theTemplateWidget = template;
			theTemplateStruct = templateStruct;
			// TODO This code is broken now since I've made the model an observable value
			theModelParser = new DefaultModelValueReferenceParser(theTemplateWidget.getValueParser(), null) {
				@Override
				protected void applyModification() {
					super.applyModification();
					// Make evaluation recognize "model"
					if(getEvaluationEnvironment() instanceof prisms.lang.DefaultEvaluationEnvironment) {
						((prisms.lang.DefaultEvaluationEnvironment) getEvaluationEnvironment())
							.addVariableSource(new prisms.lang.VariableSource() {
								@Override
								public Variable [] getDeclaredVariables() {
									Map<String, Variable> vars = new LinkedHashMap<>();
									addVars(theTemplateStruct, vars);
									return vars.values().toArray(new Variable[vars.size()]);
								}

								private void addVars(TemplateStructure struct, Map<String, Variable> vars) {
									for(String model : struct.getModels())
										if(!vars.containsKey(model))
											vars.put(model, new VariableImpl(new Type(MuisAppModel.class), model, true));
									if(struct.getSuperStructure() != null)
										addVars(struct.getSuperStructure(), vars);
								}

								@Override
								public Variable getDeclaredVariable(String name) {
									TemplateStructure struct = theTemplateStruct;
									while(struct != null) {
										if(prisms.util.ArrayUtils.contains(struct.getModels(), name))
											return new VariableImpl(new Type(MuisAppModel.class), name, true);
										struct = struct.getSuperStructure();
									}
									return null;
								}
							});
					}
					ObservableItemEvaluator<? super ParsedIdentifier> oldEval = getEvaluator().getObservableEvaluatorFor(
						ParsedIdentifier.class);
					getEvaluator().addEvaluator(ParsedIdentifier.class, new ObservableItemEvaluator<ParsedIdentifier>() {
						@Override
						public ObservableValue<?> evaluateObservable(ParsedIdentifier item, ObservableEvaluator evaluator,
							EvaluationEnvironment env, boolean asType) throws EvaluationException {
							ObservableValue<MuisAppModel> model = theTemplateWidget.getModel(item.getName());
							if(model != null)
								return model;
							else if(oldEval != null)
								return oldEval.evaluateObservable(item, evaluator, env, asType);
							else
								return null;
						}
					});
				}
			};
		}

		/** @return The templated widget that this environment parses for */
		public MuisTemplate getTemplateWidget() {
			return theTemplateWidget;
		}

		/** @return The template structure that this environment parses for */
		public TemplateStructure getTemplateStructure() {
			return theTemplateStruct;
		}

		@Override
		public MuisClassView cv() {
			return theTemplateWidget.cv();
		}

		@Override
		public MuisMessageCenter msg() {
			return theTemplateWidget.msg();
		}

		@Override
		public MuisValueReferenceParser getValueParser() {
			return theModelParser;
		}
	}

	private TemplateStructure theTemplateStructure;

	/**
	 * The {@link org.muis.core.mgr.AttributeManager#accept(Object, MuisAttribute) wanter} for the {@link TemplateStructure#role role}
	 * attribute on this element's content
	 */
	private final Object theRoleWanter;

	private final Map<AttachPoint, AttachPointInstance> theAttachPoints;

	private final Map<String, MuisAppModel> theModels;
	private final Map<String, DefaultObservableValue<MuisAppModel>> theModelObservables;
	private final Map<String, Observer<ObservableValueEvent<MuisAppModel>>> theModelControllers;

	private final Map<MuisContent, MuisElement> theStaticContent;

	// Valid during initialization only (prior to initChildren())--will be null after that

	private Map<AttachPoint, List<MuisElement>> theAttachmentMappings;

	private Set<MuisElement> theUninitialized;

	private MuisLayout theLayout;

	private List<MuisBehavior<?>> theBehaviors;

	/** Creates a templated widget */
	public MuisTemplate() {
		theRoleWanter = new Object();
		theAttachPoints = new LinkedHashMap<>();
		theModels = new LinkedHashMap<>();
		theModelObservables = new HashMap<>();
		theModelControllers = new HashMap<>();
		theStaticContent = new HashMap<>();
		theAttachmentMappings = new HashMap<>();
		theUninitialized = new HashSet<>();
		theBehaviors = new ArrayList<>();

		life().runWhen(() -> {
			try {
				MuisEnvironment env = getDocument().getEnvironment();
				theTemplateStructure = TemplateStructure.getTemplateStructure(env, MuisTemplate.this.getClass());
				initModels(theTemplateStructure);
			} catch(MuisException e) {
				msg().fatal("Could not generate template structure", e);
			}
		}, MuisConstants.CoreStage.INIT_SELF.toString(), 1);
		life().runWhen(() -> {
			for(Class<? extends MuisBehavior<?>> behaviorClass : theTemplateStructure.getBehaviors()) {
				MuisBehavior<?> behavior;
				try {
					behavior = behaviorClass.newInstance();
				} catch(InstantiationException | IllegalAccessException e) {
					msg().error(
						"Could not instantiate behavior " + behaviorClass.getName() + " for templated widget "
							+ theTemplateStructure.getDefiner().getName(), e);
					continue;
				}
				try {
					addBehavior(behavior);
				} catch(RuntimeException e) {
					msg().error(
						"Failed to install behavior " + behaviorClass.getName() + " on templated widget " + getClass().getName(), e);
				}
			}
		}, MuisConstants.CoreStage.INIT_CHILDREN.toString(), 1);
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
	protected MuisContainer<MuisElement> getContainer(AttachPoint attach) throws IllegalArgumentException {
		AttachPointInstance instance = theAttachPoints.get(attach);
		if(instance == null)
			throw new IllegalArgumentException("Unrecognized attach point: " + attach + " in " + getClass().getName());
		return instance.getContainer();
	}

	/**
	 * @param attach The attach point to get the element at
	 * @return The element attached at the given attach point. May be null.
	 */
	protected MuisElement getElement(AttachPoint attach) {
		AttachPointInstance instance = theAttachPoints.get(attach);
		if(instance == null)
			throw new IllegalArgumentException("Unrecognized attach point: " + attach + " in " + getClass().getName());
		return instance.getValue();
	}

	/**
	 * @param attach The attach point to set the element at
	 * @param element The element to set for the given attach point. May be null if the attach point is not required.
	 * @return The element that occupied the attach point before this call
	 * @throws IllegalArgumentException If the given element may not be set as occupying the given attach point
	 */
	protected MuisElement setElement(AttachPoint attach, MuisElement element) throws IllegalArgumentException {
		AttachPointInstance instance = theAttachPoints.get(attach);
		if(instance == null)
			throw new IllegalArgumentException("Unrecognized attach point: " + attach + " in " + getClass().getName());
		return instance.setValue(element);
	}

	private void initModels(TemplateStructure template) {
		for(String modelName : template.getModels()) {
			DefaultObservableValue<MuisAppModel> modelObs = theModelObservables.get(modelName);
			if(modelObs != null)
				continue; // Model type overridden by subclass
			modelObs = new DefaultObservableValue<MuisAppModel>() {
				@Override
				public Type getType() {
					return new Type(MuisAppModel.class);
				}

				@Override
				public MuisAppModel get() {
					MuisAppModel ret = theModels.get(modelName);
					if(ret == null) {
						ret = template.getModel(modelName, msg());
						theModels.put(modelName, ret);
					}
					return ret;
				}
			};
			theModelObservables.put(modelName, modelObs);
			theModelControllers.put(modelName, modelObs.control(null));
		}

		if(template.getSuperStructure() != null)
			initModels(template.getSuperStructure());
	}

	/**
	 * @param name The name of the model to set for this templated widget
	 * @param model The model to set
	 */
	protected void setModel(String name, Object model) {
		Observer<ObservableValueEvent<MuisAppModel>> controller = (Observer<ObservableValueEvent<MuisAppModel>>) (Observer<?>) theModelControllers
			.get(name);
		if(controller == null)
			throw new IllegalArgumentException("No such model " + name + " in template " + getTemplate().getDefiner().getSimpleName());
		MuisAppModel appModel;
		if(model instanceof MuisAppModel)
			appModel = (MuisAppModel) model;
		else
			appModel = new org.muis.core.model.MuisWrappingModel(model, msg());
		MuisAppModel old = theModels.put(name, appModel);
		controller.onNext(new ObservableValueEvent<>(theModelObservables.get(name), old, appModel, null));
	}

	/**
	 * @param name The name of the model to get
	 * @return The model that this templated widget uses to hook into its component widgets
	 */
	protected ObservableValue<MuisAppModel> getModel(String name) {
		return theModelObservables.get(name);
	}

	@Override
	public SizeGuide getWSizer() {
		if(theLayout != null)
			return theLayout.getWSizer(this, getChildren().toArray());
		else
			return super.getWSizer();
	}

	@Override
	public SizeGuide getHSizer() {
		if(theLayout != null)
			return theLayout.getHSizer(this, getChildren().toArray());
		else
			return super.getWSizer();
	}

	@Override
	public void doLayout() {
		if(theLayout != null)
			theLayout.layout(this, getChildren().toArray());
		super.doLayout();
	}

	/**
	 * @param <E> The type of element the behavior applies to
	 * @param behavior The behavior to install in this widget
	 */
	protected <E> void addBehavior(MuisBehavior<E> behavior) {
		behavior.install((E) this);
		theBehaviors.add(behavior);
	}

	/**
	 * @param <E> The type of element the behavior applies to
	 * @param behavior The behavior to uninstall from this widget
	 */
	protected <E> void removeBehavior(MuisBehavior<E> behavior) {
		if(!theBehaviors.remove(behavior))
			throw new IllegalArgumentException("This behavior is not installed on this element");
		behavior.uninstall((E) this);
	}

	/** @return All behaviors installed in this widget */
	protected List<MuisBehavior<?>> getBehaviors() {
		return Collections.unmodifiableList(theBehaviors);
	}

	@Override
	protected void registerChild(MuisElement child) {
		super.registerChild(child);
		if(theLayout != null)
			theLayout.childAdded(this, child);
	}

	@Override
	protected void unregisterChild(MuisElement child) {
		super.unregisterChild(child);
		if(theLayout != null)
			theLayout.childRemoved(this, child);
	}

	@Override
	public ElementList<? extends MuisElement> initChildren(MuisElement [] children) {
		if(theTemplateStructure == null)
			return getChildManager(); // Failed to parse template structure
		if(theAttachmentMappings == null)
			throw new IllegalArgumentException("initChildren() may only be called once on an element");

		/* Initialize this templated widget using theTemplateStructure from the top (direct extension of MuisTemplate2) down
		 * (to this templated class) */
		try {
			initTemplate(theTemplateStructure);
		} catch(MuisParseException e) {
			msg().fatal("Failed to implement widget structure for templated type " + MuisTemplate.this.getClass().getName(), e);
			return getChildManager();
		}

		initExternalChildren(children, theTemplateStructure);

		// Verify we've got all required attach points satisfied, etc.
		if(!verifyTemplateStructure(theTemplateStructure))
			return super.ch();

		initTemplateChildren(this, theTemplateStructure.getWidgetStructure());

		if(theLayout != null)
			theLayout.initChildren(this, getChildren().toArray());

		// Don't need these anymore
		theAttachmentMappings = null;
		theUninitialized = null;

		return new AttachPointSetChildList();
	}

	private void initTemplate(TemplateStructure structure) throws MuisParseException {
		if(structure.getSuperStructure() != null)
			initTemplate(structure.getSuperStructure());

		if(structure.getLayoutClass() != null) {
			try {
				theLayout = structure.getLayoutClass().newInstance();
			} catch(InstantiationException | IllegalAccessException e) {
				msg().error("Could not instantiate layout type " + structure.getLayoutClass().getName(), e);
			}
		}

		TemplateParseEnv templateCtx = new TemplateParseEnv(this, structure);
		for(Map.Entry<String, String> att : structure.getWidgetStructure().getAttributes().entrySet()) {
			if(!atts().isSet(att.getKey())) {
				try {
					atts().set(att.getKey(), att.getValue(), templateCtx);
				} catch(MuisException e) {
					msg().error(
						"Templated root attribute " + att.getKey() + "=" + att.getValue() + " failed for templated widget "
							+ theTemplateStructure.getDefiner().getName(), e);
				}
			} else if(att.getKey().equals("style")) {
				org.muis.core.style.MuisStyle elStyle = atts().get(StyleAttributeType.STYLE_ATTRIBUTE);
				org.muis.core.style.MuisStyle templateStyle;
				org.muis.core.style.SealableStyle newStyle = new org.muis.core.style.SealableStyle();
				boolean mod = false;
				try {
					templateStyle = StyleAttributeType.parseStyle(this, att.getValue());
					for(StyleAttribute<?> styleAtt : templateStyle.attributes()) {
						if(!elStyle.isSet(styleAtt)) {
							mod = true;
							newStyle.set((StyleAttribute<Object>) styleAtt, templateStyle.get(styleAtt));
						}
					}
					if(mod) {
						for(StyleAttribute<?> styleAtt : elStyle.attributes())
							newStyle.set((StyleAttribute<Object>) styleAtt, elStyle.get(styleAtt));
						atts().set(StyleAttributeType.STYLE_ATTRIBUTE, newStyle);
					}
				} catch(MuisException e) {
					msg().error("Could not parse style attribute of template", e);
				}
			}
		}

		for(MuisContent content : structure.getWidgetStructure().getChildren())
			createTemplateChild(structure, this, content, getDocument().getEnvironment().getContentCreator(), templateCtx);
	}

	private MuisElement createTemplateChild(TemplateStructure template, MuisElement parent, MuisContent child,
		org.muis.core.parser.MuisContentCreator creator, MuisParseEnv templateCtx) throws MuisParseException {
		MuisElement ret;
		AttachPoint ap = template.getAttachPoint(child);
		List<MuisElement> mappings = null;
		if(ap != null) {
			mappings = theAttachmentMappings.get(ap);
			if(mappings == null) {
				mappings = new ArrayList<>();
				theAttachmentMappings.put(ap, mappings);
			}
		}
		if(child instanceof WidgetStructure) {
			WidgetStructure cw = (WidgetStructure) child;
			if(cw.getNamespace() == null && cw.getTagName().equals(TemplateStructure.GENERIC_ELEMENT))
				return null;
			if(ap != null) {
				if(!ap.implementation)
					return null;
				WidgetStructure apStruct = (WidgetStructure) child;
				WidgetStructure implStruct = new WidgetStructure(apStruct.getParent(), apStruct.getClassView(), apStruct.getNamespace(),
					apStruct.getTagName());
				for(Map.Entry<String, String> att : apStruct.getAttributes().entrySet()) {
					if(!att.getKey().startsWith(TemplateStructure.TEMPLATE_PREFIX))
						implStruct.addAttribute(att.getKey(), att.getValue());
				}
				for(MuisContent content : apStruct.getChildren())
					implStruct.addChild(content);
				implStruct.seal();
				ret = creator.getChild(parent, implStruct, false);
				ret.atts().accept(theRoleWanter, template.role);
				try {
					ret.atts().set(template.role, ap);
					ret.atts().set(TemplateStructure.IMPLEMENTATION, "true", templateCtx);
				} catch(MuisException e) {
					throw new IllegalStateException("Should not have thrown exception here", e);
				}
			} else
				ret = creator.getChild(parent, child, false);
		} else
			ret = creator.getChild(parent, child, false);
		if(ap != null)
			mappings.add(ret);
		else if(!theStaticContent.containsKey(child))
			theStaticContent.put(child, ret);

		theUninitialized.add(ret);

		if(child instanceof WidgetStructure)
			for(MuisContent sub : ((WidgetStructure) child).getChildren())
				createTemplateChild(template, ret, sub, creator, templateCtx);

		return ret;
	}

	private void initExternalChildren(MuisElement [] children, TemplateStructure template) {
		AttachPoint [] roles = new AttachPoint[children.length];
		for(int c = 0; c < children.length; c++) {
			children[c].atts().accept(theRoleWanter, template.role);
			roles[c] = children[c].atts().get(template.role);
			if(roles[c] == null) {
				roles[c] = template.getDefaultAttachPoint();
				try {
					children[c].atts().set(template.role, roles[c]);
				} catch(MuisException e) {
					throw new IllegalArgumentException("Should not get error here", e);
				}
			}
			if(roles[c] == null) {
				msg().error("No role specified for child of templated widget " + template.getDefiner().getName(), "child", children[c]);
				return;
			}
			if(!roles[c].external) {
				msg().error(
					"Role \"" + roles[c] + "\" is not specifiable externally for templated widget " + template.getDefiner().getName(),
					"child", children[c]);
				return;
			}
			if(!roles[c].type.isInstance(children[c])) {
				msg()
					.error(
						"Children fulfilling role \"" + roles[c] + "\" in templated widget " + template.getDefiner().getName()
							+ " must be of type " + roles[c].type.getName() + ", not " + children[c].getClass().getName(), "child",
						children[c]);
				return;
			}
			List<MuisElement> attaches = theAttachmentMappings.get(roles[c]);
			if(attaches == null)
				throw new IllegalStateException(MuisTemplate.class.getSimpleName() + " implementation error: No attachment mappings for "
					+ template.getDefiner().getSimpleName() + "." + roles[c]);
			attaches.clear(); // The given children should replace in-place implementations
		}
		/*child.addListener(MuisConstants.Events.ATTRIBUTE_CHANGED, new org.muis.core.event.AttributeChangedListener<AttachPoint>(
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
					} catch(MuisException e) {
						child.msg().error("Should not get an exception here", e);
					}
				} finally {
					theCallbackLock = false;
				}
			}
		});*/
		for(int c = 0; c < children.length; c++) {
			List<MuisElement> attaches = theAttachmentMappings.get(roles[c]);
			if(!roles[c].multiple && !attaches.isEmpty()) {
				msg().error(
					"Multiple children fulfilling role \"" + roles[c] + "\" in templated widget " + template.getDefiner().getName(),
					"child", children[c]);
				return;
			}
			MuisContent widgetStruct = template.getWidgetStructure(roles[c]);
			if(widgetStruct instanceof WidgetStructure) {
				for(Map.Entry<String, String> attr : ((WidgetStructure) widgetStruct).getAttributes().entrySet()) {
					if(attr.getKey().startsWith(TemplateStructure.TEMPLATE_PREFIX))
						continue;
					try {
						children[c].atts().set(attr.getKey(), attr.getValue(), children[c].getParent());
					} catch(MuisException e) {
						children[c].msg().error(
							"Template-specified attribute " + attr.getKey() + "=" + attr.getValue() + " is not supported by content", e);
					}
				}
			}
			attaches.add(children[c]);
		}
	}

	private boolean verifyTemplateStructure(TemplateStructure struct) {
		boolean ret = true;
		if(struct.getSuperStructure() != null)
			ret &= verifyTemplateStructure(struct.getSuperStructure());
		for(AttachPoint ap : struct)
			ret &= verifyAttachPoint(struct, ap);
		return ret;
	}

	private boolean verifyAttachPoint(TemplateStructure struct, AttachPoint ap) {
		if(ap.required && theAttachmentMappings.get(ap).isEmpty()) {
			msg().error("No widget specified for role " + ap.name + " for template " + struct.getDefiner().getName());
			return false;
		}
		return true;
	}

	private void initTemplateChildren(MuisElement parent, WidgetStructure structure) {
		if(parent != this && !theUninitialized.contains(parent))
			return;
		List<MuisElement> ret = new ArrayList<>();
		List<AttachPoint> attaches = new ArrayList<>();
		for(MuisContent childStruct : structure.getChildren()) {
			AttachPoint ap = theTemplateStructure.getAttachPoint(childStruct);
			if(ap != null) {
				attaches.add(ap);
				for(MuisElement child : theAttachmentMappings.get(ap)) {
					if(ap.exposeAtts)
						new org.muis.util.MuisAttributeExposer(this, child, msg());
					ret.add(child);
					if(childStruct instanceof WidgetStructure)
						initTemplateChildren(child, (WidgetStructure) childStruct);
				}
			} else {
				MuisElement child = theStaticContent.get(childStruct);
				ret.add(child);
				if(childStruct instanceof WidgetStructure)
					initTemplateChildren(child, (WidgetStructure) childStruct);
			}
		}
		MuisElement [] children = ret.toArray(new MuisElement[ret.size()]);

		try {
			parent.atts().set(TemplateStructure.IMPLEMENTATION, null, this);
		} catch(MuisException e) {
			throw new IllegalStateException("Should not get error here", e);
		}

		ElementList<?> childList;
		if(parent == this)
			childList = super.initChildren(children);
		else
			childList = parent.initChildren(children);

		for(AttachPoint attach : attaches)
			theAttachPoints.put(attach, new AttachPointInstance(attach, this, childList));
	}

	static void assertFits(AttachPoint attach, MuisElement e) {
		if(e == null) {
			if(attach.required)
				throw new IllegalArgumentException("Attach point " + attach + " is required--may not be set to null");
			return;
		}
		if(!attach.type.isInstance(e))
			throw new IllegalArgumentException(e.getClass().getName() + " may not be assigned to attach point " + attach + " (type "
				+ attach.type.getName() + ")");
	}

	private class AttachPointSetChildList extends AbstractElementList<MuisElement> {
		AttachPointSetChildList() {
			super(MuisTemplate.this);
		}

		@Override
		public Subscription subscribe(Observer<? super ChildEvent> observer) {
			// TODO Listeners not supported yet
			return () -> {
			};
		}

		@Override
		public int size() {
			int ret = 0;
			for(@SuppressWarnings("unused")
			MuisElement item : this)
				ret++;
			return ret;
		}

		@Override
		public AttachPointSetIterator iterator() {
			return listIterator();
		}

		@Override
		public MuisElement [] toArray() {
			ArrayList<MuisElement> ret = new ArrayList<>();
			for(MuisElement item : this)
				ret.add(item);
			return ret.toArray(new MuisElement[ret.size()]);
		}

		@Override
		public boolean add(MuisElement e) {
			AttachPoint role = e.atts().get(theTemplateStructure.role);
			if(role == null)
				role = theTemplateStructure.getDefaultAttachPoint();
			if(role == null) {
				throw new UnsupportedOperationException("Templated widget " + MuisTemplate.class.getName()
					+ " does not have a default attach point, and therefore does not support addition"
					+ " of children without a role assignment");
			}
			if(!role.external)
				throw new UnsupportedOperationException("The " + role.name + " attach point is not externally-exposed");
			if(!role.mutable)
				throw new UnsupportedOperationException("The " + role.name + " attach point is not mutable");
			if(!role.type.isInstance(e))
				throw new UnsupportedOperationException("The " + role.name + " attach point's elements must be of type "
					+ role.type.getName() + ", not " + e.getClass().getName());
			if(role.multiple)
				return getContainer(role).getContent().add(e);
			else {
				if(getElement(role) != null)
					throw new UnsupportedOperationException("The " + role.name
						+ " attach point only supports a single element and is already occupied");
				setElement(role, e);
				return true;
			}
		}

		@Override
		public boolean remove(Object o) {
			if(!(o instanceof MuisElement))
				return false;
			MuisElement e = (MuisElement) o;
			AttachPoint role = e.atts().get(theTemplateStructure.role);
			if(role == null)
				role = theTemplateStructure.getDefaultAttachPoint();
			if(role == null)
				return false;
			if(!role.type.isInstance(e))
				return false;
			if(!role.external)
				throw new UnsupportedOperationException("The " + role.name + " attach point is not externally-exposed");
			if(!role.mutable)
				throw new UnsupportedOperationException("The " + role.name + " attach point is not mutable");
			if(role.multiple) {
				org.muis.core.mgr.ElementList<? extends MuisElement> content = getContainer(role).getContent();
				if(role.required && content.size() == 1 && content.get(0).equals(e))
					throw new UnsupportedOperationException("The " + role.name
						+ " attach point is required and only has one element left in it");
				return content.remove(e);
			} else {
				if(!e.equals(getElement(role)))
					return false;
				if(role.required)
					throw new UnsupportedOperationException("The " + role.name + " attach point is required");
				setElement(role, null);
				return true;
			}
		}

		@Override
		public boolean addAll(Collection<? extends MuisElement> c) {
			boolean ret = false;
			for(MuisElement item : c)
				ret |= add(item);
			return ret;
		}

		@Override
		public boolean addAll(int index, Collection<? extends MuisElement> c) {
			for(MuisElement child : c) {
				add(index, child);
				index++;
			}
			return c.size() > 0;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			boolean ret = false;
			for(Object item : c)
				ret |= remove(item);
			return ret;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean ret = false;
			Iterator<MuisElement> iter = iterator();
			while(iter.hasNext()) {
				if(!c.contains(iter.next())) {
					iter.remove();
					ret = true;
				}
			}
			return ret;
		}

		@Override
		public void clear() {
			for(AttachPoint ap : theTemplateStructure) {
				if(!ap.external)
					continue;
				if(ap.required)
					throw new UnsupportedOperationException("Template has required attach points--can't be cleared");
				if(!ap.mutable) {
					if((ap.multiple && !getContainer(ap).getContent().isEmpty()) || (!ap.multiple && getElement(ap) != null))
						throw new UnsupportedOperationException("Template has non-empty, immutable attach points--can't be cleared");
				}
			}
			for(AttachPoint ap : theTemplateStructure) {
				if(ap.multiple)
					getContainer(ap).getContent().clear();
				else
					setElement(ap, null);
			}
		}

		@Override
		public MuisElement get(int index) {
			if(index < 0)
				throw new IndexOutOfBoundsException("" + index);
			int origIndex = index;
			int length = 0;
			for(MuisElement item : this) {
				length++;
				if(index == 0)
					return item;
				index--;
			}
			throw new IndexOutOfBoundsException(origIndex + " out of " + length);
		}

		@Override
		public MuisElement set(int index, MuisElement element) {
			if(index < 0)
				throw new IndexOutOfBoundsException("" + index);
			ListIterator<MuisElement> iter = listIterator();
			MuisElement ret = null;
			int i = 0;
			for(i = 0; i <= index && iter.hasNext(); i++)
				ret = iter.next();
			if(i == index + 1) {
				iter.set(element);
				return ret;
			}
			throw new IndexOutOfBoundsException(index + " out of " + (i - 1));
		}

		@Override
		public void add(int index, MuisElement element) {
			if(index < 0)
				throw new IndexOutOfBoundsException("" + index);
			ListIterator<MuisElement> iter = listIterator();
			int i = 0;
			for(i = 0; i < index && iter.hasNext(); i++)
				iter.next();
			if(i == index) {
				iter.add(element);
				return;
			}
			throw new IndexOutOfBoundsException(index + " out of " + i);
		}

		@Override
		public MuisElement remove(int index) {
			if(index < 0)
				throw new IndexOutOfBoundsException("" + index);
			ListIterator<MuisElement> iter = listIterator();
			MuisElement ret = null;
			int i = 0;
			for(i = 0; i <= index && iter.hasNext(); i++)
				ret = iter.next();
			if(i == index + 1) {
				iter.remove();
				return ret;
			}
			throw new IndexOutOfBoundsException(index + " out of " + (i - 1));
		}

		@Override
		public AttachPointSetIterator listIterator() {
			return new AttachPointSetIterator();
		}

		@Override
		public ListIterator<MuisElement> listIterator(int index) {
			ListIterator<MuisElement> ret = listIterator();
			int i;
			for(i = 0; i < index && ret.hasNext(); i++)
				ret.next();
			if(i == index)
				return ret;
			throw new IndexOutOfBoundsException(index + " out of " + i);
		}

		@Override
		public List<MuisElement> subList(int fromIndex, int toIndex) {
			return new org.muis.core.mgr.SubList<>(this, fromIndex, toIndex);
		}

		@Override
		public boolean addAll(MuisElement [] children) {
			return addAll(Arrays.asList(children));
		}

		@Override
		public boolean addAll(int index, MuisElement [] children) {
			return addAll(index, Arrays.asList(children));
		}

		/** Iterates over the elements in the set this templated element's external attach points */
		private class AttachPointSetIterator implements ListIterator<MuisElement> {
			private ListIterator<AttachPoint> theAPIter = (ListIterator<AttachPoint>) theTemplateStructure.iterator();

			private AttachPoint theLastAttachPoint;

			private ListIterator<? extends MuisElement> theAPContainer;

			private MuisElement theAPElement;

			private boolean calledHasNext = false;

			private boolean calledHasPrevious = false;

			private int theDirection;

			private int theIndex;

			@Override
			public boolean hasNext() {
				if(calledHasNext)
					return true;
				calledHasNext = true;
				calledHasPrevious = false;
				while((theAPContainer == null || !theAPContainer.hasNext()) && theAPElement == null && theAPIter.hasNext()) {
					theAPContainer = null;
					theLastAttachPoint = theAPIter.next();
					if(theLastAttachPoint.external) {
						if(theLastAttachPoint.multiple)
							theAPContainer = getContainer(theLastAttachPoint).getContent().listIterator();
						else
							theAPElement = getElement(theLastAttachPoint);
					}
				}
				return (theAPContainer != null && theAPContainer.hasNext()) || theAPElement != null;
			}

			@Override
			public MuisElement next() {
				if(!calledHasNext && !hasNext())
					throw new java.util.NoSuchElementException();
				theIndex++;
				theDirection = 1;
				calledHasNext = false;
				calledHasPrevious = false;
				if(theAPContainer != null)
					return theAPContainer.next();
				else {
					MuisElement ret = theAPElement;
					theAPElement = null;
					return ret;
				}
			}

			/** @return The attach point that the last returned element belongs to */
			@SuppressWarnings("unused")
			AttachPoint getLastAttachPoint() {
				return theLastAttachPoint;
			}

			@Override
			public boolean hasPrevious() {
				if(calledHasPrevious)
					return true;
				calledHasPrevious = true;
				calledHasNext = false;
				while((theAPContainer == null || !theAPContainer.hasPrevious()) && theAPElement == null && theAPIter.hasPrevious()) {
					theAPContainer = null;
					theLastAttachPoint = theAPIter.previous();
					if(theLastAttachPoint.external) {
						if(theLastAttachPoint.multiple)
							theAPContainer = getContainer(theLastAttachPoint).getContent().listIterator();
						else
							theAPElement = getElement(theLastAttachPoint);
					}
				}
				return (theAPContainer != null && theAPContainer.hasPrevious()) || theAPElement != null;
			}

			@Override
			public MuisElement previous() {
				if(!calledHasPrevious && !hasPrevious())
					throw new java.util.NoSuchElementException();
				theDirection = -1;
				calledHasNext = false;
				calledHasPrevious = false;
				if(theAPContainer != null)
					return theAPContainer.previous();
				else {
					MuisElement ret = theAPElement;
					theAPElement = null;
					return ret;
				}
			}

			@Override
			public int nextIndex() {
				return theIndex;
			}

			@Override
			public int previousIndex() {
				return theIndex - 1;
			}

			@Override
			public void set(MuisElement e) {
				if(theDirection == 0)
					throw new IllegalStateException("next() or previous() must be called prior to calling set");
				if(!theLastAttachPoint.mutable)
					throw new UnsupportedOperationException("The " + theLastAttachPoint.name + " attach point is not mutable");
				if(!theLastAttachPoint.type.isInstance(e))
					throw new UnsupportedOperationException("The " + theLastAttachPoint.name
						+ " attach points only supports elements of type " + theLastAttachPoint.type.getName() + ", not "
						+ e.getClass().getName());
				if(theAPContainer != null)
					((ListIterator<MuisElement>) theAPContainer).set(e);
				else
					setElement(theLastAttachPoint, e);
			}

			@Override
			public void add(MuisElement e) {
				if(theLastAttachPoint == null) {
					AttachPointSetChildList.this.add(theIndex, e);
					return;
				}
				if(!theLastAttachPoint.mutable)
					throw new UnsupportedOperationException("The " + theLastAttachPoint.name + " attach point is not mutable");
				if(!theLastAttachPoint.multiple)
					throw new UnsupportedOperationException("The " + theLastAttachPoint.name
						+ " attach point does not support multiple elements");
				if(!theLastAttachPoint.type.isInstance(e))
					throw new UnsupportedOperationException("The " + theLastAttachPoint.name
						+ " attach points only supports elements of type " + theLastAttachPoint.type.getName() + ", not "
						+ e.getClass().getName());
				((ListIterator<MuisElement>) theAPContainer).add(e);
			}

			@Override
			public void remove() {
				if(theDirection == 0)
					throw new IllegalStateException("next() or previous() must be called prior to calling remove");
				if(!theLastAttachPoint.mutable)
					throw new UnsupportedOperationException("The " + theLastAttachPoint.name + " attach point is not mutable");
				if(theAPContainer != null)
					theAPContainer.remove();
				else
					setElement(theLastAttachPoint, null);
				if(theDirection > 0)
					theIndex--;
				else
					theIndex++;
			}
		}
	}

	private static class AttachPointInstanceContainer implements MuisContainer<MuisElement> {
		private AttachPointInstanceElementList theContent;

		AttachPointInstanceContainer(AttachPoint ap, MuisTemplate template, ElementList<?> parentChildren) {
			theContent = new AttachPointInstanceElementList(ap, template, parentChildren);
		}

		@Override
		public ElementList<MuisElement> getContent() {
			return theContent;
		}
	}

	private static class AttachPointInstanceElementList extends org.muis.util.FilteredElementList<MuisElement> {
		private final AttachPoint theAttach;

		AttachPointInstanceElementList(AttachPoint ap, MuisTemplate template, ElementList<?> parentChildren) {
			super(template, parentChildren);
			theAttach = ap;
		}

		@Override
		protected boolean filter(MuisElement element) {
			return theAttach.template.getRole(element) == theAttach;
		}

		@Override
		protected void assertMutable() {
			if(!theAttach.mutable)
				throw new UnsupportedOperationException("Attach point " + theAttach + " of template "
					+ theAttach.template.getDefiner().getName() + " is not mutable");
		}

		@Override
		protected void assertFits(MuisElement e) {
			if(e == null)
				throw new IllegalArgumentException("Cannot add null elements to an element container");
			MuisTemplate.assertFits(theAttach, e);
			if(contains(e))
				throw new IllegalArgumentException("Element is already in this container");
		}

		@Override
		protected MuisElement [] newArray(int size) {
			return new MuisElement[size];
		}
	}
}
