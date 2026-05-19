package com.ouazzou.miniaws.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class GpsTracker {

    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;

    // Interface pour remonter les coordonnées vers nos écrans
    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);
        void onError(String errorMessage);
    }

    public GpsTracker(Context context) {
        this.context = context;
        // Initialisation du moteur GPS de Google
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void getCurrentLocation(Activity activity, LocationCallback callback) {
        // 1. Vérification de sécurité : L'utilisateur a-t-il dit OUI à la permission ?
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Si NON, on fait apparaître la popup pour demander la permission
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            callback.onError("Permission GPS non accordée.");
            return;
        }

        // 2. Récupération de la dernière position connue (Ultra rapide)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(activity, location -> {
                    if (location != null) {
                        // Succès ! On renvoie les coordonnées
                        callback.onLocationReceived(location.getLatitude(), location.getLongitude());
                    } else {
                        // Échec (souvent car le GPS du téléphone est physiquement désactivé)
                        callback.onError("Veuillez activer le GPS de votre téléphone.");
                    }
                })
                .addOnFailureListener(e -> callback.onError("Erreur réseau GPS : " + e.getMessage()));
    }
}