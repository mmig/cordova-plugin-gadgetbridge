package de.dfki.iui.mmir.plugins.gadgetbridge.utils;

import java.util.HashSet;

import org.apache.cordova.CordovaInterface;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

//import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_ALARM_CLOCK;
//import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.ORIGIN_INCOMING_CALL;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_ACTIVATE_DISPLAY_ON_LIFT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_DISPLAY_ON_LIFT_END;
//import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_DISPLAY_ON_LIFT_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DATEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DISPLAY_ITEMS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB_END;
//import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB_SCHEDULED;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_DO_NOT_DISTURB_START;
//import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_ENABLE_TEXT_NOTIFICATIONS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_GOAL_NOTIFICATION;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_END;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_THRESHOLD;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_ADDRESS;
//import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_DEVICE_TIME_OFFSET_HOURS;
//import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_USE_HR_FOR_SLEEP_DETECTION;
//import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_USER_ALIAS;
//import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.VIBRATION_COUNT;
//import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.getNotificationPrefKey;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_STEPS_GOAL;

public class ApplySettings {


	private static final HashSet<String> APPLY_SETTINGS_NAMES = new HashSet<String>();
	static {
		APPLY_SETTINGS_NAMES.add(PREF_MIBAND_USE_HR_FOR_SLEEP_DETECTION);
		APPLY_SETTINGS_NAMES.add("heartrate_measurement_interval");
		APPLY_SETTINGS_NAMES.add(PREF_MI2_GOAL_NOTIFICATION);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_DATEFORMAT);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_DISPLAY_ITEMS);
		APPLY_SETTINGS_NAMES.add(PREF_ACTIVATE_DISPLAY_ON_LIFT);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_INACTIVITY_WARNINGS);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_INACTIVITY_WARNINGS_THRESHOLD);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_INACTIVITY_WARNINGS_START);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_INACTIVITY_WARNINGS_END);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_INACTIVITY_WARNINGS_DND);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_INACTIVITY_WARNINGS_DND_START);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_INACTIVITY_WARNINGS_DND_END);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_DO_NOT_DISTURB_START);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_DO_NOT_DISTURB_END);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_DO_NOT_DISTURB);
		APPLY_SETTINGS_NAMES.add(PREF_MI2_DO_NOT_DISTURB_SCHEDULED);
		APPLY_SETTINGS_NAMES.add(PREF_DISPLAY_ON_LIFT_END);
		APPLY_SETTINGS_NAMES.add(PREF_USER_STEPS_GOAL);
		APPLY_SETTINGS_NAMES.add(PREF_MIBAND_ADDRESS);
	}

	private CordovaInterface cordova;

	public ApplySettings(CordovaInterface cordova){
		this.cordova = cordova;
	}

	public void applySettings(String name, Object newVal){
		if(APPLY_SETTINGS_NAMES.contains(name)){

			if(this.enableHeartrateSleepSupport(name, newVal)){
				return;
			}

			if(this.heartrateMeasurementInterval(name, newVal)){
				return;
			}

			if(this.goalNotification(name, newVal)){
				return;
			}

			if(this.setDateFormat(name, newVal)){
				return;
			}

			if(this.displayPages(name, newVal)){
				return;
			}

			if(this.activateDisplayOnLift(name, newVal)){
				return;
			}

			if(this.rotateWristCycleInfo(name, newVal)){
				return;
			}

			if(this.inactivityWarnings(name, newVal)){
				return;
			}

			if(this.inactivityWarningsThreshold(name, newVal)){
				return;
			}

			if(this.inactivityWarningsStart(name, newVal)){
				return;
			}

			if(this.inactivityWarningsEnd(name, newVal)){
				return;
			}

			if(this.inactivityWarningsDnd(name, newVal)){
				return;
			}

			if(this.inactivityWarningsDndStart(name, newVal)){
				return;
			}

			if(this.inactivityWarningsDndEnd(name, newVal)){
				return;
			}

			if(this.fitnessGoal(name, newVal)){
				return;
			}

			//TODO add missing lift settings (see below)

		}
	}

