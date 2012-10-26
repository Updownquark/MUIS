package org.muis.core;

/**
 * A MuisPermission is a request or a requirement for a toolkit to use a capability. According to the environment's security policy, these
 * permissions may be granted automatically, presented to the user for acceptance or rejection, or denied automatically.
 */
public class MuisPermission
{
	/** The type of permission requested */
	public static enum Type
	{
		/** Gives an application access to the client's CPU */
		CPU("Processor Time", "cpu", ProcessorSubTypes.values()),
		/** Gives an application access to the client's transient (fast-access) memory */
		Memory("Memory Access", "memory", MemorySubTypes.values()),
		/** Gives an application access to all or part of the user's profile */
		ProfileAccess("Profile Access", "profile", ProfileAccessSubTypes.values()),
		/**
		 * Gives an application access to read and/or write to all or part of the persistent storage connected to the user's computer
		 */
		Storage("Persistent Storage", "storage", StorageSubTypes.values()),
		/** Gives an application permission to create network connections */
		NetworkAccess("Network Access", "network", NetworkSubTypes.values()),
		/** Gives an application access to printers available to the computer */
		PrinterAccess("Printer Access", "printer", new SubType[0]),
		/**
		 * Gives an application permission to spawn its own threads to do work without waiting for user events
		 */
		CreateThread("Create Thread", "thread", ThreadSubTypes.values()),
		/** Gives an application permission to spawn external processes */
		CreateProcess("Create Process", "process", ProcessSubTypes.values()),
		/** Gives an application permission to use JNI to run native code */
		NativeCode("Native Code", "native", new SubType[0]),
		/** Gives an application permission to create new UI windows to display information */
		CreateWindow("Create Window", "window", WindowSubTypes.values()),
		/** Gives an application permission to make its user interface full-screen */
		FullScreen("Full Screen", "fullscreen", new SubType[0]);

		/** The display for the permission type */
		public final String display;

		/** The value of the type attribute used to create a permission of this type */
		public final String key;

		private final SubType [] theSubTypes;

		Type(String disp, String k, SubType [] subTypes)
		{
			display = disp;
			key = k;
			theSubTypes = subTypes;
		}

		/**
		 * @return The subtypes available for this type
		 */
		public SubType [] getSubTypes()
		{
			return theSubTypes;
		}

		/**
		 * @param key The key of the permission type
		 * @return The permission type whose key is given, or null if no such permission type exists
		 */
		public static Type byKey(String key)
		{
			for(Type t : values())
				if(t.key.equals(key))
					return t;
			return null;
		}
	}

	/** A sub-type of permission requested */
	public static interface SubType
	{
		/**
		 * @return The display for the sub type
		 */
		String getDisplay();

		/**
		 * @return The attribute value used to key to this sub type
		 */
		String getKey();

		/**
		 * @return The parameters associated with the subtype
		 */
		Parameter [] getParameters();
	}

	/** A parameter that may be specified for a sub-type to further define the requested permission */
	public static interface Parameter
	{
		/**
		 * @return The displayable name of the parameter
		 */
		String getName();

		/**
		 * @return The attribute-value reference of the parameter
		 */
		String getKey();

		/**
		 * Validates the parameter value
		 *
		 * @param value The value specified
		 * @return An error string to display, or null if the value is valid
		 */
		String validate(String value);
	}

	static String validateIntParam(String value, String key, int min, int max)
	{
		if(value == null)
			return "No " + key + " specified";
		int ret;
		try
		{
			ret = Integer.parseInt(value);
		} catch(NumberFormatException e)
		{
			return key + " specified is not a number: " + value;
		}
		if(ret < min || ret > max)
			return key + " specified must be between " + min + " and " + max;
		return null;
	}

	static String validateFloatParam(String value, String key, float min, float max)
	{
		if(value == null)
			return "No " + key + " specified";
		float ret;
		try
		{
			ret = Float.parseFloat(value);
		} catch(NumberFormatException e)
		{
			return key + " specified is not a number: " + value;
		}
		if(ret < min || ret > max)
			return key + " specified must be between " + min + " and " + max;
		return null;
	}

	static String validateBoolParam(String value, String key)
	{
		if(value == null)
			return null; // Assumed false
		if("true".equalsIgnoreCase(key) || "false".equalsIgnoreCase(key))
			return null;
		return key + " must be either true or false";
	}

	/** Subtypes for {@link Type#CPU} */
	public static enum ProcessorSubTypes implements SubType
	{
		/** The percent of processor time a toolkit is requesting to use for very short bursts */
		SecondPercent("second"),
		/** The percent of processor time a toolkit is requesting to use for finite periods */
		MinutePercent("minute"),
		/**
		 * The overall percent of processor time a toolkit is requesting to use over the lifetime of the application
		 */
		TotalPercent("long-term");

		private final String theKey;

		ProcessorSubTypes(String key)
		{
			theKey = key;
		}

		@Override
		public String getDisplay()
		{
			return name();
		}

		@Override
		public String getKey()
		{
			return theKey;
		}

