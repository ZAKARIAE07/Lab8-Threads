package com.example.lab8threads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    // ── Références vers les vues ──────────────────────────────────────────────
    private TextView    txtStatus;
    private ProgressBar progressBar;
    private ImageView   img;

    // ── Handler lié au UI Thread ──────────────────────────────────────────────
    private Handler mainHandler;

    // =========================================================================
    // onCreate
    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Lier les vues XML
        txtStatus   = findViewById(R.id.txtStatus);
        progressBar = findViewById(R.id.progressBar);
        img         = findViewById(R.id.img);

        Button btnLoadThread = findViewById(R.id.btnLoadThread);
        Button btnCalcAsync  = findViewById(R.id.btnCalcAsync);
        Button btnToast      = findViewById(R.id.btnToast);

        // 2. Handler rattaché au Main Looper (UI Thread)
        mainHandler = new Handler(Looper.getMainLooper());

        // 3. Bouton Toast : UI toujours réactive
        btnToast.setOnClickListener(v ->
                Toast.makeText(getApplicationContext(),
                        "UI réactive ✓", Toast.LENGTH_SHORT).show()
        );

        // 4. Chargement image via Thread classique
        btnLoadThread.setOnClickListener(v -> loadImageWithThread());

        // 5. Calcul lourd via AsyncTask
        btnCalcAsync.setOnClickListener(v -> new HeavyCalcTask(this).execute());
    }

    // =========================================================================
    // PARTIE 1 — Thread + Handler
    // =========================================================================
    private void loadImageWithThread() {

        // Mise à jour UI avant de lancer le thread (on est encore sur le UI Thread)
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        txtStatus.setText("Statut : chargement image (Thread)...");

        new Thread(() -> {

            // --- WORKER THREAD : zone de travail long ---

            // Simulation d'un téléchargement ou d'un décodage lourd
            try {
                Thread.sleep(1000); // 1 seconde
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Chargement du bitmap (opération CPU)
            Bitmap bitmap = BitmapFactory.decodeResource(
                    getResources(), R.mipmap.ic_launcher);

            // --- Retour sur le UI Thread via Handler ---
            // img.setImageBitmap() NE PEUT PAS être appelé depuis un Worker Thread.
            mainHandler.post(() -> {
                img.setImageBitmap(bitmap);
                progressBar.setVisibility(View.INVISIBLE);
                txtStatus.setText("Statut : image chargée ✓ (Thread)");
            });

        }).start(); // IMPORTANT : start() lance vraiment le thread
    }

    // =========================================================================
    // PARTIE 2 — AsyncTask
    // =========================================================================

    /**
     * HeavyCalcTask
     *
     * Paramètres génériques :
     *   Void    → pas de paramètre d'entrée pour execute()
     *   Integer → type de la progression (0..100)
     *   Long    → type du résultat final renvoyé par doInBackground()
     */
    private static class HeavyCalcTask extends AsyncTask<Void, Integer, Long> {

        private final WeakReference<MainActivity> activityReference;

        // Constructeur pour passer la référence de l'activité
        HeavyCalcTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        // ── 1. Avant le traitement : UI Thread ───────────────────────────────
        @Override
        protected void onPreExecute() {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            activity.progressBar.setVisibility(View.VISIBLE);
            activity.progressBar.setProgress(0);
            activity.txtStatus.setText("Statut : calcul lourd (AsyncTask)...");
        }

        // ── 2. Traitement long : Worker Thread ───────────────────────────────
        //      JAMAIS toucher une View ici !
        @Override
        protected Long doInBackground(Void... voids) {
            long result = 0;

            for (int i = 1; i <= 100; i++) {
                if (isCancelled()) break;

                // Simulation d'un calcul intensif
                for (int k = 0; k < 200_000; k++) {
                    result += (i * k) % 7;
                }

                // Envoie la valeur i à onProgressUpdate() sur le UI Thread
                publishProgress(i);
            }

            return result;
        }

        // ── 3. Pendant la progression : UI Thread ────────────────────────────
        @Override
        protected void onProgressUpdate(Integer... values) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            activity.progressBar.setProgress(values[0]);
        }

        // ── 4. Après le traitement : UI Thread ───────────────────────────────
        @Override
        protected void onPostExecute(Long result) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            activity.progressBar.setVisibility(View.INVISIBLE);
            activity.txtStatus.setText("Statut : calcul terminé ✓  résultat = " + result);
        }
    }
}
