import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.zeith.gson.stream.*;
import org.zeith.gson.stream.itf.reg.InterfaceRegistry;

import java.util.concurrent.*;

public class TestExportRegistry
{
	@Test
	public void testRegistry()
	{
		Gson gson = new Gson();
		
		var pipe = GsonPacketChannel.pipedPair(gson);
		var pool = Executors.newCachedThreadPool();
		
		System.out.println("Starting server...");
		var f1 = pool.submit(() -> startServer(pool, pipe[0]));
		
		System.out.println("Starting client...");
		var f2 = pool.submit(() -> startClient(pool, pipe[1]));
		
		try
		{
			f1.get();
			f2.get();
		} catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void startServer(ExecutorService pool, GsonPacketChannel channel)
	{
		MultiJsonChannel mjs = new MultiJsonChannel(channel);
		mjs.listenAsync(pool);
		InterfaceRegistry reg = new InterfaceRegistry(true, false, pool, mjs);
		
		System.out.println("Registry exported");
		
		reg.exported("{test}", TestInterface.class, new TestInterface()
				{
					@Override
					public int sum(int a, int b)
					{
						return a + b;
					}
					
					@Override
					public void callback()
					{
						System.out.println("Callback");
						Thread.dumpStack();
					}
				}
		);
	}
	
	private void startClient(ExecutorService pool, GsonPacketChannel channel)
	{
		MultiJsonChannel mjs = new MultiJsonChannel(channel);
		mjs.listenAsync(pool);
		InterfaceRegistry reg = new InterfaceRegistry(false, false, pool, mjs);
		
		System.out.println("Registry imported");
		
		var ti = reg.imported("{test}", TestInterface.class);
		
		System.out.println("SUM: " + ti.sum(1, 4));
		assert ti.sum(1, 4) == 5;
		
		ti.callback();
	}
}