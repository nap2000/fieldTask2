/*
 * Copyright (C) 2011 Cloudtec Pty Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.smap.smapTask.android.tasks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.smap.smapTask.android.listeners.TaskDownloaderListener;
import org.smap.smapTask.android.taskModel.FormLocator;
import org.smap.smapTask.android.taskModel.TaskResponse;
import org.smap.smapTask.android.utilities.ManageForm;
import org.smap.smapTask.android.utilities.ManageForm.ManageFormDetails;
import org.smap.smapTask.android.utilities.ManageFormResponse;
import org.smap.smapTask.android.utilities.Utilities;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.database.Assignment;
import org.odk.collect.android.database.TaskAssignment;
import org.odk.collect.android.listeners.FormDownloaderListener;
import org.odk.collect.android.listeners.InstanceUploaderListener;
import org.odk.collect.android.logic.FormDetails;
import org.odk.collect.android.logic.PropertyManager;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.tasks.DownloadFormsTask;
import org.odk.collect.android.tasks.InstanceUploaderTask;
import org.odk.collect.android.tasks.InstanceUploaderTask.Outcome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import loaders.TaskEntry;

/**
 * Background task for downloading tasks 
 * 
 * @author Neil Penman (neilpenman@gmail.com)
 */
public class DownloadTasksTask extends AsyncTask<Void, String, HashMap<String, String>> {
    
	private TaskDownloaderListener mStateListener;
	HashMap<String, String> results = null;
    SharedPreferences settings = null;
    ArrayList<TaskEntry> tasks = new ArrayList<TaskEntry>();
    HashMap<Long, TaskStatus> taskMap = new HashMap<Long, TaskStatus>();
    HttpResponse getResponse = null;
    DefaultHttpClient client = null;
    Gson gson = null;
    TaskResponse tr = null;                         // Data returned from the server
    int statusCode;
    String serverUrl = null;                        // Current server
    String source = null;                           // Server name
    String taskURL = null;                          // Url to get tasks
    int count;                                      // Record number of deletes

    String username = null;
    String password = null;
	
	/*
	 * class used to store status of existing tasks in the database and their database id
	 * A hash is created of the data stored in these object to uniquely identify the task
	 */
	
	private class TaskStatus {
		@SuppressWarnings("unused")
		public long tid;
		@SuppressWarnings("unused")
		public String status;
		@SuppressWarnings("unused")
		public boolean keep;
		
		public TaskStatus(long tid, String status) {
			this.tid = tid;
			this.status = status;
			keep = false;
		}
	}

	
	@Override
    protected HashMap<String, String> doInBackground(Void... values) {
	
		results = new HashMap<String,String>();
        settings = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance().getBaseContext());
        source = Utilities.getSource();
        serverUrl = settings.getString(PreferencesActivity.KEY_SERVER_URL, null);
        taskURL = serverUrl + "/surveyKPI/myassignments";

        // Get the username and password
        username = settings.getString(PreferencesActivity.KEY_USERNAME, null);
        password = settings.getString(PreferencesActivity.KEY_PASSWORD, null);

        /*
         * Get an array of the existing tasks on the phone and create a hashmap indexed on the task id
         */
        Utilities.getTasks(tasks);
        for(TaskEntry t : tasks) {
            TaskStatus ts = new TaskStatus(t.id, t.taskStatus);
            taskMap.put(t.taskId, ts);
        }

        synchronise();      // Synchronise the phone with the server

