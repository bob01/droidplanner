package com.droidplanner.dialogs.mission;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import com.MAVLink.Messages.enums.MAV_ROI;
import com.droidplanner.R;
import com.droidplanner.widgets.SeekBarWithText.SeekBarWithText;
import com.droidplanner.widgets.SeekBarWithText.SeekBarWithText.OnTextSeekBarChangedListner;

public class DialogMissionRoi extends DialogMission implements
		OnTextSeekBarChangedListner {
    private static final int SELECTION_WPNEXT = 0;
    private static final int SELECTION_LOCATION = 1;

    private Spinner roiTypeSpinner;
	private SeekBarWithText altitudeSeekBar;


	@Override
	protected int getResource() {
		return R.layout.dialog_mission_roi;
	}
	
	protected View buildView() {
		super.buildView();

        roiTypeSpinner = (Spinner) view
                .findViewById(R.id.spinnerRoiType);
        roiTypeSpinner.setSelection(selectionFromRoiType(wp.getParam1()));
        roiTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                wp.setParameters(roiTypeFromSelection(position), wp.getParam2(), wp.getParam3(), wp.getParam4());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

		altitudeSeekBar = (SeekBarWithText) view
				.findViewById(R.id.altitudeView);
		altitudeSeekBar.setValue(wp.getHeight());
		altitudeSeekBar.setOnChangedListner(this);

		return view;
	}

	@Override
	public void onSeekBarChanged() {
		wp.setHeight(altitudeSeekBar.getValue());
	}

    private static int selectionFromRoiType(float mavRoi) {
        if (mavRoi == MAV_ROI.MAV_ROI_WPNEXT) {
            return SELECTION_WPNEXT;
        } else if(mavRoi == MAV_ROI.MAV_ROI_LOCATION) {
            return SELECTION_LOCATION;
        } else {
            // default
            return SELECTION_LOCATION;
        }
    }

    private static float roiTypeFromSelection(int selection) {
        if(selection == SELECTION_WPNEXT) {
            return MAV_ROI.MAV_ROI_WPNEXT;
        } else if(selection == SELECTION_LOCATION) {
            return MAV_ROI.MAV_ROI_LOCATION;
        } else {
            // default
            return MAV_ROI.MAV_ROI_LOCATION;
        }
    }

}
