package cz.znj.kvr.sw.exp.java.disassemble;


/**
 * -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly
 */
public class GrowingBranchesMain
{
	public static void              main(String[] args)
	{
		System.exit(new GrowingBranchesMain().run());
	}

	public int                      run()
	{
		for (int i = 0; i < 10000000; ++i) {
			value = (char) (i%5000);
			runnable = (i&1) == 0 ? this::growingBranchesSequential : this::growingBranchesHierarchical;
			runnable.run();
		}
		return 0;
	}

	public void                     growingBranchesSequential()
	{
		char i = value;
		if (i <= 63) {
			more = 1;
		}
		else if (i >= 1024 && i <= 2047) {
			more = 2;
		}
		else if (i == 4096) {
			more = 3;
		}
	}

	public void                     growingBranchesHierarchical()
	{
		char i = value;
		if (i <= 63) {
			more = 1;
		}
		else if (i >= 1024) {
			if (i <= 2047) {
				more = 2;
			}
			else if (i == 4096) {
				more = 3;
			}
		}
	}

	private volatile Runnable	runnable;

	private volatile char           value;

	private volatile int            more;
}
