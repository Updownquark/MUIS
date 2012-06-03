package org.wam.core;

import prisms.util.ArrayUtils;

/**
 * Allows complex widgets to be created more easily by addressing a template WAM file that is
 * reproduced in each instance of the widget
 */
public abstract class WamTemplate extends WamElement
{
	/**
	 * The attribute in a child of a template definition which marks the child as able to be
	 * replaced in a template instance
	 */
	public static final WamAttribute<String> ATTACH_POINT = new WamAttribute<String>("attachPoint",
		WamAttribute.stringAttr);

	/**
	 * The attribute in a child of a template instance which marks the child as replacing an attach
	 * point from the definition
	 */
	public static final WamAttribute<String> USE_AS = new WamAttribute<String>("useAs",
		WamAttribute.stringAttr);

	/** The definition of an attach point under this template widget */
	protected static class AttachPointDef
	{
		final String name;

		Class<? extends WamElement> type;

		int stage;

		boolean external;

		boolean required;

		boolean multiple;

		boolean isDefault;

		AttachPointDef(String aName, Class<? extends WamElement> aType, int aStage, boolean ext,
			boolean req, boolean mult, boolean def)
		{
			name = aName;
			type = aType;
			stage = aStage;
			external = ext;
			required = req;
			multiple = mult;
			isDefault = def;
		}
	}

	private AttachPointDef [] theAPDefs;

	private java.util.HashMap<String, WamElement> theAttaches;

	private int theTemplateStage;

	/**
	 * Creates a templated element
	 */
	public WamTemplate()
	{
		theAPDefs = new AttachPointDef [0];
		theAttaches = new java.util.HashMap<String, WamElement>();
	}

	/**
	 * @return The address (absolute or relative to the implementation class) of the WAM XML file
	 *         containing this template's content.
	 */
	protected abstract String getTemplateFile();

	/**
	 * Marks this template as requiring an attach point of the given name and type
	 * 
	 * @param name The name of the attach point to require
	 * @param type The type of element that must be present as the attach point in the templated
	 *        XML. May be null if the value may be any WAM element.
	 * @param external Whether the attach point can be specified in the instantiation of this
	 *        element. If the attach point is so specified, the attach point in the template will be
	 *        replaced with the externally-specified element.
	 * @param required Whether the attach point <i>must</i> be specified in each instantiation of
	 *        this element.
	 * @param multiple Whether the attach point can be specified multiple times externally. If
	 *        specified multiple times, TODO
	 * @param def Whether children specified externally for this element and not assigned an
	 *        attachPoint should be assigned to this attach point. There can only be one default
	 *        attach point per element.
	 */
	protected void defineAttachPoint(String name, Class<? extends WamElement> type,
		boolean external, boolean required, boolean multiple, boolean def)
	{
		if(required && !external)
		{
			error("An internal attach point cannot be marked as required: " + name, null);
			required = false;
		}
		if(def)
			for(AttachPointDef apd : theAPDefs)
				if(apd.isDefault && !apd.name.equals(name))
				{
					error("Attach point " + apd.name
						+ " is already marked as a default attach point"
						+ " for this type. Attach point " + name + " cannot also be default.", null);
					def = false;
					break;
				}
		for(AttachPointDef apd : theAPDefs)
			if(apd.name.equals(name))
			{
				apd.stage = theTemplateStage;
				if(!apd.type.isAssignableFrom(type))
					error("Attach point " + apd.name + " is already marked as type " + apd.type
						+ ". It cannot be made to be type " + type, null);
				else
					apd.type = type;
				if(external && !apd.external)
				{
					error("Attach point " + apd.name
						+ " cannot be changed from internal to external", null);
					required = false;
				}
				else
					apd.external = external;
				apd.required = required;
				apd.multiple = multiple;
				return;
			}
		AttachPointDef newType = new AttachPointDef(name, type, theTemplateStage, external,
			required, multiple, def);
		theAPDefs = prisms.util.ArrayUtils.add(theAPDefs, newType);
	}

	/**
	 * @param name The name of the attach point to get the definition for
	 * @return The definition for the given attach point
	 */
	protected AttachPointDef getAttachDef(String name)
	{
		for(AttachPointDef def : theAPDefs)
			if(def.name.equals(name))
				return def;
		return null;
	}

	/**
	 * Initializes this templated element's content using an external WAM file. For the first
	 * invocation of this method on each instance, the file's content is loaded into this element's
	 * children. Each subsequent invocation has a dual effect--it first loads the content into the
	 * templated element as if it were being instantiated, substituting attach points where
	 * specified by the useAs attribute; then it uses the result of this substitution as the
	 * template for when the element is instantiated.
	 * 
	 * This method should be called from
	 * {@link #init(WamDocument, WamToolkit, WamClassView, WamElement, String, String)} after the
	 * super call.
	 * 
	 * @param template The location of the template WAM file to load
	 */
	protected void initTemplate(String template)
	{
		java.net.URL templateURL;
		if(template.startsWith("http"))
			try
			{
				templateURL = new java.net.URL(template);
			} catch(java.net.MalformedURLException e)
			{
				error("Could not access template URL " + template, e, "url", template);
				return;
			}
		else
		{
			templateURL = getClass().getResource(template);
			if(templateURL == null)
			{
				error("Could not access template URL " + template, null, "url", template);
				return;
			}
		}
		try
		{
			WamElement [] content = getDocument().getParser().parseContent(
				new java.io.InputStreamReader(templateURL.openStream()), this, true);
			initChildren(content);
		} catch(java.io.IOException e)
		{
			error("Could not read template XML " + template, e, "url", template);
		} catch(org.wam.parser.WamParseException e)
		{
			error("Could not parse template XML " + template, e, "url", template);
		}
		theTemplateStage++;
	}

