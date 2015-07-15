package com.dinnerbell.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import sun.misc.BASE64Encoder;

/**
 * Servlet implementation class GetMealsServlet
 * 
 * Returns a list of meals (nearby?)
 * 
 * @author Nina
 * 
 *         TODO integrate user-location and compute nearest meals
 */
public class GetMealsServlet extends HttpServlet {

	private static final long serialVersionUID = 4132673996393804709L;
	Logger logger = Logger.getGlobal();

	GeoCoder gc = new GeoCoder();

	DAO dao = new DAO();

	/**
	 * Retrieves all meals
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		JSONArray meals = new JSONArray();

		// establish database connection
		Connection conn = dao.connect();

		Statement stmt = null;

		if (request.getParameter("min_rating") != null && !request.getParameter("min_rating").isEmpty()
				&& request.getParameter("coor") != null && !request.getParameter("coor").isEmpty()
				&& request.getParameter("radius") != null && !request.getParameter("radius").isEmpty()) {

			String min_param = request.getParameter("min_rating");
			String coor = request.getParameter("coor");
			String radius = request.getParameter("radius");
			String[] latlong = new String[2];
			latlong = coor.split(",");
			Double lat = Double.valueOf(latlong[0]);
			Double lng = Double.valueOf(latlong[1]);

			// Radius of 1 km = 0.009; 3 km = 0.02, 5 km = 0.045; 10 km = 0,09
			Double latlngChange;
			if (radius.equals("1.0")) {
				latlngChange = 0.009;
			} else if (radius.equals("3.0")) {
				latlngChange = 0.02;
			} else if (radius.equals("5.0")) {
				latlngChange = 0.045;
			} else {
				latlngChange = 0.09;
			}
			double min_lat = lat - latlngChange;
			double max_lat = lat + latlngChange;
			double min_lng = lng - latlngChange;
			double max_lng = lng + latlngChange;

			// prepare queries for meals with minimum rating and
			// proximity to user
			String meals_query = "SELECT m.meal_id, m.user_id, m.meal_name, m.meal_price, "
					+ "m.meal_servings, m.meal_bookings, m.rating_count, m.category, m.thumbnail, m.meal_time, "
					+ "u.street_no, u.postal_code, u.user_name, u.city, u.country, u.latitude, u.longitude " + "FROM T_MEALS m "
					+ "join T_USERS u on u.user_id=m.user_id " + "WHERE u.latitude > '" + String.valueOf(min_lat)
					+ "' AND u.latitude < '" + String.valueOf(max_lat) + "' AND u.longitude > '"
					+ String.valueOf(min_lng) + "' AND u.longitude < '" + String.valueOf(max_lng)
					+ "' AND m.rating_count >= " + min_param + " order by m.meal_time asc, m.rating_count desc";

			try {
				// execute statements
				stmt = conn.createStatement();
				ResultSet rs_meals = stmt.executeQuery(meals_query);

				while (rs_meals.next()) {
					JSONObject o = new JSONObject();
					o.put("meal_id", rs_meals.getString("MEAL_ID"));
					o.put("user_id", rs_meals.getString("USER_ID"));
					o.put("name", rs_meals.getString("MEAL_NAME"));
					o.put("meal_price", rs_meals.getString("MEAL_PRICE"));
					o.put("meal_servings", rs_meals.getString("MEAL_SERVINGS"));
					o.put("meal_bookings", rs_meals.getString("MEAL_BOOKINGS"));
					o.put("rating", rs_meals.getString("RATING_COUNT"));
					o.put("category", rs_meals.getString("CATEGORY"));
					o.put("time", rs_meals.getString("MEAL_TIME"));
					o.put("user_name", rs_meals.getString("USER_NAME"));
					o.put("street", rs_meals.getString("STREET_NO"));
					o.put("city", rs_meals.getString("CITY"));
					o.put("postal_code", rs_meals.getString("POSTAL_CODE"));
					o.put("lat", rs_meals.getString("LATITUDE"));
					o.put("long", rs_meals.getString("LONGITUDE"));
					Double lat_meal = Double.valueOf(rs_meals.getString("LATITUDE"));
					Double lng_meal = Double.valueOf(rs_meals.getString("LONGITUDE"));
					o.put("country", rs_meals.getString("COUNTRY"));

					// convert thumbnail-image to JSON
					String image = null;
					if (rs_meals.getBinaryStream("THUMBNAIL") != null) {
						try {
							InputStream in = rs_meals.getBinaryStream("THUMBNAIL");
							byte[] byteimage = IOUtils.toByteArray(in);
							BASE64Encoder encoder = new BASE64Encoder();
							image = encoder.encodeBuffer(byteimage);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					o.put("thumbnail", image);

					// add distance to user in ##.## km
					float distance = gc.distFrom(new Float(lat).floatValue(), new Float(lng).floatValue(),
							new Float(lat_meal).floatValue(), new Float(lng_meal).floatValue());

					o.put("distance", String.valueOf(distance));

					meals.add(o);
				}

				response.setStatus(HttpServletResponse.SC_OK);
			} catch (SQLException e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_CONFLICT);
			} finally {
				// close connection
				dao.close(conn);
			}
		} else if (request.getParameter("min_rating") != null && !request.getParameter("min_rating").isEmpty()) {

			String min_param = request.getParameter("min_rating");

			// prepare queries for meals and tags with minimum rating of
			// min_param
			String meals_query = "SELECT m.meal_id, m.user_id, m.meal_name, m.meal_price, "
					+ "m.meal_servings, m.meal_bookings, m.rating_count, m.category, m.thumbnail, m.meal_time, "
					+ "u.user_name, u.street_no, u.postal_code, u.city, u.country, u.latitude, u.longitude "
					+ "FROM T_MEALS m " + "join T_USERS u on u.user_id=m.user_id " + "WHERE m.rating_count >= "
					+ min_param + " order by m.meal_time asc, m.rating_count desc";

			try {
				// execute statements
				stmt = conn.createStatement();
				ResultSet rs_meals = stmt.executeQuery(meals_query);

				while (rs_meals.next()) {
					JSONObject o = new JSONObject();
					o.put("meal_id", rs_meals.getString("MEAL_ID"));
					o.put("user_id", rs_meals.getString("USER_ID"));
					o.put("name", rs_meals.getString("MEAL_NAME"));
					o.put("meal_price", rs_meals.getString("MEAL_PRICE"));
					o.put("meal_servings", rs_meals.getString("MEAL_SERVINGS"));
					o.put("meal_bookings", rs_meals.getString("MEAL_BOOKINGS"));
					o.put("rating", rs_meals.getString("RATING_COUNT"));
					o.put("category", rs_meals.getString("CATEGORY"));
					o.put("time", rs_meals.getString("MEAL_TIME"));
					o.put("user_name", rs_meals.getString("USER_NAME"));
					o.put("street", rs_meals.getString("STREET_NO"));
					o.put("city", rs_meals.getString("CITY"));
					o.put("postal_code", rs_meals.getString("POSTAL_CODE"));
					o.put("country", rs_meals.getString("COUNTRY"));
					o.put("lat", rs_meals.getString("LATITUDE"));
					o.put("long", rs_meals.getString("LONGITUDE"));
					o.put("distance", "null");

					// convert thumbnail-image to JSON
					String image = null;
					if (rs_meals.getBinaryStream("THUMBNAIL") != null) {
						try {
							InputStream in = rs_meals.getBinaryStream("THUMBNAIL");
							byte[] byteimage = IOUtils.toByteArray(in);
							BASE64Encoder encoder = new BASE64Encoder();
							image = encoder.encodeBuffer(byteimage);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					o.put("thumbnail", image);

					meals.add(o);
				}

				response.setStatus(HttpServletResponse.SC_OK);
			} catch (SQLException e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_CONFLICT);
			} finally {
				// close connection
				dao.close(conn);
			}
		} else {
			// prepare queries for places
			String meals_query = "SELECT m.meal_id, m.user_id, m.meal_name, m.meal_price, "
					+ "m.meal_servings, m.meal_bookings, m.rating_count, m.category, m.thumbnail, m.meal_time, "
					+ "u.user_name, u.street_no, u.postal_code, u.city, u.country, u.latitude, u.longitude "
					+ "FROM T_MEALS m " + "join T_USERS u on u.user_id=m.user_id ";

			try {
				// execute statements
				stmt = conn.createStatement();
				ResultSet rs_meals = stmt.executeQuery(meals_query);

				while (rs_meals.next()) {
					JSONObject o = new JSONObject();
					o.put("meal_id", rs_meals.getString("MEAL_ID"));
					o.put("user_id", rs_meals.getString("USER_ID"));
					o.put("name", rs_meals.getString("MEAL_NAME"));
					o.put("meal_price", rs_meals.getString("MEAL_PRICE"));
					o.put("meal_servings", rs_meals.getString("MEAL_SERVINGS"));
					o.put("meal_bookings", rs_meals.getString("MEAL_BOOKINGS"));
					o.put("rating", rs_meals.getString("RATING_COUNT"));
					o.put("category", rs_meals.getString("CATEGORY"));
					o.put("time", rs_meals.getString("MEAL_TIME"));
					o.put("user_name", rs_meals.getString("USER_NAME"));
					o.put("street", rs_meals.getString("STREET_NO"));
					o.put("city", rs_meals.getString("CITY"));
					o.put("postal_code", rs_meals.getString("POSTAL_CODE"));
					o.put("country", rs_meals.getString("COUNTRY"));
					o.put("lat", rs_meals.getString("LATITUDE"));
					o.put("long", rs_meals.getString("LONGITUDE"));
					o.put("distance", "null");

					// convert thumbnail-image to JSON
					String image = null;
					if (rs_meals.getBinaryStream("THUMBNAIL") != null) {
						try {
							InputStream in = rs_meals.getBinaryStream("THUMBNAIL");
							byte[] byteimage = IOUtils.toByteArray(in);
							BASE64Encoder encoder = new BASE64Encoder();
							image = encoder.encodeBuffer(byteimage);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					o.put("thumbnail", image);

					meals.add(o);
				}

				response.setStatus(HttpServletResponse.SC_OK);
			} catch (SQLException e) {
				e.printStackTrace();
				response.setStatus(HttpServletResponse.SC_CONFLICT);
			} finally {
				// close connection
				dao.close(conn);
			}
		}

		// return places-array in JSON
		String json = meals.toJSONString();
		response.setContentType("text/javascript");
		response.getOutputStream().print(json);
		response.flushBuffer();
	}

	/**
	 * Search for meals matching the given search-string
	 * 
	 * POST object must be in STRING format
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 *
	 */
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse res) throws ServletException, IOException {

		// establish database connection
		Connection conn = dao.connect();

		Statement stmt = null;

		String searchparam = null;
		String lat_user = null;
		String lng_user = null;

		// retrieve data from request
		if (request.getParameter("searchparam") != null && !request.getParameter("searchparam").isEmpty())
			searchparam = request.getParameter("searchparam").toLowerCase();
		if (request.getParameter("lat") != null && !request.getParameter("lat").isEmpty())
			lat_user = request.getParameter("lat");
		if (request.getParameter("lng") != null && !request.getParameter("lng").isEmpty())
			lng_user = request.getParameter("lng");

		JSONArray meals = new JSONArray();

		// prepare query
		String search_query = "SELECT m.meal_id, m.meal_name, m.user_id, m.category, m.meal_price, m.meal_servings, m.meal_bookings, m.meal_time, m.thumbnail, u.city, u.country, u.latitude, u.longitude, "
				+ "u.postal_code, m.rating_count, u.street_no " + "FROM T_MEALS m "
				+ "join T_USERS u on m.user_id=u.user_id " + "WHERE lower(m.meal_name) like '%" + searchparam
				+ "%' or lower(u.user_name) like '%" + searchparam + "%'";

		try {
			// query database
			stmt = conn.createStatement();
			ResultSet rs_meals = stmt.executeQuery(search_query);

			// convert result to JSON
			while (rs_meals.next()) {
				JSONObject o = new JSONObject();

				o.put("meal_id", rs_meals.getString("MEAL_ID"));
				o.put("user_id", rs_meals.getString("USER_ID"));
				o.put("category", rs_meals.getString("CATEGORY"));
				o.put("price", rs_meals.getString("MEAL_PRICE"));
				o.put("meal_servings", rs_meals.getString("MEAL_SERVINGS"));
				o.put("meal_bookings", rs_meals.getString("MEAL_BOOKINGS"));
				o.put("name", rs_meals.getString("MEAL_NAME"));
				o.put("city", rs_meals.getString("CITY"));
				o.put("time", rs_meals.getString("MEAL_TIME"));
				o.put("street", rs_meals.getString("STREET_NO"));
				o.put("postal_code", rs_meals.getString("POSTAL_CODE"));
				o.put("lat", rs_meals.getString("LATITUDE"));
				o.put("long", rs_meals.getString("LONGITUDE"));
				Double lat_meal = Double.valueOf(rs_meals.getString("LATITUDE"));
				Double lng_meal = Double.valueOf(rs_meals.getString("LONGITUDE"));
				o.put("country", rs_meals.getString("COUNTRY"));
				o.put("rating", rs_meals.getString("RATING_COUNT"));

				// convert thumbnail-image to JSON
				String image = null;
				if (rs_meals.getBinaryStream("THUMBNAIL") != null) {
					try {
						InputStream in = rs_meals.getBinaryStream("THUMBNAIL");
						byte[] byteimage = IOUtils.toByteArray(in);
						BASE64Encoder encoder = new BASE64Encoder();
						image = encoder.encodeBuffer(byteimage);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				o.put("thumbnail", image);

				// add distance to user in ##.## km
				float distance = gc.distFrom(new Float(lat_user).floatValue(), new Float(lng_user).floatValue(),
						new Float(lat_meal).floatValue(), new Float(lng_meal).floatValue());

				o.put("distance", String.valueOf(distance));

				meals.add(o);
			}

			res.setStatus(HttpServletResponse.SC_OK);
		} catch (SQLException e) {
			e.printStackTrace();
			res.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} finally {
			// close connection
			dao.close(conn);
		}

		// return result to client
		String json = meals.toJSONString();
		res.setContentType("text/javascript");
		res.getOutputStream().print(json);
		res.flushBuffer();

	}
}
