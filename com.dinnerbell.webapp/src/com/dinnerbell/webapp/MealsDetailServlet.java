/**
 * 
 */
package com.dinnerbell.webapp;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
//import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import oracle.jdbc.OraclePreparedStatement;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Servlet implementation class MealsDetailServlet
 * 
 * Returns details about a meal or creates a new one
 * 
 * @author Nina
 * 
 */
public class MealsDetailServlet extends HttpServlet {

	private static final long serialVersionUID = 8683792088124350496L;
	Logger logger = Logger.getGlobal();
	DAO dao = new DAO();

	/**
	 * Pictures in original size
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest
	 *      , javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// establish database connection
		Connection conn = dao.connect();
		Statement stmt = null;

		try {
			String searchparam = request.getParameter("image_id");
			String query = "SELECT i.image, i.last_modified, u.user_name " + "FROM T_IMAGES i "
					+ "JOIN T_USERS u on i.user_id = u.user_id " + "WHERE image_id = " + searchparam;

			stmt = conn.createStatement();
			ResultSet rs_images = stmt.executeQuery(query);
			JSONObject place = new JSONObject();

			while (rs_images.next()) {
				InputStream in = rs_images.getBinaryStream("IMAGE");
				byte[] byteimage = IOUtils.toByteArray(in);
				BASE64Encoder encoder = new BASE64Encoder();
				String image = encoder.encodeBuffer(byteimage);
				place.put("image", image);
				place.put("last_modified", rs_images.getDate("LAST_MODIFIED"));
				place.put("user_name", rs_images.getString("USER_NAME"));
			}

			String json = place.toJSONString();
			response.setContentType("text/javascript");
			response.getOutputStream().print(json);
			response.flushBuffer();

			response.setStatus(HttpServletResponse.SC_CREATED);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
		} finally {
			dao.close(conn);
		}

	}

	/**
	 * Gets details for a meal
	 * 
	 * POST object must contain the meal_id
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 * 
	 *      TODO pics, category
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// establish database connection
		Connection conn = dao.connect();

		Statement stmt = null;
		// Statement stmt2 = null;
		Statement stmt3 = null;
		JSONObject meal = new JSONObject();

		try {
			// retrieve data from request
			BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
			String data = br.readLine();

			// and convert to JSON
			Object object = JSONValue.parse(data);
			JSONObject job = (JSONObject) object;

			String query_details = null;
			// String query_ingredients = null;
			String booked_or_not = null;

			Integer total = 0;
			String user_id = null;
			String meal_id = null;

			meal_id = (String) job.get("meal_id");
			user_id = (String) job.get("user_id");

			// prepare queries
			query_details = "SELECT u.rating_count, m.meal_description, m.meal_ingredients, u.user_name, u.picture_small "
					+ "FROM T_MEALS m " + "JOIN T_USERS u on u.user_id = m.user_id " + "WHERE m.meal_id = " + meal_id;

			// query_ingredients = "SELECT i.ing_name " + "FROM T_INGREDIENTS i
			// "
			// + "JOIN T_MEAL_INGREDIENTS mi on mi.ing_id = i.ing_id " + "WHERE
			// mi.meal_id = " + meal_id;

			if (!user_id.equals("00")) {
				booked_or_not = "SELECT COUNT(*) AS total FROM T_BOOKED_MEALS WHERE user_ID = " + user_id
						+ " AND meal_id = " + meal_id;
			}
			// query database
			stmt = conn.createStatement();
			ResultSet rs_meals = stmt.executeQuery(query_details);
			// stmt2 = conn.createStatement();
			// ResultSet rs_ings = stmt2.executeQuery(query_ingredients);
			stmt3 = conn.createStatement();
			if (!user_id.equals("00")) {
				ResultSet rss = stmt3.executeQuery(booked_or_not);
				while (rss.next()) {
					total = rss.getInt("total");
				}
			}
			// convert to JSON
			while (rs_meals.next()) {
				meal.put("rating", rs_meals.getString("RATING_COUNT"));
				meal.put("user_name", rs_meals.getString("USER_NAME"));
				meal.put("description", rs_meals.getString("MEAL_DESCRIPTION"));
				meal.put("ingredients", rs_meals.getString("MEAL_INGREDIENTS"));
				if (!user_id.equals("00")) {
					meal.put("booked", total);
				}
				// JSONArray ings = new JSONArray();

				// convert user-image to JSON
				String image = null;
				if (rs_meals.getBinaryStream("PICTURE_SMALL") != null) {
					try {
						InputStream in = rs_meals.getBinaryStream("PICTURE_SMALL");
						byte[] byteimage = IOUtils.toByteArray(in);
						BASE64Encoder encoder = new BASE64Encoder();
						image = encoder.encodeBuffer(byteimage);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				meal.put("image", image);

				// while (rs_ings.next()) {
				// JSONObject obj = new JSONObject();
				// obj.put("ingredient", rs_ings.getString("ING_NAME"));
				// ings.add(obj);
				// }
				// meal.put("ingredients", ings);
			}
			resp.setStatus(HttpServletResponse.SC_OK);
		} catch (SQLException e) {
			e.printStackTrace();
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} finally {
			// close connection
			dao.close(conn);
		}

		// return result to client
		String json = meal.toJSONString();
		resp.setContentType("text/javascript");
		resp.getOutputStream().print(json);
		resp.flushBuffer();
	}

	/**
	 * Creates a new meal
	 * 
	 * PUT-JSON object should contain user_id, name, price, servings, category,
	 * 
	 * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 * 
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		Statement stmt = null;
		String add_meal = null;
		String json = null;
		String image = null;

		// establish database connection
		Connection conn = dao.connect();

		if (req.getParameter("json") != null && !req.getParameter("json").isEmpty())
			json = req.getParameter("json");
		if (req.getParameter("image") != null && !req.getParameter("image").isEmpty())
			image = req.getParameter("image");

		JSONObject job = new JSONObject();

		try {
			// and convert to JSON
			job = (JSONObject) JSONValue.parse(json);

			// variables representing the meal
			String user_id = null;
			String name = null;
			String price = null;
			Double meal_price = 0.0;
			String servings = null;
			Integer meal_servings = 0;
			String description = null;
			String ingredients = null;
			Long time = null;

			byte[] decodedBytes = null;
			byte[] imageInByte = null;
			byte[] middle_byte = null;

			InputStream in_image = null;
			InputStream in_thumb = null;
			InputStream in_image_middle = null;

			if (job.get("user_id") != null)
				user_id = (String) job.get("user_id");
			if (job.get("name") != null)
				name = (String) job.get("name");
			if (job.get("price") != null)
				price = (String) job.get("price");
			if (job.get("servings") != null)
				servings = (String) job.get("servings");
			if (job.get("description") != null)
				description = (String) job.get("description");
			if (job.get("ingredients") != null)
				ingredients = (String) job.get("ingredients");
			if (job.get("time") != null)
				time = (Long) job.get("time");

			meal_price = Double.parseDouble(price);
			meal_servings = Integer.valueOf(servings);

			// convert image from BASE64 to normal size & thumbnail
			if (image != null && !image.isEmpty()) {
				// encode image to resize
				BASE64Decoder decoder = new BASE64Decoder();
				decodedBytes = decoder.decodeBuffer(image);

				// create thumbnail; 96px,96px
				InputStream in = new ByteArrayInputStream(decodedBytes);
				BufferedImage bImageFromConvert = ImageIO.read(in);
				BufferedImage bImage_small = Scalr.resize(bImageFromConvert, 96);
				// convert BufferedImage to byte array
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(bImage_small, "png", baos);
				baos.flush();
				imageInByte = baos.toByteArray();

				// create middle-sized version, width is 250px
				InputStream img_middle = new ByteArrayInputStream(decodedBytes);
				BufferedImage bImage_middle = ImageIO.read(img_middle);
				BufferedImage middle = Scalr.resize(bImage_middle, 250);
				// convert BufferedImage to byte array
				ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
				ImageIO.write(middle, "png", baos2);
				baos2.flush();
				middle_byte = baos2.toByteArray();

				// blob-inputstreams
				in_thumb = new ByteArrayInputStream(imageInByte);
				in_image = new ByteArrayInputStream(decodedBytes);
				in_image_middle = new ByteArrayInputStream(middle_byte);
			}

			// create INSERT statement
			if (image == null) {
				add_meal = "INSERT INTO T_MEALS ( USER_ID, MEAL_NAME, MEAL_PRICE, MEAL_SERVINGS, MEAL_TIME, MEAL_DESCRIPTION, MEAL_INGREDIENTS ) "
						+ "VALUES ('" + user_id + "', '" + name + "', " + meal_price + ", " + meal_servings + ", "
						+ time + ", '" + description + "', '" + ingredients + "')";

				stmt = conn.createStatement();
				stmt.executeQuery(add_meal);

				// get the new ID of this place
				Long meal_id = (long) 0;

				PreparedStatement getMealID = conn.prepareStatement(
						"Select meal_id from T_MEALS where MEAL_NAME = '" + name + "' and user_id = '" + user_id + "'");

				ResultSet id = getMealID.executeQuery();

				if (id.next()) {
					if (id.getString("meal_id") != null)
						meal_id = (long) Integer.valueOf(id.getString("place_id"));
				}
			} else {
				// statements to store images and meal
				OraclePreparedStatement ps = (OraclePreparedStatement) conn.prepareStatement(
						"INSERT INTO T_MEALS ( USER_ID, MEAL_NAME, MEAL_PRICE, MEAL_SERVINGS, MEAL_TIME, MEAL_DESCRIPTION, MEAL_INGREDIENTS, THUMBNAIL ) "
								+ "VALUES ('" + user_id + "', '" + name + "', " + meal_price + ", " + meal_servings
								+ ", " + time + ", '" + description + "', '" + ingredients + "' , ?)");
				ps.setBinaryStream(1, in_thumb, imageInByte.length);
				ps.executeUpdate();

				// get the new ID of this place
				Long meal_id = (long) 0;

				PreparedStatement getMealID = conn.prepareStatement(
						"Select meal_id from T_MEALS where MEAL_NAME = '" + name + "' and user_id = '" + user_id + "'");

				ResultSet id = getMealID.executeQuery();

				if (id.next()) {
					if (id.getString("meal_id") != null)
						meal_id = (long) Integer.valueOf(id.getString("meal_id"));

					// add pictures
					PreparedStatement ps2 = conn
							.prepareStatement("INSERT INTO T_IMAGES ( MEAL_ID, USER_ID, IMAGE, IMAGE_MIDDLE) VALUES("
									+ String.valueOf(meal_id) + "," + user_id + ",?,?)");
					ps2.setBinaryStream(1, in_image, decodedBytes.length);
					ps2.setBinaryStream(2, in_image_middle, middle_byte.length);
					ps2.executeUpdate();
				}

				conn.commit();

			}

			resp.setStatus(HttpServletResponse.SC_CREATED);
		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
		} finally {
			// close connection
			dao.close(conn);
		}
		resp.setContentType("text/javascript");
		resp.flushBuffer();
	}
}
