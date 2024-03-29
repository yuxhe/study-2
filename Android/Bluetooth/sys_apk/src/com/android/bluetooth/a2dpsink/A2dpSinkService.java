/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bluetooth.a2dpsink;

import android.bluetooth.BluetoothAudioConfig;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothA2dpSink;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import com.android.bluetooth.a2dpsink.mbs.A2dpMediaBrowserService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides Bluetooth A2DP Sink profile, as a service in the Bluetooth application.
 * @hide
 */
public class A2dpSinkService extends ProfileService {
    private static final boolean DBG = true;
    private static final String TAG = "LZLB_A2dpSinkService";

    private A2dpSinkStateMachine mStateMachine;
    private static A2dpSinkService sA2dpSinkService;

    protected String getName() {
		Log.e(TAG,"getName");
        return TAG;
    }

    protected IProfileServiceBinder initBinder() {
    Log.e(TAG,"initBinder");
        return new BluetoothA2dpSinkBinder(this);
    }

    protected boolean start() {
    Log.e(TAG,"start");
        if (DBG) {
            Log.d(TAG, "start()");
        }
        // Start the media browser service.
        Intent startIntent = new Intent(this, A2dpMediaBrowserService.class);
        startService(startIntent);
        mStateMachine = A2dpSinkStateMachine.make(this, this);
        setA2dpSinkService(this);
        return true;
    }

    protected boolean stop() {
    Log.e(TAG,"stop");
        if (DBG) {
            Log.d(TAG, "stop()");
        }
        mStateMachine.doQuit();
        Intent stopIntent = new Intent(this, A2dpMediaBrowserService.class);
        stopService(stopIntent);
        return true;
    }

    protected boolean cleanup() {
    Log.e(TAG,"cleanup");
        if (mStateMachine!= null) {
            mStateMachine.cleanup();
        }
        clearA2dpSinkService();
        return true;
    }

    //API Methods

    public static synchronized A2dpSinkService getA2dpSinkService(){
    Log.e(TAG,"getA2dpSinkService");
        if (sA2dpSinkService != null && sA2dpSinkService.isAvailable()) {
            if (DBG) Log.d(TAG, "getA2dpSinkService(): returning " + sA2dpSinkService);
            return sA2dpSinkService;
        }
        if (DBG)  {
            if (sA2dpSinkService == null) {
                Log.d(TAG, "getA2dpSinkService(): service is NULL");
            } else if (!(sA2dpSinkService.isAvailable())) {
                Log.d(TAG,"getA2dpSinkService(): service is not available");
            }
        }
        return null;
    }

    private static synchronized void setA2dpSinkService(A2dpSinkService instance) {
    Log.e(TAG,"setA2dpSinkService");
        if (instance != null && instance.isAvailable()) {
            if (DBG) Log.d(TAG, "setA2dpSinkService(): set to: " + sA2dpSinkService);
            sA2dpSinkService = instance;
        } else {
            if (DBG)  {
                if (sA2dpSinkService == null) {
                    Log.d(TAG, "setA2dpSinkService(): service not available");
                } else if (!sA2dpSinkService.isAvailable()) {
                    Log.d(TAG,"setA2dpSinkService(): service is cleaning up");
                }
            }
        }
    }

    private static synchronized void clearA2dpSinkService() {
    Log.e(TAG,"clearA2dpSinkService");
        sA2dpSinkService = null;
    }

    public boolean connect(BluetoothDevice device) {
    Log.e(TAG,"connect");
        enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                       "Need BLUETOOTH ADMIN permission");

        int connectionState = mStateMachine.getConnectionState(device);
        if (connectionState == BluetoothProfile.STATE_CONNECTED ||
            connectionState == BluetoothProfile.STATE_CONNECTING) {
            return false;
        }

