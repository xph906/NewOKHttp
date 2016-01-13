package okhttp3.sample;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.util.logging.Level;
import static okhttp3.internal.Internal.logger;

public class DebugClass {
	private static final String ENDPOINT = "https://api.github.com/repos/square/okhttp/contributors";
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
		Response response = client.newCall(request).execute();
		logger.log(Level.INFO, "sent requests");

		// Deserialize HTTP response to concrete type.
		ResponseBody body = response.body();
		Reader charStream = body.charStream();
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
