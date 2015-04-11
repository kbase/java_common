package us.kbase.common.mongo;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import us.kbase.common.mongo.exceptions.MongoAuthException;
import us.kbase.common.mongo.exceptions.InvalidHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 4) makes the exceptions thrown by the client initialization easier to deal
 * with.
 *  
 * @author gaprice@lbl.gov
 *
 */
public class GetMongoDB {
	
	private static Logger getLogger() {
		return LoggerFactory.getLogger(GetMongoDB.class);
	}
	
	private static final Map<String, MongoClient> HOST_TO_CLIENT = 
			new HashMap<String, MongoClient>();
	
	public synchronized static void closeAllConnections() {
		for (final String host: HOST_TO_CLIENT.keySet()) {
			HOST_TO_CLIENT.get(host).close();
		}
		HOST_TO_CLIENT.clear();
	}
	
	private synchronized static MongoClient getMongoClient(final String host)
			throws UnknownHostException, InvalidHostException {
		//Only make one instance of MongoClient per JVM per mongo docs
		final MongoClient client;
		if (!HOST_TO_CLIENT.containsKey(host)) {
			// Don't print to stderr
			java.util.logging.Logger.getLogger("com.mongodb")
					.setLevel(Level.OFF);
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
	 * Retries 600s times (e.g. 10m), logs every 10 retries.
	 * @param host the MongoDB host address.
	 * @param database the database to get.
	 * @return the MongoDB database instance.
	 * @throws UnknownHostException if the host is unknown
	 * @throws InvalidHostException if the host is an invalid Mongo address
	 * @throws IOException if an IO exception occurs
	 * @throws InterruptedException if the thread is interrupted while it's
	 * sleeping between connection attempts
	 */
	public static DB getDB(final String host, final String database)
			throws UnknownHostException, InvalidHostException, IOException,
			InterruptedException {
		return getDB(host, database, 600, 10);
	}
	
	/**
	 * Gets a database from a MongoDB instance without authentication.
	 * @param host the MongoDB host address.
	 * @param database the database to get.
	 * @param retryCount - the number of times to retry the MongoDB
	 * connection, 1 retry / sec. 
	 * @param logIntervalCount - how often to log the retries. Logs occur when
	 * retries % logIntervalCount = 0.
	 * @return the MongoDB database instance.
	 * @throws UnknownHostException if the host is unknown
	 * @throws InvalidHostException if the host is an invalid Mongo address
	 * @throws IOException if an IO exception occurs
	 * @throws InterruptedException if the thread is interrupted while it's
	 * sleeping between connection attempts
	 */
	public static DB getDB(final String host, final String database,
			final int retryCount, final int logIntervalCount)
			throws UnknownHostException, InvalidHostException, IOException,
			InterruptedException {
		if (database == null || database.isEmpty()) {
			throw new IllegalArgumentException(
					"database may not be null or the empty string");
		}
		final DB db = getMongoClient(host).getDB(database);
		int retries = 0;
		while (true) {
			try {
				db.getCollectionNames();
				break;
			} catch (MongoException.Network men) {
				if (retries >= retryCount) {
					throw (IOException) men.getCause();
				}
				if (retries % logIntervalCount == 0) {
					getLogger().info(
							"Retrying MongoDB connection {}/{}, attempt {}/{}",
							host, database, retries, retryCount);
				}
				Thread.sleep(1000);
			}
			retries++;
		}
		return db;
	}
	
	/**
	 * Gets a database from a MongoDB instance with authentication.
	 * Retries 600s times (e.g. 10m), logs every 10 retries.
	 * @param host the MongoDB host address.
	 * @param database the database to get.
	 * @param user the MongoDB user with access to the database.
	 * @param pwd the MongoDB user's password.
	 * @return the MongoDB database instance.
	 * @throws UnknownHostException if the host is unknown
	 * @throws InvalidHostException if the host is an invalid Mongo address
	 * @throws IOException if an IO exception occurs
	 * @throws MongoAuthException if the provided credentials are incorrect.
	 * @throws InterruptedException if the thread is interrupted while it's
	 * sleeping between connection attempts
	 */
	public static DB getDB(final String host, final String database,
			final String user, final String pwd)
			throws UnknownHostException, InvalidHostException, IOException,
			MongoAuthException, InterruptedException {
		return getDB(host, database, user, pwd, 600, 10);
	}
	
	/**
	 * Gets a database from a MongoDB instance with authentication.
	 * @param host the MongoDB host address.
	 * @param database the database to get.
	 * @param user the MongoDB user with access to the database.
	 * @param pwd the MongoDB user's password.
	 * @param retryCount - the number of times to retry the MongoDB
	 * connection, 1 retry / sec. 
	 * @param logIntervalCount - how often to log the retries. Logs occur when
	 * retries % logIntervalCount = 0.
	 * @return the MongoDB database instance.
	 * @throws UnknownHostException if the host is unknown
	 * @throws InvalidHostException if the host is an invalid Mongo address
	 * @throws IOException if an IO exception occurs
	 * @throws MongoAuthException if the provided credentials are incorrect.
	 * @throws InterruptedException if the thread is interrupted while it's
	 * sleeping between connection attempts
	 */
	public static DB getDB(final String host, final String database,
			final String user, final String pwd,
			final int retryCount, final int logIntervalCount)
			throws UnknownHostException, InvalidHostException, IOException,
			MongoAuthException, InterruptedException {
		if (database == null || database.isEmpty()) {
			throw new IllegalArgumentException(
					"database may not be null or the empty string");
		}
		final DB db = getMongoClient(host).getDB(database);
		int retries = 0;
		while (true) {
			try {
				db.authenticate(user, pwd.toCharArray());
				break;
			} catch (MongoException.Network men) {
				if (retries >= retryCount) {
					throw (IOException) men.getCause();
				}
				if (retries % logIntervalCount == 0) {
					getLogger().info(
							"Retrying MongoDB connection {}/{}, attempt {}/{}",
							host, database, retries, retryCount);
				}
				Thread.sleep(1000);
			}
			retries++;
		}
		try {
			db.getCollectionNames();
		} catch (MongoException me) {
			throw new MongoAuthException("Not authorized for database "
					+ database, me);
		}
		return db;
	}
	
	public static void main(String[] args) throws Exception {
		//TODO require mongo for tests or change to a different module or something
		System.out.println(getDB("localhost", "ws_test", "ws", "foo"));
	}

}
