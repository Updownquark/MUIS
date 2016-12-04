package org.quick.core.model;

import java.util.*;
import java.util.function.Consumer;

import org.quick.core.parser.QuickParseException;

/** Enforces custom constraints on {@link QuickModelConfig} instances */
public class QuickModelConfigValidator {
	private interface Constraint {
		void validate(QuickModelConfig config, int count) throws QuickParseException;
	}

	private interface ConstraintBuilderInternal {
		Constraint build();
	}

	private static class ConfigChecker implements Constraint {
		final String path;
		final int min;
		final int max;
		final Boolean withText;
		final Map<String, List<Constraint>> childConstraints;
		final boolean acceptsUnmatched;

		ConfigChecker(String path, int min, int max, Boolean withText, Map<String, List<Constraint>> childConstraints,
			boolean withUnmatched) {
			this.path = path;
			this.min = min;
			this.max = max;
			this.withText = withText;
			this.childConstraints = childConstraints;
			acceptsUnmatched = withUnmatched;
		}

		@Override
		public void validate(QuickModelConfig config, int count) throws QuickParseException {
			if (min == 1 && count == 0)
				throw new QuickParseException(path + " is required");
			else if (min == 0 && count > max)
				throw new QuickParseException(path + " is allowed a maximum of " + max + " times: " + count);
			else if (max == Integer.MAX_VALUE && count < min)
				throw new QuickParseException(path + " must be specified at least " + min + " times: " + count);
			if (count < min || count > max)
				throw new QuickParseException(path + " is required between " + min + " and " + max + " times: " + count);
			if (withText != null) {
				if (withText && config.getText() == null)
					throw new QuickParseException(path + " requires text content");
				else if (!withText && config.getText() != null)
					throw new QuickParseException(path + " does not allow text content");
			}
			validateChildren(config, childConstraints, acceptsUnmatched);
		}
	}

	private final Map<String, List<Constraint>> theConstraints;
	private final boolean acceptsUnmatched;

	private QuickModelConfigValidator(Map<String, List<Constraint>> constraints, boolean withUnmatched) {
		theConstraints = constraints;
		acceptsUnmatched = withUnmatched;
	}

	/**
	 * @param config The config to validate
	 * @throws QuickParseException If the config fails validation
	 */
	public void validate(QuickModelConfig config) throws QuickParseException {
		validateChildren(config, theConstraints, acceptsUnmatched);
	}

	private static void validateChildren(QuickModelConfig config, Map<String, List<Constraint>> constraints, boolean acceptUnmatched)
		throws QuickParseException {
		Map<String, Integer> counts = new HashMap<>();
		for (Map.Entry<String, QuickModelConfig> cfg : config.getAllConfigs())
			counts.compute(cfg.getKey(), (k, oldCount) -> oldCount == null ? 1 : oldCount + 1);
		for (Map.Entry<String, QuickModelConfig> cfg : config.getAllConfigs()) {
			List<Constraint> constraint = constraints.get(cfg.getKey());
			if (constraint == null) {
				if (acceptUnmatched)
					continue;
				else
					throw new QuickParseException(
						"Unexpected config point: " + cfg.getKey() + ". Expected one of " + constraints.keySet());
			}
			int count = counts.get(cfg.getKey());
			for (Constraint c : constraint)
				c.validate(cfg.getValue(), count);
		}
	}

	private static Map<String, List<Constraint>> build(Map<String, List<ConstraintBuilder>> builders) {
		Map<String, List<Constraint>> built = new LinkedHashMap<>();
		for (Map.Entry<String, List<ConstraintBuilder>> entry : builders.entrySet()) {
			List<Constraint> constraints = new ArrayList<>();
			for (ConstraintBuilderInternal builder : entry.getValue())
				constraints.add(builder.build());
			built.put(entry.getKey(), constraints);
		}
		return Collections.unmodifiableMap(built);
	}

	/** Builds contraints for sub-configs */
	public interface ChildConstraintBuilder {
		/**
		 * Sets this builder to allow unmatched config points
		 *
		 * @return This builder
		 */
		ChildConstraintBuilder withUnmatched();

		/**
		 * @param childNames The names of the children to allow
		 * @return This builder
		 */
		ChildConstraintBuilder withConfig(String... childNames);

		/**
		 * @param childName The name of the child to allow
		 * @param builder Builds a constraint for children with the given name
		 * @return This builder
		 */
		ChildConstraintBuilder forConfig(String childName, Consumer<ConstraintBuilder> builder);
	}

	/** @return A builder to build a validator */
	public static Builder build() {
		return new Builder();
	}

