package com.danilov.supermanga.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.danilov.supermanga.R;
import com.danilov.supermanga.core.application.ApplicationSettings;
import com.danilov.supermanga.core.application.MangaApplication;
import com.danilov.supermanga.core.dialog.CustomDialog;
import com.danilov.supermanga.core.dialog.RateDialog;
import com.danilov.supermanga.core.notification.headsupold.remote.impl.RemoteButton;
import com.danilov.supermanga.core.onlinestorage.GoogleDriveConnector;
import com.danilov.supermanga.core.onlinestorage.OnlineStorageConnector;
import com.danilov.supermanga.core.service.OnlineStorageProfileService;
import com.danilov.supermanga.core.service.ServiceConnectionListener;
import com.danilov.supermanga.core.util.Constants;
import com.danilov.supermanga.core.util.Utils;
import com.danilov.supermanga.core.view.UnderToolbarScrollView;
import com.danilov.supermanga.core.view.ViewV16;
import com.danilov.supermanga.core.widget.RelativeTimeTextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.software.shell.fab.ActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Created by Semyon on 28.08.2015.
 */
public class ProfileActivity extends BaseToolbarActivity {

    private UnderToolbarScrollView scrollView;
    private ActionButton takePhoto;
    private ViewGroup fakeBar;
    private ViewGroup fakeBarInner;
    private TextView userNameTextView;

    private View googleSyncCard;
    private View userNameCard;
    private View googleSyncButton;
    private View emailCard;
    private RelativeTimeTextView googleAccountTextView;

    private TextView userNameSmall;
    private TextView email;
    private TextView timeRead;
    private TextView downloadPath;
    private TextView mangasComplete;
    private TextView megabytesDownloaded;
    private SwitchCompat showBtnsSwitch;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        fakeBar = findViewWithId(R.id.fake_bar);
        takePhoto = findViewWithId(R.id.take_photo);
        userNameTextView = findViewWithId(R.id.user_name);
        googleSyncCard = findViewWithId(R.id.google_sync_card);
        userNameCard = findViewWithId(R.id.user_name_card);
        emailCard = findViewWithId(R.id.email_card);
        googleAccountTextView = findViewWithId(R.id.google_account);
        googleSyncButton = findViewWithId(R.id.google_sync_button);

        userNameSmall = findViewWithId(R.id.user_name_small);
        email = findViewWithId(R.id.email);
        timeRead = findViewWithId(R.id.time_read);
        downloadPath = findViewWithId(R.id.download_path);
        mangasComplete = findViewWithId(R.id.mangas_complete);
        megabytesDownloaded = findViewWithId(R.id.megabytes_downloaded);
        showBtnsSwitch = findViewWithId(R.id.show_btns_switch);

