package com.yangs.dji.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.TextureView;

import com.yangs.dji.AppApplication;
import com.yangs.dji.R;

import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

/**
 * Created by yangs on 2017/5/18 0018.
 */

public class RealtimeVideo extends Activity implements TextureView.SurfaceTextureListener {
    private TextureView frame;
    private DJICodecManager djiCodecManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.realtimevideo);
        IntentFilter filter = new IntentFilter();
        filter.addAction(AppApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(receiver, filter);
        frame = (TextureView) this.findViewById(R.id.realtime_frame);
        frame.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (djiCodecManager == null) {
            djiCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (djiCodecManager != null) {
            djiCodecManager.cleanSurface();
            djiCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BaseProduct product = AppApplication.getProduct();
            if (product != null) {
                if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                    if (VideoFeeder.getInstance().getVideoFeeds() != null && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                        VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(mReceivedVideoDataCallBack);
                    } else {
                        AppApplication.showDialog("图传模块出现了问题!", RealtimeVideo.this);
                    }
                } else {
                    AppApplication.showDialog("不支持该设备!", RealtimeVideo.this);
                }
            } else {
                AppApplication.showDialog("异常，没有连接成功!", RealtimeVideo.this);
            }
        }
    };
    private VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
        @Override
        public void onReceive(byte[] bytes, int i) {
            if (djiCodecManager != null) {
                djiCodecManager.sendDataToDecoder(bytes, i);
            }
        }
    };
}
