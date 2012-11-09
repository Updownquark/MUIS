package org.muis.core;

import java.util.*;

import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.event.MuisEventListener;
import org.muis.core.event.MuisEventType;
import org.muis.core.parser.MuisContent;
import org.muis.core.parser.MuisParseException;
import org.muis.core.parser.WidgetStructure;
import org.muis.core.tags.Template;

/**
 * Allows complex widgets to be created more easily by addressing a template MUIS file with widget definitions that are reproduced in each
 * instance of the widget. The template file also may contain attach point definitions, allowing the widget's content to be specified from
 * the XML invoking the widget.
 */
public abstract class MuisTemplate2 extends MuisElement {
	/** The attribute in a child of a template instance which marks the child as replacing an attach point from the definition */
	public static final MuisAttribute<String> ROLE = new MuisAttribute<String>("role", MuisProperty.stringAttr);

	/** The prefix for template-related attributes in the template file */
	public static final String TEMPLATE_PREFIX = "template";

	/** The attribute specifying that an element is an attach point definition. The value of the attribute is the name of the attach point. */
	public static final String ATTACH_POINT = TEMPLATE_PREFIX + "attach-point";

	/**
	 * The attribute specifying whether an attach point may be specified externally (from the XML invoking the templated widget) or not.
	 * Internal-only attach points may simply be used as binding points, preventing a templated widget implementation from having to search
	 * and find a widget. Default is true.
	 */
	public static final String EXTERNAL = TEMPLATE_PREFIX + "external";

	/**
	 * The attribute specifying whether an attach point MUST be specified externally (from the XML invoking the templated widget). Default
	 * is false.
	 */
	public static final String REQUIRED = TEMPLATE_PREFIX + "required";

	/** The attribute specifying whether multiple widgets may be specified for the attach point. Default is false. */
	public static final String MULTIPLE = TEMPLATE_PREFIX + "multiple";

	/**
	 * The attribute specifying an attach point as the default attach point for the widget. Content in a templated widget's invocation from
	 * XML that does not specify a {@link #ROLE} will be added at the default attach point. At most one default attach point may be
	 * specified for a widget.
	 */
	public static final String DEFAULT = TEMPLATE_PREFIX + "default";

	/**
	 * The attribute specifying that the element defining the attach point also defines a widget that will be placed at the attach point
	 * unless overridden externally. Attach points that specify {@link #MULTIPLE} may not be implementations. Default is false except for
	 * {@link #EXTERNAL internal-only} attach points, which <b>MUST</b> be implementations.
	 */
	public static final String IMPLEMENTATION = TEMPLATE_PREFIX + "implementation";

	public static final String MUTABLE = TEMPLATE_PREFIX + "mutable";

	/** An attach point under a template widget */
	public static class AttachPoint {
		/** The name of the attach point */
		public final String name;

		/** The type of element that may occupy the attach point */
		public final Class<? extends MuisElement> type;

		/**
		 * Whether the attach point may be specified externally
		 *
		 * @see MuisTemplate2#EXTERNAL
		 */
		public final boolean external;

		/**
		 * Whether the attach point is required to be specified externally
		 *
		 * @see MuisTemplate2#REQUIRED
		 */
		public final boolean required;

		/**
		 * Whether the attach point may be specified more than once
		 *
		 * @see MuisTemplate2#MULTIPLE
		 */
		public final boolean multiple;

		/**
		 * Whether the attach point is the default attach point for its widget
		 *
		 * @see MuisTemplate2#DEFAULT
		 */
		public final boolean isDefault;

		/**
		 * Whether the attach point specifies an implementation
		 *
		 * @see MuisTemplate2#IMPLEMENTATION
		 */
		public final boolean implementation;

		/**
		 * Whether the element or set of elements at the attach point can be changed generically. This only affects the mutability of the
		 * attach point as accessed from the list returned from {@link MuisTemplate2#initChildren(MuisElement[])}.
		 *
		 * @see MuisTemplate2#MUTABLE
		 */
		public final boolean mutable;