//copied & modified from nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPreferencesActivity
// 2018-05-15 (may need to update these, if that class changes!)


	private boolean enableHeartrateSleepSupport(String name, Object newVal){
		if(PREF_MIBAND_USE_HR_FOR_SLEEP_DETECTION.equals(name)){
			GBApplication.deviceService().onEnableHeartRateSleepSupport(Boolean.TRUE.equals(newVal));
			return true;
		}
		return false;
	}

	private boolean heartrateMeasurementInterval(String name, Object newVal){
		if("heartrate_measurement_interval".equals(name)){
			GBApplication.deviceService().onSetHeartRateMeasurementInterval(Integer.parseInt((String) newVal));
			return true;
		}
		return false;
	}

	private boolean goalNotification(String name, Object newVal){
		if(PREF_MI2_GOAL_NOTIFICATION.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_GOAL_NOTIFICATION);
				}
			});
			return true;
		}
		return false;
	}

	private boolean setDateFormat(String name, Object newVal){
		if(PREF_MI2_DATEFORMAT.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_DATEFORMAT);
				}
			});
			return true;
		}
		return false;
	}

	private boolean displayPages(String name, Object newVal){
		if(PREF_MI2_DISPLAY_ITEMS.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_DISPLAY_ITEMS);
				}
			});
			return true;
		}
		return false;
	}

	private boolean activateDisplayOnLift(String name, Object newVal){
		if(PREF_ACTIVATE_DISPLAY_ON_LIFT.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_ACTIVATE_DISPLAY_ON_LIFT);
				}
			});
			return true;
		}
		return false;
	}

	private boolean rotateWristCycleInfo(String name, Object newVal){
		if(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO);
				}
			});
			return true;
		}
		return false;
	}

	private boolean inactivityWarnings(String name, Object newVal){
		if(PREF_MI2_INACTIVITY_WARNINGS.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS);
				}
			});
			return true;
		}
		return false;
	}

	private boolean inactivityWarningsThreshold(String name, Object newVal){
		if(PREF_MI2_INACTIVITY_WARNINGS_THRESHOLD.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_THRESHOLD);
				}
			});
			return true;
		}
		return false;
	}

	private boolean inactivityWarningsStart(String name, Object newVal){
		if(PREF_MI2_INACTIVITY_WARNINGS_START.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_START);
				}
			});
			return true;
		}
		return false;
	}

	private boolean inactivityWarningsEnd(String name, Object newVal){
		if(PREF_MI2_INACTIVITY_WARNINGS_END.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_END);
				}
			});
			return true;
		}
		return false;
	}

	private boolean inactivityWarningsDnd(String name, Object newVal){
		if(PREF_MI2_INACTIVITY_WARNINGS_DND.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_DND);
				}
			});
			return true;
		}
		return false;
	}

	private boolean inactivityWarningsDndStart(String name, Object newVal){
		if(PREF_MI2_INACTIVITY_WARNINGS_DND_START.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_DND_START);
				}
			});
			return true;
		}
		return false;
	}

	private boolean inactivityWarningsDndEnd(String name, Object newVal){
		if(PREF_MI2_INACTIVITY_WARNINGS_DND_END.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_MI2_INACTIVITY_WARNINGS_DND_END);
				}
			});
			return true;
		}
		return false;
	}

//    String doNotDisturbState = prefs.getString(MiBandConst.PREF_MI2_DO_NOT_DISTURB, PREF_MI2_DO_NOT_DISTURB_OFF);
//    boolean doNotDisturbScheduled = doNotDisturbState.equals(PREF_MI2_DO_NOT_DISTURB_SCHEDULED);

