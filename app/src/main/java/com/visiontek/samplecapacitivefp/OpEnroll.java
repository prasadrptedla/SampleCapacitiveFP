package com.visiontek.samplecapacitivefp;

import android.widget.Button;

import com.digitalpersona.android.ptapi.PtConnectionI;
import com.digitalpersona.android.ptapi.PtConstants;
import com.digitalpersona.android.ptapi.PtException;
import com.digitalpersona.android.ptapi.callback.PtGuiStateCallback;
import com.digitalpersona.android.ptapi.resultarg.PtBirArg;
import com.digitalpersona.android.ptapi.struct.PtBir;
import com.digitalpersona.android.ptapi.struct.PtGuiSampleImage;
import com.digitalpersona.android.ptapi.struct.PtInputBir;


public abstract class OpEnroll extends Thread {

    private PtConnectionI mConn;
    private int mFingerId;
    private Button thumb;

    public OpEnroll(PtConnectionI Conn,int FingerId){
        super("EnrollmentThread" + FingerId);

        System.out.println("FingerIddd:"+FingerId);
        this.mConn=Conn;
        this.mFingerId=FingerId;

    }

    public OpEnroll(Button left_thumb) {

        this.thumb=left_thumb;
    }


    /**
     * Simple conversion PtBir to PtInputBir
     */
    private static PtInputBir MakeInputBirFromBir(PtBir aBir) {
        PtInputBir aInputBir = new PtInputBir();
        aInputBir.form = PtConstants.PT_FULLBIR_INPUT;
        aInputBir.bir = aBir;
        return aInputBir;
    }

    /**
     * Obtain finger template.
     */
    private PtInputBir enroll() throws PtException {

        PtGuiStateCallback guiCallback = new PtGuiStateCallback() {
            public byte guiStateCallbackInvoke(int guiState, int message,
                                               byte progress, PtGuiSampleImage sampleBuffer, byte[] data)
                    throws PtException {
                String s = PtHelper.GetGuiStateCallbackMessage(guiState,
                        message, progress);

                if (s != null) {
                    onDisplayMessage(s);
                }

                return isInterrupted() ? PtConstants.PT_CANCEL
                        : PtConstants.PT_CONTINUE;
            }
        };

        PtBirArg newTemplate = new PtBirArg();

        try {
            // Register notification callback of operation state
            // Valid for entire PTAPI session lifetime
            mConn.setGUICallbacks(null, guiCallback);

            // Enroll finger, don't store template directly to device but return
            // it to host
            // to allow verification, if finger isn't already enrolled
            mConn.enroll(PtConstants.PT_PURPOSE_ENROLL, null, newTemplate,
                    null, null, PtConstants.PT_BIO_INFINITE_TIMEOUT, null,
                    null, null);
        } catch (PtException e) {
            onDisplayMessage("Enrollment failed - " + e.getMessage());
            throw e;
        }

        // Convert obtained BIR to INPUT BIR class
        return MakeInputBirFromBir(newTemplate.value);
    }


    abstract protected void onDisplayMessage(String message);


}
