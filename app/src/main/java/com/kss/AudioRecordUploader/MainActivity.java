package com.kss.AudioRecordUploader;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.kss.AudioRecordUploader.databinding.ActivityMainBinding;
import com.kss.AudioRecordUploader.utils.Constant;
import com.kss.AudioRecordUploader.utils.SharedPrefrenceObj;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }

        if (!TextUtils.isEmpty(SharedPrefrenceObj.getSharedValue(MainActivity.this, Constant.AGENT_NUMBBR))) {
            callNextActivity();
            return;
        }

        binding.btnSubmit.setOnClickListener(this);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSubmit:
                String number = binding.tVAgentNumber.getText().toString().replace("'", "");
                String email = binding.tVAgentEmail.getText().toString().replace("'", "");

                if (TextUtils.isEmpty(number) || TextUtils.isEmpty(email)) {
                    Toast.makeText(MainActivity.this, R.string.mobile_email_error_message, Toast.LENGTH_LONG).show();
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(MainActivity.this, R.string.email_error_message, Toast.LENGTH_LONG).show();
                } else {
                    SharedPrefrenceObj.setSharedValue(MainActivity.this, Constant.AGENT_NUMBBR, "+91" + number);
                    SharedPrefrenceObj.setSharedValue(MainActivity.this, Constant.AGENT_EMAIL, email);
                    callNextActivity();
                }
                break;
        }
    }

    private void callNextActivity() {
        Intent intent = new Intent(MainActivity.this, ContactActivity.class);
        startActivity(intent);
        finish();
    }

}
