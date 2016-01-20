package okhttp3.sample;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.RealCall;
import okhttp3.Request;
import okhttp3.Request.RequestTimingANP;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.util.logging.Level;

import static okhttp3.internal.Internal.logger;

public class DebugClass {
	//private static final String ENDPOINT = "https://api.github.com/repos/square/okhttp/contributors";
	private static final String ENDPOINT = "http://pagead2.googlesyndication.com/pagead/js/r20160112/r20151006/show_ads_impl.js";
	private static final Gson GSON = new Gson();
	private static final TypeToken<List<Contributor>> CONTRIBUTORS = new TypeToken<List<Contributor>>() {
	};
	//private final static Logger LOGGER = Logger.getLogger(DebugClass.class.getName());

	static class Contributor {
		String login;
		int contributions;
	}
	
	public void makeRequest(String url) throws Exception{
		OkHttpClient client = new OkHttpClient().newBuilder()
				.build();
		
		// Create request for remote resource.
		Request request = new Request.Builder().url(url).build();

		// Execute the request and retrieve the response.
		Call c = client.newCall(request);
		Response response = c.execute();
		logger.log(Level.WARNING, "Load url: "+url);
		// Deserialize HTTP response to concrete type.
		long t3 = System.currentTimeMillis();
		ResponseBody body = response.body();
		long size1 = body.contentLength();
		String str = body.string();
		long size2 = str.length();
		long t5 = System.currentTimeMillis();
		logger.log(Level.WARNING, 
				String.format("Response length before string():%d length:%d",size1, size2) );
		if(c instanceof RealCall){
			long t1 = ((RealCall)c).getStartTimeANP();
			long t2 = ((RealCall)c).getEndTimeANP();
			List<RequestTimingANP> timingsANP = ((RealCall)c).getTimingsANP();
			List<String> urlsANP = ((RealCall)c).getUrlsANP();
			logger.log(Level.WARNING, 
					String.format("Overall delay: %d", t2-t1));
			if(timingsANP.size() != urlsANP.size()){
				throw new Exception("the sizes of urlsANP and timingsANP are not the same ");
			}
			Iterator<String> urlIter = urlsANP.iterator();
			Iterator<RequestTimingANP> timingIter = timingsANP.iterator();
			while(urlIter.hasNext()){
				String curURL = urlIter.next();
				RequestTimingANP timing = timingIter.next();
				long dnsDelay = timing.getDnsEndTimeANP() - timing.getDnsStartTimeANP();
				long connSetupDelay = timing.getConnSetupEndTimeANP() - timing.getConnSetupStartTimeANP();
				long reqWriteDelay = timing.getReqWriteEndTimeANP() - timing.getReqWriteStartTimeANP();
				long respDelay = timing.getRespEndTimeANP() - timing.getReqWriteStartTimeANP();
				long TTFB = timing.getRespStartTimeANP() - timing.getReqWriteEndTimeANP();
				long respTransDelay = timing.getRespEndTimeANP() - timing.getRespStartTimeANP();
				long overallDelay = timing.getRespEndTimeANP() - timing.getReqStartTimeANP();
				logger.log(Level.WARNING,
						String.format(
								"accurateRespTime:%b overall:%dms dns:%dms, connSetup:%dms (handshake:%dms), " + 
										"server:%dms, resp:%dms (1.reqwrite:%dms 2.TTFB:%dms, 3.respTrans:%dms ) \n for URL:%s\n", 
								timing.isAccurateEndTimeANP(), overallDelay, dnsDelay, connSetupDelay, 
								timing.getHandshakeTimeANP(), timing.getEstimatedServerDelay(), respDelay, reqWriteDelay,  TTFB, respTransDelay, curURL));
			}
		}
	}

	public static void main(String... args) throws Exception {
		DebugClass client = new DebugClass();
		client.makeRequest(ENDPOINT);
		
		String url = "http://52.32.112.201:3000/get-mini-file";
		client.makeRequest(url);
		url = "http://52.32.112.201:3000/get-small-file";
		client.makeRequest(url);
		url = "http://52.32.112.201:3000/get-medium-file";
		client.makeRequest(url);
		url = "http://52.32.112.201:3000/get-large-file";
		client.makeRequest(url);
		
		url = "http://52.32.112.201:3000/sleep-50";
		client.makeRequest(url);
		url = "http://52.32.112.201:3000/sleep-200";
		client.makeRequest(url);
		url = "http://52.32.112.201:3000/sleep-500";
		client.makeRequest(url);
		url = "http://52.32.112.201:3000/sleep-2000";
		client.makeRequest(url);
	}

	private DebugClass() {
		// No instances.
	}
}

class LoggingInterceptor implements Interceptor {
	private String tag;

	public LoggingInterceptor(String tag) {
		this.tag = tag;
	}

	@Override
	public Response intercept(Interceptor.Chain chain) throws IOException {
		Request request = chain.request();

		long t1 = System.nanoTime();
		logger.log(Level.INFO, String.format(this.tag+" Sending request %s on %s",
				request.url(), chain.connection()));

		Response response = chain.proceed(request);

		long t2 = System.nanoTime();
		logger.log(Level.INFO,String.format(this.tag+" Received response for %s in %.1fms",
				response.request().url(), (t2 - t1) / 1e6d) );

		return response;
	}
}
