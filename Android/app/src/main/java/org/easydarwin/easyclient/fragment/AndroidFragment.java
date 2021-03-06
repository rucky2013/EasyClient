/*
	Copyright (c) 2012-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/
package org.easydarwin.easyclient.fragment;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.easydarwin.easyclient.MyApplication;
import org.easydarwin.easyclient.R;
import org.easydarwin.easyclient.activity.EasyPlayerActivity;
import org.easydarwin.easyclient.activity.MainActivity;
import org.easydarwin.easyclient.adapter.OnlineCameraAdapter;
import org.easydarwin.easyclient.callback.DeviceInfoCallback;
import org.easydarwin.easyclient.callback.LiveVOCallback;
import org.easydarwin.easyclient.config.DarwinConfig;
import org.easydarwin.easyclient.domain.Device;
import org.easydarwin.easyclient.domain.DeviceHeader;
import org.easydarwin.easyclient.domain.DeviceInfoWrapper;
import org.easydarwin.easyclient.domain.LiveVO;
import org.easydarwin.okhttplibrary.OkHttpUtils;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Request;

/**
 * Created by Kim on 2016/6/16
 */
public class AndroidFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener{
    private static final String TAG="AndroidFragment";
    private Context mContext;
    private String mServerIp;
    private String mServerPort;
    private OnlineCameraAdapter liveVOAdapter;
    private boolean mIsPrepared;/* 标志位，标志已经初始化完成 */

    SwipeRefreshLayout mSwipeRefreshLayout;
    GridView mGrid_live;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_android, container, false);
        mSwipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.swip_refersh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mGrid_live = (GridView)view.findViewById(R.id.grid_live);
        mGrid_live.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        mIsPrepared = true;
        Log.d(TAG, "kim onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "kim onStart");
    }

    @Override
    public void onRefresh() {
        getDevices();
    }

    private void getDevices() {
        mServerIp = MainActivity.getSettingPref().getString(DarwinConfig.SERVER_IP, DarwinConfig.DEFAULT_SERVER_IP);
        mServerPort = MainActivity.getSettingPref().getString(DarwinConfig.SERVER_PORT, DarwinConfig.DEFAULT_SERVER_PORT);
        getDevices(mServerIp, mServerPort);
    }

    /**
     * 获取直播地址列表
     *
     * @param ip   服务器地址
     * @param port 服务器端口号
     */
    private void getDevices(String ip, String port) {
        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            return;
        }

        String url = String.format("http://%s:%s/api/getdevicelist?AppType=EasyCamera&TerminalType=Android", ip, port);
        Log.d(TAG, "camera url="+url);
        OkHttpUtils.post().url(url).build().execute(new LiveVOCallback() {

            @Override
            public void onBefore(Request request) {
                mSwipeRefreshLayout.setRefreshing(false);
                MainActivity.instance.showWaitProgress("");
            }

            @Override
            public void onAfter() {
                MainActivity.instance.hideWaitProgress();
            }

            @Override
            public void onError(Call call, Exception e) {
                Toast.makeText(mContext, "onError:" + e.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(LiveVO liveVO) {
                List<Device> devices = liveVO.getEasyDarwin().getBody().getDevices();
                if (devices.size() == 0) {
                    MainActivity.instance.showToadMessage("暂无直播信息");
                    liveVOAdapter = new OnlineCameraAdapter(new ArrayList<Device>());
                } else {
                    liveVOAdapter = new OnlineCameraAdapter(devices);
                    int screenW = mContext.getResources().getDisplayMetrics().widthPixels;
                    int columnum = mGrid_live.getNumColumns();
                    int space = (int)(mContext.getResources().getDimension(R.dimen.gridview_horizontalspacing));
                    int itemWidth = (int)((screenW-(columnum-1)*space)/columnum);
                    int itemHeight = (int) (itemWidth * 4 / 3.0 + 0.5f);
                    liveVOAdapter.setmSnapshotWidth(itemWidth);
                    liveVOAdapter.setmSnapshotHeight(itemHeight);
                }
                mGrid_live.setAdapter(liveVOAdapter);
            }
        });
    }

    private void getDeviceRtspUrl(String serial){
        String url=String.format("http://%s:%s/api/getdevicestream?device=%s&protocol=RTSP",
                MyApplication.getInstance().getIp(),
                MyApplication.getInstance().getPort(),serial);
        OkHttpUtils.post().url(url).build().execute(new DeviceInfoCallback(){
            @Override
            public void onBefore(Request request) {
                mSwipeRefreshLayout.setRefreshing(false);
                MainActivity.instance.showWaitProgress("");
            }

            @Override
            public void onAfter() {
                MainActivity.instance.hideWaitProgress();
            }
            @Override
            public void onError(Call call, Exception e) {
                Toast.makeText(getContext(), "onError:" + e.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(DeviceInfoWrapper deviceInfoWrapper) {
                if(deviceInfoWrapper.getEasyDarwin().getBody()==null){
                    DeviceHeader header=deviceInfoWrapper.getEasyDarwin().getHeader();
                    Toast.makeText(getContext(), header.getErrorString()+"(" +header.getErrorNum()+")",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(getContext(), EasyPlayerActivity.class);
                intent.putExtra(DarwinConfig.CAM_Serial, deviceInfoWrapper.getEasyDarwin().getBody().getURL());
                startActivity(intent);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Device device = (Device) parent.getAdapter().getItem(position);
        getDeviceRtspUrl(device.getSerial());
    }

    @Override
    protected void lazyLoad() {
        Log.d(TAG, "lazyLoad. mIsPrepared="+mIsPrepared+", isVisible="+isVisible);
        if(mIsPrepared && MainActivity.instance.mStarted){
            getDevices();
        }
    }
}
