package com.dinnerbell.webapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Servlet implementation class BookMealServlet
 * 
 * Lets a user create a booking for a meal
 * 
 * <url-pattern>/BookMeal</url-pattern>
 * 
 * @author Nina
 */

public class BookMealServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getGlobal();
	DAO dao = new DAO();

	/**
	 * Creates a new booking for a meal
	 * 
	 * PUT-JSON object should contain user_id, meal_id, booked servings
	 * 
	 * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 * 
	 */
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		Statement stmt = null;
		String add_booking = null;
		Statement stmt2 = null;
		String update_meals = null;
		String json = null;

		// establish database connection
		Connection conn = dao.connect();

		try {
			// retrieve data from request
			BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
			String data = br.readLine();

			// and convert to JSON
			Object obj = JSONValue.parse(data);

			JSONObject job = (JSONObject) obj;

			Long user_id = null;
			Long meal_id = null;
			String servings = null;

			meal_id = (Long) job.get("meal_id");
			user_id = (Long) job.get("user_id");
			servings = (String) job.get("servings");

			// create INSERT statement
			add_booking = "INSERT INTO T_BOOKED_MEALS " + "(BOOKED_SERVINGS, MEAL_ID, USER_ID) " + "VALUES (" + servings
					+ ", " + meal_id + ", " + user_id + ")";

			// execute statement
			stmt = conn.createStatement();
			stmt.executeQuery(add_booking);

			// set overall rating count in places table
			update_meals = "UPDATE T_MEALS SET MEAL_BOOKINGS = " + servings + " WHERE meal_id = " + meal_id;

			stmt2 = conn.createStatement();
			stmt2.executeQuery(update_meals);

			resp.setStatus(HttpServletResponse.SC_CREATED);
		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
			System.out.println(e.getCause());
		} finally {
			// close connection
			dao.close(conn);
		}
	}
}