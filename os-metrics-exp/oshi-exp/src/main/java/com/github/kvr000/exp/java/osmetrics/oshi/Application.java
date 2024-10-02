package com.github.kvr000.exp.java.osmetrics.oshi;


import oshi.SystemInfo;
import oshi.hardware.NetworkIF;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class Application
{
	public static void main(String[] args) throws Exception
	{
		var application = new Application();
		application.printInterfaces();
	}

	public void printInterfaces()
	{
		SystemInfo systemInfo = new SystemInfo();
		List<NetworkIF> networkIFs = systemInfo.getHardware().getNetworkIFs();

		HashMap<String, Long> received = new HashMap<>();
		HashMap<String, Long> sent = new HashMap<>();

		for (NetworkIF net : networkIFs) {
			net.updateAttributes();

			if (isLoopback(net)) {
				System.out.println("Loopback Interface: " + net.getName());
			} else {
				System.out.println("External Interface: " + net.getName());
			}
			System.out.println("\tIP Address: " + String.join(" ", net.getIPv4addr()));
			System.out.println("\tMAC Address: " + net.getMacaddr());
			System.out.println("\tReceived bytes: " + net.getBytesRecv());
			System.out.println("\tSent bytes: " + net.getBytesSent());

			received.put(net.getName(), net.getBytesRecv());
			sent.put(net.getName(), net.getBytesSent());
		}

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        for (NetworkIF net : networkIFs) {
			net.updateAttributes();

			if (isLoopback(net)) {
				System.out.println("Loopback Interface: " + net.getName());
			} else {
				System.out.println("External Interface: " + net.getName());
			}
			System.out.println("\tReceived speed: " + (net.getBytesRecv() - received.getOrDefault(net.getName(), 0L)));
			System.out.println("\tSent speed: " + (net.getBytesSent() - sent.getOrDefault(net.getName(), 0L)));

			received.put(net.getName(), net.getBytesRecv());
			sent.put(net.getName(), net.getBytesSent());
		}

	}

	private boolean isLoopback(NetworkIF net)
	{
		//return net.queryNetworkInterface().isLoopback();
		return Arrays.stream(net.getIPv4addr()).anyMatch(ip -> ip.startsWith("127.")) ||
				Arrays.stream(net.getIPv6addr()).anyMatch(ip -> ip.equals("::1"));
	}
}
