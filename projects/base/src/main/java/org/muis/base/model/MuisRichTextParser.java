package org.muis.base.model;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

import org.muis.core.*;
import org.muis.core.event.MuisEvent;
import org.muis.core.mgr.MuisMessage.Type;
import org.muis.core.mgr.MuisMessageCenter;
import org.muis.core.model.ModelValueReferenceParser;
import org.muis.core.model.MuisDocumentModel.StyledSequence;
import org.muis.core.model.MuisModelValue;
import org.muis.core.parser.MuisParseException;
import org.muis.core.style.*;

/** Parses and formats text in the MUIS rich format */
public class MuisRichTextParser {
	/**
	 * A simple style attribute - value pair
	 *
	 * @param <T> The type of the attribute
	 */
	public static class StyleValue<T> {
		/** The styel attribute */
		public final StyleAttribute<T> attr;

		/** The attribute value */
		public final T value;

		/**
		 * @param att The style attribute
		 * @param val The attribute value
		 */
		public StyleValue(StyleAttribute<T> att, T val) {
			attr = att;
			value = val;
		}
	}

	/** Style tags that this parser recognizes */
	public static final Map<String, StyleValue<?>> tags;

	static {
		Map<String, StyleValue<?>> tgs = new java.util.LinkedHashMap<>();
		try {
			tgs.put("b", new StyleValue<>(FontStyle.weight, FontStyle.weight.getType().parse(new NoMVParseEnv(null), "bold")));
			tgs.put("i", new StyleValue<>(FontStyle.slant, FontStyle.slant.getType().parse(new NoMVParseEnv(null), "italic")));
			tgs.put("ul", new StyleValue<>(FontStyle.underline, FontStyle.Underline.on));
			tgs.put("uldot", new StyleValue<>(FontStyle.underline, FontStyle.Underline.dotted));
			tgs.put("uldash", new StyleValue<>(FontStyle.underline, FontStyle.Underline.dashed));
			tgs.put("ulth", new StyleValue<>(FontStyle.underline, FontStyle.Underline.heavy));
			tgs.put("strike", new StyleValue<>(FontStyle.strike, true));
		} catch(org.muis.core.MuisException e) {
			throw new IllegalStateException("Could not build default rich font tags", e);
		}
		tags = java.util.Collections.unmodifiableMap(tgs);
	}

	/**
	 * Parses rich-formatted text into a document
	 *
	 * @param model The rich document to put the content into
	 * @param richText The text to parse
	 * @param env The parsing environment to use to parse styles
	 * @throws MuisException If the text cannot be parsed
	 */
	public void parse(RichDocumentModel model, String richText, MuisParseEnv env) throws MuisException {
		env = new NoMVParseEnv(env);
		boolean isEscaped = false;
		for(int i = 0; i < richText.length(); i++) {
			char ch = richText.charAt(i);
			if(isEscaped) {
				if(!isEscapable(ch))
					throw new MuisParseException("Escaped character '" + ch + "' is not escapable in rich text");
				model.append(ch);
				isEscaped = false;
			} else if(ch == '\\')
				isEscaped = true;
			else if(ch == '{')
				i = parseTag(model, richText, i, env);
			else if(ch == '}')
				throw new MuisParseException("Unmatched '}' found in rich text at character " + i);
			else
				model.append(ch);
		}
	}

	private static boolean isEscapable(char ch) {
		switch (ch) {
		case '{':
		case '}':
		case '\\':
			return true;
		default:
			return false;
		}
	}

