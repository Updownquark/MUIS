package org.wam.style;

/**
 * A domain for style attributes
 */
public interface StyleDomain extends Iterable<StyleAttribute<?>>
{
	/**
	 * @return The domain's name
	 */
	String getName();

	/**
	 * An iterator type to iterate through an array of attributes
	 */
	class DomainAttributeIterator implements java.util.Iterator<StyleAttribute<?>>
	{
		private final StyleAttribute<?> [] theAttributes;

		int index;

		DomainAttributeIterator(StyleAttribute<?> [] attribs)
		{
			theAttributes = attribs;
		}

		public boolean hasNext()
		{
			return index < theAttributes.length;
		}

		public StyleAttribute<?> next()
		{
			return theAttributes[index++];
		}

		public void remove()
		{
			throw new UnsupportedOperationException(
				"Remove is not supported by StyleDomain.iterator()");
		}
	}
}
