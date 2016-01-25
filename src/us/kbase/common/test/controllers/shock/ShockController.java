package us.kbase.common.test.controllers.shock;

import static us.kbase.common.test.controllers.ControllerCommon.checkExe;
import static us.kbase.common.test.controllers.ControllerCommon.findFreePort;
import static us.kbase.common.test.controllers.ControllerCommon.makeTempDirs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;

import us.kbase.common.mongo.GetMongoDB;

/** Q&D Utility to run a Shock server for the purposes of testing from
 * Java.
 * @author gaprice@lbl.gov
 *
 */
public class ShockController {
	
	//TODO share common code with AweController
	
	private final static String SHOCK_CONFIG_FN = "shock.cfg";
	private final static String SHOCK_CONFIG =
			"us/kbase/common/test/controllers/shock/conf/" +
					SHOCK_CONFIG_FN;
	
	private final static List<String> TEMP_DIRS =
			new LinkedList<String>();
	static {
		TEMP_DIRS.add("shock/site");
		TEMP_DIRS.add("shock/data");
		TEMP_DIRS.add("shock/logs");
	}
	
	//TODO might need a proper version class that can do ranges etc
	private final static Map<String, String> VERSION_MAP =
			new HashMap<String, String>();
	static {
		VERSION_MAP.put("7ADD72D33FA63C7C031E3A5717006009", "0.8.23");
		VERSION_MAP.put("7AA8762CF2A9E4E450CB025AE7AD968B", "0.9.6");
		VERSION_MAP.put("45593bbd8ad0716fe931596ebc91fb75".toUpperCase(),
				"0.9.12");
	}
	
	private final Path tempDir;
	
	private final Process shock;
	private final int port;
	private final String version;

	public ShockController(
			final String shockExe,
			final Path rootTempDir,
			final String adminUser,
			final String mongohost,
			final String shockMongoDBname,
			final String mongouser,
			final String mongopwd)
					throws Exception {
		tempDir = makeTempDirs(rootTempDir, "ShockController-", TEMP_DIRS);
		port = findFreePort();
		
		checkExe(shockExe, "shock server");
		version = getVersion(shockExe);
		
		Velocity.init();
		VelocityContext context = new VelocityContext();
		context.put("port", port);
		context.put("tempdir", tempDir.toAbsolutePath().toString());
		context.put("mongohost", mongohost);
		context.put("mongodbname", shockMongoDBname);
		context.put("mongouser", mongouser == null ? "" : mongouser);
		context.put("mongopwd", mongopwd == null ? "" : mongopwd);
		context.put("shockadmin", adminUser);
		
		File shockcfg = tempDir.resolve(SHOCK_CONFIG_FN).toFile();
		
		generateConfig(SHOCK_CONFIG, context, shockcfg);
		
		final DB shockDB;
		if (mongouser != null) {
			shockDB = GetMongoDB.getDB(mongohost, shockMongoDBname, mongouser,
					mongopwd);
		} else {
			shockDB = GetMongoDB.getDB(mongohost, shockMongoDBname);
		}
		
		setupWorkarounds(shockDB, adminUser, version);

		ProcessBuilder servpb = new ProcessBuilder(shockExe, "--conf",
				shockcfg.toString())
				.redirectErrorStream(true)
				.redirectOutput(tempDir.resolve("shock_server.log").toFile());
		
		shock = servpb.start();
		Thread.sleep(1000); //wait for server to start
	}
	
	private void setupWorkarounds(DB shockDB, String adminUser,
			String version) {
		if ("0.8.23".equals(version)) {
			// the version of 0.8.23 above actually works fine without this,
			// but it's a few commits beyond the actual 0.8.23 tag, so if the
			// exact tagged version is added it'll need the admin insert
			addAdminUser(shockDB, adminUser);
		} else if ("0.9.6".equals(version)) {
			setCollectionVersions(shockDB, 2, 2, 1);
			addAdminUser(shockDB, adminUser);
		} else if ("0.9.12".equals(version)) {
			setCollectionVersions(shockDB, 4, 2, 1);
			addAdminUser(shockDB, adminUser);
		} else {
			//no workarounds possible
		}
	}
	
	private void addAdminUser(DB shockDB, String adminUser) {
		final DBObject a = new BasicDBObject("username", adminUser);
		a.put("uuid", "095abbb0-07cc-43b3-8fd9-98edfb2541be");
		a.put("fullname", "");
		a.put("email", "");
		a.put("password", "");
		a.put("shock_admin", true);
		shockDB.getCollection("Users").save(a);
	}

	private void setCollectionVersions(DB shockDB, int nodever,
			int aclver, int authver) {
		final DBObject n = new BasicDBObject("name", "Node");
		n.put("version", nodever);
		final DBObject acl = new BasicDBObject("name", "ACL");
		acl.put("version", aclver);
		final DBObject auth = new BasicDBObject("name", "Auth");
		auth.put("version", authver);
		shockDB.getCollection("Versions").insert(Arrays.asList(n, acl, auth));
	}

	/** Returns the Shock version determined from the file MD5. Returns null
	 * for versions that haven't been added to the version map in this class.
	 * In this case startup workarounds cannot be applied and Shock may not
	 * start.
	 * @return the Shock version.
	 */
	public String getVersion() {
		return version;
	}

	private String getVersion(String shockExe) throws IOException {
		final MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException(
					"apparently the MD5 algorithm doesn't exist. Who knew.",
					nsae);
		}
		md.update(Files.readAllBytes(Paths.get(shockExe)));
		final byte[] digest = md.digest();
		final String digestInHex = DatatypeConverter.printHexBinary(digest)
				.toUpperCase();
		
//		System.out.println(digestInHex);
		if (!VERSION_MAP.containsKey(digestInHex)) {
			return null;
		}
		return VERSION_MAP.get(digestInHex);
	}

	public int getServerPort() {
		return port;
	}
	
	public Path getTempDir() {
		return tempDir;
	}
	
	public void destroy(boolean deleteTempFiles) throws IOException {
		if (shock != null) {
			shock.destroy();
		}
		if (tempDir != null && deleteTempFiles) {
			FileUtils.deleteDirectory(tempDir.toFile());
		}
	}

	private void generateConfig(final String configFile,
			final VelocityContext context, File file)
			throws IOException {
		String template = IOUtils.toString(new BufferedReader(
				new InputStreamReader(
						getClass().getClassLoader()
						.getResourceAsStream(configFile))));
		
		StringWriter sw = new StringWriter();
		Velocity.evaluate(context, sw, "shockconfig", template);
		PrintWriter pw = new PrintWriter(file);
		pw.write(sw.toString());
		pw.close();
	}
	
	public static void main(String[] args) throws Exception {
		ShockController ac = new ShockController(
				"/kb/deployment/bin/shock-server",
				Paths.get("workspacetemp"),
				"kbasetest2",
				"localhost",
				"delete_shock_db",
				"foo", "foo");
		System.out.println(ac.getServerPort());
		Scanner reader = new Scanner(System.in);
		System.out.println("any char to shut down");
		//get user input for a
		reader.next();
		ac.destroy(false);
		reader.close();
	}
	
}
