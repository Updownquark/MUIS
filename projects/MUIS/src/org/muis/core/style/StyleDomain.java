package org.muis.core.style;

/** A domain for style attributes */
public interface StyleDomain extends Iterable<StyleAttribute<?>>
{
	/** @return The domain's name */
	String getName();

	/** An iterator type to iterate through an array of attributes */
	public class DomainAttributeIterator implements java.util.Iterator<StyleAttribute<?>>
	{
		private final StyleAttribute<?> [] theAttributes;

		int index;

		/** @param attribs The attributes to iterate through */
		public DomainAttributeIterator(StyleAttribute<?> [] attribs)
		{
			theAttributes = attribs;
		}

		@Override
		public boolean hasNext()
		{
			return index < theAttributes.length;
		}

		@Override
		public StyleAttribute<?> next()
		{
			return theAttributes[index++];
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException(
				"Remove is not supported by StyleDomain.iterator()");
		}
	}
}
