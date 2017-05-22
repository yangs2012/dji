package com.yangs.dji;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

import static android.support.v7.appcompat.R.styleable.AlertDialog;

/**
 * Created by winutalk on 2017/4/16.
 */

public class AppApplication extends Application {
    private static BaseProduct mbaseProduct;
    private static Context context;
    public static final String FLAG_CONNECTION_CHANGE = "dji_connectiom_change";

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        DJISDKManager.getInstance().registerApp(this, sdkManagerCallback);

    }

    public static synchronized BaseProduct getProduct() {
        if (mbaseProduct != null)
            return DJISDKManager.getInstance().getProduct();
        else
            return null;
    }

    private DJISDKManager.SDKManagerCallback sdkManagerCallback = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(final DJIError djiError) {
            final Handler handler = new Handler(Looper.getMainLooper());
            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        AppApplication.showToast("注册设备成功!");
                        DJISDKManager.getInstance().startConnectionToProduct();
                    }
                });
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        AppApplication.showToast(djiError.toString());
                    }
                });
            }
        }

        @Override
        public void onProductChange(BaseProduct baseProduct, BaseProduct baseProduct1) {
            if (baseProduct1 != null) {
                mbaseProduct = baseProduct1;
                Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
                sendBroadcast(intent);
            }
        }
    };

    public static void showToast(final String msg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showDialog(final String msg, final Context context) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context).setTitle("提示").setMessage(msg).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
            }
        });
    }
}
