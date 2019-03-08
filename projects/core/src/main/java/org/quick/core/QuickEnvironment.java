package org.quick.core;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.observe.Observable;
import org.observe.SimpleObservable;
import org.observe.collect.ObservableCollection;
import org.observe.util.TypeTokens;
import org.qommons.Transactable;
import org.qommons.collect.CollectionLockingStrategy;
import org.qommons.collect.StampedLockingStrategy;
import org.qommons.tree.BetterTreeList;
import org.quick.core.mgr.HierarchicalResourcePool;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.parser.DefaultStyleParser;
import org.quick.core.parser.QuickContentCreator;
import org.quick.core.parser.QuickDocumentParser;
import org.quick.core.parser.QuickPropertyParser;
import org.quick.core.parser.QuickStyleParser;
import org.quick.core.parser.QuickToolkitParser;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.prop.antlr.AntlrPropertyParser;
import org.quick.core.style.CompoundStyleSheet;
import org.quick.core.style.StyleSheet;

/** The environment that Quick documents operate in */
public class QuickEnvironment implements QuickParseEnv {
	/** The location of the core toolkit */
	public static final java.net.URL CORE_TOOLKIT = QuickEnvironment.class.getResource("/QuickRegistry.xml");

	private static class EnvironmentStyle extends CompoundStyleSheet {
		EnvironmentStyle(ObservableCollection<StyleSheet> dependencies) {
			super(dependencies);
		}

		@Override
		public String toString() {
			return "Quick Env Style";
		}
	}

	private QuickToolkitParser theToolkitParser;
	private QuickDocumentParser theDocumentParser;
	private QuickContentCreator theContentCreator;
	private QuickStyleParser theStyleParser;
	private QuickPropertyParser thePropertyParser;
	private ExpressionContext theContext;

	private final ReentrantReadWriteLock theAttributeLocker;
	private final CollectionLockingStrategy theContentLocker;
	private final QuickMessageCenter theMessageCenter;
	private final java.util.Map<String, QuickToolkit> theToolkits;
	private final QuickCache theCache;
	private final EnvironmentStyle theStyle;
	private final HierarchicalResourcePool theResourcePool;
	private final SimpleObservable<Void> theDeath;

	private ObservableCollection<StyleSheet> theStyleDependencyController;

	private QuickEnvironment() {
		theAttributeLocker = new ReentrantReadWriteLock();
		theContentLocker = new StampedLockingStrategy();
		theMessageCenter = new QuickMessageCenter(this, null, null);
		theToolkits = new java.util.concurrent.ConcurrentHashMap<>();
		theCache = new QuickCache();
		theStyleDependencyController = ObservableCollection.create(TypeTokens.get().of(StyleSheet.class),
			new BetterTreeList<>(theContentLocker));
		theResourcePool = new HierarchicalResourcePool(this, HierarchicalResourcePool.ROOT, Transactable.transactable(theAttributeLocker),
			true);
		theDeath = new SimpleObservable<>(null, false, theAttributeLocker, null);
		theDeath.take(1).act(v -> theResourcePool.setActive(false));
		theStyle = new EnvironmentStyle(theStyleDependencyController.flow().unmodifiable().collect());
	}

	public ReentrantReadWriteLock getAttributeLocker() {
		return theAttributeLocker;
	}

	public CollectionLockingStrategy getContentLocker() {
		return theContentLocker;
	}

	public HierarchicalResourcePool getResourcePool() {
		return theResourcePool;
	}

	@Override
	public QuickClassView cv() {
		QuickClassView ret = new QuickClassView(this, null, getCoreToolkit());
		ret.seal();
		return ret;
	}

	/** @return The document parser for the environment */
	public QuickDocumentParser getDocumentParser() {
		return theDocumentParser;
	}


	/** @return The content creator for the environment */
	public QuickContentCreator getContentCreator() {
		return theContentCreator;
	}

	/** @return The parser to parse style sheets in this environment */
	public QuickStyleParser getStyleParser() {
		return theStyleParser;
	}

	/** @return The parser to parse property values in this environment */
	public QuickPropertyParser getPropertyParser() {
		return thePropertyParser;
	}

	/** @return The message center for this environment */
	public QuickMessageCenter getMessageCenter() {
		return theMessageCenter;
	}

	/** @return An observable that will fire once when this environment is destroyed */
	public Observable<?> getDeath() {
		return theDeath.readOnly();
	}

	/**
	 * @return The message center for this environment
	 * @see #getMessageCenter()
	 */
	@Override
	public QuickMessageCenter msg() {
		return getMessageCenter();
	}

	@Override
	public ExpressionContext getContext() {
		return theContext;
	}

	/** @return The resource cache for this environment */
	public QuickCache getCache() {
		return theCache;
	}

	/** @return The sum of all toolkit styles in this environment */
	public StyleSheet getStyle() {
		return theStyle;
	}

	/** @return All toolkits in this environment */
	public Collection<QuickToolkit> getToolkits() {
		return Collections.unmodifiableCollection(theToolkits.values());
	}

