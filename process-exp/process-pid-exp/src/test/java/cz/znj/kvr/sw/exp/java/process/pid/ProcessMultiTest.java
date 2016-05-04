package cz.znj.kvr.sw.exp.java.process.pid;


import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class ProcessMultiTest
{
       @Test(timeout = 4000L)
       public void testPassiveWait() throws IOException, InterruptedException, ExecutionException
       {
               List<CompletableFuture<ProcessHandle>> futures = new ArrayList<>();
               for (int i = Runtime.getRuntime().availableProcessors()*16; i-- > 0; ) {
                       Process process = Runtime.getRuntime().exec(new String[]{ "sleep", "1" });
                       futures.add(process.toHandle().onExit());
               }
               CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
       }
}
