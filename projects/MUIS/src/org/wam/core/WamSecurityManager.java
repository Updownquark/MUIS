package org.wam.core;

import java.io.FilePermission;
import java.security.*;

public class WamSecurityManager extends SecurityManager
{
	public WamSecurityManager()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public void checkPermission(Permission perm, Object context)
	{
		WamToolkit toolkit = (WamToolkit) context;
		String name = perm.getName();
		if(perm == null)
			throw new SecurityException("Null permission!");
		else if(perm instanceof AllPermission)
		{

		}
		else if(perm instanceof UnresolvedPermission)
		{
		}
		else if(perm instanceof SecurityPermission)
		{
		}
		else if(perm instanceof java.awt.AWTPermission)
		{
		}
		else if(perm instanceof FilePermission)
		{
		}
		else if(perm instanceof java.io.SerializablePermission)
		{
		}
		else if(perm instanceof java.lang.reflect.ReflectPermission)
		{
		}
		else if(perm instanceof RuntimePermission)
		{
		}
		else if(perm instanceof java.net.NetPermission)
		{
		}
		else if(perm instanceof java.sql.SQLPermission)
		{
		}
		else if(perm instanceof java.util.PropertyPermission)
		{
		}
		else if(perm instanceof java.util.logging.LoggingPermission)
		{
		}
		else if(perm instanceof javax.net.ssl.SSLPermission)
		{
		}
		else if(perm instanceof javax.security.auth.AuthPermission)
		{
		}
		else if(perm instanceof javax.security.auth.PrivateCredentialPermission)
		{
		}
		else if(perm instanceof javax.security.auth.kerberos.DelegationPermission)
		{
		}
		else if(perm instanceof javax.security.auth.kerberos.ServicePermission)
		{
		}
		else if(perm instanceof javax.sound.sampled.AudioPermission)
		{
		}
		else
			throw new SecurityException("Unrecognized permission type: "
				+ perm.getClass().getName());
	}
}
