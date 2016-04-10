package cz.znj.kvr.sw.exp.java.httpclient;

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

/**
 * Created by rat on 2015-09-20.
 */
public class ProxyTest
{
	public static void main(String[] args) throws Exception {
//		CredentialsProvider credsProvider = new BasicCredentialsProvider();
//		credsProvider.setCredentials(
//			new AuthScope("proxy.internal.company.com", 8080),
//			new UsernamePasswordCredentials("myusername", ""));
//		CloseableHttpClient httpclient = HttpClients.custom()
//			.setDefaultCredentialsProvider(credsProvider).build();
		CloseableHttpClient httpclient = WinHttpClients.createDefault();
		try {
			//HttpHost target = new HttpHost("www.verisign.com", 443, "https");
			HttpHost target = new HttpHost("target-server.example.com", 443, "https");
			HttpHost proxy = new HttpHost("proxy.internal.company.com", 8080);

			RequestConfig config = RequestConfig.custom()
				.setProxy(proxy)
				.build();
			HttpGet httpget = new HttpGet("/en/login/");
			httpget.setConfig(config);

			System.out.println("Executing request " + httpget.getRequestLine() + " to " + target + " via " + proxy);

			CloseableHttpResponse response = httpclient.execute(target, httpget);
			try {
				System.out.println("----------------------------------------");
				System.out.println(response.getStatusLine());
				//EntityUtils.consume(response.getEntity());
				System.out.print(response.toString());
				IOUtils.copy(response.getEntity().getContent(), System.out);
			} finally {
				response.close();
			}
		} finally {
			httpclient.close();
		}
	}
}
