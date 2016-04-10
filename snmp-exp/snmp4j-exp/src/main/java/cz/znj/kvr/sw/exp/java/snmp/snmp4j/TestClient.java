package cz.znj.kvr.sw.exp.java.snmp.snmp4j;

import com.google.common.base.Throwables;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.USM;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.File;
import java.io.IOException;


/**
 * @author
 *  Zbynek Vyskovsky
 */
public class TestClient extends BaseAgent
{
	private Snmp snmp;

	public static void main(String[] args)
	{
		System.exit(new TestClient("udp:127.0.0.1/2001").process());
	}

	public TestClient(String address)
	{
		//super(address);
		super(new File("target/conf.agent"), new File("target/bootCounter.agent"), new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
	}

	public int process()
	{
		init();
		getServer().addContext(new OctetString("public"));
		finishInit();
		this.run();
		return 0;
	}

	public void init()
	{
		try {
			super.init();
			//DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(this.transportMappings[0]);
			//transportMappings[0].listen();
		}
		catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	@Override
	protected void initTransportMappings() throws IOException
	{
		this.transportMappings = new TransportMapping[1];
		this.transportMappings[0] = new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/2001"));
	}

	@Override
	protected void registerManagedObjects()
	{
	}

	@Override
	protected void unregisterManagedObjects()
	{
	}

	@Override
	protected void addUsmUser(USM usm)
	{
	}

	@Override
	protected void addNotificationTargets(SnmpTargetMIB snmpTargetMIB, SnmpNotificationMIB snmpNotificationMIB)
	{
	}

	@Override
	protected void addViews(VacmMIB vacmMIB)
	{
	}

	@Override
	protected void addCommunities(SnmpCommunityMIB snmpCommunityMIB)
	{
	}
}
