package com.example.app1;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import java.util.List;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;

public class MainActivity extends AppCompatActivity {

    private TextView speedLabel;
    private TextView bssLabel;

    private TextView bsi;
    private Button startSpdt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speedLabel = findViewById(R.id.speedLabel);
        bssLabel = findViewById(R.id.bss);

        bsi = findViewById(R.id.bsi);
        startSpdt = findViewById(R.id.startSpdt);
        startSpdt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeedTest();
            }
        });

    }

    public void startSpeedTest() {
        speedLabel.setText("Testing internet speed...");
        bssLabel.setText("Please be patient");
        new Thread(new Runnable() {
            @Override
            public void run() {

                float[] speeds = runSpeedTest();

                if (speeds != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String baseStationInfo = getBaseStationInfo();
                            bsi.setText("base station information:"+baseStationInfo);
                            // Update UI elements with speed results
                            speedLabel.setText("Upload Speed: " + speeds[0] + " Mbps");
                            bssLabel.setText("Download Speed: " + speeds[1] + " Mbps");
                        }
                    });

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Handle error case
                            speedLabel.setText("Speed test failed");
                            bssLabel.setText("Please check your network connection");
                        }
                    });
                }
            }
        }).start();


    }

    private float[] runSpeedTest() {
        OkHttpClient client = new OkHttpClient();

        // Check connectivity
        if (!isNetworkAvailable()) {
            // Handle no network connectivity case
            return null;
        }

        // Measure upload speed
        Request request = new Request.Builder()
                .url("https://www.speedtest.net/")
                .build();
        long uploadStartTime = System.nanoTime();
        try (Response response = client.newCall(request).execute()) {
            long uploadEndTime = System.nanoTime();
            long elapsedTime = uploadEndTime - uploadStartTime;
            float uploadSpeed = calculateSpeed(response.body().contentLength(), elapsedTime);

            // Measure download speed
            request = new Request.Builder()
                    .url("https://www.speedtest.net/")
                    .build();
            long downloadStartTime = System.nanoTime();
            try (Response downloadResponse = client.newCall(request).execute()) {
                long downloadEndTime = System.nanoTime();
                elapsedTime = downloadEndTime - downloadStartTime;
                float downloadSpeed = calculateSpeed(downloadResponse.body().contentLength(), elapsedTime);
                return new float[]{uploadSpeed, downloadSpeed};
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null; // Or handle the error accordingly
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private float calculateSpeed(long dataSize, long elapsedTime) {
        long dataSizeBits = dataSize * 8;
        double elapsedTimeSeconds = (double) elapsedTime / 1_000_000_000;

        return (float) (-dataSizeBits / elapsedTimeSeconds) ;// (1024 * 1024);
    }

    private String getBaseStationInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            return "Location permission not granted";
        }
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);


        if (telephonyManager == null) {
            return "TelephonyManager is null";
        }
        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
        if (cellInfoList != null && !cellInfoList.isEmpty()) {
            CellInfo cellInfo = cellInfoList.get(0);
            //define strings of info
            String cidString = "";
            String lacString = "";
            String mccString = "";
            String mncString = "";
            //define network service via type
            if (cellInfo instanceof CellInfoGsm) {
                CellIdentityGsm cellIdentityGsm =
                        ((CellInfoGsm) cellInfo).getCellIdentity();
                cidString = String.valueOf(cellIdentityGsm.getCid());
                lacString = String.valueOf(cellIdentityGsm.getLac());
                mccString = cellIdentityGsm.getMccString();
                mncString = cellIdentityGsm.getMncString();
            } else if (cellInfo instanceof CellInfoCdma) {
                CellIdentityCdma cellIdentityCdma =((CellInfoCdma) cellInfo).getCellIdentity();
                cidString = String.valueOf(cellIdentityCdma.getBasestationId());
                lacString = String.valueOf(cellIdentityCdma.getSystemId());
                mccString = String.valueOf(cellIdentityCdma.getNetworkId());
                mncString = String.valueOf(cellIdentityCdma.getSystemId());
                mccString = mccString.substring(0, 3);
                mncString = mncString.substring(3);
            } else if (cellInfo instanceof CellInfoLte) {
                CellIdentityLte cellIdentityLte =((CellInfoLte) cellInfo).getCellIdentity();
                cidString = String.valueOf(cellIdentityLte.getCi());
                lacString = String.valueOf(cellIdentityLte.getTac());
                mccString = cellIdentityLte.getMccString();
                mncString = cellIdentityLte.getMncString();
            } else if (cellInfo instanceof CellInfoNr) {
                CellIdentityNr nr = (CellIdentityNr) ((CellInfoNr) cellInfo).getCellIdentity();
                cidString = String.valueOf(nr.getNci());
                lacString = String.valueOf(nr.getTac());
                mccString = nr.getMccString();
                mncString = nr.getMncString();
            }
            return "\nCID: " + cidString + "\n" +
                    "LAC: " + lacString + "\n" +
                    "MCC: " + mccString + "\n" +
                    "MNC: " + mncString+ "\n" +
                    cellInfo.toString();
        }
        //handle errors


        return telephonyManager.getSimOperatorName().toString()+
                telephonyManager.getAllCellInfo()+
                telephonyManager.getNetworkOperator().toString()+"\nBase station information not available\n(Perhaps you can try to open location permission)";
    }
}
