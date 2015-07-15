/**
 * 
 */
package com.dinnerbell.webapp;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author Nina
 * 
 */
public class GeoCoder {

	public GeoCoder() {
		super();
	}

	@SuppressWarnings("null")
	public static String[] coordinates2address(double lat, double lng) {

		Document geocoderResultDocument = null;
		try {

			// prepare a URL to the geocoder
			URL url = new URL(
					"http://maps.googleapis.com/maps/api/geocode/xml?latlng="
							+ lat + "," + lng + "&sensor=false");

			// prepare an HTTP connection to the geocoder
//			System.getProperties().put("proxySet", "true");
//			System.getProperties().put("http.proxyHost",
//					"proxy.hs-karlsruhe.de");
//			System.getProperties().put("http.proxyPort", "8888");
//
//			final String authUser = "aula1011";
//			final String authPassword = "IbdG88";
//			Authenticator.setDefault(new Authenticator() {
//				public PasswordAuthentication getPasswordAuthentication() {
//					return new PasswordAuthentication(authUser, authPassword
//							.toCharArray());
//				}
//			});
//
//			System.setProperty("http.proxyUser", authUser);
//			System.setProperty("http.proxyPassword", authPassword);
			HttpURLConnection hconn = (HttpURLConnection) url.openConnection();

			try {
				// open the connection and get results as InputSource.
				hconn.connect();
				InputSource geocoderResultInputSource = new InputSource(
						hconn.getInputStream());

				// read result and parse into XML Document
				geocoderResultDocument = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder().parse(geocoderResultInputSource);
			} finally {
				hconn.disconnect();
			}

			// prepare XPath
			XPath xpath = XPathFactory.newInstance().newXPath();

			// extract the result
			NodeList resultNodeList1 = null;
			NodeList resultNodeList2 = null;
			NodeList resultNodeList3 = null;
			NodeList resultNodeList4 = null;
			NodeList resultNodeList5 = null;

			// city, route, no, zip, country
			String[] result = new String[5];

			// a) obtain the formatted_address field for the first result
			resultNodeList1 = (NodeList) xpath
					.evaluate(
							"//address_component[type/text() = \"locality\"]/long_name",
							geocoderResultDocument, XPathConstants.NODESET);
			Node node1 = resultNodeList1.item(0);
			if (node1 != null || node1.getTextContent() != null)
				result[0] = node1.getTextContent();
			resultNodeList2 = (NodeList) xpath
					.evaluate(
							"//address_component[type/text() = \"route\"]/long_name",
							geocoderResultDocument, XPathConstants.NODESET);
			Node node2 = resultNodeList2.item(0);
			if (node2 != null || node2.getTextContent() != null)
				result[1] = node2.getTextContent();
			resultNodeList3 = (NodeList) xpath
					.evaluate(
							"//address_component[type/text() = \"street_number\"]/long_name",
							geocoderResultDocument, XPathConstants.NODESET);
			Node node3 = resultNodeList3.item(0);
			if (node3 != null)
				result[2] = node3.getTextContent();
			resultNodeList4 = (NodeList) xpath
					.evaluate(
							"//address_component[type/text() = \"postal_code\"]/long_name",
							geocoderResultDocument, XPathConstants.NODESET);
			Node node4 = resultNodeList4.item(0);
			if (node4 != null || node4.getTextContent() != null)
				result[3] = node4.getTextContent();
			resultNodeList5 = (NodeList) xpath
					.evaluate(
							"//address_component[type/text() = \"country\"]/long_name",
							geocoderResultDocument, XPathConstants.NODESET);
			Node node5 = resultNodeList5.item(0);
			if (node5 != null || node5.getTextContent() != null)
				result[4] = node5.getTextContent();

			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String address2coordinates(String addr) {
		try {
			// URL prefix to the geocoder
			String GEOCODER_REQUEST_PREFIX_FOR_XML = "http://maps.google.com/maps/api/geocode/xml";

			// prepare a URL to the geocoder
			URL url = new URL(GEOCODER_REQUEST_PREFIX_FOR_XML + "?address="
					+ URLEncoder.encode(addr, "UTF-8") + "&sensor=false");

			// prepare an HTTP connection to the geocoder
//			System.getProperties().put("proxySet", "true");
//			System.getProperties().put("http.proxyHost",
//					"proxy.hs-karlsruhe.de");
//			System.getProperties().put("http.proxyPort", "8888");
//
//			final String authUser = "aula1011";
//			final String authPassword = "IbdG88";
//			Authenticator.setDefault(new Authenticator() {
//				public PasswordAuthentication getPasswordAuthentication() {
//					return new PasswordAuthentication(authUser, authPassword
//							.toCharArray());
//				}
//			});
//
//			System.setProperty("http.proxyUser", authUser);
//			System.setProperty("http.proxyPassword", authPassword);
//
			HttpURLConnection hconn = (HttpURLConnection) url.openConnection();

			Document geocoderResultDocument = null;
			try {
				// open the connection and get results as InputSource.
				hconn.connect();
				InputSource geocoderResultInputSource = new InputSource(
						hconn.getInputStream());

				// read result and parse into XML Document
				geocoderResultDocument = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder().parse(geocoderResultInputSource);
			} finally {
				hconn.disconnect();
			}

			// prepare XPath
			XPath xpath = XPathFactory.newInstance().newXPath();

			// extract the result
			NodeList resultNodeList = null;

			// a) obtain the formatted_address field for every result
			resultNodeList = (NodeList) xpath.evaluate(
					"/GeocodeResponse/result/formatted_address",
					geocoderResultDocument, XPathConstants.NODESET);

			// b) extract the locality for the first result
			resultNodeList = (NodeList) xpath
					.evaluate(
							"/GeocodeResponse/result[1]/address_component[type/text()='locality']/long_name",
							geocoderResultDocument, XPathConstants.NODESET);

			// c) extract the coordinates of the first result
			resultNodeList = (NodeList) xpath.evaluate(
					"/GeocodeResponse/result[1]/geometry/location/*",
					geocoderResultDocument, XPathConstants.NODESET);
			float lat = Float.NaN;
			float lng = Float.NaN;
			for (int i = 0; i < resultNodeList.getLength(); ++i) {
				Node node = resultNodeList.item(i);
				if ("lat".equals(node.getNodeName()))
					lat = Float.parseFloat(node.getTextContent());
				if ("lng".equals(node.getNodeName()))
					lng = Float.parseFloat(node.getTextContent());
			}

			return lat + ";" + lng;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Calculates the distance in ##.## km from place to client
	 * 
	 * @param Float
	 *            user_lat
	 * @param Float
	 *            user_lng
	 * @param Float
	 *            place_lat
	 * @param Float
	 *            place_lng
	 * @return distance to place in km
	 */
	public float distFrom(float lat1, float lng1, float lat2, float lng2) {
		double earthRadius = 6371;

		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2)
				* Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = earthRadius * c;
		dist = dist * 100;
		dist = Math.round(dist);
		dist = dist / 100;
		return new Float(dist).floatValue();
	}

}
