package com.kss.AudioRecordUploader.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;


import androidx.room.Room;

import com.kss.AudioRecordUploader.database.DataBaseAdapter;
import com.kss.AudioRecordUploader.model.Data;
import com.kss.AudioRecordUploader.model.MissedCallLog;
import com.kss.AudioRecordUploader.network.retrofit.RFInterface;
import com.kss.AudioRecordUploader.network.retrofit.responsemodels.RmResultResponse;
import com.kss.AudioRecordUploader.network.retrofit.responsemodels.RmUploadFileResponse;
import com.kss.AudioRecordUploader.utils.Constant;
import com.kss.AudioRecordUploader.utils.MyDatabase;
import com.kss.AudioRecordUploader.utils.Networkstate;
import com.kss.AudioRecordUploader.utils.SharedPrefrenceObj;
import com.kss.AudioRecordUploader.utils.Todo;
import com.kss.AudioRecordUploader.utils.Utility;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";

    private Context context;

    static boolean RINGING = false;

    public static String callerPhoneNumber="";


    private MediaRecorder rec = null;
    public static boolean recoderstarted = false;

    ArrayList<Todo> todoArrayList = new ArrayList<>();
    MyDatabase myDatabase;

    public static String callerStartTime="";
    public static String callerEndTime="";


    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        TelephonyManager manager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        manager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                Log.e(TAG,"incomingNumber===>"+incomingNumber);
                if(!incomingNumber.equalsIgnoreCase("")&&incomingNumber!=null)
                {
                    callerPhoneNumber = ""+incomingNumber;
                }

                if (TelephonyManager.CALL_STATE_IDLE == state ){
                    try {
                        Log.e(TAG,"CALL_STATE_IDLE===>");
                        Log.e(TAG,"callerPhoneNumber===>"+callerPhoneNumber);
                        if(recoderstarted && !callerPhoneNumber.equalsIgnoreCase(""))
                        {
                            Log.e(TAG,"CALL_STATE_IDLE===>recoderstarted");
                            Thread.sleep(2000);
                            recoderstarted = false;

                            Date date = new Date();
                            String stringTime = ""+ DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(date);
                            callerEndTime = stringTime;

                            Log.e(TAG,"callerEndTime===>"+callerEndTime);

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/M/yyyy hh:mm:ss");

                            Date date1 = simpleDateFormat.parse(""+callerStartTime);
                            Date date2 = simpleDateFormat.parse(""+callerEndTime);

                            Log.e(TAG,"callerStartTime===>After"+DateFormat.getDateTimeInstance().format(date1));
                            Log.e(TAG,"callerEndTime===>After"+DateFormat.getDateTimeInstance().format(date2));

                            String dateDiff = ""+printDifference(date1, date2);

                            myDatabase = Room.databaseBuilder(context, MyDatabase.class, MyDatabase.DB_NAME).fallbackToDestructiveMigration().build();

                            Todo todo = new Todo();
                            todo.name = ""+callerPhoneNumber;
                            todo.description = ""+callerStartTime;
                            todo.category = ""+dateDiff;

                            todoArrayList.add(todo);
                            insertList(todoArrayList);

                            if (Networkstate.haveNetworkConnection(context)) {
                                checkFolderAndUploadFile();
                            }


                            Log.e(TAG,"CALL_STATE_IDLE===>Last");
                        }
                    }
                    catch(Exception e) {
                        Log.e("Exception","CALL_STATE_IDLE===>"+e.getMessage());
                        e.printStackTrace();
                    }
                }
                else if(TelephonyManager.CALL_STATE_OFFHOOK==state){
                    try {
                        Log.e(TAG,"CALL_STATE_OFFHOOK===>");
                        if(recoderstarted==false)
                        {

                            Log.e(TAG,"CALL_STATE_OFFHOOK===>First");
                            recoderstarted=true;

                            Date date = new Date();
                            String stringTime = ""+DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(date);
                            callerStartTime = stringTime;
                            Log.e(TAG,"callerStartTime===>"+callerStartTime);

                        }
                    } catch (Exception e) {
                        Log.e("Exception","CALL_STATE_OFFHOOK===>"+e.getMessage());
                        e.printStackTrace();
                    }
                }
                else if(TelephonyManager.CALL_STATE_RINGING==state){
                    try {
                        Log.e(TAG,"CALL_STATE_RINGING===>");
                    } catch (Exception e) {
                        Log.e("Exception","CALL_STATE_RINGING===>"+e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void checkFolderAndUploadFile() {

        File[] datefolder = Constant.getCallRecordingDir().listFiles();

        if (datefolder != null) {
            Arrays.sort(datefolder, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            if (datefolder.length != 0) {
                new uploadFile(datefolder).execute("");
            } else {
                Toast toast = Toast.makeText(context, "NO FILE AVAILABLE", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

            }
        }
    }

    class uploadFile extends AsyncTask<String, String, String> {

        private File[] datefolder = null;
        private RFInterface rfInterface;

        public uploadFile(File[] datefolder) {
            this.datefolder = datefolder;
            rfInterface = Utility.getRetrofitInterface(Constant.URL);
        }

        @Override
        protected String doInBackground(String... strings) {

            for (int i = 0; i < datefolder.length; i++) {
                File audioFile = datefolder[i];
                if (audioFile.getName().contains("Call")) {

                    String agentMobileNumber = SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_NUMBBR);
                    String agentEmailID = SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_EMAIL);

                    Log.e(TAG,"agentMobileNumber===> "+agentMobileNumber);
                    Log.e(TAG,"agentEmailID===> "+agentEmailID);

                    String clientMobileNo = Constant.getClientNumber(context, audioFile.getName());
                    String audioFileExtension = audioFile.getName().substring(audioFile.getName().lastIndexOf('.') + 1);
                    int durationInSeconds = Constant.getDurationInSecond(context, audioFile);

                    RequestBody agentMobileNumberRequestBody = RequestBody.create(MediaType.parse("text"), agentMobileNumber);
                    RequestBody agentEmailIDRequestBody = RequestBody.create(MediaType.parse("text"), agentEmailID);
                    RequestBody clientMobileNumberRequestBody = RequestBody.create(MediaType.parse("text"), clientMobileNo);
                    RequestBody totalDurationRequestBody = RequestBody.create(MediaType.parse("text"), "" + durationInSeconds);
                    RequestBody audioFileRequestBody = RequestBody.create(MediaType.parse("audio"), audioFile);

                    RequestBody isLastRequestBody = null;
                    if (i == datefolder.length - 1) {
                        isLastRequestBody = RequestBody.create(MediaType.parse("text"), "1");
                    } else {
                        isLastRequestBody = RequestBody.create(MediaType.parse("text"), "0");
                    }

                    MultipartBody.Part audioMultipartBodyPart = MultipartBody.Part.createFormData("audio", audioFile.getName(), audioFileRequestBody);

                    //upload(agentMobileNumberRequestBody, agentEmailIDRequestBody, clientMobileNumberRequestBody, totalDurationRequestBody, audioMultipartBodyPart, isLastRequestBody, i);
                }
            }

            return null;
        }

        private void upload(RequestBody agentMobileNumberRequestBody, RequestBody agentEmailIDRequestBody, RequestBody clientMobileNumberRequestBody,
                            RequestBody totalDurationRequestBody, MultipartBody.Part audioMultipartBodyPart, RequestBody isLastRequestBody, int fileCounter) {
            try {
                Response<RmUploadFileResponse> executeUploadFileResponse = rfInterface.uploadFile(
                        agentMobileNumberRequestBody, agentEmailIDRequestBody, clientMobileNumberRequestBody,
                        totalDurationRequestBody, audioMultipartBodyPart, isLastRequestBody
                ).execute();

                if (executeUploadFileResponse.isSuccessful()) {
                    if (executeUploadFileResponse.body().getSuccess()) {

                        Data data = executeUploadFileResponse.body().getData();

                        File uploadedAudioFile = new File(Constant.getCallRecordingDir(), data.getFileName().substring(data.getFileName().indexOf("/")));

                        Log.d(TAG, "onSuccess: uploadedAudioFile: exists:" + uploadedAudioFile.exists());

                        if (uploadedAudioFile.exists()) uploadedAudioFile.delete();

                        if (data.getIsLast().equalsIgnoreCase("1")) {
                            context.sendBroadcast(
                                    new Intent().setAction("MANUAL_FILE_UPLOAD_COMPLETE")
                            );
                        }

                    } else {
                        publishProgress((fileCounter + 1) + " file found error while uploading");
                    }
                } else {
                    publishProgress((fileCounter + 1) + " file found http connection fails while uploading");
                }

            } catch (Exception e) {
                System.err.println(e);
                publishProgress(e.getLocalizedMessage());
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //TODO
            Toast toast = Toast.makeText(context, values[0], Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    class UploadMissedCall extends AsyncTask<String, String, String> {

        private Context context;
        private RFInterface rfInterface;

        public UploadMissedCall(Context context) {
            this.context = context;
            rfInterface = Utility.getRetrofitInterface(Constant.URL);
        }

        @Override
        protected String doInBackground(String... strings) {

            String agentMobileNumber = SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_NUMBBR);
            String agentEmailID = SharedPrefrenceObj.getSharedValue(context, Constant.AGENT_EMAIL);

            DataBaseAdapter dataBaseAdapter = new DataBaseAdapter(context).open();
            Log.d(TAG, "agentMobileNumber: uploadedAudioFile: exists:" + agentMobileNumber);

            ArrayList<MissedCallLog> allMissedCallLog = dataBaseAdapter.getAllMissedCallLog();

            if (allMissedCallLog != null) {
                for (MissedCallLog missedCallLog : allMissedCallLog) {
                    try {
                        Response<RmResultResponse> response = rfInterface.uploadMissedCallLog(
                                missedCallLog.getMobileNumber(), agentMobileNumber,
                                agentEmailID, missedCallLog.getDateTime()
                        ).execute();

                        if (response.isSuccessful()) {
                            if (response.body().getSuccess()) {
                                dataBaseAdapter.deleteMissedCallLog(String.valueOf(missedCallLog.getId()));
                            }
                        }

                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
            }

            dataBaseAdapter.close();

            return null;
        }
    }

    public String printDifference(Date startDate, Date endDate) {
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        Log.e(TAG,""+elapsedHours+":"+elapsedMinutes+":"+elapsedSeconds);

        return ""+elapsedHours+":"+elapsedMinutes+":"+elapsedSeconds;
    }

    @SuppressLint("StaticFieldLeak")
    private void insertList(List<Todo> todoList) {
        new AsyncTask<List<Todo>, Void, Void>() {
            @Override
            protected Void doInBackground(List<Todo>... params) {
                myDatabase.daoAccess().insertTodoList(params[0]);

                return null;

            }

            @Override
            protected void onPostExecute(Void voids) {
                super.onPostExecute(voids);

            }
        }.execute(todoList);

    }

}
