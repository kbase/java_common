package us.kbase.common.awe.task;

import java.util.Map;

import us.kbase.common.taskqueue.JobStatuses;

/**
 * Any AWE task that you would like to run with help of AWETaskHolder should
 * implement this interface.
 * @author rsutormin
 */
public interface AweJobInterface {
	/**
	 * Return null in case you don't know what could be description of your task.
	 */
	public String getDescription();
	/**
	 * Return json-text of InitProgress structure described in UJS spec. 
	 * In case you don't know what it is about just return null.
	 */
	public String getInitProgess();
	/**
	 * Time in milliseconds (like System.currentTimeMills() returns) of expected moment
	 * when job is supposed to be done. If not sure just return null.
	 */
	public Long getEstimatedFinishTime();
	/**
	 * Technical settings necessary for this task.
	 */
	public void init(String jobId, String token, Map<String, String> config) throws Exception;
	/**
	 * Instance of JobStatuses is necessary in current implementation unfortunately.
	 * Please use example from some project which is using this AWE helper.
	 */
	public JobStatuses getJobStatuses() throws Exception;
}
