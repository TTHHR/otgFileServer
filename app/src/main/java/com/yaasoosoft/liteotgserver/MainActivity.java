package com.yaasoosoft.liteotgserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.yaasoosoft.liteotgserver.utils.ServerUtil;

public class MainActivity extends AppCompatActivity {
    private Switch workSwitch;
    private final String TAG="main";
    private boolean hasOtgPremission=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        workSwitch=findViewById(R.id.workSwitch);
        Button setButton=findViewById(R.id.settingButton);
        setButton.setOnClickListener(v->{
            startActivity(new Intent(this,SettingsActivity.class));
        });

        workSwitch.setOnCheckedChangeListener((v,val)->{
            if(val&&hasOtgPremission&&checkOTG())
            {
                SharedPreferences pre = PreferenceManager.getDefaultSharedPreferences(this);
                String path=pre.getString("rootPath","/sdcard");
                Log.e(TAG,"rootPath "+path);
                int port=Integer.parseInt(pre.getString("port","8080"));
                try {
                    ServerUtil.getIns().start(port,path);
                } catch (Exception e) {
                    Log.e(TAG,e.toString());

                }
            }
            else
            {
                ServerUtil.getIns().stop();
                workSwitch.setChecked(false);
            }

        });
        requestPermission();
    }
    private boolean checkOTG()
    {
        return true;
    }
    private void requestPermission()
    {
        int permission_write=ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission_read= ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if(permission_write!= PackageManager.PERMISSION_GRANTED
                || permission_read!=PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "软件需要OTG的读写权限", Toast.LENGTH_SHORT).show();
            //申请权限，特征码自定义为1，可在回调时进行相关判断
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }
        else
        {
            hasOtgPremission=true;
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //权限已成功申请
                    hasOtgPremission=true;
                }else{
                    //用户拒绝授权
                    Toast.makeText(this, "当前软件没有权限，无法工作", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}