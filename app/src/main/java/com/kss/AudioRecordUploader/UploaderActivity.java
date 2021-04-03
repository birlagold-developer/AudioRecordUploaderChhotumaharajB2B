package com.kss.AudioRecordUploader;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.kss.AudioRecordUploader.databinding.ActivityUploaderBinding;
import com.kss.AudioRecordUploader.receiver.CallReceiver;
import com.kss.AudioRecordUploader.utils.Constant;
import com.kss.AudioRecordUploader.utils.SharedPrefrenceObj;

public class UploaderActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "UploaderActivity";

    private ActivityUploaderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tVAgentNumber.setInputType(InputType.TYPE_NULL);
        binding.tVAgentEmail.setInputType(InputType.TYPE_NULL);

        binding.tVAgentNumber.setText(SharedPrefrenceObj.getSharedValue(UploaderActivity.this, Constant.AGENT_NUMBBR));
        binding.tVAgentEmail.setText(SharedPrefrenceObj.getSharedValue(UploaderActivity.this, Constant.AGENT_EMAIL));

        binding.btnUploader.setOnClickListener(this);

        registerReceiver(broadcastReceiver, new IntentFilter("MANUAL_FILE_UPLOAD_COMPLETE"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(UploaderActivity.this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.RECORD_AUDIO, Manifest.permission.PROCESS_OUTGOING_CALLS}, 1000);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnUploader:
                sendBroadcast(
                        new Intent(UploaderActivity.this, CallReceiver.class)
                                .setAction("MANUAL_FILE_UPLOAD")
                );
                binding.ProgressBar.setVisibility(View.VISIBLE);
                binding.btnUploader.setEnabled(false);
                break;
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            binding.ProgressBar.setVisibility(View.GONE);
            binding.btnUploader.setEnabled(true);
            Toast toast = Toast.makeText(UploaderActivity.this, R.string.upload_complete_message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}
