package org.quick.core.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.quick.core.QuickToolkit;

/** Represents the version of a resource such as a {@link QuickToolkit} */
public final class Version {
	/** The pattern by which version strings are parsed */
	public static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?<tag>[a-zA-Z0-9!-\\)\\[\\]\\{\\}]+)?");
	/** The major revision of this version */
	public final int major;
	/** The minor revision of this version */
	public final int minor;
	/** The patch of this version */
	public final int patch;
	/** The tag for this version (may be null) */
	public final String tag;

	/**
	 * @param maj The major revision for this version
	 * @param min The minor revision for this version
	 * @param ptch The patch for this version
	 * @param tg The tag of this version (may be null)
	 */
	public Version(int maj, int min, int ptch, String tg) {
		major = maj;
		minor = min;
		patch = ptch;
		tag = tg;
	}

	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(!(o instanceof Version))
			return false;
		Version v = (Version) o;
		return major == v.major && minor == v.minor && patch == v.patch && java.util.Objects.equals(tag, v.tag);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(major, minor, patch, tag);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(major).append('.').append(minor).append('.').append(patch);
		if(tag != null)
			str.append(tag);
		return str.toString();
	}

	/**
	 * Parses a version from a string
	 * 
	 * @param version The version string
	 * @return The parsed version
	 */
	public static Version fromString(String version) {
		Matcher matcher = VERSION_PATTERN.matcher(version);
		if(!matcher.matches())
			throw new IllegalArgumentException(version + " is not a valid version.");
		int major = Integer.parseInt(matcher.group(1));
		int minor = Integer.parseInt(matcher.group(2));
		int patch = Integer.parseInt(matcher.group(3));
		String tag = matcher.group("tag");
		return new Version(major, minor, patch, tag);
	}
}
