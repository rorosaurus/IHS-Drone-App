package com.dji.sdk.sample.demo.ihs;

import android.app.Service;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private Button circleBtn;

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
        circleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // find our current location.  we'll center the circle here.
                LocationCoordinate3D droneLocation = flightController.getState().getAircraftLocation();

                // create a mission, using the lat/long of our current location and some other values we choose.
                HotpointMission mission = new HotpointMission(
                        new LocationCoordinate2D(droneLocation.getLatitude(), droneLocation.getLongitude()), // 2D point to circle around
                        5, // altitude in meters (~16ft)
                        5, // radius of circle in meters (~25ft)
                        18, // angular velocity in degrees per second (full rotation in ~20 seconds)
                        false, // move clockwise?
                        HotpointStartPoint.NORTH, // where should the drone start to traverse the circle?
                        HotpointHeading.TOWARDS_HOT_POINT // which way should the drone face while circling?
                );
                // start the mission!
                hotpointMissionOperator.startMission(mission, logCallback);
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
        circleBtn = (Button) findViewById(R.id.ihs_circle_btn);
    }

    @Override
    public int getDescription() {
        return R.string.ihs_page_title;
    }

    //endregion
}

