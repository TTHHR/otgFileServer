package com.yaasoosoft.liteotgserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.partition.Partition;


public class MainActivity extends AppCompatActivity implements MainInterface{
    private Switch workSwitch;
    private final String TAG="main";
    private boolean hasOtgPremission=false;
    private PendingIntent mPendingIntent;
    UsbMassStorageDevice[] storageDevices;
    private UsbMassStorageDevice mUsbDevice;
    TextView logView;
    ServerPresenter serverPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        workSwitch=findViewById(R.id.workSwitch);
        Button setButton=findViewById(R.id.settingButton);
        setButton.setOnClickListener(v->{
            startActivity(new Intent(this,SettingsActivity.class));
        });
        logView=findViewById(R.id.logView);
        serverPresenter=new ServerPresenter(this);
        workSwitch.setOnCheckedChangeListener((v,val)->{
            if(val&&hasOtgPremission)
            {

                SharedPreferences pre = PreferenceManager.getDefaultSharedPreferences(this);
                String path=pre.getString("rootPath","/");
                log("rootPath "+path);
                int port=Integer.parseInt(pre.getString("port","8080"));
                serverPresenter.setPort(port);
                serverPresenter.setRootPath(path);
                serverPresenter.start();

            }
            else
            {
                serverPresenter.stop();
            }

        });
        requestPermission();
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
    public boolean readDevice() {
        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
        log("开始去读Otg设备");
        storageDevices = UsbMassStorageDevice.getMassStorageDevices(this);
        mPendingIntent = PendingIntent.getBroadcast(this,0,new Intent("com.android.usb.USB_PERMISSION"),PendingIntent.FLAG_IMMUTABLE);
        if (storageDevices.length == 0) {
            log("没有检测到U盘s");
            return false;
        }
        for (UsbMassStorageDevice device : storageDevices){
            if (usbManager.hasPermission(device.getUsbDevice())){
                log("检测到有权限，延迟1秒开始读取....");
                try {
                    Thread.sleep(1000 );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mUsbDevice=device;
                readDevice(device);
                return true;
            }else {
                log("检测到有设备，但是没有权限，申请权限....");
                usbManager.requestPermission(device.getUsbDevice(),mPendingIntent);
                return false;
            }
        }
        return false;
    }
    private void readDevice(UsbMassStorageDevice device) {
        try {
            device.init();
            Partition partition = device.getPartitions().get(0);
            FileSystem currentFs = partition.getFileSystem();
            Log.i(TAG,"------------FileSystem---------");
            UsbFile root = currentFs.getRootDirectory();
            String deviceName = currentFs.getVolumeLabel();
            log("正在读取U盘" + deviceName +" "+root.getAbsolutePath());
            serverPresenter.setRootFile(root);
        } catch (Exception e) {
            e.printStackTrace();
            log("读取失败:"+e.getMessage());
        }finally {
        }
    }

    public void log(String string)
    {
        runOnUiThread(()->{
            logView.append(string);
            logView.append("\n");
        });
    }

    @Override
    public void setRunState(boolean v) {
        runOnUiThread(()->{
            workSwitch.setChecked(v);
        });
    }

    @Override
    protected void onDestroy() {
        for (UsbMassStorageDevice s:storageDevices){
            if (s== mUsbDevice) {
                s.getPartitions().stream().close();
            }
        }
        mUsbDevice.close();
        serverPresenter.stop();
        super.onDestroy();
    }
}