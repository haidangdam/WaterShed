package com.example.haidangdam.watershed.controller;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.haidangdam.watershed.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import model.User;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

  private static final int FACEBOOK_REQUEST_CODE = 1709;
  private static final int RC_SIGN_IN = 9001;
  private final FirebaseDatabase database = FirebaseDatabase.getInstance();
  Button LogInButton;
  Button ResetPassword;
  EditText userNameLogin;
  EditText passwordLogin;
  Button registrationButton;
  SignInButton googleSignIn;
  DatabaseReference refUser;
  DataSnapshot data;
  private FirebaseAuth mAuth;
  private FirebaseAuth.AuthStateListener mAuthListener;
  private ProgressDialog progressDialog;
  private LoginButton logInFacebook;
  private CallbackManager callbackManager;
  private GoogleApiClient mGoogleApiClient;
  private String pathUser = "userID";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FacebookSdk.sdkInitialize(getApplicationContext(), FACEBOOK_REQUEST_CODE);
    setContentView(R.layout.activity_login);
    LogInButton = (Button) findViewById(R.id.email_sign_in_button);
    userNameLogin = (EditText) findViewById(R.id.email);
    passwordLogin = (EditText) findViewById(R.id.password);
    registrationButton = (Button) findViewById(R.id.registration_sign_in_button);
    logInFacebook = (LoginButton) findViewById(R.id.login_button);
    logInFacebook.setReadPermissions("email", "public_profile");
    progressDialog = new ProgressDialog(this);
    googleSignIn = (SignInButton) findViewById(R.id.sign_in_button);
    callbackManager = CallbackManager.Factory.create();
    refUser = database.getReference(pathUser);
    refUser.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        data = dataSnapshot;
      }

      @Override
      public void onCancelled(DatabaseError dError) {
        Log.d("Watershed app", dError.getDetails());
      }
    });

    if (getIntent().getExtras() != null) {
      putDataIntoField(getIntent().getExtras());
    }

    registrationButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        goToRegistrationActivity();
      }
    });
    mAuth = FirebaseAuth.getInstance();
    mAuthListener = new FirebaseAuth.AuthStateListener() {
      @Override
      public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
          if (data != null && !data.hasChild(user.getUid())) {
            User userData = new User(user.getEmail(), "Worker");
            userData.setName(user.getDisplayName());
            refUser.child(user.getUid()).setValue(userData);

          }
          Log.d("MAIN ACTIVITY", "USER SIGN IN");
        } else {
          LoginManager.getInstance().logOut();
          Log.d("MAIN ACTIVITY", "USER SIGN OUT");
        }
      }
    };
    LogInButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (userNameLogin.getText().toString().isEmpty() |
            passwordLogin.getText().toString().isEmpty()) {
          processEmptyTextField();
        } else {
          authenticateUser();
        }
      }

    });
    logInFacebook.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
      @Override
      public void onSuccess(LoginResult loginResult) {
        Log.d("Android", "Facebook login: " + loginResult);
        handleFacebookLoginToken(loginResult.getAccessToken());
      }

      @Override
      public void onCancel() {
        Log.d("Facebook", "User cancel");
      }

      @Override
      public void onError(FacebookException exception) {
        Toast.makeText(getApplicationContext(), "Log in failed: " + exception.toString(),
            Toast.LENGTH_LONG).show();
      }
    });
    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(getString(R.string.default_web_client_id))
        .requestEmail().build();
    mGoogleApiClient = new GoogleApiClient.Builder(this).
        addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();
    googleSignIn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        signInWithGoogleActivity();
      }
    });
    ResetPassword = (Button) findViewById(R.id.reset_password_button_login);
    ResetPassword.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(getApplicationContext(), ResetPassword.class));
      }
    });
  }

  /**
   * Action when the either user input is empty
   */
  private void processEmptyTextField() {
    Context context = getApplicationContext();
    CharSequence string = "You need to type password or user name login";
    int duration = Toast.LENGTH_LONG;

    Toast toast = Toast.makeText(context, string, duration);
    toast.show();
  }


  /**
   * Go to registration screen when hitting registration button
   */
  private void goToRegistrationActivity() {
    Intent registrationActivity = new Intent(this, RegistrationActivity.class);
    startActivity(registrationActivity);
  }


  /**
   * Put all the data passed from the registration to the appropriate field
   *
   * @param data The data that pass from the registration
   */
  private void putDataIntoField(Bundle data) {
    Log.d("Device in put Data", "here");
    userNameLogin.setText(data.getString(RegistrationActivity.username));
    passwordLogin.setText(data.getString(RegistrationActivity.password));
  }


  /**
   * Authenticate the password and email from Firebase Database
   */
  private void authenticateUser() {
    progressDialog.setMessage("Please wait!!");
    progressDialog.show();
    mAuth.signInWithEmailAndPassword(userNameLogin.getText().toString(),
        passwordLogin.getText().toString())
        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
          @Override
          public void onComplete(@NonNull Task<AuthResult> result) {
            progressDialog.dismiss();
            if (!result.isSuccessful()) {
              Toast.makeText(getApplicationContext(), result.getException().toString(),
                  Toast.LENGTH_LONG).show();
            } else {
              processCorrectPasswordAndUsername();
            }
          }
        });
  }


  /**
   * If it is the right password, move to the main activity
   */
  private void processCorrectPasswordAndUsername() {
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
  }


  /**
   * Sign in with facebook using Firebase
   *
   * @param token The token confirmation from Facebook
   */
  private void handleFacebookLoginToken(AccessToken token) {
    AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
    mAuth.signInWithCredential(credential).addOnCompleteListener(this,
        new OnCompleteListener<AuthResult>() {
          @Override
          public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
              processCorrectPasswordAndUsername();
            } else {
              Toast.makeText(getApplicationContext(), "Login failed: " + task.getException(),
                  Toast.LENGTH_LONG).show();
            }
          }
        });
  }

  /**
   * After running authorization with Facebook or , coming back to the main screen, this method will
   * be invoked Call Back Manager will get the data from log in activity an run the registration.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (FacebookSdk.isFacebookRequestCode(requestCode)) {
      callbackManager.onActivityResult(requestCode, resultCode, data);
    }
    if (requestCode == RC_SIGN_IN) {
      GoogleSignInResult googleSignIn = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
      if (googleSignIn.isSuccess()) {
        GoogleSignInAccount googleAccount = googleSignIn.getSignInAccount();
        signInWithGoogleThroughFirebase(googleAccount);
      }
    }
  }

  /**
   * This activity will start the background call to Google server to authenticate user
   */
  private void signInWithGoogleActivity() {
    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
    startActivityForResult(signInIntent, RC_SIGN_IN);
  }

  /**
   * After having the google account successfully authenticated by google, we will get it sign it
   * with our app through Firebase
   *
   * @param acct the google account we get from the user and authenticated by google
   */
  private void signInWithGoogleThroughFirebase(GoogleSignInAccount acct) {
    AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
    mAuth.signInWithCredential(credential).addOnCompleteListener(this,
        new OnCompleteListener<AuthResult>() {
          @Override
          public void onComplete(@NonNull Task<AuthResult> task) {
            if (!task.isSuccessful()) {
              Toast.makeText(getApplicationContext(), "Authentication failed: "
                  + task.getException(), Toast.LENGTH_LONG).show();
            } else {
              processCorrectPasswordAndUsername();
            }
          }
        });
  }

  /**
   * Call this when the user come BACK to the activity
   */
  @Override
  public void onStart() {
    super.onStart();
    mAuth.addAuthStateListener(mAuthListener);
    mGoogleApiClient.connect();
  }

  /**
   * Call this when the activity is no longer visible
   */
  @Override
  public void onStop() {
    super.onStop();
    if (mAuthListener != null) {
      mAuth.removeAuthStateListener(mAuthListener);
    }
    mGoogleApiClient.disconnect();
  }

  /**
   * Call this when the user resume to the activity
   */
  @Override
  public void onResume() {
    super.onResume();
    mGoogleApiClient.connect();
  }

  /**
   * Call this when the user leave the activity
   */
  @Override
  public void onPause() {
    super.onPause();
    mGoogleApiClient.disconnect();
  }
}

