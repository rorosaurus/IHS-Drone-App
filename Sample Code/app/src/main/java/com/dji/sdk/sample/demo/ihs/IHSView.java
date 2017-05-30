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
import dji.common.util.CommonCallbacks;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.GetCallback;
import dji.keysdk.callback.SetCallback;
import dji.sdk.flightcontroller.FlightController;
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
        flightController = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();
        setUpListeners();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void setUpListeners() {
        takeOffBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.startTakeoff(logCallback);
            }
        });
        landBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.startLanding(logCallback);
            }
        });
        returnHomeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                flightController.startGoHome(logCallback);
            }
        });
        circleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // here's where we do our stuff!
            }
        });
    }

    //region Helper Method
    private void initUI(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);

        layoutInflater.inflate(R.layout.view_ihs, this, true);

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

