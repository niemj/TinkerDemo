package com.tinker.deeson.mytinkerdemo;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.debug.hv.ViewServer;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewServer.get(this).addWindow(this);
        Log.d("json", "onCreate: " + Utils.isDeviceRooted());
        Log.d("json", "UDID:" + Utils.getHardDevice());

        Button btnkill = (Button) findViewById(R.id.btn_kill);
        Button btnload = (Button) findViewById(R.id.btn_load);
        TextView tvtitle = (TextView) findViewById(R.id.tv_title);

        btnload.setOnClickListener(this);
        btnkill.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setBackground(false);
        ViewServer.get(this).setFocusedWindow(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.setBackground(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_load:
                loadPatch();
                break;
            case R.id.btn_kill:
                killApp();
                break;
        }
    }

    /**
     * 加载热补丁插件
     */
    private void loadPatch() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/myTinkerDemo/";
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        } else {
            Toast.makeText(this, "该路径已存在！", Toast.LENGTH_SHORT).show();
        }
        TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(),
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/myTinkerDemo/TinkerPatch");
        Log.d("patch", "path:" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/myTinkerDemo/TinkerPatch");
    }

    /**
     * 杀死应用加载补丁
     */
    private void killApp() {
        ShareTinkerInternals.killAllOtherProcess(getApplicationContext());
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ViewServer.get(this).removeWindow(this);
    }
}
