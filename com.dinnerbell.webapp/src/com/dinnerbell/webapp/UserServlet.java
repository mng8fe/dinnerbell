/**
 * 
 */
package com.dinnerbell.webapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

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
 * Servlet implementation class UserServlet
 * 
 * create new user, view user profile
 * <url-pattern>/PlacesUser</url-pattern>
 * 
 * @author Nina
 * 
 */
public class UserServlet extends HttpServlet {

	private static final long serialVersionUID = 7595794892010366381L;
	DAO dao = new DAO();

	/**
	 * create a new user
	 * 
	 * PUT-JSON object should contain user_name, password, email, salt
	 * 
	 * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 * 
	 *      TODO user-login?
	 */
	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		Statement stmt = null;
		Statement stmt_fb1 = null;
		Statement stmt_fb2 = null;
		String add_user = null;

		// establish database connection
		Connection conn = dao.connect();

		try {
			// retrieve data from request
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
			String data = br.readLine();

			// and convert to JSON
			Object obj = JSONValue.parse(data);
			JSONObject job = (JSONObject) obj;

			String user_name = null;
			String password = null;
			String email = null;
			String salt = null;
			String fb_id = null;
			String fb_name = null;

			if (job.get("user_name") != null)
				user_name = (String) job.get("user_name");
			if (job.get("password") != null)
				password = (String) job.get("password");
			if (job.get("email") != null)
				email = (String) job.get("email");
			if (job.get("salt") != null)
				salt = (String) job.get("salt");

			// check if user is facebookuser
			if (job.get("fb_name") != null && job.get("fb_id") != null) {
				fb_id = (String) job.get("fb_id");
				fb_name = (String) job.get("fb_name");

				// check if fb-user is already registered
				String is_user = "SELECT * FROM T_USERS WHERE fb_user = '"
						+ fb_id + "'";
				stmt_fb1 = conn.createStatement();
				ResultSet rs_fb = stmt_fb1.executeQuery(is_user);

				// if fb_user is already registered, return dinnerbell-user_id to
				// client
				if (rs_fb.next()) {
					String user_id = rs_fb.getString("USER_ID");
					PrintWriter out = response.getWriter();
					out.print(user_id);
					
				// if not, create user in database
				} else {
					String add_fb_user = "INSERT INTO T_USERS (USER_NAME, FB_USER) VALUES ('"
							+ fb_name + "' , '" + fb_id + "')";
					stmt_fb2 = conn.createStatement();
					stmt_fb2.executeQuery(add_fb_user);

					// retrieve dinnerbell-user_id for created fb-user
					String is_user_now = "SELECT * FROM T_USERS WHERE fb_user = '"
							+ fb_id + "'";
					stmt_fb1 = conn.createStatement();
					ResultSet rs_fb_userid = stmt_fb1.executeQuery(is_user_now);

					// return dinnerbell-user_id to client
					if (rs_fb_userid.next()) {
						String user_id = rs_fb_userid.getString("USER_ID");
						PrintWriter out = response.getWriter();
						out.print(user_id);
					}
					response.setStatus(HttpServletResponse.SC_CREATED);
				}
				// in case of a non facebook-user, create user with
				// password&salt
			} else {
				// create INSERT statement
				add_user = "INSERT INTO T_USERS (USER_NAME, PASSWORD, EMAIL, SALT) VALUES ('"
						+ user_name
						+ "' , '"
						+ password
						+ "' , '"
						+ email
						+ "' , ' " + salt + " ')";

				// execute statement
				stmt = conn.createStatement();
				stmt.executeQuery(add_user);

				response.setStatus(HttpServletResponse.SC_CREATED);
			}
			
//			// geocode address to coordinates
//			if ((gps_long == null || gps_lat == null) && ((street != null && !street.isEmpty())
//					&& ((postal_code != null && !postal_code.isEmpty()) || (city != null && !city.isEmpty())))) {
//				GeoCoder GC = new GeoCoder();
//				String addr = street;
//				if (postal_code != null && !postal_code.isEmpty() && (city == null || city.isEmpty()))
//					addr += " " + postal_code;
//				else if (city != null && !city.isEmpty() && (postal_code == null || postal_code.isEmpty()))
//					addr += " " + city;
//				else
//					addr += " " + postal_code + " " + city;
//				String[] coor = GC.address2coordinates(addr).split(";");
//
//				gps_lat = coor[0];
//				gps_long = coor[1];
//			}
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
		} finally {
			// close connection
			dao.close(conn);
		}

		// display result for testing purposes
		// PrintWriter out = response.getWriter();
		// out.print(add_user);
	}

	/**
	 * retrieve user with name, comments, favorite meals, address
	 * 
	 * POST object must contain the user_id
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 * 
	 *      
	 */
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Statement stmt = null;
		Statement stmt4 = null;
		Statement stmt3 = null;
		String query_user = null;
