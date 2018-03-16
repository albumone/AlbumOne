package com.grunskis.albumone.albums;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.AccountPicker;
import com.grunskis.albumone.data.source.remote.PicasaWebDataSource;

import timber.log.Timber;

public class PicasawebAlbumsActivity extends RemoteAlbumsActivity {
    private static final String BACKEND_NAME = "Google Photos";
    private static final String PREF_AUTH_TOKEN = "PREF_AUTH_TOKEN_GOOGLE_PHOTOS";

    private final int PICK_ACCOUNT_REQUEST = 1;
    private final int REQUEST_AUTHENTICATE = 2;

    AccountManager mAccountManager;
    Account mSelectedAccount;

    //SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(BACKEND_NAME);

        //Context context = PicasawebAlbumsActivity.this;
        String authToken = getAuthToken(this);
//        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//        String authToken = mSharedPreferences.getString(BACKEND_AUTH_TOKEN, null);
        if (authToken == null) {
            authenticate();
        } else {
            initPresenter(authToken);
        }
    }

    private void initPresenter(String authToken) {
        PicasaWebDataSource dataSource = PicasaWebDataSource.getInstance();
        dataSource.setAuthToken(authToken);
        createPresenter(dataSource);
    }

    @Override
    protected String getAuthTokenPreferenceKey() {
        return PREF_AUTH_TOKEN;
    }

    private void authenticate() {
        mAccountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);

        Intent intent = AccountPicker.newChooseAccountIntent(
                null, null, new String[]{"com.google"},
                false, null, null, null, null);

        startActivityForResult(intent, PICK_ACCOUNT_REQUEST);
    }

    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        switch (requestCode) {
            case PICK_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    String accountType = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
                    Timber.d("Selected Account %s %s", accountName, accountType);
                    mSelectedAccount = new Account(accountName, accountType);

                    mAccountManager.getAuthToken(
                            mSelectedAccount,                     // Account retrieved using getAccountsByType()
                            "lh2",            // Auth scope
                            null,                        // Authenticator-specific options
                            this,                           // Your activity
                            new OnTokenAcquired(),          // Callback called when a token is successfully acquired
                            null);    // Callback called if an error occ

                    // TODO: 3/16/2018 handle error
                }
                break;
            case REQUEST_AUTHENTICATE:
                if (resultCode == RESULT_OK) {
                    mAccountManager.getAuthToken(
                            mSelectedAccount,                     // Account retrieved using getAccountsByType()
                            "lh2",            // Auth scope
                            null,                        // Authenticator-specific options
                            this,                           // Your activity
                            new OnTokenAcquired(),          // Callback called when a token is successfully acquired
                            null);    // Callback called if an error occ
                }
                break;
        }
    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
            try {
                Bundle b = accountManagerFuture.getResult();

                if (b.containsKey(AccountManager.KEY_INTENT)) {
                    Intent intent = b.getParcelable(AccountManager.KEY_INTENT);
                    int flags = intent.getFlags();
                    intent.setFlags(flags);
                    flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK; // TODO: 3/16/2018 wtf?
                    startActivityForResult(intent, REQUEST_AUTHENTICATE);
                    return;
                }

                if (b.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                    String authToken = b.getString(AccountManager.KEY_AUTHTOKEN);
                    //mSharedPreferences.edit().putString(BACKEND_AUTH_TOKEN, authToken).apply();
                    setAuthToken(PicasawebAlbumsActivity.this, authToken);

//                    // TODO: 3/19/2018 make this dry
//                    PicasaWebDataSource dataSource = PicasaWebDataSource.getInstance();
//                    dataSource.setAuthToken(authToken);
//                    createPresenter(dataSource);
                    initPresenter(authToken);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
