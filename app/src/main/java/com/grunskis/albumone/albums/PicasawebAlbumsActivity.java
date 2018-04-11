package com.grunskis.albumone.albums;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.AccountPicker;
import com.grunskis.albumone.R;
import com.grunskis.albumone.data.source.remote.PicasaWebDataSource;

import timber.log.Timber;

public class PicasawebAlbumsActivity extends RemoteAlbumsActivity {
    private final int PICK_ACCOUNT_REQUEST = 1;
    private final int REQUEST_AUTHENTICATE = 2;

    private AccountManager mAccountManager;
    private Account mSelectedAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.backend_google_photos));

        mRemoteDataSource = PicasaWebDataSource.getInstance(this);
        if (mRemoteDataSource.isAuthenticated()) {
            loadAlbums(savedInstanceState == null);
        } else {
            authenticate();
        }
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

                    Timber.i("Selected Account %s %s", accountName, accountType);
                    mSelectedAccount = new Account(accountName, accountType);

                    mAccountManager.getAuthToken(
                            mSelectedAccount,                     // Account retrieved using getAccountsByType()
                            "lh2",            // Auth scope
                            null,                        // Authenticator-specific options
                            this,                           // Your activity
                            new OnTokenAcquired(),          // Callback called when a token is successfully acquired
                            null);    // Callback called if an error occ
                } else {
                    finish();
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
                    if (intent != null) {
                        int flags = intent.getFlags();
                        flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
                        intent.setFlags(flags);
                        startActivityForResult(intent, REQUEST_AUTHENTICATE);
                    }
                    return;
                }

                if (b.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                    String authToken = b.getString(AccountManager.KEY_AUTHTOKEN);
                    mRemoteDataSource.setAuthToken(authToken);
                    loadAlbums(true);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }
}