//		String query_usermeals = null;
		String query_comment = null;

		// establish database connection
		Connection conn = dao.connect();
		JSONObject user = new JSONObject();

		try {
			// retrieve data from request
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
			String data = br.readLine();

			// search statements for username, comments and created meals
			query_user = "SELECT USER_NAME, PICTURE, LATITUDE, LONGITUDE, STREET_NO, POSTAL_CODE, CITY, COUNTRY, MOTTO " + "FROM T_USERS " + "WHERE USER_ID = " + data;
			query_comment = "SELECT C.MEAL_COMMENT, C.MEAL_ID, M.MEAL_NAME " 
					+ "FROM T_COMMENTS C "
					+ "JOIN T_MEALS M on M.MEAL_ID = C.MEAL_ID " 
					+ "WHERE C.USER_ID = " + data;
//			query_usermeals = "SELECT MEAL_ID, LONGITUDE , LATITUDE, COUNTRY "
//					+ "FROM EBW13DB03.PLACES WHERE USER_ID = '" + data + "'";

			// execute statements
			stmt = conn.createStatement();
			ResultSet rs_userdata = stmt.executeQuery(query_user);
//			stmt3 = conn.createStatement();
//			ResultSet rs_favdata = stmt3.executeQuery(query_favs);
			stmt3 = conn.createStatement();
			ResultSet rs_commentdata = stmt3.executeQuery(query_comment);


			// convert results to JSON
			while (rs_userdata.next()) {
				user.put("user_name", rs_userdata.getString("USER_NAME"));
				user.put("lat", rs_userdata.getString("LATITUDE"));
				user.put("long", rs_userdata.getString("LONGITUDE"));
				user.put("street", rs_userdata.getString("STREET_NO"));
				user.put("postal", rs_userdata.getString("POSTAL_CODE"));
				user.put("city", rs_userdata.getString("CITY"));
				user.put("country", rs_userdata.getString("COUNTRY"));
				user.put("motto", rs_userdata.getString("MOTTO"));
//				JSONArray favs = new JSONArray();
				JSONArray comments = new JSONArray();
				
				// convert thumbnail-image to JSON
				String image = null;
				if (rs_userdata.getBinaryStream("PICTURE") != null) {
					try {
						InputStream in = rs_userdata.getBinaryStream("PICTURE");
						byte[] byteimage = IOUtils.toByteArray(in);
						BASE64Encoder encoder = new BASE64Encoder();
						image = encoder.encodeBuffer(byteimage);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				user.put("image", image);

//				// add favorite meals of this user
//				while (rs_favdata.next()) {
//					JSONObject obj = new JSONObject();
//					obj.put("meal_id", rs_favdata.getString("MEAL_ID"));
//					obj.put("meal_name", rs_favdata.getString("MEAL_NAME"));
//					favs.add(obj);
//				}
//				user.put("favorites", favs);

				// add the last 10 comments from this user
				int i = 1;
				while (i <= 10 && rs_commentdata.next()) {
					JSONObject obj = new JSONObject();
					obj.put("comment", rs_commentdata.getString("MEAL_COMMENT"));
					obj.put("meal_id", rs_commentdata.getString("MEAL_ID"));
					obj.put("meal_name", rs_commentdata.getString("MEAL_NAME"));
					comments.add(obj);
					i++;
				}
				user.put("comments", comments);
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
		String json = user.toJSONString();
		response.setContentType("text/javascript");
		response.getOutputStream().print(json);
		response.flushBuffer();
	}

	/**
	 * log in
	 * 
	 * QUERY object should be the user_name
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Statement stmt = null;
		String query_user = null;
		String user_name = null;

		// establish database connection
		Connection conn = dao.connect();
		JSONObject user = new JSONObject();

		try {
			user_name = request.getParameter("user_name");

			// prepare search statement
			query_user = "SELECT U.USER_ID, U.PASSWORD, U.SALT " + "FROM T_USERS U WHERE U.USER_NAME LIKE '%"
					+ user_name + "%'";

			// execute statements
			stmt = conn.createStatement();
			ResultSet rs_userdata = stmt.executeQuery(query_user);

			while (rs_userdata.next()) {
				user.put("user_id", rs_userdata.getString("USER_ID"));
				user.put("password", rs_userdata.getString("PASSWORD"));
				user.put("salt", rs_userdata.getString("SALT"));
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
		String json = user.toJSONString();
		response.setContentType("text/javascript");
		response.getOutputStream().print(json);
		response.getOutputStream().print(user_name);
		response.flushBuffer();
	}
}
