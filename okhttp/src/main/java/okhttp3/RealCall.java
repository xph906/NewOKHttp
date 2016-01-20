/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import okhttp3.Request.RequestTimingANP;
import okhttp3.internal.CustomRecordFormatter;
import okhttp3.internal.NamedRunnable;
import okhttp3.internal.http.HttpEngine;
import okhttp3.internal.http.RequestException;
import okhttp3.internal.http.RouteException;
import okhttp3.internal.http.StreamAllocation;
import static okhttp3.internal.Internal.logger;
import static okhttp3.internal.http.HttpEngine.MAX_FOLLOW_UPS;

public final class RealCall implements Call {
	private final OkHttpClient client;

	// Guarded by this.
	private boolean executed;
	volatile boolean canceled;

	/**
	 * The application's original request unadulterated by redirects or auth
	 * headers.
	 */
	Request originalRequest;
	HttpEngine engine;
	
	/* NetProphet fields */
	private List<String> urlsANP;
	private List<RequestTimingANP> timingsANP;
	private long startTimeANP;
	private long endTimeANP;

	protected RealCall(OkHttpClient client, Request originalRequest) {
		this.client = client;
		this.originalRequest = originalRequest;
		
		/* NetProphet initialization*/
		urlsANP = new ArrayList<String>();
		timingsANP = new ArrayList<RequestTimingANP>();
		startTimeANP = 0;
		endTimeANP = 0;
	}

	/* NetProphet Getter and Setter */
	public List<String> getUrlsANP() {
		return urlsANP;
	}

	public void setUrlsANP(List<String> urlsANP) {
		this.urlsANP = urlsANP;
	}

	public List<RequestTimingANP> getTimingsANP() {
		return timingsANP;
	}

	public void setTimingsANP(List<RequestTimingANP> timingsANP) {
		this.timingsANP = timingsANP;
	}

	public long getStartTimeANP() {
		return startTimeANP;
	}

	public void setStartTimeANP(long startTimeANP) {
		this.startTimeANP = startTimeANP;
	}

	public long getEndTimeANP() {
		long accurateEndTime = endTimeANP;
		for(RequestTimingANP timing : timingsANP){
			if(timing.getRespEndTimeANP() > accurateEndTime)
				accurateEndTime = timing.getRespEndTimeANP();
		}
		
		return accurateEndTime;
	}

	public void setEndTimeANP(long endTimeANP) {
		this.endTimeANP = endTimeANP;
	}
	//End
	
	@Override
	public Request request() {
		return originalRequest;
	}


	@Override
	public Response execute() throws IOException {
		synchronized (this) {
			if (executed)
				throw new IllegalStateException("Already Executed");
			executed = true;
		}
		long startTime = System.currentTimeMillis();
		long endTime = 0, diffTime = 0;
		try {
			// add this realcall to runningSyncCalls
			client.dispatcher().executed(this);

			Response result = getResponseWithInterceptorChain(false);
			if (result == null) {
				throw new IOException("Canceled");
			}
			return result;
		} finally {
			client.dispatcher().finished(this);
			endTime = System.currentTimeMillis();
			diffTime = endTime - startTime;
			logger.log(Level.INFO, "RealCall: Latency of executing request: "
					+ diffTime + " url:" + originalRequest.url().toString());
		}

	}

	Object tag() {
		return originalRequest.tag();
	}

	@Override
	public void enqueue(Callback responseCallback) {
		enqueue(responseCallback, false);
	}

	void enqueue(Callback responseCallback, boolean forWebSocket) {
		synchronized (this) {
			if (executed)
				throw new IllegalStateException("Already Executed");
			executed = true;
		}
		client.dispatcher().enqueue(
				new AsyncCall(responseCallback, forWebSocket));
	}

	@Override
	public void cancel() {
		canceled = true;
		if (engine != null)
			engine.cancel();
	}

