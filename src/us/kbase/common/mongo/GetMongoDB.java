package us.kbase.common.mongo;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import us.kbase.common.mongo.exceptions.MongoAuthException;
import us.kbase.common.mongo.exceptions.InvalidHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;

/**
 * Convenience methods for getting Mongo DB instances.
 * 
 * Does the following:
 * 1) ensures there's only one MongoClient per host. Per the Mongo docs,
 * there should only be one MongoClient per process (presumably per host
 * [multiple host applications would be very weird, but the capability is
 * available]).
 * 2) turns off the very annoying com.mongodb logger.
 * 3) sets autoConnectRetry to true.
 * 4) makes the exceptions thrown by the client easier to deal with.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class GetMongoDB {
	
	private static final Map<String, MongoClient> HOST_TO_CLIENT = 
			new HashMap<String, MongoClient>();
	
	private static MongoClient getMongoClient(final String host) throws
			UnknownHostException, InvalidHostException {
		//Only make one instance of MongoClient per JVM per mongo docs
		final MongoClient client;
		if (!HOST_TO_CLIENT.containsKey(host)) {
			// Don't print to stderr
			Logger.getLogger("com.mongodb").setLevel(Level.OFF);
			final MongoClientOptions opts = MongoClientOptions.builder()
					.autoConnectRetry(true).build();
			try {
				client = new MongoClient(host, opts);
			} catch (NumberFormatException nfe) {
				//throw a better exception if 10gen ever fixes this
				throw new InvalidHostException(host
						+ " is not a valid mongodb host");
			}
			HOST_TO_CLIENT.put(host, client);
		} else {
			client = HOST_TO_CLIENT.get(host);
		}
		return client;
	}
	
	/**
	 * Gets a database from a MongoDB instance without authentication.
	 * @param host the MongoDB host address.
	 * @param database the database to get.
	 * @return the MongoDB database instance.
	 * @throws UnknownHostException if the host is unknown
	 * @throws InvalidHostException if the host is an invalid Mongo address
	 * @throws IOException if an IO exception occurs
	 */
	public static DB getDB(final String host, final String database) throws
			UnknownHostException, InvalidHostException, IOException {
		final DB db = getMongoClient(host).getDB(database);
		try {
			db.getCollectionNames();
		} catch (MongoException.Network men) {
			throw (IOException) men.getCause();
		}
		return db;
	}
	
	/**
	 * Gets a database from a MongoDB instance with authentication.
	 * @param host the MongoDB host address.
	 * @param database the database to get.
	 * @param user the MongoDB user with access to the database.
	 * @param pwd the MongoDB user's password.
	 * @return the MongoDB database instance.
	 * @throws UnknownHostException if the host is unknown
	 * @throws InvalidHostException if the host is an invalid Mongo address
	 * @throws IOException if an IO exception occurs
	 * @throws MongoAuthException if the provided credentials are incorrect.
	 */
	public static DB getDB(final String host, final String database,
			final String user, final String pwd) throws
			UnknownHostException, InvalidHostException, IOException,
			MongoAuthException {
		final DB db = getMongoClient(host).getDB(database);
		try {
			db.authenticate(user, pwd.toCharArray());
		} catch (MongoException.Network men) {
			throw (IOException) men.getCause();
		}
		try {
			db.getCollectionNames();
		} catch (MongoException me) {
			throw new MongoAuthException("Not authorized for database "
					+ database, me);
		}
		return db;
	}

}