	/** Builds {@link QuickModelConfigValidator}s */
	public static class Builder implements ChildConstraintBuilder {
		private final Map<String, List<ConstraintBuilder>> theConstraints;
		private boolean acceptUnmatched;

		private Builder() {
			theConstraints = new LinkedHashMap<>();
		}

		@Override
		public Builder withUnmatched() {
			acceptUnmatched = true;
			return this;
		}

		@Override
		public Builder withConfig(String... childNames) {
			for (String childName : childNames) {
				List<ConstraintBuilder> cs = theConstraints.get(childName);
				if (cs == null) {
					cs = new ArrayList<>();
					theConstraints.put(childName, cs);
				}
				ConstraintBuilder childBuilder = new ConstraintBuilder(childName);
				childBuilder.withText(true);
				cs.add(childBuilder);
			}
			return this;
		}

		@Override
		public Builder forConfig(String childName, Consumer<ConstraintBuilder> builder) {
			List<ConstraintBuilder> cs = theConstraints.get(childName);
			if (cs == null) {
				cs = new ArrayList<>();
				theConstraints.put(childName, cs);
			}
			ConstraintBuilder childBuilder = new ConstraintBuilder(childName);
			cs.add(childBuilder);
			builder.accept(childBuilder);
			return this;
		}

		/** @return The built validator */
		public QuickModelConfigValidator build() {
			return new QuickModelConfigValidator(QuickModelConfigValidator.build(theConstraints), acceptUnmatched);
		}
	}

	/** Builds constraints for child configs */
	public static class ConstraintBuilder implements ChildConstraintBuilder, ConstraintBuilderInternal {
		private final String thePath;
		private int theMin;
		private int theMax = 1;
		private Boolean isWithText = Boolean.FALSE;
		private final Map<String, List<ConstraintBuilder>> childConstraints = new LinkedHashMap<>();
		private boolean acceptUnmatched;

		private ConstraintBuilder(String path) {
			this.thePath = path;
		}

		/**
		 * Specifies this child constraint as required
		 *
		 * @return This builder
		 */
		public ConstraintBuilder required() {
			theMin = 1;
			return this;
		}

		/**
		 * @param times The min number of times this child may be specified
		 * @return This builder
		 */
		public ConstraintBuilder atLeast(int times) {
			theMin = times;
			if (theMax == 1)
				theMax = Integer.MAX_VALUE;
			return this;
		}

		/**
		 * @param times The max number of times this child may be specified
		 * @return This builder
		 */
		public ConstraintBuilder atMost(int times) {
			theMax = times;
			return this;
		}

		/**
		 * @param min The min number of times this child may be specified
		 * @param max The max number of times this child may be specified
		 * @return This builder
		 */
		public ConstraintBuilder between(int min, int max) {
			this.theMin = min;
			this.theMax = max;
			return this;
		}

		/**
		 * Specifies that this child may be present any number of times
		 *
		 * @return This builder
		 */
		public ConstraintBuilder anyTimes() {
			theMin = 0;
			theMax = Integer.MAX_VALUE;
			return this;
		}

		/**
		 * @param withText Whether this child config must or must not specify text
		 * @return This builder
		 */
		public ConstraintBuilder withText(boolean withText) {
			isWithText = withText ? null : Boolean.TRUE;
			return this;
		}

		@Override
		public ConstraintBuilder withUnmatched() {
			acceptUnmatched = true;
			return this;
		}

		@Override
		public ConstraintBuilder withConfig(String... childNames) {
			for (String childName : childNames) {
				List<ConstraintBuilder> cs = childConstraints.computeIfAbsent(childName, n -> new ArrayList<>());
				ConstraintBuilder childBuilder = new ConstraintBuilder(childName);
				cs.add(childBuilder);
			}
			return this;
		}

		@Override
		public ConstraintBuilder forConfig(String childName, Consumer<ConstraintBuilder> builder) {
			List<ConstraintBuilder> cs = childConstraints.computeIfAbsent(childName, n -> new ArrayList<>());
			if (cs == null) {
				cs = new ArrayList<>();
				childConstraints.put(childName, cs);
			}
			ConstraintBuilder childBuilder = new ConstraintBuilder(thePath + "/" + childName);
			cs.add(childBuilder);
			builder.accept(childBuilder);
			return this;
		}

		@Override
		public ConfigChecker build() {
			return new ConfigChecker(thePath, theMin, theMax, isWithText, QuickModelConfigValidator.build(childConstraints),
				acceptUnmatched);
		}
	}
}
