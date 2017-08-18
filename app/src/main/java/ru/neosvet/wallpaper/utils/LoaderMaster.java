package ru.neosvet.wallpaper.utils;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by NeoSvet on 06.08.2017.
 */

public class LoaderMaster extends AppCompatActivity {
    public interface IService {
        void setAct(LoaderMaster act);
    }

    protected final byte NO_SERVICE = 0, BOUND_SERVICE = 1, UNBOUND_SERVICE = 2;
    protected Intent intSrv = null;
    private ServiceConnection sConn = null;
    protected byte mode_srv = NO_SERVICE;

    protected void restoreActivityState(Bundle state) {
        if (state != null) {
            mode_srv = state.getByte(Lib.MODE);
            connectToLoader();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mode_srv == BOUND_SERVICE) {
            unbindService(sConn);
            mode_srv = UNBOUND_SERVICE;
        }
        outState.putByte(Lib.MODE, mode_srv);
        super.onSaveInstanceState(outState);
    }

    public void onConnect(IService srv) {

    }

    public static class MyBinder extends Binder {
        private IService loader;

        public MyBinder(IService loader) {
            this.loader = loader;
        }

        public IService getService() {
            return loader;
        }
    }

    protected void connectToLoader() {
        if (sConn == null) {
            sConn = new ServiceConnection() {
                public void onServiceConnected(ComponentName name, IBinder service) {
                    MyBinder binder = (MyBinder) service;
                    IService srv = binder.getService();
                    if (srv != null) {
                        onConnect(srv);
                        mode_srv = BOUND_SERVICE;
                    }
                }

                public void onServiceDisconnected(ComponentName name) {
                    mode_srv = UNBOUND_SERVICE;
                }
            };
        }
        if (mode_srv == UNBOUND_SERVICE) {
            bindService(intSrv, sConn, 0);
        }
    }

    protected void startLoader() {
        startService(intSrv);
        mode_srv = UNBOUND_SERVICE;
        connectToLoader();
    }

    public void finishLoader() {
        if (mode_srv == BOUND_SERVICE) {
            unbindService(sConn);
        }
        mode_srv = NO_SERVICE;
    }
}
