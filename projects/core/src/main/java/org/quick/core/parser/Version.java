package org.quick.core.parser;

public final class Version {
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
}
