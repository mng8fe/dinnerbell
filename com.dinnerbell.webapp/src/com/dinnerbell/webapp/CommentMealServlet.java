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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Servlet implementation class CommentPlaceServlet
 * 
 * Receives comments on meals
 * 
 */
public class CommentMealServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getGlobal();
	DAO dao = new DAO();

	/**
	 * Returns all comments of a meal
	 * 
	 * Request parameters must contain meal_id and user_id
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Statement stmt = null;
		Statement stmt2 = null;
		String meal_id = null;
		String user_id = null;

		// establish database connection
		Connection conn = dao.connect();
		JSONArray comments = new JSONArray();

		try {
			meal_id = request.getParameter("meal_id");
			user_id = request.getParameter("user_id");

			// prepare search statement
			String query_comment = "select u.user_name, c.meal_comment, c.comment_time " + "from T_COMMENTS c "
					+ "join T_USERS u on u.user_id = c.user_id " + "where meal_id = " + meal_id
					+ " order by c.comment_time asc";

			String query_fav = "SELECT * FROM T_FAVORITES WHERE USER_ID = " + user_id + " AND MEAL_ID = " + meal_id;

			// execute statements
			stmt = conn.createStatement();
			stmt2 = conn.createStatement();
			ResultSet rs_userdata = stmt.executeQuery(query_comment);
			ResultSet rs_comment = stmt2.executeQuery(query_fav);

			JSONObject favourite = new JSONObject();

			if (rs_comment.next()) {
				favourite.put("isFavorite", "true");
			} else {
				favourite.put("isFavorite", "false");
			}
			comments.add(favourite);

			while (rs_userdata.next()) {
				JSONObject comment = new JSONObject();
				comment.put("user_name", rs_userdata.getString("USER_NAME"));
				comment.put("comment", rs_userdata.getString("MEAL_COMMENT"));
				comment.put("time", rs_userdata.getString("COMMENT_TIME"));
				comments.add(comment);
			}

			response.setStatus(HttpServletResponse.SC_OK);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
		} finally {
			// close connection
			dao.close(conn);
		}

		// return result to client
		String json = comments.toJSONString();
		response.setContentType("text/javascript");
		response.getOutputStream().print(json);
		response.flushBuffer();
	}

	/**
	 * Inserts a comment on a meal in the T_COMMENTS table
	 * 
	 * POST-JSON object must contain "user_id", "meal_id" and "comment"
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Statement stmt = null;
		String add_comment = null;
		// establish database connection
		Connection conn = dao.connect();

		try {

			// retrieve data from request
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
			String data = br.readLine();

			// and convert to JSON
			Object obj = JSONValue.parse(data);
			JSONObject job = (JSONObject) obj;

			String meal_id = null;
			String user_id = null;
			String comment = null;
			Long time = null;

			if (job.get("user_id") != null)
				user_id = (String) job.get("user_id");
			if (job.get("meal_id") != null)
				meal_id = (String) job.get("meal_id");
			if (job.get("comment") != null)
				comment = (String) job.get("comment");
			if (job.get("time") != null)
				time = (Long) job.get("time");

			// create INSERT statement
			add_comment = "INSERT INTO T_COMMENTS (USER_ID, MEAL_ID, MEAL_COMMENT, COMMENT_TIME) " + "VALUES ('"
					+ user_id + "', '" + meal_id + "', '" + comment + "', '" + time + "')";

			// execute statement
			stmt = conn.createStatement();
			stmt.executeQuery(add_comment);

			response.setStatus(HttpServletResponse.SC_CREATED);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
		} finally {
			// close connection
			dao.close(conn);
		}

		// display SQL statement for testing purposes
		// PrintWriter out = response.getWriter();
		// out.print(add_comment);
	}
}
