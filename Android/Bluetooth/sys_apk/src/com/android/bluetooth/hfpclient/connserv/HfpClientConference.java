/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.bluetooth.hfpclient.connserv;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.os.Bundle;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

public class HfpClientConference extends Conference {
    private static final String TAG = "LZLB_HfpClientConference";

    private BluetoothDevice mDevice;
    private BluetoothHeadsetClient mHeadsetProfile;

    public HfpClientConference(PhoneAccountHandle handle,
            BluetoothDevice device, BluetoothHeadsetClient client) {
        super(handle);
        mDevice = device;
        mHeadsetProfile = client;
        boolean manage = HfpClientConnectionService.hasHfpClientEcc(client, device);
        setConnectionCapabilities(Connection.CAPABILITY_SUPPORT_HOLD |
                Connection.CAPABILITY_HOLD |
                (manage ? Connection.CAPABILITY_MANAGE_CONFERENCE : 0));
        setActive();
    }

    @Override
    public void onDisconnect() {
        Log.e(TAG, "onDisconnect");
        mHeadsetProfile.terminateCall(mDevice, 0);
        setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
    }

    @Override
    public void onMerge(Connection connection) {
        Log.e(TAG, "onMerge " + connection);
        addConnection(connection);
    }

    @Override
    public void onSeparate(Connection connection) {
        Log.e(TAG, "onSeparate " + connection);
        ((HfpClientConnection) connection).enterPrivateMode();
        removeConnection(connection);
    }

    @Override
    public void onHold() {
        Log.e(TAG, "onHold");
        mHeadsetProfile.holdCall(mDevice);
    }

    @Override
    public void onUnhold() {
    	Log.e(TAG, "onUnhold");
        if (getPrimaryConnection().getConnectionService()
                .getAllConnections().size() > 1) {
            Log.w(TAG, "Ignoring unhold; call hold on the foreground call");
            return;
        }
        Log.d(TAG, "onUnhold");
        mHeadsetProfile.acceptCall(mDevice, BluetoothHeadsetClient.CALL_ACCEPT_HOLD);
    }

    @Override
    public void onPlayDtmfTone(char c) {
    	
        Log.e(TAG, "onPlayDtmfTone " + c);
        if (mHeadsetProfile != null) {
            mHeadsetProfile.sendDTMF(mDevice, (byte) c);
        }
    }

    @Override
    public void onConnectionAdded(Connection connection) {
        Log.e(TAG, "onConnectionAdded " + connection);
        if (connection.getState() == Connection.STATE_HOLDING &&
                getState() == Connection.STATE_ACTIVE) {
            connection.onAnswer();
        } else if (connection.getState() == Connection.STATE_ACTIVE &&
                getState() == Connection.STATE_HOLDING) {
            mHeadsetProfile.acceptCall(mDevice, BluetoothHeadsetClient.CALL_ACCEPT_NONE);
        }
    }
}
