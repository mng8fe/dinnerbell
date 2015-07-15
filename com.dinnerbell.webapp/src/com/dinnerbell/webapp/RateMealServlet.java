package com.dinnerbell.webapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Servlet implementation class RateMealServlet
 * 
 * Lets a user create or view the rating of a meal
 * 
 * <url-pattern>/RateMeal</url-pattern> 
 * 
 * @author Nina
 */
public class RateMealServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	Logger logger = Logger.getGlobal();
	DAO dao = new DAO();

	/**
	 * Rates a meal
	 * 
	 * PUT-JSON object should contain meal_id, user_id, rating
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPut(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		Statement stmt = null;
		String add_rating = null;
		Statement stmt2 = null;
		String add_rating2 = null;
		Statement stmt3 = null;
		String add_rating3 = null;
		Statement stmt4 = null;
		String count_rating = null;
		// establish database connection
		Connection conn = dao.connect();

		try {
			// retrieve data from request
			BufferedReader br = new BufferedReader(new InputStreamReader(
					request.getInputStream()));
			String data = br.readLine();

			// and convert to JSON
			Object obj = JSONValue.parse(data);

			JSONObject job = (JSONObject) obj;

			Long user_id = null;
			Long meal_id = null;
			Long rating = (long) 0;
		
			meal_id = (Long) job.get("meal_id");
			user_id = (Long) job.get("user_id");
			rating = (Long) job.get("rating");
			
			// create INSERT statement
			add_rating = "INSERT INTO T_RATINGS "
					+ "(RATING, MEAL_ID, USER_ID) "
					+ "VALUES (" + rating + ", " + meal_id + ", " + user_id
					+ ")";

			// execute statement
			stmt = conn.createStatement();
			stmt.executeQuery(add_rating);

			// set overall rating count in places table
			add_rating2 = "SELECT RATING FROM T_RATINGS where meal_id = "
					+ meal_id;

			stmt2 = conn.createStatement();
			ResultSet rs = stmt2.executeQuery(add_rating2);


			rating = (long) 0;
			
			while (rs.next()) {
				rating += rs.getLong("RATING");
			}

			count_rating = "SELECT COUNT(*) AS total FROM T_RATINGS where meal_id = " + meal_id;
			stmt4 = conn.createStatement();
			ResultSet rss = stmt4.executeQuery(count_rating);
			Integer total = 0;
			while (rss.next()) {
				total = rss.getInt("total");
			}
			
			
			double c1 = rating / total;
			
			// create UPDATE statement
			add_rating3 = "UPDATE T_MEALS SET RATING_COUNT = " 
							+ Math.round(c1 * 2f) / 2f
					+ " WHERE MEAL_ID = " + meal_id;

			// execute statement
			stmt3 = conn.createStatement();
			stmt3.executeQuery(add_rating3);

			response.setStatus(HttpServletResponse.SC_CREATED);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
			System.out.println(e.getCause());
		} finally {
			// close connection
			dao.close(conn);
		}
	}
}
