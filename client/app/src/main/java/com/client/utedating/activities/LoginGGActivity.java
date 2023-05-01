package com.client.utedating.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.client.utedating.R;
import com.client.utedating.models.User;
import com.client.utedating.models.UserModel;
import com.client.utedating.retrofit.AuthApiService;
import com.client.utedating.retrofit.RetrofitClient;
import com.client.utedating.sharedPreferences.SharedPreferencesClient;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginGGActivity extends AppCompatActivity {
    private static final String TAG = "LoginGGActivity";
    GoogleSignInClient mGoogleSignInClient;
    SignInButton buttonSigninGG;
    int RC_SIGN_IN = 100;
    AuthApiService authApiService;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_ggactivity);

        mAuth = FirebaseAuth.getInstance();
        authApiService = RetrofitClient.getInstance().create(AuthApiService.class);

        buttonSigninGG = findViewById(R.id.sign_in_button);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        buttonSigninGG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                // Signed in successfully, show authenticated UI.
                firebaseAuth(account.getIdToken());
            } catch (ApiException e) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            }
        }
    }

    private void firebaseAuth(String idToken) {
        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            assert user != null;
                            authApiService.signup(user.getDisplayName(), user.getEmail(), String.valueOf(user.getPhotoUrl())).enqueue(new Callback<UserModel>() {
                                @Override
                                public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                                    if (response.isSuccessful()) {
                                        UserModel userModel = response.body();
                                        User mUser = userModel.getResult();
                                        Log.e("TAG", mUser.toString());

                                        Intent intent;
                                        if (response.body().getMessage().equals("Signin Success")) {
                                            if (mUser.getBirthday().equals("") || mUser.getGender().equals("") || mUser.getDateWith().equals("") || mUser.getAbout().equals("") || mUser.getFaculty().equals("") ||mUser.getInterests().size() == 0 ||mUser.getLocation() == null) {
                                                intent = new Intent(LoginGGActivity.this, InitialActivity.class);
                                            } else {
                                                intent = new Intent(LoginGGActivity.this, MainActivity.class);
                                            }
                                        } else {
                                            intent = new Intent(LoginGGActivity.this, InitialActivity.class);
                                        }
                                        SharedPreferencesClient sharedPreferencesClient = new SharedPreferencesClient(LoginGGActivity.this);
                                        sharedPreferencesClient.putUserInfo("user", mUser);

                                        startActivity(intent);
                                        finish();
                                    }
                                }

                                @Override
                                public void onFailure(Call<UserModel> call, Throwable t) {
                                    Toast.makeText(LoginGGActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });

                            finish();
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            //updateUI(null);
                        }
                    }
                });
    }
//    public void saveUserInfo(User user){
//        SharedPreferences sharedPreferences = getSharedPreferences("USERINFO", Context.MODE_PRIVATE);
//        Gson gson = new Gson();
//        String json = gson.toJson(user);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString("user", json);
//        editor.apply();
//    }

}