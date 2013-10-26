package com.droidplanner;

import android.app.Application;

import android.content.Context;
import android.os.Handler;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.ardupilotmega.msg_heartbeat;
import com.MAVLink.Messages.enums.MAV_MODE_FLAG;
import com.droidplanner.MAVLink.MavLinkMsgHandler;
import com.droidplanner.MAVLink.MavLinkStreamRates;
import com.droidplanner.drone.Drone;
import com.droidplanner.file.DirectoryPath;
import com.droidplanner.file.IO.VehicleProfile;
import com.droidplanner.file.IO.VehicleProfileReader;
import com.droidplanner.helpers.FollowMe;
import com.droidplanner.helpers.RecordMe;
import com.droidplanner.helpers.TTS;
import com.droidplanner.service.MAVLinkClient;
import com.droidplanner.service.MAVLinkClient.OnMavlinkClientListner;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.util.HashMap;

public class DroidPlannerApp extends Application implements
		OnMavlinkClientListner {

    private static final String VEHICLEPROFILE_PATH = "VehicleProfiles";
    private static long HEARTBEAT_NORMAL_TIMEOUT = 5000;
	private static long HEARTBEAT_LOST_TIMEOUT = 15000;

	public Drone drone;
	private MavLinkMsgHandler mavLinkMsgHandler;
	public FollowMe followMe;
	public RecordMe recordMe;
	public ConnectionStateListner conectionListner;
	public OnSystemArmListener onSystemArmListener;
	private TTS tts;

	enum HeartbeatState {
		FIRST_HEARTBEAT, LOST_HEARTBEAT, NORMAL_HEARTBEAT
	}

	private HeartbeatState heartbeatState;
	private Handler watchdog = new Handler();
	private Runnable watchdogCallback = new Runnable()
	{
		@Override
		public void run()
		{
			onHeartbeatTimeout();
		}
	};

	public interface OnWaypointUpdateListner {
		public void onWaypointsUpdate();
	}

	public interface ConnectionStateListner {
		public void notifyConnected();
		
		public void notifyDisconnected();
	}

	public interface OnSystemArmListener {
		public void notifyArmed();
		
		public void notifyDisarmed();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		tts = new TTS(this);
		MAVLinkClient MAVClient = new MAVLinkClient(this, this);
		drone = new Drone(tts, MAVClient, getApplicationContext());
		followMe = new FollowMe(this, drone);
		recordMe = new RecordMe(this, drone);
		mavLinkMsgHandler = new com.droidplanner.MAVLink.MavLinkMsgHandler(
				drone);
        loadVehicleProfiles(this);
	}

    @Override
	public void notifyReceivedData(MAVLinkMessage msg) {
		if(msg.msgid == msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT){
			msg_heartbeat msg_heart = (msg_heartbeat) msg;
			if((msg_heart.base_mode & (byte) MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED) == (byte) MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED){
				notifyArmed();
			}
			else {
				notifyDisarmed();
			}
			onHeartbeat();
		}
		mavLinkMsgHandler.receiveData(msg);
	}

	@Override
	public void notifyDisconnected() {
		conectionListner.notifyDisconnected();
		tts.speak("Disconnected");

		// stop watchdog
		watchdog.removeCallbacks(watchdogCallback);
	}

	@Override
	public void notifyConnected() {
		MavLinkStreamRates.setupStreamRatesFromPref(this);
		conectionListner.notifyConnected();
		// don't announce 'connected' until first heartbeat received

		// start watchdog
		heartbeatState = HeartbeatState.FIRST_HEARTBEAT;
		restartWatchdog(HEARTBEAT_NORMAL_TIMEOUT);
	}

	@Override
	public void notifyArmed() {
		onSystemArmListener.notifyArmed();
	}

	@Override
	public void notifyDisarmed() {
		onSystemArmListener.notifyDisarmed();
	}

	private void onHeartbeat() {

		switch(heartbeatState) {
			case FIRST_HEARTBEAT:
				tts.speak("Connected");
				break;

			case LOST_HEARTBEAT:
				tts.speak("Data link restored");
				break;
		}

		heartbeatState = HeartbeatState.NORMAL_HEARTBEAT;
		restartWatchdog(HEARTBEAT_NORMAL_TIMEOUT);
	}

	private void onHeartbeatTimeout() {
		tts.speak("Data link lost, check connection.");
		heartbeatState = HeartbeatState.LOST_HEARTBEAT;
		restartWatchdog(HEARTBEAT_LOST_TIMEOUT);
	}

	private void restartWatchdog(long timeout)
	{
		// re-start watchdog
		watchdog.removeCallbacks(watchdogCallback);
		watchdog.postDelayed(watchdogCallback, timeout);
	}

    private void loadVehicleProfiles(Context context) {
        drone.vehicleProfiles = new HashMap<String, VehicleProfile>();
        try {
            // TODO: load all
            final VehicleProfile profile = loadVehicleProfile(context, "ArduCopter2.xml");
            if(profile != null)
                drone.vehicleProfiles.put("ArduCopter2", profile);
        } catch (Exception e) {
            // nop
        }
    }

    private VehicleProfile loadVehicleProfile(Context context, String fileName) throws IOException, XmlPullParserException {
        final String path = VEHICLEPROFILE_PATH + File.separator + fileName;

        final InputStream inputStream;
        final File file = new File(DirectoryPath.getDroidPlannerPath() + path);
        if(file.exists()) {
            // load from file
            inputStream = new FileInputStream(file);
        }
        else {
            // load from resource
            inputStream = context.getAssets().open(path);
        }

        return VehicleProfileReader.open(inputStream);
    }
}
