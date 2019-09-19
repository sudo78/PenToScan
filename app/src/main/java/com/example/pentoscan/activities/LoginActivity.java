package com.example.pentoscan.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.pentoscan.API;
import com.example.pentoscan.R;
import com.example.pentoscan.helpers.InputValidation;
import com.example.pentoscan.sql.DatabaseHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private final AppCompatActivity activity = LoginActivity.this;
    Context context;
//    ConstraintLayout constraintLayout;

//    private  TextInputLayout textInputLayoutEmail;
//      private TextInputLayout textInputLayoutPassword;

    private EditText textInputEditTextEmail;
    private EditText textInputEditTextPassword;
    private Button appCompatButtonLogin;
    private TextView textViewLinkRegister;

    private InputValidation inputValidation;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        initViews();
        context = LoginActivity.this;

        initListeners();
        initObjects();
    }

    /**
     * This method is to initialize views
     */
    @SuppressLint("WrongViewCast")
    private void initViews() {

//        nestedScrollView = findViewById(R.id.nestedScrollView);
//
//        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmail);
//        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword);

        textInputEditTextEmail = findViewById(R.id.textInputEditTextEmail);
        textInputEditTextPassword = findViewById(R.id.textInputEditTextPassword);

        appCompatButtonLogin = findViewById(R.id.appCompatButtonLogin);

        textViewLinkRegister = findViewById(R.id.textViewLinkRegister);

    }

    /**
     * This method is to initialize listeners
     */
    private void initListeners() {
        appCompatButtonLogin.setOnClickListener(this);
        textViewLinkRegister.setOnClickListener(this);
    }

    /**
     * This method is to initialize objects to be used
     */
    private void initObjects() {
        databaseHelper = new DatabaseHelper(activity);
        inputValidation = new InputValidation(activity);

    }

    /**
     * This implemented method is to listen the click on view
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.appCompatButtonLogin:
                loginDataToApi();
                verifyFromSQLite();
                break;
            case R.id.textViewLinkRegister:
                // Navigate to RegisterActivity
                Intent intentRegister = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intentRegister);
                overridePendingTransition(android.R.anim.fade_in ,android.R.anim.fade_out);
                break;
        }
    }

    private void loginDataToApi() {
        String email_string = textInputEditTextEmail.getText().toString();
        String password_string = textInputEditTextPassword.getText().toString();
        login_api_hint(email_string, password_string);
    }

    public void sharedResponse(String response) {
        SharedPreferences mPrefs = getSharedPreferences("IDvalue", 0);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("Token", response);
        editor.commit();
    }

    private void login_api_hint(final String email, final String password) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        StringRequest string_request = new StringRequest(Request.Method.POST, API.login, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObj = new JSONObject(response);
                    String token = jsonObj.getString("key");
                    sharedResponse(token);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Intent cameraIntent = new Intent(LoginActivity.this, CameraActivity.class);
//            accountsIntent.putExtra("EMAIL", textInputEditTextEmail.getText().toString().trim());
                emptyInputEditText();
                startActivity(cameraIntent);



                Toast.makeText(LoginActivity.this, "You are login successfully", Toast.LENGTH_LONG).show();


            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        int statusCode = error.networkResponse.statusCode;
                        JSONObject err = null;
                        String msg = null;
                        String errorText = null;

                        try {
                            errorText = new String(error.networkResponse.data, "UTF-8");
                            err = new JSONObject(errorText);
                            msg = err.getJSONArray("non_field_errors").getString(0);


                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        switch (statusCode) {

                            case 400:

                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(LoginActivity.this, "u r not logged in", Toast.LENGTH_LONG).show();
                                break;
                        }


                    }
                }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                HashMap<String, String> postParam = new HashMap<String, String>();
                postParam.put("email", email);
                postParam.put("password", password);

                return new JSONObject(postParam).toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };
        requestQueue.add(string_request);

    }

    /**
     * This method is to validate the input text fields and verify login credentials from SQLite
     */
    private void verifyFromSQLite() {
        if (!inputValidation.isInputEditTextFilled(textInputEditTextEmail, getString(R.string.error_message_email))) {
            return;
        }
        if (!inputValidation.isInputEditTextEmail(textInputEditTextEmail, getString(R.string.error_message_email))) {
            return;
        }
        if (!inputValidation.isInputEditTextFilled(textInputEditTextPassword, getString(R.string.error_message_password))) {
            return;
        }

        if (databaseHelper.checkUser(textInputEditTextEmail.getText().toString().trim()
                , textInputEditTextPassword.getText().toString().trim())) {



        } else {
            // Snack Bar to show success message that record is wrong
            //Snackbar.make(constraintLayout, getString(R.string.error_valid_email_password), Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * This method is to empty all input edit text
     */
    private void emptyInputEditText() {
        textInputEditTextEmail.setText(null);
        textInputEditTextPassword.setText(null);
    }

}
