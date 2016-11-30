package com.example.zacharius.sma;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity
{
    private final int MAX_LOGIN_ATTEMPTS = 5; //max number of times a user can attempt to login
    private final int LOGOUT_TIME = 5*60;  //number of seconds user is logged out of system after exceeding logout timer

    private int loginAttempts; //number of times user has tried to login
    private static boolean logIn;//tells us whether user is currently able to attempt login
    private static int secondsLeft_logout;//how many more seconds will user be logged out

    public static Context context;

    private ServerComm server;

    public  static int credentials = 0;//0 - waiting for auth response
                               //1 - auth came back true
                               //2 - auth came back false
    public static String errMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        context = getApplicationContext();

        loginAttempts = 0;
        logIn = true;

        server = new ServerComm("198.27.65.177", 4269);
        Intent serverListener = new Intent(getApplicationContext(), ServerComm.ServerListener.class);
        getApplicationContext().startService(serverListener);
    }


    //executed when user presses SMA button on Login Page
    public void onEnterCredentials(View v)
    {


        //grab EditTexts from Login Page
        EditText idView =  (EditText) findViewById(R.id.ID);
        EditText passwordView = (EditText) findViewById(R.id.password);

        //grab user entered id and password
        String id = idView.getText().toString();
        String password = passwordView.getText().toString();

        //clear fields in both EditTexts
        idView.setText("");
        passwordView.setText("");

        Log.d("Login activity", "beggining login action");

        Login login = new Login();
        login.execute(id, password);

        Intent serverListener = new Intent(getApplicationContext(), ServerComm.ServerListener.class);
        getApplicationContext().startService(serverListener);

    }

    //allow timer to check seconds left
    public static int getSecondsLeft()
    {
        return secondsLeft_logout;
    }

    //allow timer to change seconds left
    public static void setSecondsLeft(int secondsLeft)
    {
        secondsLeft_logout = secondsLeft;
    }

    //allow timer to change logIn
    public static void setLogin(boolean login)
    {
        LoginActivity.logIn = login;
    }

    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class Login extends AsyncTask<String, Void, Boolean>
    {


        String loginMsg;

        private Login(){}

        @Override
        protected Boolean doInBackground(String... strings)
        {
            String id = strings[0];
            String password = strings[1];
            

            if(server.getServer() != null)
            {
                if(logIn){

                    Log.d("login","login true");
                    server.checkCredentials(id, password);

                    while(credentials == 0)
                    {

                    };

                    if(credentials == 1)
                    {
                        Log.d("Login", "login passed");
                        return true;


                    } else if(++loginAttempts > MAX_LOGIN_ATTEMPTS)
                    {
                        logIn = false;
                        secondsLeft_logout = LOGOUT_TIME;
                        (new Thread(new LogoutTimer())).start();
                        loginMsg = "Invalid login, you have exceeded max login attempt limit and will be logged out for: " + (LOGOUT_TIME/60) + " minutes";
                    }
                    //ask user to try login attempt again
                    else
                    {
                        loginMsg = "Invalid login, try again\n Attempts Left: " + (MAX_LOGIN_ATTEMPTS - loginAttempts );
                        publishProgress();
                    }
                }
                //tell user how long it is till he can login again
                else
                {
                    loginMsg = "You are currently suspended from system\n Seconds Left : " + secondsLeft_logout;
                    publishProgress();
                }
            }
            else
            {
                loginMsg = "can't login; not connected to server";
                publishProgress();
            }



            return false;

        }

        @Override
        protected void onProgressUpdate(Void... values)
        {
            super.onProgressUpdate(values);
            Log.d("Login", loginMsg);
            if(LoginActivity.context != null)
            {
                Toast.makeText(context,
                        loginMsg ,
                        Toast.LENGTH_SHORT)
                        .show();
            }

        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            super.onPostExecute(result);

            if(result)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Intent i = new Intent(context, ContactListActivity.class);
                        startActivity(i);
                    }
                });

            }

        }
    }
}