//    private boolean doNotDisturbStart(String name, Object newVal){
//if(PREF_MI2_DO_NOT_DISTURB_START);
//    doNotDisturbStart.setEnabled(doNotDisturbScheduled);
//    doNotDisturbStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//        @Override
//        public boolean onPreferenceChange(Preference preference, Object newVal) {
//            invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    GBApplication.deviceService().onSendConfiguration(PREF_MI2_DO_NOT_DISTURB_START);
//                }
//            });
//            return true;
//        }
//    }

//    private boolean doNotDisturbEnd(String name, Object newVal){
//if(PREF_MI2_DO_NOT_DISTURB_END);
//    doNotDisturbEnd.setEnabled(doNotDisturbScheduled);
//    doNotDisturbEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//        @Override
//        public boolean onPreferenceChange(Preference preference, Object newVal) {
//            invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    GBApplication.deviceService().onSendConfiguration(PREF_MI2_DO_NOT_DISTURB_END);
//                }
//            });
//            return true;
//        }
//    }
//
//    private boolean doNotDisturb(String name, Object newVal){
//if(PREF_MI2_DO_NOT_DISTURB);
//    doNotDisturb.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//        @Override
//        public boolean onPreferenceChange(Preference preference, Object newVal) {
//            final boolean scheduled = PREF_MI2_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());
//
//            doNotDisturbStart.setEnabled(scheduled);
//            doNotDisturbEnd.setEnabled(scheduled);
//
//            invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    GBApplication.deviceService().onSendConfiguration(PREF_MI2_DO_NOT_DISTURB);
//                }
//            });
//            return true;
//        }
//    }

//    String displayOnLiftState = prefs.getString(MiBandConst.PREF_ACTIVATE_DISPLAY_ON_LIFT, PREF_MI2_DO_NOT_DISTURB_OFF);
//    boolean displayOnLiftScheduled = displayOnLiftState.equals(PREF_MI2_DO_NOT_DISTURB_SCHEDULED);
//
//    private boolean displayOnLiftStart(String name, Object newVal){
//if(PREF_DISPLAY_ON_LIFT_START);
//    displayOnLiftStart.setEnabled(displayOnLiftScheduled);
//    displayOnLiftStart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//        @Override
//        public boolean onPreferenceChange(Preference preference, Object newVal) {
//            invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    GBApplication.deviceService().onSendConfiguration(PREF_DISPLAY_ON_LIFT_START);
//                }
//            });
//            return true;
//        }
//    }
//
//
//    private boolean displayOnLiftEnd(String name, Object newVal){
//if(PREF_DISPLAY_ON_LIFT_END);
//    displayOnLiftEnd.setEnabled(displayOnLiftScheduled);
//    displayOnLiftEnd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//        @Override
//        public boolean onPreferenceChange(Preference preference, Object newVal) {
//            invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    GBApplication.deviceService().onSendConfiguration(PREF_DISPLAY_ON_LIFT_END);
//                }
//            });
//            return true;
//        }
//    }
//
//
//    private boolean displayOnLift(String name, Object newVal){
//if(PREF_ACTIVATE_DISPLAY_ON_LIFT);
//    displayOnLift.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//        @Override
//        public boolean onPreferenceChange(Preference preference, Object newVal) {
//            final boolean scheduled = PREF_MI2_DO_NOT_DISTURB_SCHEDULED.equals(newVal.toString());
//
//            displayOnLiftStart.setEnabled(scheduled);
//            displayOnLiftEnd.setEnabled(scheduled);
//
//            invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    GBApplication.deviceService().onSendConfiguration(PREF_ACTIVATE_DISPLAY_ON_LIFT);
//                }
//            });
//            return true;
//        }
//    }

	private boolean fitnessGoal(String name, Object newVal){
		if(PREF_USER_STEPS_GOAL.equals(name)){
			invokeLater(new Runnable() {
				@Override
				public void run() {
					GBApplication.deviceService().onSendConfiguration(PREF_USER_STEPS_GOAL);
				}
			});
			return true;
		}
		return false;
	}

	private void invokeLater(Runnable applySettingsTask){
		this.cordova.getThreadPool().execute(applySettingsTask);
	}

}