        mStateMachine.sendMessage(A2dpSinkStateMachine.CONNECT, device);
        return true;
    }

    boolean disconnect(BluetoothDevice device) {
    Log.e(TAG,"disconnect");
        enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                       "Need BLUETOOTH ADMIN permission");
        int connectionState = mStateMachine.getConnectionState(device);
        if (connectionState != BluetoothProfile.STATE_CONNECTED &&
            connectionState != BluetoothProfile.STATE_CONNECTING) {
            return false;
        }

        mStateMachine.sendMessage(A2dpSinkStateMachine.DISCONNECT, device);
        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {
    Log.e(TAG,"getConnectedDevices");
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return mStateMachine.getConnectedDevices();
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
    	Log.e(TAG,"getDevicesMatchingConnectionStates");
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return mStateMachine.getDevicesMatchingConnectionStates(states);
    }

    int getConnectionState(BluetoothDevice device) {
    Log.e(TAG,"getConnectionState");
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return mStateMachine.getConnectionState(device);
    }

    public boolean setPriority(BluetoothDevice device, int priority) {
    	Log.e(TAG,"setPriority");
        enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                       "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(),
            Settings.Global.getBluetoothA2dpSrcPriorityKey(device.getAddress()),
            priority);
        if (DBG) {
            Log.d(TAG,"Saved priority " + device + " = " + priority);
        }
        return true;
    }

    public int getPriority(BluetoothDevice device) {
    	Log.e(TAG,"getPriority");
        enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                       "Need BLUETOOTH_ADMIN permission");
        int priority = Settings.Global.getInt(getContentResolver(),
            Settings.Global.getBluetoothA2dpSrcPriorityKey(device.getAddress()),
            BluetoothProfile.PRIORITY_UNDEFINED);
        return priority;
    }

    /**
     * Called by AVRCP controller to provide information about the last user intent on CT.
     *
     * If the user has pressed play in the last attempt then A2DP Sink component will grant focus to
     * any incoming sound from the phone (and also retain focus for a few seconds before
     * relinquishing. On the other hand if the user has pressed pause/stop then the A2DP sink
     * component will take the focus away but also notify the stack to throw away incoming data.
     */
    public void informAvrcpPassThroughCmd(BluetoothDevice device, int keyCode, int keyState) {
    Log.e(TAG,"informAvrcpPassThroughCmd");
        if (mStateMachine != null) {
            if (keyCode == BluetoothAvrcpController.PASS_THRU_CMD_ID_PLAY &&
                keyState == BluetoothAvrcpController.KEY_STATE_RELEASED) {
                mStateMachine.sendMessage(A2dpSinkStateMachine.EVENT_AVRCP_CT_PLAY);
            } else if ((keyCode == BluetoothAvrcpController.PASS_THRU_CMD_ID_PAUSE ||
                       keyCode == BluetoothAvrcpController.PASS_THRU_CMD_ID_STOP) &&
                       keyState == BluetoothAvrcpController.KEY_STATE_RELEASED) {
                mStateMachine.sendMessage(A2dpSinkStateMachine.EVENT_AVRCP_CT_PAUSE);
            }
        }
    }

    /**
     * Called by AVRCP controller to provide information about the last user intent on TG.
     *
     * Tf the user has pressed pause on the TG then we can preempt streaming music. This is opposed
     * to when the streaming stops abruptly (jitter) in which case we will wait for sometime before
     * stopping playback.
     */
    public void informTGStatePlaying(BluetoothDevice device, boolean isPlaying) {
    Log.e(TAG,"informTGStatePlaying");
        if (mStateMachine != null) {
            if (!isPlaying) {
                mStateMachine.sendMessage(A2dpSinkStateMachine.EVENT_AVRCP_TG_PAUSE);
            } else {
                mStateMachine.sendMessage(A2dpSinkStateMachine.EVENT_AVRCP_TG_PLAY);
            }
        }
    }

    synchronized boolean isA2dpPlaying(BluetoothDevice device) {
    Log.e(TAG,"isA2dpPlaying");
        enforceCallingOrSelfPermission(BLUETOOTH_PERM,
                                       "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "isA2dpPlaying(" + device + ")");
        }
        return mStateMachine.isPlaying(device);
    }

    BluetoothAudioConfig getAudioConfig(BluetoothDevice device) {
    Log.e(TAG,"getAudioConfig");
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return mStateMachine.getAudioConfig(device);
    }

    //Binder object: Must be static class or memory leak may occur
    private static class BluetoothA2dpSinkBinder extends IBluetoothA2dpSink.Stub
        implements IProfileServiceBinder {
        private static final String TAG ="LZLB_BluetoothA2dpSinkBinder";
        private A2dpSinkService mService;

        private A2dpSinkService getService() {
			Log.e(TAG,"getService");
            if (!Utils.checkCaller()) {
                Log.w(TAG,"A2dp call not allowed for non-active user");
                return null;
            }

            if (mService != null && mService.isAvailable()) {
                return mService;
            }
            return null;
        }

        BluetoothA2dpSinkBinder(A2dpSinkService svc) {
        Log.e(TAG,"BluetoothA2dpSinkBinder");
            mService = svc;
        }

        public boolean cleanup()  {
        Log.e(TAG,"cleanup");
            mService = null;
            return true;
        }

        public boolean connect(BluetoothDevice device) {
        Log.e(TAG,"connect");
            A2dpSinkService service = getService();
            if (service == null) return false;
            return service.connect(device);
        }

        public boolean disconnect(BluetoothDevice device) {
        Log.e(TAG,"disconnect");
            A2dpSinkService service = getService();
            if (service == null) return false;
            return service.disconnect(device);
        }

        public List<BluetoothDevice> getConnectedDevices() {
        Log.e(TAG,"getConnectedDevices");
            A2dpSinkService service = getService();
            if (service == null) return new ArrayList<BluetoothDevice>(0);
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        Log.e(TAG,"getDevicesMatchingConnectionStates");
            A2dpSinkService service = getService();
            if (service == null) return new ArrayList<BluetoothDevice>(0);
            return service.getDevicesMatchingConnectionStates(states);
        }

        public int getConnectionState(BluetoothDevice device) {
        Log.e(TAG,"getConnectionState");
            A2dpSinkService service = getService();
            if (service == null) return BluetoothProfile.STATE_DISCONNECTED;
            return service.getConnectionState(device);
        }

        public boolean isA2dpPlaying(BluetoothDevice device) {
        Log.e(TAG,"isA2dpPlaying");
            A2dpSinkService service = getService();
            if (service == null) return false;
            return service.isA2dpPlaying(device);
        }

        public boolean setPriority(BluetoothDevice device, int priority) {
        Log.e(TAG,"setPriority");
            A2dpSinkService service = getService();
            if (service == null) return false;
            return service.setPriority(device, priority);
        }

        public int getPriority(BluetoothDevice device) {
        Log.e(TAG,"getPriority");
            A2dpSinkService service = getService();
            if (service == null) return BluetoothProfile.PRIORITY_UNDEFINED;
            return service.getPriority(device);
        }

        public BluetoothAudioConfig getAudioConfig(BluetoothDevice device) {
        Log.e(TAG,"getAudioConfig");
            A2dpSinkService service = getService();
            if (service == null) return null;
            return service.getAudioConfig(device);
        }
    };

    @Override
    public void dump(StringBuilder sb) {
    Log.e(TAG,"dump");
        super.dump(sb);
        if (mStateMachine != null) {
            mStateMachine.dump(sb);
        }
    }
}
