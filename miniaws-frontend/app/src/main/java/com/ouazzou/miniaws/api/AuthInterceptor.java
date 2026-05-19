package com.ouazzou.miniaws.api;

import androidx.annotation.NonNull;
import com.ouazzou.miniaws.utils.Constants;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Si on a un token en mémoire, on l'ajoute au Header
        if (Constants.CURRENT_TOKEN != null && !Constants.CURRENT_TOKEN.isEmpty()) {
            Request newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + Constants.CURRENT_TOKEN)
                    .build();
            return chain.proceed(newRequest);
        }

        // Sinon, on envoie la requête telle quelle
        return chain.proceed(originalRequest);
    }
}