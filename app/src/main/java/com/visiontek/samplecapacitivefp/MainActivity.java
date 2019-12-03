package com.visiontek.samplecapacitivefp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.digitalpersona.android.ptapi.PtConnectionAdvancedI;
import com.digitalpersona.android.ptapi.PtConstants;
import com.digitalpersona.android.ptapi.PtException;
import com.digitalpersona.android.ptapi.PtGlobal;
import com.digitalpersona.android.ptapi.struct.PtInfo;
import com.digitalpersona.android.ptapi.struct.PtSessionCfgV5;
import com.digitalpersona.android.ptapi.usb.PtUsbHost;

public class MainActivity extends Activity {

    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.java.ptapi.dpfpddusbhost.USB_PERMISSION";

    Button left_thumb;
    private  Thread mRunningOp = null;
    private PtConnectionAdvancedI mConn = null;
    private final Object mCond = new Object();
    private PtGlobal mPtGlobal = null;
    private PtInfo mSensorInfo = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (initializePtapi()) {

            Context applContext = getApplicationContext();
            PendingIntent mPermissionIntent;

            mPermissionIntent = PendingIntent.getBroadcast(applContext, 0,
                    new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            applContext.registerReceiver(mUsbReceiver, filter);


            try {
                if (PtUsbHost.PtUsbCheckAndRequestPermissions(applContext,
                        mPermissionIntent)) {
                    openPtapiSession();
                    setEnrollButtonListener();
                }//
            } catch (PtException e) {
                e.printStackTrace();
            }


            //openPtapiSession();
            //setEnrollButtonListener();

        }
    }

    private void  setEnrollButtonListener()
    {
        left_thumb=(Button)findViewById(R.id.left_thumb);

        left_thumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(MainActivity.this, "Button clicked to enroll", Toast.LENGTH_SHORT).show();

                synchronized (mCond) {
                    if (mRunningOp == null) {

                        mRunningOp= new OpEnroll(left_thumb) {

                            @Override
                            protected void onDisplayMessage(String message) {
                                dislayMessage(message);
                            }
                           /* @Override
                            protected void onFinished() {
                                synchronized (mCond) {
                                    mRunningOp = null;
                                    mCond.notifyAll(); // notify onDestroy that
                                    // operation has
                                    // finished
                                }
                            }*/
                        };
                        mRunningOp.start();

                    }

                }
            }
        });


    }
    private boolean initializePtapi() {

        //	System.out.println("In Initialze Ptapi");

        // Load PTAPI library
        Context aContext = getApplicationContext();
        mPtGlobal = new PtGlobal(aContext);

        try {
            // Initialize PTAPI interface
            mPtGlobal.initialize();
            return true;
        } catch (UnsatisfiedLinkError ule) {
            // Library wasn't loaded properly during PtGlobal object
            // construction
            dislayMessage("libjniPtapi.so not loaded");
            mPtGlobal = null;
            return false;

        } catch (PtException e) {
            //dislayMessage(e.getMessage());
            return true;
        }
    }

    private void dislayMessage(String message) {

        mHandler.sendMessage(mHandler.obtainMessage(0, 0, 0, message));
    }
    private Handler mHandler = new Handler() {
        public void handleMessage(Message aMsg) {
            ((TextView) findViewById(R.id.EnrollmentTextView))
                    .setText((String) aMsg.obj);
        }
    };

    private void configureOpenedDevice() throws PtException {

        	System.out.println("In Configure Devices");
        PtSessionCfgV5 sessionCfg = (PtSessionCfgV5) mConn
                .getSessionCfgEx(PtConstants.PT_CURRENT_SESSION_CFG);
        sessionCfg.callbackLevel |= PtConstants.CALLBACKSBIT_NO_REPEATING_MSG;
        mConn.setSessionCfgEx(PtConstants.PT_CURRENT_SESSION_CFG, sessionCfg);
    }


    private void openPtapiSessionInternal() throws PtException {
        // Try to open device
        try {

            System.out.println("In ptapi Internal Definition");
            mConn = (PtConnectionAdvancedI) mPtGlobal.open("USB");
            mSensorInfo = mConn.info();

        } catch (PtException e) {
            throw e;
        }

        configureOpenedDevice();
    }



    private void openPtapiSession() {
        try {
            // Try to open session

            System.out.println("Before ptapi Internal");
            openPtapiSessionInternal();

            System.out.println("After ptapi Internal API");

            // Device successfully opened
            return;
        } catch (PtException e) {
            dislayMessage("Error during device opening - " + e.getMessage());
        }
    }

    private void closeSession() {


        //	System.out.println("In Closse Session");
        if (mConn != null) {
            try {
                mConn.close();
            } catch (PtException e) {
                // Ignore errors
            }
            mConn = null;
        }
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

    public void onReceive(Context context, Intent intent) {

        //		System.out.println("In BroadCast Receiver");
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                UsbDevice device = (UsbDevice) intent
                        .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(
                        UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        openPtapiSession();
                        setEnrollButtonListener();
                    }
                } else {
                    System.exit(0);
                }
            }
        }
    }
};


}
