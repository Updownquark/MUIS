package org.muis.core;

import org.muis.core.parser.MuisContent;
import org.muis.core.parser.MuisParseException;
import org.muis.core.parser.WidgetStructure;
import org.muis.core.tags.AttachPoint;

public abstract class MuisTemplate2 extends MuisElement {
	/** The attribute in a child of a template definition which marks the child as able to be replaced in a template instance */
	public static final MuisAttribute<String> ATTACH_POINT = new MuisAttribute<String>("attachPoint", MuisProperty.stringAttr);

	/** The attribute in a child of a template instance which marks the child as replacing an attach point from the definition */
	public static final MuisAttribute<String> ROLE = new MuisAttribute<String>("role", MuisProperty.stringAttr);

	private static class AttachPointDef {
		final AttachPoint annotation;

		final Class<? extends MuisElement> definer;

		AttachPointDef(Class<? extends MuisElement> defining, AttachPoint ap) {
			definer = defining;
			annotation = ap;
		}
	}

	public static class TemplateStructure {
		private final Class<? extends MuisTemplate2> theDefiner;

		private final TemplateStructure theSuperStructure;

		private final WidgetStructure theWidgetStructure;

		private AttachPoint [] theAttachPoints;

		public TemplateStructure(Class<? extends MuisTemplate2> definer, TemplateStructure superStructure, WidgetStructure widgetStructure) {
			theDefiner = definer;
			theSuperStructure = superStructure;
			theWidgetStructure = widgetStructure;
		}

		public Class<? extends MuisTemplate2> getDefiner() {
			return theDefiner;
		}

		public TemplateStructure getSuperStructure() {
			return theSuperStructure;
		}

		public WidgetStructure getWidgetStructure() {
			return theWidgetStructure;
		}
	}

	public static MuisCache.CacheItemType<Class<? extends MuisTemplate2>, TemplateStructure, MuisException> TEMPLATE_STRUCTURE_CACHE_TYPE;

	static {
		TEMPLATE_STRUCTURE_CACHE_TYPE = new MuisCache.CacheItemType<Class<? extends MuisTemplate2>, MuisTemplate2.TemplateStructure, MuisException>() {
			@Override
			public TemplateStructure generate(MuisDocument doc, Class<? extends MuisTemplate2> key) throws MuisException {
				return genTemplateStructure(doc.getCache(), key);
			}

			@Override
			public int size(TemplateStructure value) {
				return 0; // TODO
			}
		};
	}

	private TemplateStructure theTemplateStructure;

	public MuisTemplate2() {
		life().runWhen(new Runnable(){
			@Override
			public void run(){
				try {
					theTemplateStructure = getDocument().getCache().getAndWait(getDocument(), TEMPLATE_STRUCTURE_CACHE_TYPE,
						MuisTemplate2.this.getClass());
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
				initTemplate(theTemplateStructure, new TemplateContentCreator());
			}
		}, MuisConstants.CoreStage.INIT_CHILDREN.toString(), -1);
	}

	private void initTemplate(TemplateStructure structure, org.muis.core.parser.MuisContentCreator creator) {
		if(structure.getSuperStructure() != null) {
			initTemplate(structure.getSuperStructure(), creator);
			for(org.muis.core.parser.MuisContent content : structure.getWidgetStructure().getChildren()) {
				addContent(creator.getChild(this, content));
			}
		} else {
			// Root template structure handled differently--super.initChildren needs to be called once
		}
		int todo; // TODO
	}

	@Override
	public void initChildren(MuisElement [] children) {
		// TODO Auto-generated method stub
	}

	public static TemplateStructure genTemplateStructure(MuisCache cache, Class<? extends MuisTemplate2> templateType) throws MuisException {
		int todo;// TODO
	}

	private static class TemplateContentCreator extends org.muis.core.parser.MuisContentCreator {
		@Override
		public MuisElement getChild(MuisElement parent, MuisContent child) throws MuisParseException {
			return super.getChild(parent, child);
			// TODO Auto-generated method stub
		}
	}
}
