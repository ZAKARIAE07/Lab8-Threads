package com.example.jnidemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

    public native String helloFromJNI();
    public native int factorial(int n);
    public native String reverseString(String s);
    public native int sumArray(int[] array);

    private TextView    txtStatus;
    private ProgressBar progressBar;
    private ImageView   img;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Explicitly use the R class from the jnidemo package
        setContentView(com.example.jnidemo.R.layout.activity_main);

        txtStatus   = findViewById(com.example.jnidemo.R.id.txtStatus);
        progressBar = findViewById(com.example.jnidemo.R.id.progressBar);
        img         = findViewById(R.id.img);
        LinearLayout mainLayout = findViewById(com.example.jnidemo.R.id.mainLayout);

        Button btnLoadThread = findViewById(com.example.jnidemo.R.id.btnLoadThread);
        Button btnCalcAsync  = findViewById(com.example.jnidemo.R.id.btnCalcAsync);
        Button btnToast      = findViewById(com.example.jnidemo.R.id.btnToast);
        
        Button btnJNI = new Button(this);
        btnJNI.setText("Tester JNI (Vérification)");
        if (mainLayout != null) {
            mainLayout.addView(btnJNI);
        }

        mainHandler = new Handler(Looper.getMainLooper());

        btnToast.setOnClickListener(v ->
                Toast.makeText(getApplicationContext(), "UI réactive ✓", Toast.LENGTH_SHORT).show()
        );

        btnLoadThread.setOnClickListener(v -> loadImageWithThread());
        btnCalcAsync.setOnClickListener(v -> new HeavyCalcTask(this).execute());

        btnJNI.setOnClickListener(v -> runJniTests());
    }

    private void runJniTests() {
        StringBuilder sb = new StringBuilder();
        try {
            // Test 1: Valeur normale
            int f10 = factorial(10);
            sb.append("Test 1 [fact(10)]: ").append(f10).append(f10 == 3628800 ? " ✓" : " ✗").append("\n");

            // Test 2: Valeur négative
            int fn5 = factorial(-5);
            sb.append("Test 2 [fact(-5)]: ").append(fn5).append(fn5 == -1 ? " ✓" : " ✗").append("\n");

            // Test 3: Dépassement
            int f20 = factorial(20);
            sb.append("Test 3 [fact(20)]: ").append(f20).append(f20 == -2 ? " ✓" : " ✗").append("\n");

            // Test 4: Chaîne vide
            String revEmpty = reverseString("");
            sb.append("Test 4 [reverse('')]: '").append(revEmpty).append("'").append(revEmpty.isEmpty() ? " ✓" : " ✗").append("\n");

            // Test 5: Tableau vide
            int sumEmpty = sumArray(new int[]{});
            sb.append("Test 5 [sum([])]: ").append(sumEmpty).append(sumEmpty == 0 ? " ✓" : " ✗").append("\n");

            // Hello JNI
            sb.append("\nJNI Hello: ").append(helloFromJNI());

            txtStatus.setText(sb.toString());
            Log.d("JNI_DEMO", sb.toString());

        } catch (UnsatisfiedLinkError e) {
            txtStatus.setText("Erreur JNI : " + e.getMessage());
        }
    }

    private void loadImageWithThread() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        txtStatus.setText("Statut : chargement image (Thread)...");

        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), com.example.jnidemo.R.mipmap.ic_launcher);
            mainHandler.post(() -> {
                img.setImageBitmap(bitmap);
                progressBar.setVisibility(View.INVISIBLE);
                txtStatus.setText("Statut : image chargée ✓");
            });
        }).start();
    }

    private static class HeavyCalcTask extends AsyncTask<Void, Integer, Long> {
        private final WeakReference<MainActivity> activityReference;
        HeavyCalcTask(MainActivity context) { activityReference = new WeakReference<>(context); }

        @Override
        protected void onPreExecute() {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;
            activity.progressBar.setVisibility(View.VISIBLE);
            activity.txtStatus.setText("Statut : calcul lourd...");
        }

        @Override
        protected Long doInBackground(Void... voids) {
            long result = 0;
            for (int i = 1; i <= 100; i++) {
                if (isCancelled()) break;
                for (int k = 0; k < 200_000; k++) result += (i * k) % 7;
                publishProgress(i);
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            MainActivity activity = activityReference.get();
            if (activity != null) activity.progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Long result) {
            MainActivity activity = activityReference.get();
            if (activity != null) {
                activity.progressBar.setVisibility(View.INVISIBLE);
                activity.txtStatus.setText("Calcul terminé : " + result);
            }
        }
    }
}
