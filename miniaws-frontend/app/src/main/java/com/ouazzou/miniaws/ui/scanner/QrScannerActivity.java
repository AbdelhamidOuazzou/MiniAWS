package com.ouazzou.miniaws.ui.scanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.ouazzou.miniaws.R;
import com.ouazzou.miniaws.api.ApiClient;
import com.ouazzou.miniaws.models.ServerInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrScannerActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView tvScanInstruction, tvScannedContent;
    private ProgressBar qrProgressBar;
    private MaterialCardView cardScanResult;
    private MaterialButton btnDeployQr, btnCancelScan;

    private ExecutorService cameraExecutor;
    private boolean isScanning = true;
    private String currentQrData = "";

    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        latitude = getIntent().getDoubleExtra("LATITUDE", 0.0);
        longitude = getIntent().getDoubleExtra("LONGITUDE", 0.0);

        previewView = findViewById(R.id.previewView);
        tvScanInstruction = findViewById(R.id.tvScanInstruction);
        qrProgressBar = findViewById(R.id.qrProgressBar);
        
        cardScanResult = findViewById(R.id.cardScanResult);
        tvScannedContent = findViewById(R.id.tvScannedContent);
        btnDeployQr = findViewById(R.id.btnDeployQr);
        btnCancelScan = findViewById(R.id.btnCancelScan);

        cameraExecutor = Executors.newSingleThreadExecutor();

        btnDeployQr.setOnClickListener(v -> executeDeployment());
        btnCancelScan.setOnClickListener(v -> resetScanner("Scan annulé"));

        // Demande de permission Caméra
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. La Preview (Ce qui s'affiche à l'écran)
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 2. L'Analyseur d'image (ML Kit)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                // 3. Liaison avec la caméra arrière
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CAMERA_ERROR", "Erreur lancement caméra", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @ExperimentalGetImage
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (!isScanning) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            BarcodeScanner scanner = BarcodeScanning.getClient();

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && isScanning) {
                                // 🎯 BINGO ! On a trouvé un code !
                                isScanning = false; // On bloque le scanner
                                runOnUiThread(() -> processQrCodeResult(rawValue));
                                break;
                            }
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    // ==========================================
    // 🧠 LOGIQUE RÉSEAU (Envoi du QR à Spring Boot)
    // ==========================================
    private void processQrCodeResult(String qrContent) {
        currentQrData = qrContent;
        tvScannedContent.setText(qrContent);

        // On affiche la carte de confirmation
        tvScanInstruction.setVisibility(View.GONE);
        cardScanResult.setVisibility(View.VISIBLE);
    }

    private void executeDeployment() {
        btnDeployQr.setEnabled(false);
        btnDeployQr.setText("EN COURS...");
        qrProgressBar.setVisibility(View.VISIBLE);

        ApiClient.getApi().magicDeploy(currentQrData).enqueue(new Callback<ServerInstance>() {
            @Override
            public void onResponse(Call<ServerInstance> call, Response<ServerInstance> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ServerInstance server = response.body();
                    Toast.makeText(QrScannerActivity.this, "✅ QR Validé ! Serveur " + server.getName() + " déployé.", Toast.LENGTH_LONG).show();
                    finish(); // Retour au Dashboard
                } else {
                    resetScanner("❌ Erreur API : " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ServerInstance> call, Throwable t) {
                resetScanner("⚠️ Serveur injoignable : " + t.getMessage());
            }
        });
    }

    private void resetScanner(String message) {
        if (message != null) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        isScanning = true;
        currentQrData = "";

        btnDeployQr.setEnabled(true);
        btnDeployQr.setText("LANCER LE DÉPLOIEMENT");
        qrProgressBar.setVisibility(View.GONE);
        cardScanResult.setVisibility(View.GONE);
        tvScanInstruction.setVisibility(View.VISIBLE);
        tvScanInstruction.setText("Pointez la caméra vers un QR Code");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "La permission de la caméra est requise.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}