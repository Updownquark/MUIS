package org.quick.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides most of the implementation needed for a {@link QuickDocumentModel}, supplying some caching and requiring the concrete subclass
 * only to define {@link #iterator()}
 */
public abstract class AbstractQuickDocumentModel implements QuickDocumentModel {
	private org.qommons.DemandCache<Float, List<StyledSequenceMetric>> theMetricsCache;

	/** Creates the document */
	public AbstractQuickDocumentModel() {
		theMetricsCache = new org.qommons.DemandCache<>(true);
		theMetricsCache.setPreferredSize(5);
		theMetricsCache.setHalfLife(60000);
	}

	/** Clears this document's internal metrics cache. This call is needed whenever the document's content or style changes. */
	protected void clearCache() {
		theMetricsCache.clear();
	}

	@Override
	public Iterable<StyledSequenceMetric> metrics(final int start, final float breakWidth) {
		Iterable<StyledSequenceMetric> ret;
		if (start == 0)
			ret = theMetricsCache.computeIfAbsent(breakWidth, w -> //
			toList(QuickDocumentModel.super.metrics(start, breakWidth)));
		else
			ret = QuickDocumentModel.super.metrics(start, breakWidth);
		return ret;
	}

	private static List<StyledSequenceMetric> toList(Iterable<StyledSequenceMetric> metrics) {
		ArrayList<StyledSequenceMetric> cached = new ArrayList<>();
		for (StyledSequenceMetric metric : metrics)
			cached.add(metric);
		cached.trimToSize();
		return java.util.Collections.unmodifiableList(cached);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		for (StyledSequence seq : this)
			str.append(seq);
		return str.toString();
	}
}
