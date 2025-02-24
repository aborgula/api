package com.example.pl;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import retrofit2.Call;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    private Button signOutButton;
    private TextView statusText;
    private TextView token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        signOutButton = findViewById(R.id.sign_out_button);
        token = findViewById(R.id.token);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.sign_in_button).setOnClickListener(view -> signIn());

        signOutButton.setOnClickListener(view -> signOut());

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
         if (account != null) {
         updateUI(account);
         }

    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String idToken = preferences.getString("id_token", null);

        if (idToken != null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                token.setVisibility(TextView.VISIBLE);
                token.setText("token: " + idToken);
                updateUI(account);
            }
        } else {
            statusText.setText("zaloguj się");
            findViewById(R.id.sign_in_button).setVisibility(Button.VISIBLE);
            signOutButton.setVisibility(Button.GONE);
            token.setVisibility(TextView.GONE);
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();

            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("id_token", idToken);
            editor.apply();

            token.setText("Token: " + idToken);
            token.setVisibility(TextView.VISIBLE);

            updateUI(account);
        } catch (ApiException e) {
        }
    }

    private void updateUI(GoogleSignInAccount account) {
        statusText.setText("Zalogowano jako: " + account.getDisplayName());
        findViewById(R.id.sign_in_button).setVisibility(Button.GONE);
        signOutButton.setVisibility(Button.VISIBLE);
        findViewById(R.id.token).setVisibility(TextView.VISIBLE);


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://jsonplaceholder.typicode.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        JsonPlaceholderAPI api = retrofit.create(JsonPlaceholderAPI.class);

        Call<List<User>> call = api.getUsers();
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful()) {
                    List<User> users = response.body();
                    StringBuilder usersText = new StringBuilder();
                    for (User user : users) {
                        usersText.append("Name: ").append(user.getName()).append("\n");
                        usersText.append("Email: ").append(user.getEmail()).append("\n");
                        usersText.append("\n");
                    }
                    statusText.setText(usersText.toString());
                } else {
                    statusText.setText("Błąd odpowiedzi z API");
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                statusText.setText("Błąd połączenia z API");
            }
        });
        signOutButton.setOnClickListener(view -> signOut());

    }

    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(MainActivity.this, "Wylogowano", Toast.LENGTH_SHORT).show();

            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("id_token");
            editor.apply();

            token.setText("token: " + preferences.getString("id_token", null));
            statusText.setText("Zaloguj się");
            findViewById(R.id.sign_in_button).setVisibility(Button.VISIBLE);
            signOutButton.setVisibility(Button.GONE);

        });
    }

}