		AttachPoint(String aName, Class<? extends MuisElement> aType, boolean ext, boolean req, boolean mult, boolean def, boolean impl,
			boolean isMutable) {
			name = aName;
			type = aType;
			external = ext;
			required = req;
			multiple = mult;
			isDefault = def;
			implementation = impl;
			mutable = isMutable;
		}
	}

	/** Represents the structure of a templated widget */
	public static class TemplateStructure implements Iterable<AttachPoint> {
		private final Class<? extends MuisTemplate2> theDefiner;

		private final TemplateStructure theSuperStructure;

		private final WidgetStructure theWidgetStructure;

		private final AttachPoint theDefaultAttachPoint;

		private Map<String, AttachPoint> theAttachPoints;

		private Map<AttachPoint, WidgetStructure> theAttachPointWidgets;

		/**
		 * @param definer The templated class that defines the template structure
		 * @param superStructure The parent template structure
		 * @param widgetStructure The widget structure specified in the template MUIS file
		 * @param attaches The map of attach points to the widget structure where the attach points point to
		 */
		public TemplateStructure(Class<? extends MuisTemplate2> definer, TemplateStructure superStructure, WidgetStructure widgetStructure,
			Map<AttachPoint, WidgetStructure> attaches) {
			theDefiner = definer;
			theSuperStructure = superStructure;
			theWidgetStructure = widgetStructure;
			Map<String, AttachPoint> attachPoints = new java.util.LinkedHashMap<>();
			AttachPoint defAP = null;
			for(AttachPoint ap : attaches.keySet()) {
				attachPoints.put(ap.name, ap);
				if(ap.isDefault)
					defAP = ap;
			}
			theDefaultAttachPoint = defAP;
			theAttachPoints = java.util.Collections.unmodifiableMap(attachPoints);
			theAttachPointWidgets = java.util.Collections.unmodifiableMap(attaches);
		}

		/** @return The templated class that defines this template structure */
		public Class<? extends MuisTemplate2> getDefiner() {
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

		/**
		 * @param attachPoint The attach point to get the widget structure of
		 * @return The widget structure associated with the given attach point
		 */
		public WidgetStructure getWidgetStructure(AttachPoint attachPoint) {
			return theAttachPointWidgets.get(attachPoint);
		}

		@Override
		public java.util.Iterator<AttachPoint> iterator() {
			return Collections.unmodifiableList(new ArrayList<AttachPoint>(theAttachPoints.values())).listIterator();
		}
	}

	/** The cache key to use to retrieve instances of {@link TemplateStructure} */
	public static MuisCache.CacheItemType<Class<? extends MuisTemplate2>, TemplateStructure, MuisException> TEMPLATE_STRUCTURE_CACHE_TYPE;

	static {
		TEMPLATE_STRUCTURE_CACHE_TYPE = new MuisCache.CacheItemType<Class<? extends MuisTemplate2>, MuisTemplate2.TemplateStructure, MuisException>() {
			@Override
			public TemplateStructure generate(MuisEnvironment env, Class<? extends MuisTemplate2> key) throws MuisException {
				return genTemplateStructure(env, key);
			}

			@Override
			public int size(TemplateStructure value) {
				return 0; // TODO
			}
		};
	}

	private TemplateStructure theTemplateStructure;

	private Object theRoleWanter;

	private Map<AttachPoint, MuisContainer<?>> theAttachPointContainers;

	private Map<AttachPoint, MuisElement> theAttachPointElements; // This is not good enough--doesn't support set

	// These fields are valid during initialization only (prior to initChildren())--they will be null after that

	private Map<MuisContent, MuisElement> theStructureMappings;

	private Map<WidgetStructure, List<MuisElement>> theAttachmentMappings;

