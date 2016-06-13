package org.quick.core.model;

import java.util.*;
import java.util.function.Consumer;

import org.quick.core.parser.QuickParseException;

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

	public void validate(QuickModelConfig config) throws QuickParseException {
		validateChildren(config, theConstraints, acceptsUnmatched);
	}

	private static void validateChildren(QuickModelConfig config, Map<String, List<Constraint>> constraints, boolean acceptUnmatched)
		throws QuickParseException {
		Map<String, Integer> counts = new HashMap<>();
		for (Map.Entry<String, Object> cfg : config.getAllConfigs()) {
			Integer count = counts.get(cfg.getKey());
			if (count == null)
				count = Integer.valueOf(1);
			else
				count = Integer.valueOf(count + 1);
			counts.put(cfg.getKey(), count);
		}
		for (Map.Entry<String, Object> cfg : config.getAllConfigs()) {
			List<Constraint> constraint = constraints.get(cfg.getKey());
			if (constraint == null) {
				if (acceptUnmatched)
					continue;
				else
					throw new QuickParseException("Unexpected config point: " + cfg.getKey());
			}
			int count = counts.get(cfg.getKey());
			for (Constraint c : constraint)
				c.validate(config, count);
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

	public interface ChildConstraintBuilder {
		ChildConstraintBuilder withUnmatched();

		ChildConstraintBuilder withConfig(String... childNames);

		ChildConstraintBuilder forConfig(String childName, Consumer<ConstraintBuilder> builder);
	}

	public static Builder build() {
		return new Builder();
	}

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

		public QuickModelConfigValidator build() {
			return new QuickModelConfigValidator(QuickModelConfigValidator.build(theConstraints), acceptUnmatched);
		}
	}

	public static class ConstraintBuilder implements ChildConstraintBuilder, ConstraintBuilderInternal {
		private final String path;
		private int min;
		private int max = 1;
		private Boolean withText = Boolean.FALSE;
		private Map<String, List<ConstraintBuilder>> childConstraints;
		private boolean acceptUnmatched;

		private ConstraintBuilder(String path) {
			this.path = path;
		}

		public ConstraintBuilder required() {
			min = 1;
			return this;
		}

		public ConstraintBuilder atLeast(int times) {
			min = times;
			if (max == 1)
				max = Integer.MAX_VALUE;
			return this;
		}

		public ConstraintBuilder atMost(int times) {
			max = times;
			return this;
		}

		public ConstraintBuilder between(int min, int max) {
			this.min = min;
			this.max = max;
			return this;
		}

		public ConstraintBuilder withText(boolean optional) {
			withText = optional ? null : Boolean.TRUE;
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
				List<ConstraintBuilder> cs = childConstraints.get(childName);
				if (cs == null) {
					cs = new ArrayList<>();
					childConstraints.put(childName, cs);
				}
				ConstraintBuilder childBuilder = new ConstraintBuilder(childName);
				cs.add(childBuilder);
			}
			return this;
		}

		@Override
		public ConstraintBuilder forConfig(String childName, Consumer<ConstraintBuilder> builder) {
			List<ConstraintBuilder> cs = childConstraints.get(childName);
			if (cs == null) {
				cs = new ArrayList<>();
				childConstraints.put(childName, cs);
			}
			ConstraintBuilder childBuilder = new ConstraintBuilder(path + "/" + childName);
			cs.add(childBuilder);
			builder.accept(childBuilder);
			return this;
		}

		@Override
		public ConfigChecker build() {
			return new ConfigChecker(path, min, max, withText, QuickModelConfigValidator.build(childConstraints), acceptUnmatched);
		}
	}
}
