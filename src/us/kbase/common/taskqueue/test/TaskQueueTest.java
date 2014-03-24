package us.kbase.common.taskqueue.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isNull;

import java.io.File;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import junit.framework.Assert;

import org.easymock.EasyMockSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.common.taskqueue.JobStatuses;
import us.kbase.common.taskqueue.TaskQueue;
import us.kbase.common.taskqueue.TaskQueueConfig;
import us.kbase.common.taskqueue.TaskRunner;
import us.kbase.common.utils.DbConn;

public class TaskQueueTest extends EasyMockSupport {
	private static File tmpDir = new File("temp" + System.currentTimeMillis());
	
	@BeforeClass
	public static void makeTempDir() {
		tmpDir.mkdir();
	}
	
	@AfterClass
	public static void dropTempDir() throws Exception {
		if (!tmpDir.exists())
			return;
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		File dbDir = new File(tmpDir, TaskQueue.DERBY_DB_NAME);
		try {
			DriverManager.getConnection("jdbc:derby:" + dbDir.getParent() + "/" + dbDir.getName() + ";shutdown=true");
		} catch (Exception ignore) {}
		delete(tmpDir);
	}
	
	private static void delete(File fileOrDir) {
		if (fileOrDir.isDirectory()) {
			for (File sub : fileOrDir.listFiles())
				delete(sub);
		}
		fileOrDir.delete();
	}
	
	@Test
	public void testGood() throws Exception {
		String token = "secret";
		String jobId = "job123";
		JobStatuses jbst = createStrictMock(JobStatuses.class);
		expect(jbst.createAndStartJob(eq(token), eq("queued"), anyObject(String.class), 
				anyObject(String.class), isNull(String.class))).andReturn(jobId);
		jbst.updateJob(eq(jobId), eq(token), eq("running"), isNull(String.class));
		jbst.completeJob(eq(jobId), eq(token), eq("done"), isNull(String.class), 
				anyObject(String.class), anyObject(String.class));
		final TaskQueue[] tq = {null};
		final boolean[] complete = {false};
		expectLastCall().andDelegateTo(new JobStatuses() {
			@Override
			public void updateJob(String job, String token, String status,
					String estComplete) throws Exception {
			}
			@Override
			public String createAndStartJob(String token, String status, String desc,
					String initProgressPtype, String estComplete) throws Exception {
				return null;
			}
			@Override
		    public void completeJob(String job, String token, String status, String error, String wsUrl, 
		    		String outRef) throws Exception {
				complete[0] = true;
		    }
		});
		replayAll();
		final boolean[] isOk = {false};
		tq[0] = new TaskQueue(new TaskQueueConfig(1, tmpDir, jbst, null, null),
				new TestTaskRunner() {
					@Override
					public void run(String token, TestTask inputData,
							String jobId, String outRef) throws Exception {
						isOk[0] = true;
					}
				});
		Assert.assertEquals(jobId, tq[0].addTask(new TestTask("something-saved"), token));
		while (tq[0].getTask(jobId) != null) {
			Thread.sleep(100);
		}
		tq[0].stopAllThreads();
		verifyAll();
		Assert.assertTrue(complete[0]);
		Assert.assertTrue(isOk[0]);
		Assert.assertEquals((Integer)0, TaskQueue.getDbConnection(tmpDir).collect(
				"select count(*) from " + TaskQueue.QUEUE_TABLE_NAME, new DbConn.SqlLoader<Integer>() {
			@Override
			public Integer collectRow(ResultSet rs) throws SQLException {
				return rs.getInt(1);
			}
		}).get(0));
	}

	@Test
	public void testBad() throws Exception {
		String token = "secret";
		String jobId = "job123";
		final String errorMsg = "Super error!";
		JobStatuses jbst = createStrictMock(JobStatuses.class);
		expect(jbst.createAndStartJob(eq(token), eq("queued"), anyObject(String.class), 
				anyObject(String.class), isNull(String.class))).andReturn(jobId);
		jbst.updateJob(eq(jobId), eq(token), eq("running"), isNull(String.class));
		jbst.completeJob(eq(jobId), eq(token), eq("Error: " + errorMsg), anyObject(String.class), 
				anyObject(String.class), anyObject(String.class));
		final TaskQueue[] tq = {null};
		final boolean[] complete = {false};
		expectLastCall().andDelegateTo(new JobStatuses() {
			@Override
			public void updateJob(String job, String token, String status,
					String estComplete) throws Exception {
			}
			@Override
			public String createAndStartJob(String token, String status, String desc,
					String initProgressPtype, String estComplete) throws Exception {
				return null;
			}
			@Override
		    public void completeJob(String job, String token, String status, String error, String wsUrl, 
		    		String outRef) throws Exception {
				complete[0] = true;
		    }
		});
		replayAll();
		tq[0] = new TaskQueue(new TaskQueueConfig(1, tmpDir, jbst, null, null), new TestTaskRunner() {
			@Override
			public void run(String token, TestTask inputData, String jobId, String outRef) throws Exception {
				throw new IllegalStateException(errorMsg);
			}
		});
		Assert.assertEquals(jobId, tq[0].addTask(new TestTask("something-saved"), token));
		while (tq[0].getTask(jobId) != null) {
			Thread.sleep(100);
		}
		tq[0].stopAllThreads();
		verifyAll();
		Assert.assertTrue(complete[0]);
		Assert.assertEquals((Integer)0, TaskQueue.getDbConnection(tmpDir).collect(
				"select count(*) from " + TaskQueue.QUEUE_TABLE_NAME, new DbConn.SqlLoader<Integer>() {
			@Override
			public Integer collectRow(ResultSet rs) throws SQLException {
				return rs.getInt(1);
			}
		}).get(0));
	}

	public static class TestTask {
		private String innerParam;
		
		public TestTask() {
		}
		
		public TestTask(String param) {
			this.innerParam = param;
		}
		
		public String getInnerParam() {
			return innerParam;
		}
		
		public void setInnerParam(String innerParam) {
			this.innerParam = innerParam;
		}
	}
	
	public abstract static class TestTaskRunner implements TaskRunner<TestTask> {
		@Override
		public Class<TestTask> getInputDataType() {
			return TestTask.class;
		}
		
		@Override
		public String getOutRef(TestTask inputData) {
			return null;
		}
		
		@Override
		public String getTaskDescription() {
			return "Nothing personal, just test";
		}
		
		@Override
		public void init(TaskQueueConfig mainCfg, Map<String, String> configParams) {
		}
	}
}
