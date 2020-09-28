package com.tgc.researchchat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.tgc.researchchat.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {

    private static final String PORT_1 = "8981";
    private ActivityWelcomeBinding b;
    private static final String TAG = "WelcomeActivity";
    String myiP = "", port = PORT_1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = DataBindingUtil.setContentView(this, R.layout.activity_welcome);

        b.tvScanCode.setOnClickListener(v -> {
            new IntentIntegrator(this).initiateScan(); // `this` is the current Activity
        });

        generateMyQR();

    }

    private void generateMyQR() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        myiP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(myiP, BarcodeFormat.QR_CODE, 400, 400);
            b.qrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get the results:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                if (Patterns.IP_ADDRESS.matcher(result.getContents()).matches()) {
                    String info = getInfo(result.getContents());
                    Intent intent = new Intent(WelcomeActivity.this, ChatClient.class);
                    intent.putExtra("ip&port", info);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                } else {
                    if (result.getContents().trim().contentEquals("0.0.0.0")) {
                        String gateway=myiP.replace(myiP.split("\\.")[3],"1");
                        Log.e(TAG, "onActivityResult: gateway"+gateway );
                        String info = getInfo(gateway);
                        Intent intent = new Intent(WelcomeActivity.this, ChatClient.class);
                        intent.putExtra("ip&port", info);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                    } else {
                        Toast toast = Toast.makeText(this, "Please Enter a Valid IP Address", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    String getInfo(String ip) {
        String info = ip + " " + PORT_1 + " " + PORT_1;
        Log.i(TAG, "info => " + info);
        return info;
    }
}