	/**
	 * @param model The model to alter the style of
	 * @param richText The rich text content
	 * @param index The index of the tag in the text
	 * @param env The parse environment to use to parse the style
	 * @return The location of the end brace for the tag
	 * @throws MuisException If the tag cannot be parsed
	 */
	public int parseTag(RichDocumentModel model, String richText, int index, MuisParseEnv env) throws MuisException {
		if(richText.charAt(index) != '{')
			throw new MuisParseException("The character at index " + index + " is not '{'");

		int start = index + 1;
		int end;
		boolean isEscaped = false;
		int depth = 1;
		StringBuilder tagContent = new StringBuilder();
		for(end = start; end < richText.length() && depth > 0; end++) {
			char ch = richText.charAt(end);
			if(isEscaped) {
				if(!isEscapable(ch))
					throw new MuisParseException("Escaped character '" + ch + "' is not escapable in rich text");
				tagContent.append(ch);
				isEscaped = false;
			} else if(ch == '\\')
				isEscaped = true;
			else {
				tagContent.append(ch);
				if(ch == '}')
					depth--;
				else if(ch == '{')
					depth++;
			}
		}
		if(end == richText.length())
			throw new MuisParseException("Unmatched '{' found in rich text at character " + index);
		end--;

		tagContent.delete(tagContent.length() - 1, tagContent.length()); // Remove the terminal '}'
		trim(tagContent);
		if(tagContent.length() == 0)
			throw new MuisParseException("Empty tag '" + richText.substring(start - 1, end + 1) + "' found in rich text at character "
				+ index);
		if(tagContent.charAt(0) == '/') {
			tagContent.delete(0, 1);
			trim(tagContent);
			if(tagContent.length() == 0)
				throw new MuisParseException("Empty end tag '" + richText.substring(start - 1, end + 1)
					+ "' found in rich text at character "
					+ index);
			StyleValue<?> sv = tags.get(tagContent.toString());
			if(sv != null)
				model.clear(sv.attr);
			else {
				java.awt.Color color = org.muis.core.style.Colors.parseIfColor(tagContent.toString());
				if(color != null) {
					if(color.equals(model.last().getStyle().getLocal(FontStyle.color)))
						model.clear(FontStyle.color);
					else
						throw new MuisParseException("Font color at this location (" + index + ") is not " + tagContent);
				} else {
					String [] styles = org.muis.core.style.StyleParsingUtils.splitStyles(tagContent.toString());
					for(String style : styles) {
						int equalsIdx = style.indexOf('=');
						if(equalsIdx >= 0) {
							// Clearing several attributes at once in a common domain
							String domainName = style.substring(0, equalsIdx).trim();
							StyleDomain domain;
							try {
								Class<? extends StyleDomain> domainClass;
								domainClass = env.cv().loadMappedClass(domainName, StyleDomain.class);
								domain = (StyleDomain) domainClass.getMethod("getDomainInstance").invoke(null);
							} catch(MuisException e) {
								throw new MuisParseException("Unrecognized style domain '" + domainName + "' in end tag at character "
									+ index, e);
							} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
								| SecurityException e) {
								throw new MuisParseException("Could not get domain instance for domain '" + domainName
									+ "' in end tag at character " + index, e);
							}

							String attrStr = style.substring(equalsIdx + 1).trim();
							if(attrStr.length() == 0 || attrStr.charAt(0) != '{' || attrStr.charAt(attrStr.length() - 1) != '}')
								throw new MuisParseException("Unrecognized value for domain '" + domainName + "' in end tag at character "
									+ index);
							attrStr = attrStr.substring(1, attrStr.length() - 1);
							String [] attrs = attrStr.split(";");
							Map<String, Boolean> attrsHit = new java.util.HashMap<>();
							for(String attr : attrs)
								attrsHit.put(attr, false);

							for(StyleAttribute<?> attrib : domain)
								if(attrsHit.containsKey(attrib.getName()))
									attrsHit.put(attrib.getName(), true);
							for(Map.Entry<String, Boolean> hits : attrsHit.entrySet())
								if(!hits.getValue())
									throw new MuisParseException("No such style attribute '" + hits.getKey() + "' in domain '" + domainName
										+ "' in end tag at character " + index);

							for(StyleAttribute<?> attrib : domain)
								if(attrsHit.containsKey(attrib.getName()))
									model.clear(attrib);
						} else {
							// Clearing a single attribute
							int dotIdx = style.indexOf('.');
							if(dotIdx < 0)
								throw new MuisParseException("Unrecognized end tag '" + richText.substring(start - 1, end + 1)
									+ "' found in rich text at character " + index);
							String domainName = style.substring(0, dotIdx).trim();
							StyleDomain domain;
							try {
								Class<? extends StyleDomain> domainClass;
								domainClass = env.cv().loadMappedClass(domainName, StyleDomain.class);
								domain = (StyleDomain) domainClass.getMethod("getDomainInstance").invoke(null);
							} catch(MuisException e) {
								throw new MuisParseException("Unrecognized style domain '" + domainName + "' in end tag at character "
									+ index, e);
							} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
								| SecurityException e) {
								throw new MuisParseException("Could not get domain instance for domain '" + domainName
									+ "' in end tag at character " + index, e);
							}
							String attrName = style.substring(dotIdx + 1).trim();
							StyleAttribute<?> styleAttr = null;
							for(StyleAttribute<?> attrib : domain)
								if(attrib.getName().equals(attrName)) {
									styleAttr = attrib;
									break;
								}
							if(styleAttr == null)
								throw new MuisParseException("No such style attribute '" + attrName + "' in domain '" + domainName
									+ "' in end tag at character " + index);
							model.clear(styleAttr);
						}
					}
				}
			}
		} else {
			StyleValue<?> sv = tags.get(tagContent.toString());
			if(sv != null)
				set(model, sv);
			else {
				Color color = org.muis.core.style.Colors.parseIfColor(tagContent.toString());
				if(color != null)
					model.set(FontStyle.color, color);
				else {
					MuisStyle style;
					try {
						style = org.muis.core.style.attach.StyleAttributeType.parseStyle(env, tagContent.toString());
					} catch(MuisExceptionWrapper e) {
						throw new MuisParseException("Could not parse tag " + richText.substring(start - 1, end + 1) + " at index " + index
							+ ": " + e.getMessage(), e.getCause());
					}
					for(StyleAttribute<?> att : style)
						set(model, att, style);
				}
			}
		}
		return end;
	}

	private void trim(StringBuilder str) {
		while(str.length() > 0 && Character.isWhitespace(str.charAt(0)))
			str.delete(0, 1);
		while(str.length() > 0 && Character.isWhitespace(str.charAt(str.length() - 1)))
			str.delete(str.length() - 1, str.length());
	}

	private <T> void set(RichDocumentModel model, StyleValue<T> sv) {
		model.set(sv.attr, sv.value);
	}

	private <T> void set(RichDocumentModel model, StyleAttribute<T> att, MuisStyle style) {
		model.set(att, style.get(att));
	}

	/**
	 * Formats a rich model to rich-formatted text
	 *
	 * @param model The rich model to format the content and styles of
	 * @return Rich-formatted text that would parse to give the same style and content as the given model
	 */
	public String format(RichDocumentModel model) {
		StringBuilder ret = new StringBuilder();
		MuisStyle lastStyle = null;
		for(StyledSequence seq : model) {
			Map<StyleDomain, Set<StyleAttribute<?>>> genericAtts = new java.util.HashMap<>();
			if(lastStyle != null) {
				for(StyleAttribute<?> att : lastStyle.localAttributes()) {
					if(seq.getStyle().isSet(att))
						continue;

					Object value = lastStyle.get(att);
					if(att == FontStyle.color)
						ret.append('{').append('/').append(Colors.toString((Color) value)).append('}');
					else {
						boolean found = false;
						for(Map.Entry<String, StyleValue<?>> tag : tags.entrySet()) {
							if(tag.getValue().attr == att && tag.getValue().value.equals(value)) {
								found = true;
								ret.append('{').append('/').append(tag.getKey()).append('}');
								break;
							}
						}
						if(!found) {
							Set<StyleAttribute<?>> atts = genericAtts.get(att.getDomain());
							if(atts == null) {
								atts = new java.util.LinkedHashSet<>();
								genericAtts.put(att.getDomain(), atts);
							}
							atts.add(att);
						}
					}
				}
				if(!genericAtts.isEmpty()) {
					ret.append('{').append('/');
					for(Map.Entry<StyleDomain, Set<StyleAttribute<?>>> entry : genericAtts.entrySet()) {
						Set<StyleAttribute<?>> atts = entry.getValue();
						if(atts.size() > 1) {
							ret.append(entry.getKey().getName()).append('=').append('{');
							boolean first = true;
							for(StyleAttribute<?> att : atts) {
								if(first)
									first = false;
								else
									ret.append(';');
								ret.append(att.getName());
							}
							ret.append('}');
						} else
							ret.append(entry.getKey().getName()).append('.').append(atts.iterator().next().getName());
					}
					ret.append('}');
				}
				genericAtts.clear();
			}
			for(StyleAttribute<?> att : seq.getStyle().localAttributes()) {
				Object value = seq.getStyle().get(att);
				if(lastStyle != null && lastStyle.get(att) == value)
					continue;
				if(att == FontStyle.color)
					ret.append('{').append(Colors.toString((Color) value)).append('}');
				else {
					boolean found = false;
					for(Map.Entry<String, StyleValue<?>> tag : tags.entrySet()) {
						if(tag.getValue().attr == att && tag.getValue().value.equals(value)) {
							found = true;
							ret.append('{').append(tag.getKey()).append('}');
							break;
						}
					}
					if(!found) {
						Set<StyleAttribute<?>> atts = genericAtts.get(att.getDomain());
						if(atts == null) {
							atts = new java.util.LinkedHashSet<>();
							genericAtts.put(att.getDomain(), atts);
						}
						atts.add(att);
					}
				}
			}
			if(!genericAtts.isEmpty()) {
				ret.append('{');
				for(Map.Entry<StyleDomain, Set<StyleAttribute<?>>> entry : genericAtts.entrySet()) {
					Set<StyleAttribute<?>> atts = entry.getValue();
					if(atts.size() > 1) {
						ret.append(entry.getKey().getName()).append('=').append('{');
						boolean first = true;
						for(StyleAttribute<?> att : atts) {
							if(first)
								first = false;
							else
								ret.append(';');
							ret.append(att.getName()).append('=').append(seq.getStyle().get(att));
						}
						ret.append('}');
					} else
						ret.append(entry.getKey().getName()).append('.').append(atts.iterator().next().getName()).append('=')
							.append(seq.getStyle().get(atts.iterator().next()));
				}
				ret.append('}');
			}
			for(int i = 0; i < seq.length(); i++) {
				char ch = seq.charAt(i);
				switch (ch) {
				case '\\':
				case '{':
				case '}':
					ret.append('\\');
					//$FALL-THROUGH$
				default:
					ret.append(ch);
				}
			}
		}
		return ret.toString();
	}

	private static class NoMVParseEnv implements MuisParseEnv {
		private final MuisParseEnv theWrapped;

		private final MuisMessageCenter theMsg;

		private final ModelValueReferenceParser theMVRParser;

		NoMVParseEnv(MuisParseEnv wrap) {
			theWrapped = wrap;
			theMsg = theWrapped == null ? null : new org.muis.core.mgr.MutatingMessageCenter(theWrapped.msg()) {
				@Override
				public void message(Type type, String text, MuisEvent cause, Throwable exception, Object... params) {
					if(type.compareTo(Type.ERROR) >= 0)
						throw new MuisExceptionWrapper(text, exception);
					super.message(type, text, cause, exception, params);
				}
			};
			theMVRParser = new ModelValueReferenceParser() {

				@Override
				public int getNextMVR(String value, int mvrStart) {
					return -1;
				}

				@Override
				public String extractMVR(String value, int mvrStart) throws MuisParseException {
					return null;
				}

				@Override
				public MuisModelValue<?> parseMVR(String mvr) throws MuisParseException {
					return null;
				}
			};
		}

		@Override
		public MuisClassView cv() {
			return theWrapped.cv();
		}

		@Override
		public MuisMessageCenter msg() {
			return theMsg;
		}

		@Override
		public MuisDocument doc() {
			return theWrapped.doc();
		}

		@Override
		public ModelValueReferenceParser getModelParser() {
			return theMVRParser;
		}
	}

	private static class MuisExceptionWrapper extends RuntimeException {
		public MuisExceptionWrapper(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
