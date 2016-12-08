package com.loa.trade.news;

import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.loa.trade.action.TradeAction;
import com.loa.trade.bl.DefaultConstantTradeValue;
import com.loa.trade.model.Event;

public class NewsReader {
	public static ArrayList<Event> readNewsEvents() {
		try {
			//System.out.println("Fsdfsdf");
			HttpClient client = new DefaultHttpClient();
			String getUrl = DefaultConstantTradeValue.NEWS_URL;

			HttpUriRequest getRequest = new HttpGet(getUrl);
			 HttpResponse response = client.execute(getRequest);
			 int statusCode = response.getStatusLine().getStatusCode();
			 Document doc = null;
			// System.out.println("statusCode: " + statusCode);
			        if (statusCode == 200 ){
			            HttpEntity entity = response.getEntity();
			            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			                DocumentBuilder builder = factory.newDocumentBuilder();
			                doc = builder.parse(entity.getContent());
			                return parse(doc);
			        }
		} catch (Exception e) {
			e.printStackTrace();
			TradeAction.logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	public static ArrayList<Event> parse(Document doc) {
		 try {
			 	ArrayList<Event> events = new ArrayList<Event>();
				doc.getDocumentElement().normalize();
				NodeList nList = doc.getElementsByTagName("event");
			 	for (int temp = 0; temp < nList.getLength(); temp++) {
			 		Node nNode = nList.item(temp);
			        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						Event e = new Event();
						String date = eElement.getElementsByTagName("date").item(0).getTextContent().trim();
						String time = eElement.getElementsByTagName("time").item(0).getTextContent().trim();
						try {
							//System.out.println("date : " + date + " " + time);
			        		Date date1 = DefaultConstantTradeValue.NEWS_DATE_FORMATTER.parse(date + " " + time);
			        		e.setCountry( eElement.getElementsByTagName("country").item(0).getTextContent());
					        e.setDate(date1);
					        e.setImpact(eElement.getElementsByTagName("impact").item(0).getTextContent().trim());
					        if(  DefaultConstantTradeValue.VALID_COUNTRIES.contains(e.getCountry()) && DefaultConstantTradeValue.VALID_NEWS_IMPACTS.contains(e.getImpact()) ){ 
					        	events.add(e);
					        }
						} catch (Exception e1) {
			        		e1.printStackTrace();
			        		TradeAction.logger.error(e1.getMessage(), e1);
			        	}
					}
			 	}
			        return events;
			    } catch (Exception e) {
				e.printStackTrace();
				TradeAction.logger.error(e.getMessage(), e);
			    }
		return null;
	  }
	
}
