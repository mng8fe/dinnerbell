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

import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Servlet implementation class PicturePlace
 * 
 * <url-pattern>/PicturePlace</url-pattern>
 * 
 * @author Nina
 */
public class PictureMealServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	DAO dao = new DAO();
	Logger logger = Logger.getGlobal();

	/**
	 * store images fullsize and as thumbnail in db
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 * 
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// establish database connection
		Connection conn = dao.connect();

		try {
			// variables to store in db
			String meal_id = request.getParameter("meal_id");
			String user_id = request.getParameter("user_id");
			String image = request.getParameter("image");

			// encode image to resize
			BASE64Decoder decoder = new BASE64Decoder();
			byte[] orig_byte = decoder.decodeBuffer(image);

			// create middle-sized version, width is 250px
			InputStream img_middle = new ByteArrayInputStream(orig_byte);
			BufferedImage bImage_middle = ImageIO.read(img_middle);
			BufferedImage middle = Scalr.resize(bImage_middle, 250, null, null);
			byte[] middle_byte;
			// convert BufferedImage to byte array
			ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
			ImageIO.write(middle, "png", baos2);
			baos2.flush();
			middle_byte = baos2.toByteArray();

			// create thumbnail; 96px,96px
			InputStream img_small = new ByteArrayInputStream(orig_byte);
			BufferedImage bImage_small = ImageIO.read(img_small);
			BufferedImage small = Scalr.resize(bImage_small, 96, 96, null);
			byte[] small_byte;
			// convert BufferedImage to byte array
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(small, "png", baos);
			baos.flush();
			small_byte = baos.toByteArray();

			// blob-inputstreams
			InputStream in_original = new ByteArrayInputStream(orig_byte);
			InputStream in_middle = new ByteArrayInputStream(middle_byte);
			InputStream in_small = new ByteArrayInputStream(small_byte);

			baos.close();

			// create INSERT statement
			PreparedStatement ps = conn
					.prepareStatement("INSERT INTO T_IMAGES ( MEAL_ID, USER_ID, IMAGE, IMAGE_MIDDLE) VALUES("
							+ meal_id + "," + user_id + ",?,?)");
			ps.setBinaryStream(1, in_original, orig_byte.length);
			ps.setBinaryStream(2, in_middle, middle_byte.length);
			ps.executeUpdate();

			PreparedStatement ps2 = conn
					.prepareStatement("UPDATE T_MEALS m SET m.THUMBNAIL = ? WHERE m.meal_id = ?");
			ps2.setBinaryStream(1, in_small, small_byte.length);
			ps2.setInt(2, Integer.valueOf(meal_id));
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

	/**
	 * Delivers the pictures of a given meal
	 * 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// establish database connection
		Connection conn = dao.connect();
		Statement stmt = null;

		try {
			String searchparam = request.getParameter("meal_id");
			String query = "SELECT image_id, user_id, meal_id, image_middle FROM T_IMAGES WHERE meal_id = " + searchparam;

			stmt = conn.createStatement();
			ResultSet rs_images = stmt.executeQuery(query);
			JSONArray jsonarr = new JSONArray();

			while (rs_images.next()) {
				JSONObject meal = new JSONObject();

				meal.put("meal_id", rs_images.getString("MEAL_ID"));
				meal.put("image_id", rs_images.getString("IMAGE_ID"));
				meal.put("user_id", rs_images.getString("USER_ID"));

				InputStream in = rs_images.getBinaryStream("IMAGE_MIDDLE");
				byte[] byteimage = IOUtils.toByteArray(in);

				BASE64Encoder encoder = new BASE64Encoder();
				String image = encoder.encodeBuffer(byteimage);

				meal.put("image", image);
				jsonarr.add(meal);
			}

			String json = jsonarr.toJSONString();
			response.setContentType("text/javascript");
			response.getOutputStream().print(json);
			response.getOutputStream().print(searchparam);
			response.flushBuffer();

			response.setStatus(HttpServletResponse.SC_CREATED);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
			e.printStackTrace();
		} finally {
			dao.close(conn);
		}
	}

}