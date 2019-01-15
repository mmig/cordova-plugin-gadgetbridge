package de.dfki.iui.mmir.plugins.gadgetbridge.db;

import java.util.LinkedList;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;
import org.json.JSONArray;

import android.os.AsyncTask;
import de.dfki.iui.mmir.plugins.gadgetbridge.IDeviceManager;
import de.dfki.iui.mmir.plugins.gadgetbridge.utils.ResultUtils;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class DbTaskManager {

  private static final String TAG_NAME = "GBDbManager";
  public static final String TASK_RETRIEVING_DATA = "Retrieving Data";
  public static final String TASK_REMOVING_DATA = "Removing Data";
  public static final String TASK_INSERTING_DATA = "Inserting Data";

  protected CordovaInterface cordova;
  protected IDeviceManager deviceManager;

  protected final LinkedList<DBAccess> dbTasks = new LinkedList<DBAccess>();

  protected AsyncTask retrieveTask;
  protected AsyncTask removeTask;
  protected AsyncTask insertTask;


  public DbTaskManager(CordovaInterface cordova, IDeviceManager deviceManager) {
    super();
    this.cordova = cordova;
    this.deviceManager = deviceManager;
  }


  public void executeNextDbTask(DbTaskType ignoreActiveTask){
    synchronized (dbTasks) {
      if (dbTasks.size() > 0 && !isDbTaskActive(ignoreActiveTask)) {
        LOG.d(TAG_NAME, "executing next DBAccess task...");
        dbTasks.removeFirst().execute();
      } else {
        LOG.d(TAG_NAME, "no next DBAccess task to execute");
      }
    }
  }

  public boolean queueIfDbTaskActive(DBAccess task){
    synchronized (dbTasks) {
      if (isDbTaskActive(null)) {
        LOG.d(TAG_NAME, "queuing DBAccess task");
        dbTasks.addLast(task);
        return true;
      }
    }
    LOG.d(TAG_NAME, "no need to queue DBAccess task");
    return false;
  }

  public boolean isDbTaskActive(DbTaskType ignoreActiveTask){
    if (
      (!DbTaskType.REMOVE.equals(ignoreActiveTask)   && removeTask  != null && removeTask.getStatus()  != AsyncTask.Status.FINISHED)
        || (!DbTaskType.RETRIEVE.equals(ignoreActiveTask) && retrieveTask != null && retrieveTask.getStatus() != AsyncTask.Status.FINISHED)
        || (!DbTaskType.INSERT.equals(ignoreActiveTask)   && insertTask  != null && insertTask.getStatus()  != AsyncTask.Status.FINISHED)
      ) {

      return true;
    }
    return false;
  }

  public void retrieveData(int start, int end, CallbackContext callbackContext) {
    GBDevice device = deviceManager.getDevice();
    if (device != null) {

      RetrievalTask task = new RetrievalTask(this, TASK_RETRIEVING_DATA, start, end, cordova.getActivity(), callbackContext);
      if(!queueIfDbTaskActive(task)){
        retrieveTask = task.execute();
      }

    } else {
      ResultUtils.sendNoDeviceError(callbackContext, "Could not retrieve data");
    }
  }

  public void removeData(int start, int end, boolean isRemoveAll, CallbackContext callbackContext) {
    GBDevice device = deviceManager.getDevice();
    if (device != null) {

      RemovalTask task = new RemovalTask(this, TASK_REMOVING_DATA, start, end, isRemoveAll, cordova.getActivity(), callbackContext);
      if(!queueIfDbTaskActive(task)){
        removeTask = task.execute();
      }

    } else {
      ResultUtils.sendNoDeviceError(callbackContext, "Could not remove data");
    }
  }

  public void insertData(JSONArray args, CallbackContext callbackContext) {
    GBDevice device = getDevice();
    if (device != null) {

      InsertionTask task = new InsertionTask(this, TASK_INSERTING_DATA, args, cordova.getActivity(), callbackContext);
      if(!queueIfDbTaskActive(task)){
        insertTask = task.execute();
      }

    } else {
      ResultUtils.sendNoDeviceError(callbackContext, "Could not retrieve data");
    }
  }

  public CordovaInterface getCordova() {
    return this.cordova;
  }

  public GBDevice getDevice() {
    return this.deviceManager.getDevice();
  }
}