		@Override
		public Parameter [] getParameters()
		{
			return new Parameter[] {new Parameter() {
				@Override
				public String getName()
				{
					return "Percent Usage";
				}

				@Override
				public String getKey()
				{
					return "percent";
				}

				@Override
				public String validate(String value)
				{
					return validateIntParam(value, getKey(), 5, 100);
				}
			}};
		}
	}

	/** Subtypes for {@link Type#Memory} */
	public static enum MemorySubTypes implements SubType
	{
		/** The percent of system memory the toolkit is requesting to use for short periods */
		MinuteAmount("minute"),
		/**
		 * The percent of system memory the toolkit is requesting to use over the lifetime of the application
		 */
		TotalAmount("long-term");

		private final String theKey;

		MemorySubTypes(String key)
		{
			theKey = key;
		}

		@Override
		public String getDisplay()
		{
			return name();
		}

		@Override
		public String getKey()
		{
			return theKey;
		}

		@Override
		public Parameter [] getParameters()
		{
			return new Parameter[] {new Parameter() {
				@Override
				public String getName()
				{
					return "Percent Usage";
				}

				@Override
				public String getKey()
				{
					return "percent";
				}

				@Override
				public String validate(String value)
				{
					return validateIntParam(value, getKey(), 5, 100);
				}
			}};
		}
	}

	/** Subtypes for {@link Type#ProfileAccess} */
	public static enum ProfileAccessSubTypes implements SubType
	{
		/** Represents a request by the toolkit for access to the user's profile name */
		Name("name"),
		/** Represents a request by the toolkit for access to the user profile's age */
		Age("age"),
		/** Represents a request by the toolkit for access to the user profile's gender */
		Gender("gender"),
		/** Represents a request by the toolkit for access to the user profile's marital status */
		MaritalStatus("marital-status"),
		/** Represents a request by the toolkit for access to the user profile's location */
		Location("location"),
		/** Represents a request by the toolkit for access to the user profile's occupation */
		Occupation("occupation"),
		/** Represents a request by the toolkit for complete access to the user profile */
		All("all");

		private final String theKey;

		ProfileAccessSubTypes(String key)
		{
			theKey = key;
		}

		@Override
		public String getDisplay()
		{
			return name();
		}

		@Override
		public String getKey()
		{
			return theKey;
		}

		@Override
		public Parameter [] getParameters()
		{
			return new Parameter[0];
		}
	}

	/** Subtypes for {@link Type#Storage} */
	public static enum StorageSubTypes implements SubType
	{
		/** Represents a request by the toolkit for access to a private storage directory */
		Private("private"),
		/**
		 * Represents a request by the toolkit for access to a particular location on the user's system
		 */
		Specific("specific"),
		/**
		 * Represents a request by the toolkit for access to the all of the user's system that the user himself has access to
		 */
		General("all");

		private final String theKey;

		StorageSubTypes(String key)
		{
			theKey = key;
		}

		@Override
		public String getDisplay()
		{
			return name();
		}

		@Override
		public String getKey()
		{
			return theKey;
		}

		@Override
		public Parameter [] getParameters()
		{
			switch (this)
			{
			case Private:
				return new Parameter[] {new Parameter() {
					@Override
					public String getName()
					{
						return "Amount (MB)";
					}

					@Override
					public String getKey()
					{
						return "amount";
					}

					@Override
					public String validate(String value)
					{
						return validateFloatParam(value, getKey(), 0, 1E9f);
					}

				}};
			case Specific:
				return new Parameter[] {new Parameter() {
					@Override
					public String getName()
					{
						return "Location";
					}

					@Override
					public String getKey()
					{
						return "location";
					}

					@Override
					public String validate(String value)
					{
						// TODO validate path?
						return null;
					}
				}, new Parameter() {
					@Override
					public String getName()
					{
						return "Writeable";
					}

					@Override
					public String getKey()
					{
						return "write";
					}

					@Override
					public String validate(String value)
					{
						return validateBoolParam(value, getKey());
					}
				}};
			case General:
				return new Parameter[] {new Parameter() {
					@Override
					public String getName()
					{
						return "Writeable";
					}

					@Override
					public String getKey()
					{
						return "write";
					}

					@Override
					public String validate(String value)
					{
						return validateBoolParam(value, getKey());
					}
				}};
			}
			throw new IllegalStateException("Unrecognized storage sub-type: " + this);
		}
	}

	/** Subtypes for {@link Type#NetworkAccess} */
	public static enum NetworkSubTypes implements SubType
	{
		/**
		 * Specifies that the toolkit wants to be able to make HTTP network calls to its home server
		 */
		Home("home"),
		/**
		 * Specifies that the toolkit wants to be able to make HTTP network calls to a specific location
		 */
		Specific("specific"),
		/**
		 * Specifies that the toolkit wants to be able to make HTTP network calls to any location it wants
		 */
		GeneralHttp("http"),
		/**
		 * Specifies that the toolkit wants to be able to open sockets to transfer any data in any format from any location
		 */
		GeneralSocket("all");

