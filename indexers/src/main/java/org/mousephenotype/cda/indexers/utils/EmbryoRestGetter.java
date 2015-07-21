package org.mousephenotype.cda.indexers.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mousephenotype.cda.solr.generic.util.HttpProxy;
import org.springframework.web.client.RestTemplate;

/**
 * Class for getting the embryo data from the phenoDCC on embryo data available
 * 
 * @author jwarren
 *
 */
public class EmbryoRestGetter {

	private String embryoRestUrl="http://dev.mousephenotype.org/EmbryoViewerWebApp/rest/ready";//default is dev - needs to be wired up properly with spring when going to beta and live

	public void setRestUrl(String embryoRestUrl) {
		this.embryoRestUrl = embryoRestUrl;

	}

	// public EmbryoRestData getEmbryoRestData(){
	// RestTemplate restTemplate = new RestTemplate();
	// EmbryoRestData embryoData = restTemplate.getForObject(embryoRestUrl,
	// EmbryoRestData.class);
	// System.out.println(embryoData.toString());
	// //embryoRestData.add(embryoData);
	//
	//
	// return embryoData;
	//
	// }
	//
	public EmbryoRestData getEmbryoRestData() {
		//to be replaced with SpringRestTemplate when json format redone by Neil
		
		HttpProxy proxy = new HttpProxy();
		EmbryoRestData data=new EmbryoRestData();
		
		try {
			String content=null;
			try {
				content = proxy.getContent(new URL(embryoRestUrl),true);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			List<EmbryoStrain> strains = new ArrayList<>();
			
			EmbryoStrain embryoStrain = null;
			
			JSONObject json=new JSONObject(content);
			System.out.println("json="+json.toString());
			//String []names=JSONObject.getNames(json);
			JSONArray coloniesArray=json.getJSONArray("colonies");
			for(int i=0;i<coloniesArray.length(); i++){
				JSONObject jsonObject = coloniesArray.getJSONObject(i);
					System.out.println("start of object");
					embryoStrain = new EmbryoStrain();
					embryoStrain.setColonyId(jsonObject.getString("colony_id"));
					embryoStrain.setMgi(jsonObject.getString("mgi"));
					embryoStrain.setCentre(jsonObject.getString("centre"));
					embryoStrain.setUrl(jsonObject.getString("url"));
					strains.add(embryoStrain);
			}
			

			data.setStrains(strains);
		} catch (ClientProtocolException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}

		return data;
	}
}
