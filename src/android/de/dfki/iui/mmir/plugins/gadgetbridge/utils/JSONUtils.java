package de.dfki.iui.mmir.plugins.gadgetbridge.utils;

import org.apache.cordova.LOG;
import org.json.JSONArray;

public class JSONUtils {

  private static final String TAG_NAME = "GBJSONUtils";

  public static String getString(JSONArray args, int i, String defaultValue) {
    if(args.length() > i){
      try{
        return args.getString(i);
      } catch(Exception e){
        LOG.e(TAG_NAME, "Failed to extract String argument at "+i+": "+e.getLocalizedMessage(), e);
      }
    }
    return defaultValue;
  }

  public static boolean isString(JSONArray args, int i) {
    if(args.length() > i){
      try{
        Object obj = args.getJSONObject(i);
        return obj instanceof String;
      } catch(Exception e){
        LOG.e(TAG_NAME, String.format("Could not access to extract String argument at %d as String: %s ", i, e.getLocalizedMessage()), e);
      }
    }
    return false;
  }

  /**
   * HELPER extract argument as boolean from index i.
   *
   * @param args the arguments
   * @param i the index in args
   * @param defaultValue a default value, in case the there is no boolean value in args at i
   * @return defaultValue if there was a problem, otherwise the boolean-argument value from i
   */
  public static boolean getBool(JSONArray args, int i, boolean defaultValue) {
    if(args.length() > i){
      try{
        return args.getBoolean(i);
      } catch(Exception e){
        LOG.e(TAG_NAME, "Failed to extract boolean argument at "+i+": "+e.getLocalizedMessage(), e);
      }
    }
    return defaultValue;
  }

  /**
   * HELPER extract argument as int from index i.
   *
   * @param args the arguments
   * @param i the index in args
   * @param defaultValue a default value, in case the there is no int value in args at i
   * @return defaultValue if there was a problem, otherwise the int-argument value from i
   */
  public static int getInt(JSONArray args, int i, int defaultValue) {
    if(args.length() > i){
      try{
        return args.getInt(i);
      } catch(Exception e){
        LOG.e(TAG_NAME, "Failed to extract int argument at "+i+": "+e.getLocalizedMessage(), e);
      }
    }
    return defaultValue;
  }

  /**
  * HELPER extract argument as long from index i.
  *
  * @param args the arguments
  * @param i the index in args
  * @param defaultValue a default value, in case the there is no long value in args at i
  * @return defaultValue if there was a problem, otherwise the long-argument value from i
  */
  public static long getLong(JSONArray args, int i, long defaultValue) {
    if(args.length() > i){
      try{
        return args.getLong(i);
      } catch(Exception e){
        LOG.e(TAG_NAME, "Failed to extract long argument at "+i+": "+e.getLocalizedMessage(), e);
      }
    }
    return defaultValue;
  }

}
