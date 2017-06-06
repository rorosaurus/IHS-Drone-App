package com.dji.sdk.sample.demo.ihs;

import android.app.Service;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.PresentableView;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.GetCallback;
import dji.keysdk.callback.SetCallback;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.products.Aircraft;

/**
 * Class for basic manager view in mission manager
 */
public class IHSView extends LinearLayout implements PresentableView {

    private Button takeOffBtn;
    private Button landBtn;
    private Button returnHomeBtn;
    private ToggleButton circleBtn;

    private TextView circleText;
    private SeekBar circleSeekBar;

    private final int circleRadius = 5;
    private int maxVelocity = (int) HotpointMissionOperator.maxAngularVelocityForRadius(circleRadius);

    private FlightController flightController;
    private HotpointMissionOperator hotpointMissionOperator;

    // we'll use this basic callback for anything which might throw and error, so we can log it to examine later
    private CommonCallbacks.CompletionCallback logCallback = new CommonCallbacks.CompletionCallback() {
        @Override
        public void onResult(DJIError djiError) {
            if (djiError != null) Log.e("IHS Error", djiError.toString());
        }
    };

    public IHSView(Context context) {
        super(context);
        initUI(context);
    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // initialize our SDK controller and operator objects
        flightController = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();
        hotpointMissionOperator = MissionControl.getInstance().getHotpointMissionOperator();
        setUpListeners();
        configureSettings();
    }

    private void configureSettings(){
        // ENABLE PRECISION LANDING FOR MOST ACCURATE RETURN TO HOME
        flightController.getFlightAssistant().setPrecisionLandingEnabled(true, logCallback);
        // ADD LISTENER TO AUTO-COMPLETE LANDING SEQUENCES
        FlightControllerKey landingConf = FlightControllerKey.create(FlightControllerKey.IS_LANDING_CONFIRMATION_NEEDED);
        KeyManager.getInstance().getValue(landingConf, new GetCallback() {
                    @Override
                    public void onSuccess(final @NonNull Object o) {
                        if (o instanceof Boolean && (Boolean) o) {
                            flightController.confirmLanding(logCallback);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull DJIError djiError) {
                        Log.e("IHS Error", djiError.toString());
                    }
                }
        );
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void setUpListeners() {

        // this listener is triggered when the "take off" button is clicked
        takeOffBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // tell the flight controller to take off!
                flightController.startTakeoff(logCallback);
            }
        });

        // this listener is triggered when the "land" button is clicked
        landBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // tell the flight controller to start landing!
                flightController.startLanding(logCallback);
            }
        });

        // this listener is triggered when the "return home" button is clicked
        returnHomeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // tell the flight controller to start going home!
                flightController.startGoHome(logCallback);
            }
        });

        // this listener is triggered when the "circle" button is clicked
        circleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean startCircling) {
                if (startCircling) { // we just tapped start circle

                    // find our current location.  we'll center the circle here.
                    LocationCoordinate3D droneLocation = flightController.getState().getAircraftLocation();

                    // determine if we want to go clockwise or not
                    boolean clockwise = getAngVelocity() < 0;

                    // create a mission, using the lat/long of our current location and some other values we choose.
                    HotpointMission mission = new HotpointMission(
                            new LocationCoordinate2D(droneLocation.getLatitude(), droneLocation.getLongitude()), // 2D point to circle around
                            5, // altitude in meters (~16ft)
                            circleRadius, // radius of circle in meters (~16ft)
                            18, // angular velocity in degrees per second (full rotation in ~20 seconds)
                            clockwise, // move clockwise?
                            HotpointStartPoint.NORTH, // where should the drone start to traverse the circle?
                            HotpointHeading.TOWARDS_HOT_POINT // which way should the drone face while circling?
                    );
                    // start the mission!
                    hotpointMissionOperator.startMission(mission, logCallback);
                }
                else { // we just tapped stop circle
                    hotpointMissionOperator.stop(logCallback);
                }
            }
        });

        // ADJUST CIRCLE VELOCITY
        circleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if(fromUser){
                    // update the text bar
                    setSeekerBarText();

                    // update the seekerbar and the current mission
                    hotpointMissionOperator.setAngularVelocity(getAngVelocity(), logCallback);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // unused
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // unused
            }
        });
    }

    //region Helper Method
    private void initUI(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);

        layoutInflater.inflate(R.layout.view_ihs, this, true);

        // obtain a reference to the buttons we defined in the view .xml
        takeOffBtn = (Button) findViewById(R.id.ihs_take_off_btn);
        landBtn = (Button) findViewById(R.id.ihs_land_btn);
        returnHomeBtn = (Button) findViewById(R.id.ihs_return_home_btn);

        circleBtn = (ToggleButton) findViewById(R.id.ihs_circle_btn);
        circleText = (TextView) findViewById(R.id.seekerText_title);
        circleSeekBar = (SeekBar) findViewById(R.id.seekBar);
        circleSeekBar.setMax(maxVelocity*2); // twice as long so we can do pos and negative
        circleSeekBar.setProgress((maxVelocity*3)/2);  // default angular velocity is half max
        setSeekerBarText();
    }

    private int getAngVelocity() {
        // calculate the expected velocity
        return circleSeekBar.getProgress() - maxVelocity;
    }

    private void setSeekerBarText(){
        // calculate the expected velocity
        int angVelocity = getAngVelocity();

        // update the text
        if(angVelocity == 0){
            setText(circleText, "Circle Velocity: 0°/sec\n(infinity secs for full rotation)");
        }
        else {
            setText(circleText, "Circle Velocity: "+ (angVelocity) + "°/sec"
                    + "\n(" + 360/(Math.abs(angVelocity)) + " secs for full rotation)");
        }
    }

    private void setText(final TextView tv, final String text) {
        tv.post(new Runnable() {
            @Override
            public void run() {

                tv.setText(text);
            }
        });
    }

    @Override
    public int getDescription() {
        return R.string.ihs_page_title;
    }

    //endregion
}