		private final String theKey;

		NetworkSubTypes(String key)
		{
			theKey = key;
		}

		@Override
		public String getDisplay()
		{
			return name();
		}

		@Override
		public String getKey()
		{
			return theKey;
		}

		@Override
		public Parameter [] getParameters()
		{
			switch (this)
			{
			case Specific:
				return new Parameter[] {new Parameter() {
					@Override
					public String getName()
					{
						return "Location";
					}

					@Override
					public String getKey()
					{
						return "location";
					}

					@Override
					public String validate(String value)
					{
						// TODO validate URL location?
						return null;
					}
				}};
			case Home:
			case GeneralHttp:
			case GeneralSocket:
				return new Parameter[0];
			}
			throw new IllegalStateException("Unrecognized network sub-type: " + this);
		}
	}

	/** Subtypes for {@link Type#CreateThread} */
	public static enum ThreadSubTypes implements SubType
	{
		/** Specifies the maximum number of threads the toolkit wants to create */
		Number("number");

		private final String theKey;

		ThreadSubTypes(String key)
		{
			theKey = key;
		}

		@Override
		public String getDisplay()
		{
			return "";
		}

		@Override
		public String getKey()
		{
			return theKey;
		}

		@Override
		public Parameter [] getParameters()
		{
			return new Parameter[] {new Parameter() {
				@Override
				public String getName()
				{
					return "Number";
				}

				@Override
				public String getKey()
				{
					return "number";
				}

				@Override
				public String validate(String value)
				{
					return validateIntParam(value, getKey(), 0, 100);
				}
			}};
		}
	}

	/** Subtypes for {@link Type#CreateProcess} */
	public static enum ProcessSubTypes implements SubType
	{
		/** Specifies a process command that the toolkit wants to spawn */
		Specific("specific"),
		/** Specifies that the toolkit wants to be able to call any command-line program */
		General("all");

		private final String theKey;

		ProcessSubTypes(String key)
		{
			theKey = key;
		}

		@Override
		public String getDisplay()
		{
			return "";
		}

		@Override
		public String getKey()
		{
			return theKey;
		}

		@Override
		public Parameter [] getParameters()
		{
			switch (this)
			{
			case Specific:
				return new Parameter[] {new Parameter() {
					@Override
					public String getName()
					{
						return "Command Name";
					}

					@Override
					public String getKey()
					{
						return "command";
					}

					@Override
					public String validate(String value)
					{
						// TODO validate command?
						return null;
					}
				}};
			case General:
				return new Parameter[0];
			}
			throw new IllegalStateException("Unrecognized process sub-type: " + this);
		}
	}

	/** Subtypes for {@link Type#CreateWindow} */
	public static enum WindowSubTypes implements SubType
	{
		/**
		 * Specifies the maximum number of simulataneous windows (in addition to the root window) that the toolkit wants to create
		 */
		Number("number");

		private final String theKey;

		WindowSubTypes(String key)
		{
			theKey = key;
		}

		@Override
		public String getDisplay()
		{
			return "";
		}

		@Override
		public String getKey()
		{
			return theKey;
		}

		@Override
		public Parameter [] getParameters()
		{
			return new Parameter[] {new Parameter() {
				@Override
				public String getName()
				{
					return "Number";
				}

				@Override
				public String getKey()
				{
					return "number";
				}

				@Override
				public String validate(String value)
				{
					return validateIntParam(value, getKey(), 0, 100);
				}
			}};
		}
	}

	/** The type of permission requested */
	public final Type type;

	/** The sub-type of permission requested */
	public final SubType subType;

	/** The parameter value of the permission */
	private final String [] parameters;

	/** Whether this permission is required for the toolkit to function properly */
	public final boolean required;

	/**
	 * An explanation as to why the toolkit requires or requests this permission, what it will be used for, and why the user should grant
	 * the toolkit this permission
	 */
	public final String explanation;

	/**
	 * Creates a MUIS Permission request
	 *
	 * @param _type The type of the permission requested
	 * @param _subType The sub-type of permission requested
	 * @param paramValues The value of the parameters associated with the subtype for the permission
	 * @param req Whether the permission is required for the toolkit to function properly
	 * @param explain An explanation as to why the toolkit requires or requests the permission, what it will be used for, and why the user
	 *            should grant the toolkit this permission
	 */
	public MuisPermission(Type _type, SubType _subType, String [] paramValues, boolean req, String explain)
	{
		type = _type;
		subType = _subType;
		if(paramValues.length != subType.getParameters().length)
			throw new IllegalArgumentException("Parameter values supplied must match parameter values required by sub-type");
		parameters = paramValues;
		required = req;
		explanation = explain;
	}

	/**
	 * Gets the value of one of this permission's parameters
	 *
	 * @param idx The index of the parameter to get
	 * @return The value of the parameter at the given index
	 * @see SubType#getParameters()
	 */
	public String getParamValue(int idx)
	{
		return parameters[idx];
	}
}
