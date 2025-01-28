package org.zeith.gson.stream.itf;

public class RemoteSpoofException
		extends RuntimeException
{
	public final String remoteClass;
	
	public RemoteSpoofException(String remoteClass, String message)
	{
		super(message);
		this.remoteClass = remoteClass;
	}
	
	public RemoteSpoofException(String remoteClass, String message, Throwable cause)
	{
		super(message, cause);
		this.remoteClass = remoteClass;
	}
	
	@Override
	public String toString()
	{
		String message = getLocalizedMessage();
		return (message != null) ? (remoteClass + ": " + message) : remoteClass;
	}
}