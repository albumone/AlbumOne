package com.grunskis.albumone.albums;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.grunskis.albumone.R;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import timber.log.Timber;

public class LoginActivity extends AppCompatActivity {

    private static final String API_BASE_URL = "https://api.snapline.io/v1/";

    private TextView mEmailTextView;
    private TextView mPasswordTextView;
    private Button mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEmailTextView = findViewById(R.id.input_email);
        mPasswordTextView = findViewById(R.id.input_password);

        mLoginButton = findViewById(R.id.btn_login);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
    }

    private void login() {
        String email = mEmailTextView.getText().toString();
        String password = mPasswordTextView.getText().toString();

        if (!validate(email, password)) {
            onLoginFailed();
            return;
        }

        mLoginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.Theme_AppCompat_DayNight_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        SnaplineAuthAPI apiClient = getAuthApiClient();
        AuthCredentials credentials = new AuthCredentials(email, password);
        apiClient.getAuthenticationToken(credentials).enqueue(new Callback<AuthToken>() {
            @Override
            public void onResponse(@NonNull Call<AuthToken> call,
                                   @NonNull Response<AuthToken> response) {
                progressDialog.dismiss();

                if (response.isSuccessful()) {
                    AuthToken authToken = response.body();
                    if (authToken != null) {
                        onLoginSuccess(authToken.token);
                        return;
                    }
                } else {
                    try {
                        ResponseBody error = response.errorBody();
                        if (error != null) {
                            Timber.e("Failed to authenticate! error: %s",
                                    error.string());
                        } else {
                            Timber.e("Failed to authenticate! error: null");
                        }
                    } catch (IOException e) {
                        Timber.e(e);
                    }
                }

                onLoginFailed();
            }

            @Override
            public void onFailure(@NonNull Call<AuthToken> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                // todo show auth failed message
                onLoginFailed();
            }
        });
    }

    private boolean validate(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmailTextView.setError("enter a valid email address");
            isValid = false;
        } else {
            mEmailTextView.setError(null);
        }

        if (password.isEmpty()) {
            mPasswordTextView.setError("enter your password");
            isValid = false;
        } else {
            mPasswordTextView.setError(null);
        }

        return isValid;
    }

    private void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        mLoginButton.setEnabled(true);
    }

    private void onLoginSuccess(String authToken) {
        Intent result = new Intent();
        result.setData(Uri.parse(authToken));
        setResult(RESULT_OK, result);
        finish();
    }

    private SnaplineAuthAPI getAuthApiClient() {
        OkHttpClient.Builder builder = com.grunskis.albumone.util.StethoUtil.addNetworkInterceptor(
                new OkHttpClient.Builder());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build();

        return retrofit.create(SnaplineAuthAPI.class);
    }

    interface SnaplineAuthAPI {
        @POST("authenticate")
        Call<AuthToken> getAuthenticationToken(@Body AuthCredentials credentials);
    }

    static class AuthToken {
        public final String token;

        public AuthToken(String token) {
            this.token = token;
        }
    }

    static class AuthCredentials {
        public final String email;
        public final String password;

        public AuthCredentials(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
}
