package com.goeuro.sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.io.FileUtils;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

/*This class Sends a REST request to geo location API https://api.goeuro.de/api/v1/suggest/position/en/name/ 
 *  and writes the json data into a csv format 
 */
public class GoEuroTest {

	private final static Logger LOGGER = Logger.getLogger(GoEuroTest.class
			.getName());
	// Define the constants
	private static final String endPointUrl = "https://api.goeuro.de/api/v1/suggest/position/en/name/";
	private static final String SEPERATOR = ".";
	private static final String RESULTS = "results";
	private static final String FILE_EXTENSION = "csv";
	private static final String GEO_POSITION = "geo_position";
	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";
	private static final String HTTPS = "https://";
	private static final String TLS = "TLS";

	private static HttpsURLConnection uc;

	public static void main(String[] args) throws Exception {

		StringBuilder sb = new StringBuilder(endPointUrl);

		if (args.length != 1) {
			LOGGER.severe("Pass a single city name as argument");
			throw new Exception("Enter single city name as argument");

		}

		String cityName = args[0];
		sb.append(cityName);
		String url = sb.toString();
		GoEuroTest goEuroTest = new GoEuroTest();

		String jsonResponse = goEuroTest
				.sendGetRequest(url, "application/json");

		if (uc.getResponseCode() != 200) {

			throw new Exception(
					"Not able to get the response from the Rest Service");
		}

		String formattedJson = deleteJSONNode(jsonResponse);
		// write to csv file
		try {
			JSONObject jsonObj = new JSONObject(formattedJson);
			JSONArray docs = jsonObj.getJSONArray(RESULTS);
			StringBuffer fileName = new StringBuffer(cityName);
			fileName.append(SEPERATOR).append(FILE_EXTENSION);
			File file = new File(fileName.toString());
			String csv = CDL.toString(docs);
			FileUtils.writeStringToFile(file, csv);
			LOGGER.info("The csv file name with "
					+ fileName
					+ " is written in the current  directory  ie directory where your jar is present");
		} catch (JSONException | IOException e) {
			LOGGER.severe("Exception caught in writing json to csv"
					+ e.getMessage());

		}

	}

	/*
	 * This method is used to parse the json convert from json tree to json with
	 * single node by removing geo_position
	 */

	private static String deleteJSONNode(String json) {
		Gson gson = new Gson();
		Map m = gson.fromJson(json, Map.class);
		List<Map> innerList = (List<Map>) m.get(RESULTS);
		for (Map result : innerList) {
			Map<String, Double> geo_position = (Map<String, Double>) result
					.get(GEO_POSITION);
			result.put(LATITUDE, geo_position.get(LATITUDE));
			result.put(LONGITUDE, geo_position.get(LONGITUDE));
			result.remove(GEO_POSITION);
		}
		String formattedJson = gson.toJson(m);
		return formattedJson;

	}

	/*
	 * This method send the get request using http url connection and returns
	 * the json output from Rest endpoint
	 */

	public static String sendGetRequest(String endpoint, String acceptHeader) {
		String result = null;

		if (endpoint.startsWith(HTTPS)) {
			// Send a GET request to the Location API Service
			try {

				// Send data
				String urlStr = endpoint;

				SSLContext ctx = SSLContext.getInstance(TLS);
				ctx.init(new KeyManager[0],
						new TrustManager[] { new DefaultTrustManager() },
						new SecureRandom());
				SSLContext.setDefault(ctx);
				URL url = new URL(urlStr);
				uc = (HttpsURLConnection) url.openConnection();
				uc.setHostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String arg0, SSLSession arg1) {
						return true;
					}
				});
				uc.setRequestProperty("Accept", acceptHeader);
				LOGGER.info(uc.getResponseMessage());

				BufferedReader rd = new BufferedReader(new InputStreamReader(
						uc.getInputStream()));
				StringBuffer sb = new StringBuffer();
				String line;
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();
				uc.disconnect();
				result = sb.toString();
			} catch (Exception e) {
				LOGGER.severe("Exception caught while invoking Rest WebService"
						+ e.getMessage());
			}

		}
		return result;
	}

	/*
	 * This is static inner class for SSL implementation
	 */
	private static class DefaultTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1)
				throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

}
