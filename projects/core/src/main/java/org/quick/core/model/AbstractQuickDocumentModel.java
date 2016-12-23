package org.quick.core.model;

import org.qommons.IterableUtils;

/**
 * Provides most of the implementation needed for a {@link QuickDocumentModel}, supplying some caching and requiring the concrete subclass
 * only to define {@link #iterator()}
 */
public abstract class AbstractQuickDocumentModel implements QuickDocumentModel {
	private org.qommons.DemandCache<Float, Iterable<StyledSequenceMetric>> theMetricsCache;

	/** Creates the document */
	public AbstractQuickDocumentModel() {
		theMetricsCache = new org.qommons.DemandCache<>();
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
		if(start == 0) {
			ret = theMetricsCache.get(breakWidth);
			if(ret == null) {
				ret = IterableUtils.cachingIterable(QuickDocumentModel.super.metrics(start, breakWidth).iterator());
				theMetricsCache.put(breakWidth, ret);
			}
			return ret;
		} else
			return QuickDocumentModel.super.metrics(start, breakWidth);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		for (StyledSequence seq : this)
			str.append(seq);
		return str.toString();
	}
}
