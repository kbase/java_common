package us.kbase.common.test.controllers.mongo;

import static us.kbase.common.test.controllers.ControllerCommon.checkExe;
import static us.kbase.common.test.controllers.ControllerCommon.findFreePort;
import static us.kbase.common.test.controllers.ControllerCommon.makeTempDirs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;


/** Q&D Utility to run a Mongo server for the purposes of testing from
 * Java.
 * @author gaprice@lbl.gov
 *
 */
public class MongoController {

    private final static String DATA_DIR = "data";

    private final static List<String> tempDirectories =
            new LinkedList<String>();
    static {
        tempDirectories.add(DATA_DIR);
    }

    private final Path tempDir;

    private final Process mongo;
    private final int port;

    public MongoController(
            final String mongoExe,
            final Path rootTempDir)
                    throws Exception {
        this(mongoExe, rootTempDir, false);
    }

    public MongoController(
            final String mongoExe,
            final Path rootTempDir,
            final boolean useWiredTiger)
                    throws Exception {
        checkExe(mongoExe, "mongod server");
        tempDir = makeTempDirs(rootTempDir, "MongoController-", tempDirectories);
        port = findFreePort();

        List<String> command = new LinkedList<String>();
        command.addAll(Arrays.asList(mongoExe, "--port", "" + port,
                "--dbpath", tempDir.resolve(DATA_DIR).toString(),
                "--nojournal"));
        if (useWiredTiger) {
            command.addAll(Arrays.asList("--storageEngine", "wiredTiger"));
        }
        ProcessBuilder servpb = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(tempDir.resolve("mongo.log").toFile());

        mongo = servpb.start();
        Thread.sleep(1000); //wait for server to start up
    }

    public int getServerPort() {
        return port;
    }

    public Path getTempDir() {
        return tempDir;
    }

    public void destroy(boolean deleteTempFiles) throws IOException {
        if (mongo != null) {
            mongo.destroy();
        }
        if (tempDir != null && deleteTempFiles) {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }

    public static void main(String[] args) throws Exception {
        MongoController ac = new MongoController(
                "/kb/runtime/bin/mongod",
                Paths.get("workspacetesttemp"));
        System.out.println(ac.getServerPort());
        System.out.println(ac.getTempDir());
        Scanner reader = new Scanner(System.in);
        System.out.println("any char to shut down");
        //get user input for a
        reader.next();
        ac.destroy(false);
        reader.close();
    }

}
