package org.zeith.gson.stream.itf;

public class RemoteException
		extends RuntimeException
{
	public RemoteException(String message)
	{
		super(message);
	}
	
	public RemoteException(String message, Throwable cause)
	{
		super(message, cause);
	}
	
	public RemoteException(Throwable cause)
	{
		super(cause);
	}
}