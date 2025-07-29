package com.siugnur.metiscode.utils;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.Messages;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

public class CommonUtil {
    public static OkHttpClient getHttpClient() {
        Interceptor interceptor = new Interceptor() {
            @NotNull
            @Override
            public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
                Request original = chain.request();

                if (original.url().toString().startsWith("https://ome-account.omenow.com")) {
                    return chain.proceed(original);
                }

                String token = PropertiesComponent.getInstance().getValue(Constant.TOKEN);
                String userId = PropertiesComponent.getInstance().getValue(Constant.USER_ID);
                String username = PropertiesComponent.getInstance().getValue(Constant.USERNAME);

                if (Stream.of(token, userId, username).anyMatch(t -> t == null || t.isBlank())) {
                    SwingUtilities.invokeLater(() -> Messages.showWarningDialog("用户信息缺失，请重新登录", "提示"));
                    throw new RuntimeException("用户信息缺失");
                }

                Request request = original.newBuilder()
                        .header("ome-metis-authorization", token)
                        .header("ome-metis-userid", userId)
                        .header("ome-metis-username", username)
                        .header("Content-Type", "application/json")
                        .method(original.method(), original.body())
                        .build();

                Response response = chain.proceed(request);
                if (response.code() == 401) {
                    SwingUtilities.invokeLater(() -> Messages.showWarningDialog("登录过期，请重新登录", "提示"));
                }
                return response;
            }
        };

        return new OkHttpClient().newBuilder()
                .addInterceptor(interceptor)
                .build();
    }
    private static String encodeUtf8(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static String currentDatetime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.CHINESE);
        return now.format(formatter);
    }

    public static String generateRandomID() {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int DEFAULT_LENGTH = 21;
        Random random = new Random();
        StringBuilder sb = new StringBuilder(DEFAULT_LENGTH);

        for (int i = 0; i < DEFAULT_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }

        return sb.toString();
    }
}
