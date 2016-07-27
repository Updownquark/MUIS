package org.quick.core.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Version {
	public static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?<tag>[a-zA-Z0-9!-\\)\\[\\]\\{\\}]+)?");
	public final int major;
	public final int minor;
	public final int patch;
	public final String tag;

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