	/**
	 * Searches in the templated descendants for attach points to store
	 * 
	 * @param child The child to search
	 */
	private void findAttachPoints(WamElement child)
	{
		child.acceptAttribute(ATTACH_POINT);
		String attachPointAttr = child.getAttribute(ATTACH_POINT);
		if(attachPointAttr != null)
		{
			if(theAttaches.get(attachPointAttr) != null)
				error("Multiple attach points for " + attachPointAttr, null);
			int apd;
			for(apd = 0; apd < theAPDefs.length; apd++)
				if(theAPDefs[apd].name.equals(attachPointAttr))
					break;
			if(apd == theAPDefs.length)
				error("Unexpected attach point " + attachPointAttr + "--not registered", null);
			theAttaches.put(attachPointAttr, child);
		}
		if(!(child instanceof WamTemplate))
			for(int c = 0; c < child.getChildCount(); c++)
				findAttachPoints(child.getChild(c));
	}

	/**
	 * @param attachPoint The name of the attach point to get
	 * @return The WAM element attached at the given attach point
	 */
	public WamElement getAttachPoint(String attachPoint)
	{
		return theAttaches.get(attachPoint);
	}

	/**
	 * For the first invocation, this method simply calls the super (see
	 * {@link WamElement#initChildren(WamElement[])}), then checks for attach points. For subsequent
	 * invocations, it checks the children's "useAs" attribute and inserts them into this element's
	 * subtree at the corresponding attach points, then checks for new attach points.
	 */
	@Override
	public void initChildren(WamElement [] children)
	{
		java.util.HashMap<String, WamElement> attaches = new java.util.HashMap<String, WamElement>();
		attaches.putAll(theAttaches);
		theAttaches.clear();

		if(getChildCount() == 0) // First template action
			super.initChildren(children);
		else
		{ // Insert content into attach points
			AttachPointDef defUseAs = null;
			for(AttachPointDef def : theAPDefs)
				if(def.isDefault)
				{
					defUseAs = def;
					break;
				}
			for(WamElement child : children)
				child.acceptAttribute(USE_AS);

			// Check children for legal insertion
			AttachPointDef [] uses = new AttachPointDef [0];
			for(int c = 0; c < children.length; c++)
			{
				String useAsAttr = children[c].getAttribute(USE_AS);
				if(useAsAttr == null)
				{
					if(defUseAs == null)
					{
						error("This type does not accept content without a \"useAs\" attribute",
							null);
						children = ArrayUtils.remove(children, c);
						c--;
						continue;
					}
					useAsAttr = defUseAs.name;
				}
				AttachPointDef useDef = getAttachDef(useAsAttr);
				if(useDef == null)
				{
					error("Unrecognized attach point for useAs: " + useAsAttr, null);
					children = ArrayUtils.remove(children, c);
					c--;
					continue;
				}
				if(!useDef.external)
				{
					error("Attach point " + useAsAttr + " is not exposed externally", null);
					children = ArrayUtils.remove(children, c);
					c--;
					continue;
				}
				int idx = ArrayUtils.indexOf(uses, useDef);
				if(idx < 0)
					uses = ArrayUtils.add(uses, useDef);
				else if(!useDef.multiple)
				{
					error("Attach point " + useAsAttr + " can only be used for a single element.",
						null);
					children = ArrayUtils.remove(children, c);
					c--;
					continue;
				}
			}
			// Check for unspecified required external attach points
			for(AttachPointDef def : theAPDefs)
				if(def.required && def.stage == theTemplateStage)
				{
					if(!ArrayUtils.contains(uses, def))
						error("Requred external attach point " + def.name + " not specified", null);
				}
			uses = new AttachPointDef [0];
			for(WamElement child : children)
			{
				AttachPointDef useDef = getAttachDef(child.getAttribute(USE_AS));
				int useIdx = ArrayUtils.indexOf(uses, useDef);
				WamElement toReplace = getAttachPoint(useDef.name);
				WamElement replaceParent = toReplace.getParent();
				int index = ArrayUtils.indexOf(replaceParent.getChildren(), toReplace);
				if(useIdx < 0)
				{
					// Replace the templated element with the externally-specified element
					theAttaches.put(useDef.name, child);
					replaceParent.removeChild(index);
					replaceParent.addChild(child, index);
					uses = ArrayUtils.add(uses, useDef);
				}
				else
				{
					/* Insert the externally-specified element after others that have been specified
					 * with the same use */
					index++;
					while(index < replaceParent.getChildCount()
						&& useDef.name.equals(replaceParent.getChild(index).getAttribute(USE_AS)))
						index++;
					replaceParent.addChild(child, index);
				}
			}
		}

		// Find attach points in new children
		for(int c = 0; c < getChildCount(); c++)
			findAttachPoints(getChild(c));

		for(java.util.Map.Entry<String, WamElement> entry : attaches.entrySet())
		{
			if(getAttachPoint(entry.getKey()) == null && entry.getValue().isAncestor(this))
				theAttaches.put(entry.getKey(), entry.getValue());
		}

		// Check required attach points
		for(AttachPointDef def : theAPDefs)
		{
			WamElement attached = getAttachPoint(def.name);
			if(attached == null)
				error("Required attach point " + def.name + " not present in template XML", null,
					"attachPoint", def.name);
			else if(def.type != null && !def.type.isInstance(attached))
				error("Required attach point " + def.name + " must be an instance of "
					+ def.type.getName() + " but is " + attached.getClass().getName(), null,
					"attachPoint", def.name);
		}
	}
}