	/**
	 * @param location The location of the toolkit to get
	 * @return The toolkit at the given location
	 * @throws java.io.IOException If the toolkit resources cannot be retrieved
	 * @throws org.quick.core.parser.QuickParseException If the toolkit cannot be parsed
	 */
	public QuickToolkit getToolkit(java.net.URL location) throws java.io.IOException, org.quick.core.parser.QuickParseException {
		// Need to make sure the core toolkit is loaded first
		if(CORE_TOOLKIT != null && !CORE_TOOLKIT.equals(location))
			getCoreToolkit();
		QuickToolkit toolkit = theToolkits.get(location.toString());
		if(toolkit != null)
			return toolkit;
		theAttributeLocker.writeLock().lock();
		try {
			toolkit = theToolkits.get(location.toString());
			if(toolkit != null)
				return toolkit;
			toolkit = theToolkitParser.parseToolkit(location, tk -> {
				theStyleDependencyController.add(tk.getStyle());
				theToolkits.put(location.toString(), tk);
			});
		} finally {
			theAttributeLocker.writeLock().unlock();
		}
		return toolkit;
	}

	/** @return The toolkit containing the core Quick classes */
	public QuickToolkit getCoreToolkit() {
		if(CORE_TOOLKIT == null)
			throw new IllegalStateException("No such resource " + CORE_TOOLKIT + " for core toolkit");
		try {
			return getToolkit(CORE_TOOLKIT);
		} catch(java.io.IOException e) {
			throw new IllegalStateException("Could not obtain core toolkit " + CORE_TOOLKIT, e);
		} catch(org.quick.core.parser.QuickParseException e) {
			throw new IllegalStateException("Could not parse core toolkit " + CORE_TOOLKIT, e);
		}
	}

	/** @return A builder for an environment */
	public static Builder build() {
		return new Builder();
	}

	/** Builds {@link QuickEnvironment}s */
	public static class Builder {
		private final QuickEnvironment theEnv = new QuickEnvironment();
		private final java.util.concurrent.atomic.AtomicBoolean isBuilt = new java.util.concurrent.atomic.AtomicBoolean(false);

		/**
		 * Populates this builder with default values for all needed fields
		 *
		 * @return This builder
		 */
		public Builder withDefaults() {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theContext = DefaultExpressionContext.build()//
				.build();
			theEnv.theToolkitParser = new org.quick.core.parser.DefaultToolkitParser(theEnv);
			theEnv.theDocumentParser = new org.quick.core.parser.QuickDomParser(theEnv);
			theEnv.theContentCreator = new QuickContentCreator();
			theEnv.theStyleParser = new DefaultStyleParser(theEnv);
			theEnv.thePropertyParser = new AntlrPropertyParser(theEnv);
			return this;
		}

		/**
		 * @param ctx The context for the environment
		 * @return This builder
		 */
		public Builder setContext(ExpressionContext ctx) {
			if (isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theContext = ctx;
			return this;
		}

		/**
		 * @param builder A function to build out a default expression context for the environment
		 * @return This builder This builder
		 */
		public Builder buildContext(Consumer<? super DefaultExpressionContext.Builder> builder) {
			if (isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			DefaultExpressionContext.Builder ctxBuilder = DefaultExpressionContext.build();
			builder.accept(ctxBuilder);
			theEnv.theContext = ctxBuilder.build();
			return this;
		}

		/**
		 * @param toolkitParser The toolkit parser for the environment
		 * @return This builder
		 */
		public Builder setToolkitParser(QuickToolkitParser toolkitParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theToolkitParser = toolkitParser;
			return this;
		}

		/**
		 * @param documentParser The document parser for the environment
		 * @return This builder
		 */
		public Builder setDocumentParser(QuickDocumentParser documentParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theDocumentParser = documentParser;
			return this;
		}

		/**
		 * @param contentCreator The content creator for the environment
		 * @return This builder
		 */
		public Builder setContentCreator(QuickContentCreator contentCreator) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theContentCreator = contentCreator;
			return this;
		}

		/**
		 * @param styleParser The style sheet parser for the environment
		 * @return This builder
		 */
		public Builder setStyleParser(QuickStyleParser styleParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theStyleParser = styleParser;
			return this;
		}

		/**
		 * @param propertyParser The property parser for the environment
		 * @return This builder
		 */
		public Builder setPropertyParser(QuickPropertyParser propertyParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.thePropertyParser = propertyParser;
			return this;
		}

		public Builder withDeath(Observable<?> death) {
			if (isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			death.noInit().take(1).takeUntil(theEnv.theDeath).act(v -> theEnv.theDeath.onNext(null));
			return this;
		}

		/** @return A new QuickEnvironment with this builder's settings */
		public QuickEnvironment build() {
			if(theEnv.theToolkitParser == null)
				throw new IllegalStateException("No toolkit parser set");
			if(theEnv.theDocumentParser == null)
				throw new IllegalStateException("No document parser set");
			if(theEnv.theContentCreator == null)
				throw new IllegalStateException("No content creator set");
			if(theEnv.theStyleParser == null)
				throw new IllegalStateException("No style parser set");
			if(theEnv.thePropertyParser == null)
				throw new IllegalStateException("No property parser set");
			isBuilt.set(true);
			return theEnv;
		}
	}
}
