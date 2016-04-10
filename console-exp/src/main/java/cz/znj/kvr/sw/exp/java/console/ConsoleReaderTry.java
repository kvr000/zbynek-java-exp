package cz.znj.kvr.sw.exp.java.console;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.WinHttpClients;

import java.io.Console;


/**
 * Created by rat on 2015-09-20.
 */
public class ConsoleReaderTry
{
	public static void main(String[] args) throws Exception
	{
		Console console = System.console();
		String password = String.valueOf(console.readPassword("Gimme password:"));
		System.out.println("got "+password);
	}
}