	/** Creates a templated widget */
	public MuisTemplate2() {
		theRoleWanter = new Object();
		theAttachPointContainers = new HashMap<>();
		theAttachPointElements = new HashMap<>();
		theStructureMappings = new HashMap<>();
		theAttachmentMappings = new HashMap<>();
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				try {
					MuisEnvironment env = getDocument().getEnvironment();
					theTemplateStructure = env.getCache().getAndWait(env, TEMPLATE_STRUCTURE_CACHE_TYPE, MuisTemplate2.this.getClass());
				} catch(MuisException e) {
					msg().fatal("Could not generate template structure", e);
				}
			}
		}, MuisConstants.CoreStage.INIT_SELF.toString(), 1);
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				if(theTemplateStructure == null)
					return;
				/* Initialize this templated widget using theTemplateStructure from the top (direct extension of MuisTemplate2) down
				 * (to this templated class) */
				try {
					initTemplate(theTemplateStructure);
				} catch(MuisParseException e) {
					msg().fatal("Failed to implement widget structure for templated type " + MuisTemplate2.this.getClass().getName(), e);
				}
			}
		}, MuisConstants.CoreStage.INIT_CHILDREN.toString(), -1);
	}

	private void initTemplate(TemplateStructure structure) throws MuisParseException {
		if(structure.getSuperStructure() != null) {
			initTemplate(structure.getSuperStructure());
		}
		for(MuisContent content : structure.getWidgetStructure().getChildren()) {
			MuisElement child = getChild(structure, this, content, getDocument().getEnvironment().getContentCreator());
			if(child != null) {
				if(structure.getSuperStructure() == null)
					theStructureMappings.put(content, child);
				else
					addContent(child, structure.getSuperStructure());
			}
		}
	}

	private MuisElement getChild(TemplateStructure template, MuisElement parent, MuisContent child,
		org.muis.core.parser.MuisContentCreator creator) throws MuisParseException {
		if(child instanceof WidgetStructure && ((WidgetStructure) child).getAttributes().containsKey(ATTACH_POINT)) {
			WidgetStructure apStruct = (WidgetStructure) child;
			AttachPoint ap = template.getAttachPoint(apStruct.getAttributes().get(ATTACH_POINT));
			List<MuisElement> attaches = new ArrayList<>();
			theAttachmentMappings.put(apStruct, attaches);
			if(ap.implementation) {
				WidgetStructure implStruct = new WidgetStructure(apStruct.getParent(), getDocument().getEnvironment(),
					apStruct.getClassView(), apStruct.getNamespace(), apStruct.getTagName());
				for(Map.Entry<String, String> att : apStruct.getAttributes().entrySet()) {
					if(!att.getKey().startsWith("template:"))
						implStruct.addAttribute(att.getKey(), att.getValue());
				}
				for(MuisContent content : apStruct.getChildren())
					implStruct.addChild(content);
				implStruct.seal();
				MuisElement ret = creator.getChild(parent, implStruct);
				try {
					ret.atts().set(IMPLEMENTATION, "true");
				} catch(MuisException e) {
					throw new IllegalStateException("Should not have thrown exception here", e);
				}
				attaches.add(ret);
				return ret;
			} else
				return null;
		} else {
			return creator.getChild(parent, child);
		}
	}

	private void addContent(final MuisElement child, TemplateStructure template) {
		child.atts().accept(theRoleWanter, ROLE);
		final String role = child.atts().get(ROLE);
		child.addListener(MuisConstants.Events.ATTRIBUTE_CHANGED, new org.muis.core.event.AttributeChangedListener<String>(ROLE) {
			private boolean theCallbackLock;

			@Override
			public void attributeChanged(AttributeChangedEvent<String> event) {
				if(theCallbackLock)
					return;
				child.msg().error("The " + ROLE.getName() + " attribute may not be changed");
				theCallbackLock = true;
				try {
					try {
						child.atts().set(ROLE, role);
					} catch(MuisException e) {
						child.msg().error("Should not get an exception here", e);
					}
				} finally {
					theCallbackLock = false;
				}
			}
		});
		AttachPoint ap = template.getAttachPoint(role);
		if(ap == null) {
			if(role == null)
				msg().error("No role specified for child of templated widget " + template.getDefiner().getName(), "child", child);
			else
				msg().error("Role \"" + role + "\" is not defined for templated widget " + template.getDefiner().getName(), "child", child);
			return;
		}
		List<MuisElement> attaches = theAttachmentMappings.get(ap);
		if(!ap.external) {
			msg().error("Role \"" + role + "\" is not specifiable externally for templated widget " + template.getDefiner().getName(),
				"child", child);
			return;
		}
		if(!ap.type.isInstance(child)) {
			msg().error(
				"Children fulfilling role \"" + role + "\" in templated widget " + template.getDefiner().getName() + " must be of type "
					+ ap.type.getName() + ", not " + child.getClass().getName(), "child", child);
			return;
		}
		if(ap.implementation && attaches.size() == 1 && attaches.get(0).atts().get(IMPLEMENTATION) != null)
			attaches.clear(); // Override the provided implementation
		if(!ap.multiple && !attaches.isEmpty()) {
			msg().error("Multiple children fulfilling role \"" + role + "\" in templated widget " + template.getDefiner().getName(),
				"child", child);
			return;
		}
		WidgetStructure widgetStruct = template.getWidgetStructure(ap);
		for(Map.Entry<String, String> attr : widgetStruct.getAttributes().entrySet()) {
			if(attr.getKey().startsWith(TEMPLATE_PREFIX))
				continue;
			try {
				child.atts().set(attr.getKey(), attr.getValue());
			} catch(MuisException e) {
				child.msg().error("Template-specified attribute " + attr.getKey() + "=" + attr.getValue() + " is not supported by content",
					e);
			}
		}
		theAttachmentMappings.get(widgetStruct).add(child);
	}

	protected MuisContainer<?> getContainer(AttachPoint attach) throws IllegalArgumentException {
	}

	protected MuisElement getElement(AttachPoint attach) throws IllegalArgumentException {
	}

	protected void setElement(AttachPoint attach, MuisElement element) throws IllegalArgumentException {
	}

	@Override
	public org.muis.core.mgr.MutableElementList<? extends MuisElement> initChildren(MuisElement [] children) {
		if(theStructureMappings == null)
			throw new IllegalArgumentException("initChildren() may only be called once on an element");
		for(MuisElement child : children)
			addContent(child, theTemplateStructure);

		// Verify we've got all required attach points satisfied, etc.
		if(!verifyTemplateStructure(theTemplateStructure))
			return new AttachPointSetChildList();

		MuisElement [] realChildren = getChildren(theTemplateStructure.getWidgetStructure());
		super.initChildren(realChildren);
		// Don't need these anymore
		theStructureMappings = null;
		theAttachmentMappings = null;
		return new AttachPointSetChildList();
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
		if(ap.required && theAttachmentMappings.get(struct.getWidgetStructure(ap)).isEmpty()) {
			msg().error("No widget specified for role " + ap.name + " for template " + struct.getDefiner().getName());
			return false;
		}
		return true;
	}

	private MuisElement [] getChildren(WidgetStructure structure) {
		List<MuisElement> ret = new ArrayList<>();
		for(MuisContent childStruct : structure.getChildren()) {
			if(childStruct instanceof WidgetStructure && ((WidgetStructure) childStruct).getAttributes().containsKey(ATTACH_POINT)) {
				for(MuisElement child : theAttachmentMappings.get(childStruct)) {
					ret.add(child);
					MuisElement [] subChildren = getChildren((WidgetStructure) childStruct);
					child.initChildren(subChildren);
				}
			} else {
				MuisElement child = theStructureMappings.get(childStruct);
				ret.add(child);
				MuisElement [] subChildren = getChildren((WidgetStructure) childStruct);
				child.initChildren(subChildren);
			}
		}
		return ret.toArray(new MuisElement[ret.size()]);
	}

	/**
	 * Generates a template structure for a template type
	 *
	 * @param env The MUIS environment to generate the structure within
	 * @param templateType The template type to generate the structure for
	 * @return The template structure for the given templated type
	 * @throws MuisException If an error occurs generating the structure
	 */
	public static TemplateStructure genTemplateStructure(MuisEnvironment env, Class<? extends MuisTemplate2> templateType)
		throws MuisException {
		if(!MuisTemplate2.class.isAssignableFrom(templateType))
			throw new MuisException("Only extensions of " + MuisTemplate2.class.getName() + " may have template structures: "
				+ templateType.getName());
		if(templateType == MuisTemplate2.class)
			return null;
		Class<? extends MuisTemplate2> superType = (Class<? extends MuisTemplate2>) templateType.getSuperclass();
		TemplateStructure superStructure = null;
		while(superType != MuisTemplate2.class) {
			if(superType.getAnnotation(Template.class) != null) {
				superStructure = env.getCache().getAndWait(env, TEMPLATE_STRUCTURE_CACHE_TYPE, superType);
				break;
			}
			superType = (Class<? extends MuisTemplate2>) superType.getSuperclass();
		}
		Template template = templateType.getAnnotation(Template.class);
		if(template == null) {
			if(superStructure != null)
				return superStructure;
			throw new MuisException("Concrete implementations of " + MuisTemplate2.class.getName() + " like " + templateType.getName()
				+ " must be tagged with @" + Template.class.getName() + " or extend a class that does");
		}

		java.net.URL location;
		try {
			location = MuisUtils.resolveURL(templateType.getResource(templateType.getSimpleName() + ".class"), template.location());
		} catch(MuisException e) {
			throw new MuisException("Could not resolve template path " + template.location() + " for templated widget "
				+ templateType.getName(), e);
		}
		WidgetStructure widgetStructure;
		try {
			MuisClassView classView = new MuisClassView(env, null);
			classView.addNamespace("this", (MuisToolkit) templateType.getClassLoader());
			org.muis.core.mgr.MutatingMessageCenter msg = new org.muis.core.mgr.MutatingMessageCenter(env.msg(), "Template "
				+ templateType.getName() + ": ", "template", templateType);
			widgetStructure = env.getParser().parseContent(location,
				new java.io.BufferedReader(new java.io.InputStreamReader(location.openStream())), classView, msg);
		} catch(java.io.IOException e) {
			throw new MuisException("Could not read template resource " + template.location() + " for templated widget "
				+ templateType.getName(), e);
		} catch(MuisParseException e) {
			throw new MuisException("Could not parse template resource " + template.location() + " for templated widget "
				+ templateType.getName(), e);
		}
		Map<AttachPoint, WidgetStructure> attaches = new HashMap<>();
		try {
			pullAttachPoints(widgetStructure, attaches);
		} catch(MuisException e) {
			throw new MuisException("Error in template resource " + template.location() + " for templated widget " + templateType.getName()
				+ ": " + e.getMessage(), e);
		}
		List<String> defaults = new ArrayList<>();
		for(AttachPoint ap : attaches.keySet())
			if(ap.isDefault)
				defaults.add(ap.name);
		if(defaults.size() > 1)
			throw new MuisException("More than one default attach point " + defaults + " present in template resource "
				+ template.location() + " for templated widget " + templateType.getName());
		return new TemplateStructure(templateType, superStructure, widgetStructure, attaches);
	}

	private static void pullAttachPoints(WidgetStructure structure, Map<AttachPoint, WidgetStructure> attaches) throws MuisException {
		for(MuisContent content : structure.getChildren()) {
			if(!(content instanceof WidgetStructure))
				continue;
			WidgetStructure child = (WidgetStructure) content;
			String name = child.getAttributes().get(ATTACH_POINT);
			if(name == null) {
				pullAttachPoints(child, attaches);
				continue;
			}
			if(attaches.containsKey(name))
				throw new MuisException("Duplicate attach points named \"" + name + "\"");
			Class<? extends MuisElement> type;
			if(child.getNamespace() == null && child.getTagName().equals("element"))
				type = MuisElement.class;
			else
				try {
					type = child.getClassView().loadMappedClass(child.getNamespace(), child.getTagName(), MuisElement.class);
				} catch(MuisException e) {
					throw new MuisException("Could not load element type \""
						+ (child.getNamespace() == null ? "" : (child.getNamespace() + ":")) + child.getTagName()
						+ "\" for attach point \"" + name + "\": " + e.getMessage(), e);
				}
			boolean external = getBoolean(child, EXTERNAL, true, name); // Externally-specifiable by default
			boolean implementation = getBoolean(child, IMPLEMENTATION, !external, name);
			boolean required = getBoolean(child, REQUIRED, true, name);
			boolean multiple = getBoolean(child, MULTIPLE, false, name);
			boolean def = getBoolean(child, DEFAULT, false, name);
			boolean mutable = getBoolean(child, MUTABLE, true, name);
			if(!external && (required || multiple || def || !implementation)) {
				throw new MuisException("Non-externally-specifiable attach points (" + name
					+ ") may not be required, default, or allow multiples");
			}
			if(!implementation && !external)
				throw new MuisException("Non-externally-specifiable attach points (" + name + ") must be implementations");
			if(implementation && multiple)
				throw new MuisException("Attach points (" + name + ") that allow multiples cannot be implementations");
			for(String attName : child.getAttributes().keySet()) {
				if(!attName.startsWith(TEMPLATE_PREFIX))
					continue;
				if(attName.equals(EXTERNAL) || attName.equals(IMPLEMENTATION) || attName.equals(REQUIRED) || attName.equals(MULTIPLE)
					|| attName.equals(DEFAULT))
					continue;
				throw new MuisException("Template attribute " + TEMPLATE_PREFIX + attName + " not recognized");
			}

			attaches.put(new AttachPoint(name, type, external, required, multiple, def, implementation, mutable), child);
			Map<AttachPoint, WidgetStructure> check = new HashMap<>();
			pullAttachPoints(child, check);
			if(!check.isEmpty())
				throw new MuisException("Attach points (" + name + ") may not contain attach points: " + check.keySet());
		}
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

	private class AttachPointSetChildList implements org.muis.core.mgr.MutableElementList<MuisElement> {
		AttachPointSetChildList() {
		}

		@Override
		public MuisElement getParent() {
			return MuisTemplate2.this;
		}

		@Override
		public MuisElement getLast() {
			MuisElement ret = null;
			for(MuisElement item : this)
				ret = item;
			return ret;
		}

		@Override
		public MuisElement [] at(int x, int y) {
			MuisElement [] children = sortByZ();
			MuisElement [] ret = new MuisElement[0];
			for(MuisElement child : children) {
				int relX = x - child.getX();
				if(relX < 0 || relX >= child.getWidth())
					continue;
				int relY = y - child.getY();
				if(relY < 0 || relY >= child.getHeight())
					continue;
				ret = prisms.util.ArrayUtils.add(ret, child);
			}
			return ret;
		}

		@Override
		public MuisElement [] sortByZ() {
			MuisElement [] children = toArray();
			if(children.length < 2)
				return children;
			boolean sameZ = true;
			int z = children[0].getZ();
			for(int c = 1; c < children.length; c++)
				if(children[c].getZ() != z) {
					sameZ = false;
					break;
				}
			if(!sameZ) {
				java.util.Arrays.sort(children, new java.util.Comparator<MuisElement>() {
					@Override
					public int compare(MuisElement el1, MuisElement el2) {
						return el1.getZ() - el2.getZ();
					}
				});
			}
			return children;
		}

		@Override
		public <T> void addChildListener(MuisEventType<T> type, MuisEventListener<? super T> listener) {
			// TODO Auto-generated method stub
		}

		@Override
		public void removeChildListener(MuisEventListener<?> listener) {
			// TODO Auto-generated method stub
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
		public boolean isEmpty() {
			return !iterator().hasNext();
		}

		@Override
		public boolean contains(Object o) {
			for(MuisElement item : this)
				if(item.equals(o))
					return true;
			return false;
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
		public <T> T [] toArray(T [] a) {
			ArrayList<MuisElement> ret = new ArrayList<>();
			for(MuisElement item : this)
				ret.add(item);
			return ret.toArray(a);
		}

		@Override
		public boolean add(MuisElement e) {
			String role = e.atts().get(ROLE);
			AttachPoint ap = theTemplateStructure.getAttachPoint(role);
			if(ap == null) {
				if(role != null)
					throw new UnsupportedOperationException("Templated widget " + MuisTemplate2.class.getName()
						+ " does not support role \"" + role + "\"");
				else
					throw new UnsupportedOperationException("Templated widget " + MuisTemplate2.class.getName()
						+ " does not have a default attach point, and therefore does not support addition"
						+ " of children without a role assignment");
			}
			if(!ap.external)
				throw new UnsupportedOperationException("The " + ap.name + " attach point is not externally-exposed");
			if(!ap.mutable)
				throw new UnsupportedOperationException("The " + ap.name + " attach point is not mutable");
			if(!ap.type.isInstance(e))
				throw new UnsupportedOperationException("The " + ap.name + " attach point's elements must be of type " + ap.type.getName()
					+ ", not " + e.getClass().getName());
			if(ap.multiple)
				return ((MuisContainer<MuisElement>) getContainer(ap)).getContent().add(e);
			else {
				if(getElement(ap) != null)
					throw new UnsupportedOperationException("The " + ap.name
						+ " attach point only supports a single element and is already occupied");
				setElement(ap, e);
				return true;
			}
		}

		@Override
		public boolean remove(Object o) {
			if(!(o instanceof MuisElement))
				return false;
			MuisElement e = (MuisElement) o;
			String role = e.atts().get(ROLE);
			AttachPoint ap = theTemplateStructure.getAttachPoint(role);
			if(ap == null)
				return false;
			if(!ap.type.isInstance(e))
				return false;
			if(!ap.external)
				throw new UnsupportedOperationException("The " + ap.name + " attach point is not externally-exposed");
			if(!ap.mutable)
				throw new UnsupportedOperationException("The " + ap.name + " attach point is not mutable");
			if(ap.multiple) {
				org.muis.core.mgr.ElementList<? extends MuisElement> content = getContainer(ap).getContent();
				if(ap.required && content.size() == 1 && content.get(0).equals(e))
					throw new UnsupportedOperationException("The " + ap.name
						+ " attach point is required and only has one element left in it");
				return content.remove(e);
			} else {
				if(!e.equals(getElement(ap)))
					return false;
				if(ap.required)
					throw new UnsupportedOperationException("The " + ap.name + " attach point is required");
				setElement(ap, null);
				return true;
			}
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			HashSet<MuisElement> found = new HashSet<>();
			for(MuisElement item : this) {
				if(c.contains(item))
					found.add(item);
			}
			return found.size() == c.size();
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
		public int indexOf(Object o) {
			int index = 0;
			for(MuisElement item : this) {
				if(item.equals(o))
					return index;
				else
					index++;
			}
			return -1;
		}

		@Override
		public int lastIndexOf(Object o) {
			int index = 0;
			int lastFound = -1;
			for(MuisElement item : this) {
				if(item.equals(o))
					lastFound = index;
				index++;
			}
			return lastFound;
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
			// TODO Don't want to do this right now
			throw new UnsupportedOperationException();
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

			private boolean calledHasNext = true;

			private boolean calledHasPrevious = true;

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
}