        final View rootView = getWindow().getDecorView().findViewById(android.R.id.content);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ViewV16 takePhotoView = ViewV16.wrap(takePhoto);
                takePhotoView.setPivotX(takePhoto.getWidth() / 2);
                takePhotoView.setPivotY(takePhoto.getHeight() / 2);
                scrollView.setListener(new UnderToolbarScrollView.UnderToolbarScrollListener() {

                    final int USER_NAME_MARGIN_BOTTOM = Utils.dpToPx(16);
                    final int MARGIN_TOP = Utils.dpToPx(58);
                    final int MARGIN_LEFT = Utils.dpToPx(48);
                    final int MAX_HEIGHT = fakeBar.getHeight();
                    final int MIN_HEIGHT = Utils.dpToPx(58);
                    final int EXTRA_HEIGHT = MAX_HEIGHT - MIN_HEIGHT;

                    @Override
                    public int onScroll(final int x, final int y) {

                        int height = Math.max(MIN_HEIGHT, MAX_HEIGHT - y);
                        height = Math.min(MAX_HEIGHT, height);

                        RelativeLayout.LayoutParams fakeBarParameters = (RelativeLayout.LayoutParams) fakeBar.getLayoutParams();
                        fakeBarParameters.height = height;
                        fakeBar.setLayoutParams(fakeBarParameters);


                        int curExtra = height - MIN_HEIGHT;
                        float percentage = (float) curExtra / EXTRA_HEIGHT;

                        takePhotoView.setScaleX(percentage);
                        takePhotoView.setScaleY(percentage);
                        int marginTop = (int) (percentage * MARGIN_TOP);
                        int marginLeft = (int) ((1 - percentage) * MARGIN_LEFT);
                        RelativeLayout.LayoutParams fakeBarInnerParameters = (RelativeLayout.LayoutParams) fakeBarInner.getLayoutParams();
                        fakeBarInnerParameters.topMargin = marginTop;
                        fakeBarInnerParameters.leftMargin = marginLeft;
                        fakeBarInner.setLayoutParams(fakeBarInnerParameters);


                        RelativeLayout.LayoutParams userNameParams = (RelativeLayout.LayoutParams) userNameTextView.getLayoutParams();
                        int marginBottom = (int) (percentage * USER_NAME_MARGIN_BOTTOM);
                        userNameParams.bottomMargin = marginBottom;
                        userNameTextView.setLayoutParams(userNameParams);


                        return 0;
                    }
                });
                rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fakeBarInner = findViewWithId(R.id.fake_bar_inner);
        scrollView = findViewWithId(R.id.scroll_view);
        googleSyncCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (service != null) {
                    service.connect();
                }
            }
        });
        googleSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("TEST", true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (service == null) {
                    return;
                }
                service.sendDataViaGoogle();
            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long lastUpdateTime = sharedPreferences.getLong(Constants.Settings.LAST_UPDATE_PROFILE_TIME, -1L);
        String googleProfileName = sharedPreferences.getString(Constants.Settings.GOOGLE_PROFILE_NAME, null);
        if (googleProfileName == null) {
            googleProfileName = "Нажмите, чтобы подключить";
            googleAccountTextView.setPrefix(googleProfileName);
            googleAccountTextView.setReferenceTime(RelativeTimeTextView.ONLY_PREFIX);
        } else {
            String prefix =  googleProfileName + " (" + getString(R.string.updated) + " ";
            googleAccountTextView.setPrefix(prefix);
            googleAccountTextView.setReferenceTime(lastUpdateTime);
            googleAccountTextView.setSuffix(")");
        }

        init();
    }


    private void init() {
        ApplicationSettings.UserSettings userSettings = ApplicationSettings.get(this).getUserSettings();

        final String userNameString = userSettings.getUserName();
        final String emailString = userSettings.getEmail();
        long timeReadLong = userSettings.getTimeRead();
        String downloadPathString = userSettings.getDownloadPath();
        int mangasCompleteInt = userSettings.getMangasComplete();
        long megabytesDownloadedLong = userSettings.getMegabytesDownloaded();
        boolean alwaysShowButtons = userSettings.isAlwaysShowButtons();

        long hours = TimeUnit.HOURS.convert(timeReadLong, TimeUnit.MILLISECONDS);

        userNameTextView.setText(userNameString);
        userNameSmall.setText(userNameString);
        email.setText(emailString);
        timeRead.setText(+ hours + " " + getString(R.string.hours));
        downloadPath.setText(downloadPathString);
        mangasComplete.setText(mangasCompleteInt + "");
        megabytesDownloaded.setText(megabytesDownloadedLong + "");
        showBtnsSwitch.setChecked(alwaysShowButtons);


        userNameCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                ValueDialogFragment dialogFragment = ValueDialogFragment.createDialog(getString(R.string.username), userNameString, Constants.Settings.USER_NAME);
                dialogFragment.show(getSupportFragmentManager(), ValueDialogFragment.TAG);
            }
        });
        emailCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                ValueDialogFragment dialogFragment = ValueDialogFragment.createDialog(getString(R.string.email), emailString, Constants.Settings.EMAIL);
                dialogFragment.show(getSupportFragmentManager(), ValueDialogFragment.TAG);
            }
        });
    }

    private ServiceConnection serviceConnection;
    private OnlineStorageProfileService service;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case OnlineStorageProfileService.GOOGLE_CONNECTED:
                    googleSyncButton.setVisibility(View.VISIBLE);
                    if (service != null) {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        long lastUpdateTime = sharedPreferences.getLong(Constants.Settings.LAST_UPDATE_PROFILE_TIME, -1L);
                        String accountName = service.getGoogleConnector().getAccountName();
                        String prefix =  accountName + " (" + getString(R.string.updated) + " ";
                        googleAccountTextView.setPrefix(prefix);
                        googleAccountTextView.setReferenceTime(lastUpdateTime);
                        googleAccountTextView.setSuffix(")");

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(Constants.Settings.GOOGLE_PROFILE_NAME, accountName).apply();

                    }
                    break;
                case OnlineStorageProfileService.GOOGLE_NEED_CONFIRMATION:
                    ConnectionResult connectionResult = (ConnectionResult) msg.obj;
                    if (!connectionResult.hasResolution()) {
                        // show the localized error dialog.
                        GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), ProfileActivity.this, 0).show();
                        return;
                    }

                    try {
                        connectionResult.startResolutionForResult(ProfileActivity.this, GOOGLE_AUTH_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                    break;
                case OnlineStorageProfileService.GOOGLE_SENT_SUCCESS:

                    break;
            }
        }
    };

    private static final int GOOGLE_AUTH_REQUEST_CODE = 1;

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_AUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                service.connect();
            }
        }
    }

    @Override
    protected void onResume() {
        serviceConnection = OnlineStorageProfileService.bindService(this, new ServiceConnectionListener<OnlineStorageProfileService>() {
            @Override
            public void onServiceConnected(final OnlineStorageProfileService service) {
                ProfileActivity.this.service = service;
                service.setServiceHandler(handler);
            }

            @Override
            public void onServiceDisconnected(final OnlineStorageProfileService service) {

            }
        });
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (serviceConnection != null && service != null) {
            unbindService(serviceConnection);
            service = null;
        }
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    public static class ValueDialogFragment extends DialogFragment {

        public static final String TAG = "ValueDialogFragment";

        public static final String TITLE = "TITLE";
        public static final String VALUE = "VALUE";
        public static final String PARAMETER_NAME = "PARAMETER_NAME";

        private String title = "";
        private String value = "";
        private String parameterName = "";

        private EditText editText = null;

        public ValueDialogFragment() {
            super();
        }

        public static ValueDialogFragment createDialog(final String title, final String value, final String parameterName) {
            ValueDialogFragment valueDialogFragment = new ValueDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString(TITLE, title);
            bundle.putString(VALUE, value);
            bundle.putString(PARAMETER_NAME, parameterName);
            valueDialogFragment.setArguments(bundle);
            return valueDialogFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            Bundle b = getArguments();
            if (savedInstanceState != null) {
                b = savedInstanceState;
            }

            title = b.getString(TITLE);
            value = b.getString(VALUE);
            parameterName = b.getString(PARAMETER_NAME);

            CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
            builder.setPositiveButton(getString(R.string.sv_ok), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    Context context = MangaApplication.getContext();
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    preferences.edit().putString(parameterName, editText.getText().toString()).apply();
                    ApplicationSettings.get(context).invalidate(context);
                    ProfileActivity activity = (ProfileActivity) getActivity();
                    if (activity != null) {
                        activity.init();
                    }
                    dismiss();
                }

            });
            builder.setNegativeButton(getString(R.string.sv_cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(final DialogInterface dialogInterface, final int i) {
                    dismiss();
                }

            });
            LayoutInflater layoutInflater = getActivity().getLayoutInflater();
            View contentView = layoutInflater.inflate(R.layout.dialog_enter_string, null);
            editText = (EditText) contentView.findViewById(R.id.value);
            editText.setText(value);
            builder.setView(contentView);
            builder.setTitle(title);
            return builder.build();
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
        @Override
        public void onSaveInstanceState(final Bundle outState) {
            outState.putString(TITLE, title);
            outState.putString(VALUE, editText.getText().toString());
            outState.putString(PARAMETER_NAME, parameterName);
            super.onSaveInstanceState(outState);
        }


    }

}
