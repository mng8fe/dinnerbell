package com.dinnerbell.webapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.json.simple.JSONValue;

import sun.misc.BASE64Encoder;

/**
 * Servlet implementation class FavMealServlet
 * 
 * Lets a user add favorite meals
 * 
 * @author Nina
 * 
 */
public class FavMealsServlet extends HttpServlet {

	private static final long serialVersionUID = -3820781492828852431L;
	DAO dao = new DAO();
	Logger logger = Logger.getGlobal();
	GeoCoder gc = new GeoCoder();

	/**
	 * Retrieves the favorite places of a user
	 * 
	 * POST object must be the user_id as integer
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 * 
	 *      TODO : thumbnail-pictures
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// establish database connection
		Connection conn = dao.connect();
		
		Statement stmt = null;

		String user_id = null;
		String lat_user = null;
		String lng_user = null;

		// retrieve data from request
		if (request.getParameter("user_id") != null && !request.getParameter("user_id").isEmpty())
			user_id = request.getParameter("user_id");
		if (request.getParameter("lat") != null && !request.getParameter("lat").isEmpty())
			lat_user = request.getParameter("lat");
		if (request.getParameter("lng") != null && !request.getParameter("lng").isEmpty())
			lng_user = request.getParameter("lng");
		
		
		// create query
		String search_query = " SELECT m.meal_id, m.user_id, m.meal_name, m.meal_time, m.meal_price, u.latitude, "
				+ "m.meal_servings, m.meal_bookings, u.longitude, u.street_no, u.postal_code, u.city, u.country, "
				+ "m.category, m.rating_count, m.thumbnail " + "FROM T_MEALS m "
				+ "JOIN T_USERS u on m.USER_ID = u.USER_ID " + "JOIN T_FAVORITES F on m.MEAL_ID = F.MEAL_ID "
				+ "WHERE f.user_ID = " + user_id;

		JSONArray meals = new JSONArray();

		try {
			// query database
			stmt = conn.createStatement();
			ResultSet rs_meals = stmt.executeQuery(search_query);

			// convert result into JSON
			while (rs_meals.next()) {
				JSONObject o = new JSONObject();

				o.put("meal_id", rs_meals.getString("MEAL_ID"));
				o.put("user_id", rs_meals.getString("USER_ID"));
				o.put("name", rs_meals.getString("MEAL_NAME"));
				o.put("price", rs_meals.getString("MEAL_PRICE"));
				o.put("servings", rs_meals.getString("MEAL_SERVINGS"));
				o.put("bookings", rs_meals.getString("MEAL_BOOKINGS"));
				o.put("city", rs_meals.getString("CITY"));
				o.put("street", rs_meals.getString("STREET_NO"));
				o.put("postal_code", rs_meals.getString("POSTAL_CODE"));
				o.put("lat", rs_meals.getString("LATITUDE"));
				o.put("long", rs_meals.getString("LONGITUDE"));
				o.put("country", rs_meals.getString("COUNTRY"));
				o.put("rating", rs_meals.getString("RATING_COUNT"));
				o.put("category", rs_meals.getString("CATEGORY"));
				o.put("time", rs_meals.getString("MEAL_TIME"));
				
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

				Double lat_place = null;
				Double lng_place = null;

				// add distance to user in ##.## km
				if (rs_meals.getString("LATITUDE") != null && lat_user != null && lng_user != null &&
						 rs_meals.getString("LONGITUDE") != null) {
					lat_place = Double.valueOf(rs_meals.getString("LATITUDE"));
					lng_place = Double
							.valueOf(rs_meals.getString("LONGITUDE"));

					float distance = gc.distFrom(new Float(Double.valueOf(lat_user))
							.floatValue(), new Float(Double.valueOf(lng_user)).floatValue(),
							new Float(lat_place).floatValue(), new Float(
									lng_place).floatValue());

					o.put("distance", String.valueOf(distance));
				} else {
					o.put("distance", "null");
				}		
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

		// send result to client
		String json = meals.toJSONString();
		response.setContentType("text/javascript");
		response.getOutputStream().print(json);
		response.flushBuffer();
	}

	/**
	 * Adds a meal to the users favorite meals.
	 * 
	 * PUT-JSON object must contain "meal_id" and "user_id"
	 * 
	 * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Statement stmt = null;
		String add_favourite = null;
		// establish database connection
		Connection conn = dao.connect();

		try {
			// retrieve data from request
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
			String data = br.readLine();

			// and convert to JSON
			Object obj = JSONValue.parse(data);

			JSONObject job = (JSONObject) obj;

			String user_id = null;
			String meal_id = null;

			meal_id = (String) job.get("meal_id");
			user_id = (String) job.get("user_id");

			// create INSERT statement
			add_favourite = "INSERT INTO T_FAVORITES (USER_ID, MEAL_ID) " + "VALUES ('" + user_id + "', '" + meal_id
					+ "')";

			// execute statement
			stmt = conn.createStatement();
			stmt.executeQuery(add_favourite);

			response.setStatus(HttpServletResponse.SC_CREATED);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
			System.out.println(e.getCause());
		} finally {
			// close connection
			dao.close(conn);
		}

		// display SQL statement for testing purposes
		// response.setContentType("text");
		// response.getOutputStream().print(add_favourite);
		// response.flushBuffer();
	}

	/**
	 * Delete favorite meal of user
	 * 
	 * QUERY STRING should contain the user_id and meal_id
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Statement stmt = null;
		String delete_fav = null;
		String user_id = null;
		String meal_id = null;

		// establish database connection
		Connection conn = dao.connect();

		try {
			user_id = request.getParameter("user_id");
			meal_id = request.getParameter("meal_id");

			// prepare search statement
			delete_fav = "DELETE FROM T_FAVORITES WHERE USER_ID = +" + user_id + " AND MEAL_ID = " + meal_id;

			// execute statements
			stmt = conn.createStatement();
			stmt.executeQuery(delete_fav);

			response.setStatus(HttpServletResponse.SC_OK);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
		} finally {
			// close connection
			dao.close(conn);
		}
	}
}