	@Override
	public synchronized boolean isExecuted() {
		return executed;
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	final class AsyncCall extends NamedRunnable {
		private final Callback responseCallback;
		private final boolean forWebSocket;

		private AsyncCall(Callback responseCallback, boolean forWebSocket) {
			super("OkHttp %s", originalRequest.url().toString());
			this.responseCallback = responseCallback;
			this.forWebSocket = forWebSocket;
		}

		String host() {
			return originalRequest.url().host();
		}

		Request request() {
			return originalRequest;
		}

		Object tag() {
			return originalRequest.tag();
		}

		void cancel() {
			RealCall.this.cancel();
		}

		RealCall get() {
			return RealCall.this;
		}

		@Override
		protected void execute() {
			boolean signalledCallback = false;
			try {
				Response response = getResponseWithInterceptorChain(forWebSocket);
				if (canceled) {
					signalledCallback = true;
					responseCallback.onFailure(originalRequest,
							new IOException("Canceled"));
				} else {
					signalledCallback = true;
					responseCallback.onResponse(response);
				}
			} catch (IOException e) {
				if (signalledCallback) {
					// Do not signal the callback twice!
					logger.log(Level.INFO, "Callback failure for "
							+ toLoggableString(), e);
				} else {
					Request request = engine == null ? originalRequest : engine
							.getRequest();
					responseCallback.onFailure(request, e);
				}
			} finally {
				client.dispatcher().finished(this);
			}
		}
	}

	/**
	 * Returns a string that describes this call. Doesn't include a full URL as
	 * that might contain sensitive information.
	 */
	private String toLoggableString() {
		String string = canceled ? "canceled call" : "call";
		HttpUrl redactedUrl = originalRequest.url().resolve("/...");
		return string + " to " + redactedUrl;
	}

	private Response getResponseWithInterceptorChain(boolean forWebSocket)
			throws IOException {
		Interceptor.Chain chain = new ApplicationInterceptorChain(0,
				originalRequest, forWebSocket);
		startTimeANP = System.currentTimeMillis();
		Response rs = chain.proceed(originalRequest);
		endTimeANP = System.currentTimeMillis();
		logger.log(Level.INFO, String.format("overall delay is: %d for URL: %s", 
				endTimeANP-startTimeANP, originalRequest.url().toString()));
		return rs;
	}

	class ApplicationInterceptorChain implements Interceptor.Chain {
		private final int index;
		private final Request request;
		private final boolean forWebSocket;

		ApplicationInterceptorChain(int index, Request request,
				boolean forWebSocket) {
			this.index = index;
			this.request = request;
			this.forWebSocket = forWebSocket;
		}

		@Override
		public Connection connection() {
			return null;
		}

		@Override
		public Request request() {
			return request;
		}

		// this is the entry of all interceptors
		// 1. the first time when we arrive here, it will iterate all the
		// application intereceptors.
		// 2. after handing all the application interceptors, this function will
		// be
		// called again, this time, there is no application intereceptors, so
		// getResponse will be called, which will call networking interceptors
		// summary: defaultAppInterceptor(this) -> application intereceptor (by
		// calling chain.proceed)
		// -> defaultAppInterceptor -> networking inteceptor.
		@Override
		public Response proceed(Request request) throws IOException {
			long t1 = System.currentTimeMillis();
			logger.info(String.format("DefaultInterceptor Sending request %s",
					request.url()));

			// If there's another interceptor in the chain, call that.
			if (index < client.interceptors().size()) {
				Interceptor.Chain chain = new ApplicationInterceptorChain(
						index + 1, request, forWebSocket);
				Interceptor interceptor = client.interceptors().get(index);
				Response interceptedResponse = interceptor.intercept(chain);

				if (interceptedResponse == null) {
					throw new NullPointerException("application interceptor "
							+ interceptor + " returned null");
				}
				long t2 = System.currentTimeMillis();
				//logger.info(String
				//		.format("DefaultInterceptor  WITH  Interceptor Received response %s for %dms",
				//				request.url(), t2-t1));
				
				return interceptedResponse;
			}

			// No more interceptors. Do HTTP.
			Response rs = getResponse(request, forWebSocket);

			long t2 = System.currentTimeMillis();
			logger.info(String
					.format("DefaultInterceptor WITHOUT Interceptor Received response %s for %dms",
							request.url(), t2 - t1));
			return rs;
		}
	}

	/**
	 * Performs the request and returns the response. May return null if this
	 * call was canceled.
	 */
	Response getResponse(Request request, boolean forWebSocket)
			throws IOException {
		// Copy body metadata to the appropriate request headers.
		RequestBody body = request.body();
		if (body != null) {
			Request.Builder requestBuilder = request.newBuilder();

			MediaType contentType = body.contentType();
			if (contentType != null) {
				requestBuilder.header("Content-Type", contentType.toString());
			}

			long contentLength = body.contentLength();
			if (contentLength != -1) {
				requestBuilder.header("Content-Length",
						Long.toString(contentLength));
				requestBuilder.removeHeader("Transfer-Encoding");
			} else {
				requestBuilder.header("Transfer-Encoding", "chunked");
				requestBuilder.removeHeader("Content-Length");
			}

			request = requestBuilder.build();
		}
		
		// Create the initial HTTP engine. Retries and redirects need new engine
		// for each attempt.
		engine = new HttpEngine(client, request, false, false, forWebSocket,
				null, null, null);
		
		int followUpCount = 0;
		while (true) {
			if (canceled) {
				engine.releaseStreamAllocation();
				throw new IOException("Canceled");
			}

			boolean releaseConnection = true;
			long t3 = 0, t4 = 0, t5 = 0, t6 = 0, t7 = 0;
			try {
				//FIXME: AdsProphet: test redirection requests...
				urlsANP.add(engine.getRequest().url().toString());
				t3 = System.currentTimeMillis();
				engine.getRequest().getRequestTimingANP().setReqStartTimeANP(t3);
				
				engine.sendRequest();
				
				t4 = System.currentTimeMillis();
				logger.info(String.format(
						"HTTPEngine sendRequest:%d", t4 - t3));
				
				engine.readResponse();
				
				t5 = System.currentTimeMillis();
				logger.info(String.format(
						"HTTPEngine readResponse diff %d", 
							t5 - engine.getRequest().getRequestTimingANP().getRespStartTimeANP()));
				//engine.getRequest().getRequestTimingANP().setRespStartTimeANP(t5);
				timingsANP.add(engine.getRequest().getRequestTimingANP());
				releaseConnection = false;
			} catch (RequestException e) {
				// The attempt to interpret the request failed. Give up.
				engine.getRequest().getRequestTimingANP().setSuccessfulANP(false);
				engine.getRequest().getRequestTimingANP().setErrorString(e.toString());
				engine.getRequest().getRequestTimingANP().
					setRespEndTimeANP(System.currentTimeMillis());
				timingsANP.add(request.getRequestTimingANP());
				throw e.getCause();
			} catch (RouteException e) {
				// The attempt to connect via a route failed. The request will
				// not have been sent.
				engine.getRequest().getRequestTimingANP().setSuccessfulANP(false);
				engine.getRequest().getRequestTimingANP().setErrorString(e.toString());		
				t6 = System.currentTimeMillis();
				HttpEngine retryEngine = engine.recover(
						e.getLastConnectException(), null);
				
				t7 = System.currentTimeMillis();
				logger.info(String
						.format("HTTPEngineRecover fromLastEngine:%d, recover:%d [RouteException]",
								t6 - t3, t7 - t6));
				
				engine.getRequest().getRequestTimingANP().
					setRespEndTimeANP(t7);
				timingsANP.add(request.getRequestTimingANP());
				if (retryEngine != null) {
					releaseConnection = false;
					engine = retryEngine;
					continue;
				}

				// Give up; recovery is not possible.
				throw e.getLastConnectException();
			} catch (IOException e) {
				// An attempt to communicate with a server failed. The request
				// may have been sent.
				engine.getRequest().getRequestTimingANP().setSuccessfulANP(false);
				engine.getRequest().getRequestTimingANP().setErrorString(e.toString());		
				t6 = System.currentTimeMillis();
				
				HttpEngine retryEngine = engine.recover(e, null);
				
				t7 = System.currentTimeMillis();
				logger.info(String
						.format("HTTPEngineRecover fromLastEngine:%d, recover:%d [IOException]",
								t6 - t3, t7 - t6));	
				engine.getRequest().getRequestTimingANP().
					setRespEndTimeANP(t7);
				timingsANP.add(request.getRequestTimingANP());
				if (retryEngine != null) {
					releaseConnection = false;
					engine = retryEngine;
					continue;
				}
				
				// Give up; recovery is not possible.
				throw e;
			} finally {
				// We're throwing an unchecked exception. Release any resources.
				if (releaseConnection) {
					StreamAllocation streamAllocation = engine.close();
					streamAllocation.release();
				}
			}

			Response response = engine.getResponse();
			Request followUp = engine.followUpRequest();

			if (followUp == null) {
				if (!forWebSocket) {
					engine.releaseStreamAllocation();
				}
				return response;
			}
			
			StreamAllocation streamAllocation = engine.close();

			if (++followUpCount > MAX_FOLLOW_UPS) {
				streamAllocation.release();
				throw new ProtocolException("Too many follow-up requests: "
						+ followUpCount);
			}

			if (!engine.sameConnection(followUp.url())) {
				streamAllocation.release();
				streamAllocation = null;
			}

			request = followUp;
			engine = new HttpEngine(client, request, false, false,
					forWebSocket, streamAllocation, null, response);
			
			
		}
	}
}