        return results;
    }

    @Override
    protected void onPostExecute(HashMap<String, String> value) {
        synchronized (this) {
            if (mStateListener != null) {
                mStateListener.taskDownloadingComplete(value);
            }
        }
    }

    /*
     * Clean up after cancel
     */
    @Override
    protected void onCancelled() {

    }

    @Override
    protected void onProgressUpdate(String... values) {
        synchronized (this) {
            if (mStateListener != null && values.length > 0) {
                mStateListener.progressUpdate(values[0]);
            }
        }

    }

    public void setDownloaderListener(TaskDownloaderListener sl, Context context) {
        synchronized (this) {
            mStateListener = sl;
        }
    }
  
  
    /*
     * Synchronise the tasks stored on the phone with those on the server
     */
    private void synchronise() {

    	Log.i("DownloadTasksTask", "Synchronise()");
        
        if(source != null) {
	        try {

                /*
                 * Delete tasks which were cancelled on the server and then updated on
                 * the phone during the last refresh
                 */
                count = Utilities.deleteTasksWithStatus(Utilities.STATUS_T_CANCELLED);
                if(count > 0) {
                    results.put("Cancelled Tasks", count + " deleted");
                }

                /*
                 * Mark closed any surveys that were submitted last time and not deleted
                 */
                Utilities.closeTasksWithStatus(Utilities.STATUS_T_SUBMITTED);

	            if(isCancelled()) { throw new CancelException("cancelled"); };		// Return if the user cancels

                /*
	        	 * Get tasks from the server
	        	 */
                client = new DefaultHttpClient();
                if(username != null && password != null) {
                    client.getCredentialsProvider().setCredentials(
                            new AuthScope(null, -1, null),
                            new UsernamePasswordCredentials(username, password));
                }

                // Call the service
                InputStream is = null;
                HttpGet getRequest = new HttpGet(taskURL);
                getResponse = client.execute(getRequest);
                statusCode = getResponse.getStatusLine().getStatusCode();
                if(statusCode != HttpStatus.SC_OK) {
                    Log.w(getClass().getSimpleName(), "Error:" + statusCode + " for URL " + taskURL);
                    results.put("Get Assignments", getResponse.getStatusLine().getReasonPhrase());
                    throw new Exception(getResponse.getStatusLine().getReasonPhrase());
                } else {
                    HttpEntity getResponseEntity = getResponse.getEntity();
                    is = getResponseEntity.getContent();
                }

                // De-serialise
                gson = new GsonBuilder().setDateFormat("dd/MM/yyyy hh:mm").create();
                Reader isReader = new InputStreamReader(is);
                tr = gson.fromJson(isReader, TaskResponse.class);
                Log.i(getClass().getSimpleName(), "Message:" + tr.message);

                if(isCancelled()) { throw new CancelException("cancelled"); };		// Return if the user cancels

                /*
                 * Synchronise forms
                 *  Get any forms the user does not currently have
                 *  Delete any forms that are no longer accessible to the user
                 */
                HashMap<FormDetails, String> outcome = synchroniseForms(tr.forms);
                for (FormDetails key : outcome.keySet()) {
                    results.put(key.formName, outcome.get(key));
                }

                if(isCancelled()) { throw new CancelException("cancelled"); };		// Return if the user cancels

                /*
                 * Apply task changes
                 *  Add new tasks
                 *  Update the status of tasks that have been cancelled on the server
                 */
                addAndUpdateEntries();

            	/*
            	 * Notify the server of the phone state
            	 *  (1) Update on the server all tasks that have a status of "accepted", "rejected" or "submitted" or "cancelled" or "completed"
            	 *      Note in the case of "cancelled" the client is merely acknowledging that it received the cancellation notice
            	 *  (2) Pass the list of forms and versions that have been applied back to the server
            	 */
		        updateTaskStatusToServer(tasks, username, password, serverUrl, tr);

                if(isCancelled()) { throw new CancelException("cancelled"); };		// Return if the user cancels

            	/*
            	 * Delete all entries in the database that we are finished with
            	 */
                count = Utilities.deleteTasksWithStatus(Utilities.STATUS_T_REJECTED);
                if(count > 0) {
                    results.put("Rejected Tasks", count + " deleted");
                }
                if(tr.settings.ft_delete_submitted) {
                    count = Utilities.deleteTasksWithStatus(Utilities.STATUS_T_SUBMITTED);
                    if(count > 0) {
                        results.put("Submitted Tasks", count + " deleted");
                    }
                    count = Utilities.deleteTasksWithStatus(Utilities.STATUS_T_CLOSED);
                    if(count > 0) {
                        results.put(" Tasks", count + " deleted");
                    }
                }

	        } catch(JsonSyntaxException e) {
	        	
	        	Log.e(getClass().getSimpleName(), "JSON Syntax Error:" + " for URL " + taskURL);
	        	publishProgress(e.getMessage());
	        	e.printStackTrace();
	        	results.put("Error:", e.getMessage());
	        	
	        } catch (CancelException e) {	
	        	
	        	Log.i(getClass().getSimpleName(), "Info: Download cancelled by user."); 

	        } catch (Exception e) {	
	        	
	        	Log.e(getClass().getSimpleName(), "Error:" + " for URL " + taskURL);
	        	e.printStackTrace();
	        	publishProgress(e.getMessage());
	        	results.put("Error:", e.getMessage());
	
	        }
        }
        
        
        /*
         * Submit any completed forms
         */
        Outcome outcome = submitCompletedForms();
        if(outcome != null) {
	        for (String key : outcome.mResults.keySet()) {
	        	results.put(key, outcome.mResults.get(key));
	        }
        }
        
  
    }

    private Outcome submitCompletedForms() {
       
        String selection = InstanceColumns.SOURCE + "=? and (" + InstanceColumns.STATUS + "=? or " + 
        		InstanceColumns.STATUS + "=?)";
        String selectionArgs[] = {
        		Utilities.getSource(),
        		InstanceProviderAPI.STATUS_COMPLETE,
                InstanceProviderAPI.STATUS_SUBMISSION_FAILED                 
            };

        ArrayList<Long> toUpload = new ArrayList<Long>();
        Cursor c = null;
        try {
            c = Collect.getInstance().getContentResolver().query(InstanceColumns.CONTENT_URI, null, selection,
                selectionArgs, null);
            
            if (c != null && c.getCount() > 0) {
                c.move(-1);
                while (c.moveToNext()) {
                    Long l = c.getLong(c.getColumnIndex(InstanceColumns._ID));
                    toUpload.add(Long.valueOf(l));
                }
            }

            } catch (Exception e) {
            	e.printStackTrace();
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            InstanceUploaderTask instanceUploaderTask = new InstanceUploaderTask();
            publishProgress("Submitting " + toUpload.size() + " finalised surveys");
            instanceUploaderTask.setUploaderListener((InstanceUploaderListener) mStateListener);

            Long[] toSendArray = new Long[toUpload.size()];
            toUpload.toArray(toSendArray);
            Log.i(getClass().getSimpleName(), "Submitting " + toUpload.size() + " finalised surveys");
            if(toUpload.size() > 0) {
            	return instanceUploaderTask.doInBackground(toSendArray);	// Already running a background task so call direct
            } else {
            	return null;
            }
        
    }
    /*
	 * Delete all entries in the database that have one of the provided task status values
	 *
    private void deleteTasks(ArrayList<String> statusValues) throws Exception {

        Log.i("DownloadTasksTask", "deleteTasks");
        for(TaskEntry t : tasks) {
            for(String status : statusValues) {
                if (t.taskStatus.equals(status)) {
                    Utilities.deleteTask(t.id);
                    tasks.remove(t);
                    break;
                }
            }
    	}
		
	}
	*/

    /*
	 * Loop through the entries in the database
	 *  (1) Update on the server all that have a status of "accepted", "rejected" or "submitted"
	 */
	private void updateTaskStatusToServer(ArrayList<TaskEntry> tasks, String username, String password,
			String serverUrl, TaskResponse tr) throws Exception {

        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse getResponse = null;
        
        // Add credentials
        if(username != null && password != null) {
	        client.getCredentialsProvider().setCredentials(
	                new AuthScope(null, -1, null),
	                new UsernamePasswordCredentials(username, password));
        }
        
        // Add device id to response
        tr.deviceId = new PropertyManager(Collect.getInstance().getApplicationContext())
				.getSingularProperty(PropertyManager.DEVICE_ID_PROPERTY);
        
        tr.taskAssignments = new ArrayList<TaskAssignment> ();		// Reset the passed in taskAssignments, this wil now contain the resposne

        for(TaskEntry t : tasks) {
        	
    		if(isCancelled()) { return; };		// Return if the user cancels

  	  		// Call the update service
  	  		if(t.taskStatus != null && t.isSynced.equals(Utilities.STATUS_SYNC_NO)) {
  	  			TaskAssignment ta = new TaskAssignment();
  	  			ta.assignment = new Assignment();
  	  			ta.assignment.assignment_id = (int) t.taskId;
  	  			ta.assignment.dbId = (int) t.id;
  	  			ta.assignment.assignment_status = t.taskStatus;

	            tr.taskAssignments.add(ta);
  	  		}

        }
        
        // Call the service
        String taskURL = serverUrl + "/surveyKPI/myassignments";
        HttpPost postRequest = new HttpPost(taskURL);
        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        
        Gson gson=  new GsonBuilder().disableHtmlEscaping().setDateFormat("yyyy-MM-dd").create();
		String resp = gson.toJson(tr);
		
        postParameters.add(new BasicNameValuePair("assignInput", resp));
        
        postRequest.setEntity(new UrlEncodedFormEntity(postParameters));
    	getResponse = client.execute(postRequest);
    	
    	int statusCode = getResponse.getStatusLine().getStatusCode();
    	
    	if(statusCode != HttpStatus.SC_OK) {
    		Log.w(getClass().getSimpleName(), "Error:" + statusCode + " for URL " + taskURL);
    	} else {
    		Log.w("updateTaskStatusToServer", "Status updated");
    		for(TaskAssignment ta : tr.taskAssignments) {
    			Utilities.setTaskSynchronized((long) ta.assignment.dbId);		// Mark the task status as synchronised
    		}
        	
    	}
		
	}
	
	/*
     * Loop through the entries from the source
     *   (1) Add entries that have a status of "new", "pending" or "accepted" and are not already on the phone
     *   (2) Update the status of database entries where the source status is set to "cancelled"
     */
	private void addAndUpdateEntries() throws Exception {

    	if(tr.taskAssignments != null) {
        	for(TaskAssignment ta : tr.taskAssignments) {

                if(isCancelled()) { throw new CancelException("cancelled"); };		// Return if the user cancels
	            
        		if(ta.task.type.equals("xform")) {
        			Assignment assignment = ta.assignment;
        			
    				Log.i(getClass().getSimpleName(), "Task: " + assignment.assignment_id + " Status:" +
    						assignment.assignment_status + " Mode:" + ta.task.assignment_mode +
    						" Address: " + ta.task.address + 
    						" Form: " + ta.task.form_id + " version: " + ta.task.form_version + 
    						" Type: " + ta.task.type + "Assignee: " + assignment.assignee + "Username: " + username);
            		

            		// Find out if this task is already on the phone
	          	  	TaskStatus ts = taskMap.get(Long.valueOf((long) assignment.assignment_id));
	          	  	if(ts == null) {
	          	  		Log.i(getClass().getSimpleName(), "New task: " + assignment.assignment_id);
	          	  		// New task
	          	  		if(assignment.assignment_status.equals(Utilities.STATUS_T_NEW) ||
          	  						assignment.assignment_status.equals(Utilities.STATUS_T_ACCEPTED)) {

	          	  			// Ensure the form and instance data are available on the phone
	          	  			// First make sure the initial_data url is sensible (ie null or a URL)
	          	  			if(ta.task.initial_data != null && !ta.task.initial_data.startsWith("http")) {
	          	  				ta.task.initial_data = null;	
	          	  			}
	                		
	          	  			// Add instance data
	          	  			ManageForm mf = new ManageForm();
	          	  			ManageFormResponse mfr = mf.insertInstance(ta, assignment.assignment_id);
	          	  			if(!mfr.isError) {
	          	  				results.put(ta.task.title, "Created");
	          	  			} else {
	          	  				results.put(ta.task.title, "Creation failed: " + mfr.statusMsg );
	          	  			}

	          	  		}
	          	  	} else {        	// Existing task
	          	  		Log.i(getClass().getSimpleName(), "Existing Task: " + assignment.assignment_id + " : " + assignment.assignment_status);

	          	  		if(assignment.assignment_status.equals(Utilities.STATUS_T_CANCELLED) && !ts.status.equals(Utilities.STATUS_T_CANCELLED)) {
                            Utilities.setStatusForTask(assignment.assignment_id, assignment.assignment_status);
                            results.put(ta.task.title, assignment.assignment_status);
	          	  		}
	          	  	}

        			
        		}// end process for xform task
        	}// end tasks loop
    	}
    	
    	return;
	}
	
	/*
     * Synchronise the forms on the server with those on the phone
     *   (1) Download forms on the server that are not on the phone
     *   (2) Delete forms not on the server or older versions of forms
     *       unless there is an uncompleted data instance using that form
     */
	private HashMap<FormDetails, String> synchroniseForms(List<FormLocator> forms) throws Exception {
    	

		HashMap<FormDetails, String> dfResults = null;
    	
    	if(forms == null) {
        	publishProgress("No forms to download");
    	} else {
    		
    		HashMap <String, String> formMap = new HashMap <String, String> ();
          	ManageForm mf = new ManageForm();
    		ArrayList<FormDetails> toDownload = new ArrayList<FormDetails> ();
    		
    		// Create an array of ODK form details
        	for(FormLocator form : forms) {
        		String formVersionString = String.valueOf(form.version);
        		ManageFormDetails mfd = mf.getFormDetails(form.ident, formVersionString);    // Get the form details
        		if(!mfd.exists) {
        			form.url = serverUrl + "/formXML?key=" + form.ident;	// Set the form url from the server address and form ident
        			if(form.hasManifest) {
        				form.manifestUrl = serverUrl + "/xformsManifest?key=" + form.ident;
        			}
        			
        			FormDetails fd = new FormDetails(form.name, form.url, form.manifestUrl, form.ident, formVersionString);
        			toDownload.add(fd);
        		}
        		
        		// Store a hashmap of new forms so we can delete existing forms not in the list
        		String entryHash = form.ident + "_v_" + form.version;
        		formMap.put(entryHash, entryHash);
        	}
	        
        	DownloadFormsTask downloadFormsTask = new DownloadFormsTask();
            publishProgress("Downloading " + toDownload.size() + " forms");

            Log.i(getClass().getSimpleName(), "Downloading " + toDownload.size() + " forms");
            downloadFormsTask.setDownloaderListener((FormDownloaderListener) mStateListener);
            dfResults = downloadFormsTask.doInBackground(toDownload);   // Not in background as called directly
        		
          	// Delete any forms no longer required
        	mf.deleteForms(formMap, results);
    	}
    	
    	return dfResults;
	}

	/*
     * Return true if the passed in instance file is in the odk instance database
     * Assume that if it has been deleted from the database then it can't be sent although
     * it is probably still on the sdcard
     */
    boolean instanceExists(String instancePath) {
    	boolean exists = true;
    	
    	// Get the provider URI of the instance 
        String where = InstanceColumns.INSTANCE_FILE_PATH + "=?";
        String[] whereArgs = {
            instancePath
        };
        
    	ContentResolver cr = Collect.getInstance().getContentResolver();
		Cursor cInstanceProvider = cr.query(InstanceColumns.CONTENT_URI, 
				null, where, whereArgs, null);
		if(cInstanceProvider.getCount() != 1) {
			Log.e("MainListActivity:completeTask", "Unique instance not found: count is:" + 
					cInstanceProvider.getCount());
			exists = false;
		}
		cInstanceProvider.close();
    	return exists;
    }
    
}
