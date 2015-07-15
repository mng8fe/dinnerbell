package com.dinnerbell.webapp;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.imgscalr.Scalr;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import sun.misc.BASE64Decoder;

/**
 * Servlet implementation class PictureMeal
 * 
 * url-pattern>/UserPic</url-pattern>
 * 
 * @author Nina
 */
public class UserPic extends HttpServlet {
	private static final long serialVersionUID = 1L;
	DAO dao = new DAO();
	Logger logger = Logger.getGlobal();

	
	
	/**
	 * Returns user profile
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest
	 *      , javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		// establish database connection
		Connection conn = dao.connect();
//		Statement stmt = null;
		Statement stmt3 = null;
		String searchparam = null;
		
		JSONObject user = new JSONObject();

		if(request.getParameter("user_id") != null && !request.getParameter("user_id").isEmpty())
			searchparam = request.getParameter("user_id");

		try {
			
			String query_comment = "SELECT c.MEAL_COMMENT, c.MEAL_ID, m.MEAL_NAME "
					+ "FROM T_COMMENTS c "
					+ "JOIN T_MEALS m on m.meal_id = c.meal_id "
					+ "WHERE c.USER_ID = " + searchparam;
//			String query_usermeals = "SELECT MEAL_ID, LONGITUDE , LATITUDE, COUNTRY "
//					+ "FROM EBW13DB03.PLACES WHERE USER_ID = " + searchparam;

			// execute statements
//			stmt = conn.createStatement();
//			ResultSet rs_usermeals = stmt.executeQuery(query_userplaces);
			stmt3 = conn.createStatement();
			ResultSet rs_commentdata = stmt3.executeQuery(query_comment);

			// convert results to JSON
			JSONArray comments = new JSONArray();
//			JSONArray meals = new JSONArray();

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

			// created meals
//			i = 0;
//			List<String> country = new ArrayList<>();
//			while (rs_userplaces.next()) {
//				JSONObject obj = new JSONObject();
//				obj.put("place_id", rs_userplaces.getString("PLACE_ID"));
//				obj.put("lat", rs_userplaces.getString("LATITUDE"));
//				obj.put("long", rs_userplaces.getString("LONGITUDE"));
//
//				if (!country.contains(rs_userplaces.getString("COUNTRY")))
//					country.add(rs_userplaces.getString("COUNTRY"));
//
//				meals.add(obj);
//				i++;
//			}
//			user.put("places", meals);
//			user.put("places_count", i);
//			user.put("country_count", country.size());
			
			
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
		} finally {
			dao.close(conn);
		}
		
		// return result to client
		String json = user.toJSONString();
		response.setContentType("text/javascript");
		response.getOutputStream().print(json);
		response.flushBuffer();
	}

	
	
	/**
	 * store images fullsize and as thumbnail in db
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 * 
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		// establish database connection
		Connection conn = dao.connect();

		try {
			// variables to store in db
			String user_id = request.getParameter("user_id");
			String image = request.getParameter("image");

			// encode image to resize
			BASE64Decoder decoder = new BASE64Decoder();
			byte[] decodedBytes = decoder.decodeBuffer(image);

			// create thumbnail; 96px,96px
			InputStream img_small = new ByteArrayInputStream(decodedBytes);
			BufferedImage bImage_small = ImageIO.read(img_small);
			BufferedImage small = Scalr.resize(bImage_small, 120, 120, null);
			byte[] small_byte;
			// convert BufferedImage to byte array
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(small, "png", baos);
			baos.flush();
			small_byte = baos.toByteArray();
			
			// blob-inputstreams
			InputStream in = new ByteArrayInputStream(decodedBytes);
			InputStream in_small = new ByteArrayInputStream(small_byte);

			baos.close();
			
			// create UPDATE statement
			PreparedStatement ps2 = conn
					.prepareStatement("UPDATE T_USERS U SET U.PICTURE = ?, U.PICTURE_SMALL = ? WHERE u.user_id = "
							+ user_id);
			ps2.setBinaryStream(1, in, decodedBytes.length);
			ps2.setBinaryStream(2, in_small, small_byte.length);
			ps2.executeUpdate();
			conn.commit();
			response.setStatus(HttpServletResponse.SC_CREATED);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
		} finally {
			// close db connection
			dao.close(conn);
		}

	}
}