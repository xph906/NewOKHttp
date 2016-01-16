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

	public static void main(String... args) throws Exception {
		OkHttpClient client = new OkHttpClient().newBuilder()
				.addInterceptor(new LoggingInterceptor("ApplicationInterceptor"))
				.addNetworkInterceptor(new LoggingInterceptor("NetworkInterceptor"))
				.build();
		

		logger.log(Level.INFO, "start to prepare info");
		// Create request for remote resource.
		Request request = new Request.Builder().url(ENDPOINT).build();

		logger.log(Level.INFO, "start to send requests");
		// Execute the request and retrieve the response.
		Call c = client.newCall(request);
		Response response = c.execute();
	
		// Deserialize HTTP response to concrete type.
		long t3 = System.currentTimeMillis();
		ResponseBody body = response.body();
		int nRead;
		byte[] data = new byte[116384];
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		while ((nRead = body.byteStream().read(data, 0, data.length)) != -1) {
		  buffer.write(data, 0, nRead);
		}
		buffer.flush();
		long t4 = System.currentTimeMillis()-t3;
		logger.log(Level.INFO," read byte delay: "+t4);
		/*Reader charStream = body.charStream();
		
		List<Contributor> contributors = GSON.fromJson(charStream,
				CONTRIBUTORS.getType());
		body.close();
		logger.log(Level.INFO, "received body");

		// Sort list by the most contributions.
		Collections.sort(contributors, new Comparator<Contributor>() {
			@Override
			public int compare(Contributor c1, Contributor c2) {
				return c2.contributions - c1.contributions;
			}
		});

		// Output list of contributors.
		for (Contributor contributor : contributors) {
			System.out.println(contributor.login + ": "
					+ contributor.contributions);
		}*/
		
		if(c instanceof RealCall){
			long t1 = ((RealCall)c).getStartTimeANP();
			long t2 = ((RealCall)c).getEndTimeANP();
			List<RequestTimingANP> timingsANP = ((RealCall)c).getTimingsANP();
			List<String> urlsANP = ((RealCall)c).getUrlsANP();
			logger.log(Level.INFO, 
					String.format("Start:%d End:%d Overall delay: %d", t1,t2,t2-t1));
			if(timingsANP.size() != urlsANP.size()){
				throw new Exception("the sizes of urlsANP and timingsANP are not the same ");
			}
			Iterator<String> urlIter = urlsANP.iterator();
			Iterator<RequestTimingANP> timingIter = timingsANP.iterator();
			while(urlIter.hasNext()){
				String url = urlIter.next();
				RequestTimingANP timing = timingIter.next();
				long dnsDelay = timing.getDnsEndTimeANP() - timing.getDnsStartTimeANP();
				long connSetupDelay = timing.getConnSetupEndTimeANP() - timing.getConnSetupStartTimeANP();
				long reqWriteDelay = timing.getReqWriteEndTimeANP() - timing.getReqWriteStartTimeANP();
				long respDelay = timing.getRespEndTimeANP() - timing.getReqWriteStartTimeANP();
				long overallDelay = timing.getRespEndTimeANP() - timing.getReqStartTimeANP();
				
				logger.log(Level.INFO,
						String.format("overall:%dms dns:%dms, connSetup:%dms, reqwrite:%dms resp:%dms for URL:%s", 
								overallDelay, dnsDelay, connSetupDelay, reqWriteDelay, respDelay, url));
			}
		}
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
