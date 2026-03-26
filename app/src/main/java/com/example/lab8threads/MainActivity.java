package com.example.lab8threads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jnidemo.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    static {
        System.loadLibrary("native-lib");
    }

    public native String helloFromJNI();
    public native int factorial(int n);
    public native String reverseString(String s);
    public native int sumArray(int[] array);

    private TextView txtStatus;
    private ProgressBar progressBar;
    private ImageView img;

    private Handler mainHandler;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = findViewById(R.id.txtStatus);
        progressBar = findViewById(R.id.progressBar);
        img = findViewById(R.id.img);
        LinearLayout mainLayout = findViewById(R.id.mainLayout);

        Button btnLoadThread = findViewById(R.id.btnLoadThread);
        Button btnCalcAsync = findViewById(R.id.btnCalcAsync);
        Button btnToast = findViewById(R.id.btnToast);

        Button btnJNI = new Button(this);
        btnJNI.setText(R.string.btn_test_jni);
        if (mainLayout != null) {
            mainLayout.addView(btnJNI);
        }

        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();

        btnToast.setOnClickListener(v ->
                Toast.makeText(getApplicationContext(),
                        R.string.ui_reactive, Toast.LENGTH_SHORT).show()
        );

        btnLoadThread.setOnClickListener(v -> loadImageWithThread());
        btnCalcAsync.setOnClickListener(v -> runHeavyCalculation());

        btnJNI.setOnClickListener(v -> {
            try {
                String hello = helloFromJNI();
                int fact = factorial(5);
                String reversed = reverseString("Android");
                int sum = sumArray(new int[]{1, 2, 3, 4, 5});

                String result = getString(R.string.jni_result_format, hello, fact, reversed, sum);
                txtStatus.setText(result);
                Log.d(TAG, result);
            } catch (UnsatisfiedLinkError e) {
                txtStatus.setText(getString(R.string.jni_error, e.getMessage()));
                Log.e(TAG, "JNI Error", e);
            }
        });
    }

    private void loadImageWithThread() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        txtStatus.setText(R.string.status_loading_thread);

        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted", e);
                Thread.currentThread().interrupt();
            }

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

            mainHandler.post(() -> {
                img.setImageBitmap(bitmap);
                progressBar.setVisibility(View.INVISIBLE);
                txtStatus.setText(R.string.status_loaded_thread);
            });
        }).start();
    }

    private void runHeavyCalculation() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        txtStatus.setText(R.string.status_calculating_async);

        executorService.execute(() -> {
            long result = 0;
            for (int i = 1; i <= 100; i++) {
                for (int k = 0; k < 200_000; k++) {
                    result += (i * k) % 7;
                }
                final int progress = i;
                mainHandler.post(() -> progressBar.setProgress(progress));
            }

            final long finalResult = result;
            mainHandler.post(() -> {
                progressBar.setVisibility(View.INVISIBLE);
                txtStatus.setText(getString(R.string.status_finished_async, finalResult));
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
