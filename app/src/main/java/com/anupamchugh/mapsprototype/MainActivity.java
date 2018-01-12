package com.anupamchugh.mapsprototype;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FrameLayout frameLayout;
    FirebaseUser mCurrentUser;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    selectFragment(0, mCurrentUser);
                    return true;
                case R.id.navigation_dashboard:
                    selectFragment(1, mCurrentUser);
                    return true;

            }
            return false;
        }
    };

    private BottomNavigationView.OnNavigationItemReselectedListener onNavigationItemReselectedListener
            = new BottomNavigationView.OnNavigationItemReselectedListener() {

        @Override
        public void onNavigationItemReselected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    selectFragment(0, mCurrentUser);
                    break;
                case R.id.navigation_dashboard:
                    selectFragment(1, mCurrentUser);
                    break;

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {

            Log.d("API123", "CURRENT USER IS NULL");
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d("API123", "signInAnonymously:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                updateUI(user);
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.d("API123", "signInAnonymously:failure", task.getException());
                                Toast.makeText(getApplicationContext(), "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                updateUI(null);
                            }
                        }
                    });
        } else {
            Log.d("API123", "CURRENT USER IS NOT NULL");
            updateUI(currentUser);
        }


    }


    private void updateUI(FirebaseUser user) {
        if (user != null) {
            mCurrentUser = user;
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("message");

            myRef.setValue("Hello, World!");

            frameLayout = findViewById(R.id.frameLayout);
            BottomNavigationView navigation = findViewById(R.id.navigation);
            navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
            navigation.setOnNavigationItemReselectedListener(onNavigationItemReselectedListener);
            selectFragment(0, mCurrentUser);
        }
    }


    protected void selectFragment(int i, FirebaseUser mCurrentUser) {

        switch (i) {
            case 0:
                // Action to perform when Home Menu item is selected.
                pushFragment(MyFragment.newInstance("Google Maps Page 1", true, false, mCurrentUser.getUid()));
                getSupportActionBar().setTitle("Current Location");
                break;
            case 1:
                // Action to perform when Bag Menu item is selected.
                pushFragment(MyFragment.newInstance("Google Maps Page 2", false, true, mCurrentUser.getUid()));
                getSupportActionBar().setTitle("Any Location");
                break;
        }
    }

    protected void pushFragment(Fragment fragment) {
        if (fragment == null)
            return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            if (ft != null) {
                ft.replace(R.id.frameLayout, fragment);
                ft.commit();
            }
        }
    }


}
