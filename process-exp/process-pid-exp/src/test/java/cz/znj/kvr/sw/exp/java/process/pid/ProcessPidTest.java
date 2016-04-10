package cz.znj.kvr.sw.exp.java.process.pid;


import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;


public class ProcessPidTest
{
	@Test
	public void                     testGetPid() throws IOException, InterruptedException
	{
		Process process = Runtime.getRuntime().exec("sleep 0.2");
		System.out.println("Process pid: "+getPid(process));
		process.waitFor();
	}

	@Test(timeout = 1000L)
	public void			testEvents() throws InterruptedException
	{
		WinNT.HANDLE event0 = Kernel32.INSTANCE.CreateEvent(null, false, false, null);
		WinNT.HANDLE event1 = Kernel32.INSTANCE.CreateEvent(null, false, false, null);
		new Thread(() -> {
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			if (!Kernel32.INSTANCE.SetEvent(event1))
				throw new RuntimeException("Error setting event");
			if (!Kernel32.INSTANCE.SetEvent(event0))
				throw new RuntimeException("Error setting event");
		}).start();
		Kernel32.INSTANCE.WaitForMultipleObjects(2, new WinNT.HANDLE[]{ event0, event1 }, false, 10000);
		Kernel32.INSTANCE.WaitForMultipleObjects(2, new WinNT.HANDLE[]{ event0, event1 }, false, 10000);
	}

	private int                     getPid(Process process)
	{
		if (process.getClass().getName().equals("java.lang.Win32Process") ||
			/* get the PID on windows systems */
			process.getClass().getName().equals("java.lang.ProcessImpl")) {
			return getPidWin32(process);
		}
		else if(process.getClass().getName().equals("java.lang.UNIXProcess")) {
			/* get the PID on unix/linux systems */
			try {
				Field f = process.getClass().getDeclaredField("pid");
				f.setAccessible(true);
				return f.getInt(process);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		else {
			throw new IllegalArgumentException("Unknown process class: "+process.getClass().getName());
		}
	}

	private int                     getPidWin32(Process process)
	{
		try {
			Field f = process.getClass().getDeclaredField("handle");
			f.setAccessible(true);
			long handl = f.getLong(process);

			Kernel32 kernel = Kernel32.INSTANCE;
			WinNT.HANDLE handle = new WinNT.HANDLE();
			handle.setPointer(Pointer.createConstant(handl));
			return kernel.GetProcessId(handle);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private interface CLibrary extends Library
	{
		CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);
		int getpid ();
		int waitpid(int pid, long status, int options);
	}

	public interface Kernel32 extends StdCallLibrary
	{
		Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class, new HashMap<String, Object>() {{
			put(OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
			put(OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
		}});

		int                             CloseHandle(WinNT.HANDLE handle);

		WinNT.HANDLE                    CreateEvent(WinNT.SECURITY_ATTRIBUTES securityAttributes, boolean manualReset, boolean initialState, char[] name);

		boolean				SetEvent(WinNT.HANDLE handle);

		WinNT.HANDLE                    WaitForMultipleObjects(int count, WinNT.HANDLE[] handles, boolean waitAll, int milliseconds);

		/* http://msdn.microsoft.com/en-us/library/ms683179(VS.85).aspx */
		WinNT.HANDLE                    GetCurrentProcess();

		/* http://msdn.microsoft.com/en-us/library/ms683215.aspx */
		int                             GetProcessId(WinNT.HANDLE Process);
	}
}
