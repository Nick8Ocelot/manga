package com.danilov.mangareaderplus.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.test.UiThreadTest;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.danilov.mangareaderplus.R;
import com.danilov.mangareaderplus.core.application.ApplicationSettings;
import com.danilov.mangareaderplus.core.database.DatabaseAccessException;
import com.danilov.mangareaderplus.core.database.UpdatesDAO;
import com.danilov.mangareaderplus.core.dialog.CustomDialog;
import com.danilov.mangareaderplus.core.dialog.CustomDialogFragment;
import com.danilov.mangareaderplus.core.util.DrawerStub;
import com.danilov.mangareaderplus.core.util.Promise;
import com.danilov.mangareaderplus.core.util.ServiceContainer;
import com.danilov.mangareaderplus.core.util.Utils;
import com.danilov.mangareaderplus.fragment.DownloadManagerFragment;
import com.danilov.mangareaderplus.fragment.DownloadedMangaFragment;
import com.danilov.mangareaderplus.fragment.FavoritesFragment;
import com.danilov.mangareaderplus.fragment.HistoryMangaFragment;
import com.danilov.mangareaderplus.fragment.MainFragment;
import com.danilov.mangareaderplus.fragment.RepositoryPickerFragment;
import com.danilov.mangareaderplus.fragment.SettingsFragment;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

/**
 * Created by Semyon Danilov on 07.10.2014.
 */
public class MainActivity extends BaseToolbarActivity {

    private static final String FIRST_LAUNCH = "FIRST_LAUNCH";

    private UpdatesDAO updatesDAO = null;

    private View drawerLayout;

    private View drawerMenu;

    private DrawerLayout castedDrawerLayout;

    private ActionBarDrawerToggle drawerToggle;

    private ListView drawerList;

    private boolean isOnMainFragment = false;

    private boolean isLargeLandscape = false;

    private int updatesQuantity = 0;

    private DrawerListAdapter adapter;

    private boolean firstLaunch = true;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manga_main_activity);

        updatesDAO = ServiceContainer.getService(UpdatesDAO.class);
        drawerLayout = findViewById(R.id.drawer_layout);
        isLargeLandscape = findViewById(R.id.is_large) != null; //dealing with landscape pads
        drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerMenu = findViewWithId(R.id.drawer_menu);
        // Set the adapter for the list view
        adapter = new DrawerListAdapter(this, R.layout.drawer_menu_item, DrawerMenuItem.values());
        drawerList.setAdapter(adapter);
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        if (!isLargeLandscape) {
            castedDrawerLayout = (DrawerLayout) drawerLayout;
            drawerToggle = new ActionBarDrawerToggle(this, castedDrawerLayout, R.string.sv_drawer_open, R.string.sv_drawer_close) {

                /** Called when a drawer has settled in a completely closed state. */
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                }

                /** Called when a drawer has settled in a completely open state. */
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                }

            };
            // Set the drawer toggle as the DrawerListener
            castedDrawerLayout.setDrawerListener(drawerToggle);

        } else {
            castedDrawerLayout = new DrawerStub(this);
        }
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        final ApplicationSettings applicationSettings = ApplicationSettings.get(this.getApplicationContext());
        firstLaunch = applicationSettings.isFirstLaunch();
        boolean tutorialMenuPassed = applicationSettings.isTutorialMenuPassed();
        //TODO: check if we need to display main fragment or we need to show restored
        if (firstLaunch) {
            if (savedInstanceState == null) {
                showMainFragment();
            }
        } else {
            if (savedInstanceState == null) {
                showRepositoryPickerFragment();
            }
        }
        syncToggle();
        updateQuantity();

        if (!tutorialMenuPassed) {
            View fakeMenuOpenBtn = findViewById(R.id.fake_home);
            if (fakeMenuOpenBtn != null) {
                ShowcaseView showcaseView = new ShowcaseView.Builder(this)
                        .setTarget(new ViewTarget(findViewById(R.id.fake_home)))
                        .setContentTitle(getString(R.string.tutorial_menu_title))
                        .setStyle(R.style.ShowcaseView_Dark)
                        .setContentText(getString(R.string.tutorial_menu_text))
                        .hideOnTouchOutside()
                        .build();
                showcaseView.setButtonPosition(Utils.getRightParam(this, getResources()));
                showcaseView.setOnShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(final ShowcaseView showcaseView) {
                        View v = (View) showcaseView.getParent();
                        applicationSettings.setTutorialMenuPassed(true);
                        applicationSettings.update(getApplicationContext());
                    }

                    @Override
                    public void onShowcaseViewDidHide(final ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewShow(final ShowcaseView showcaseView) {

                    }
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private CustomDialogFragment dialogFragment = null;

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        outState.putBoolean(FIRST_LAUNCH, firstLaunch);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        firstLaunch = savedInstanceState.getBoolean(FIRST_LAUNCH);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @UiThreadTest
    private void testShowHeadsUp() {
//        HeadsUpNotification headsUpNotification = new HeadsUpNotification(getApplicationContext(), 1, R.layout.test_headsup_notification);
//        headsUpNotification.show();
    }

    public void updateQuantity() {
        Promise<Integer> promise = Promise.run(new Promise.PromiseRunnable<Integer>() {
            @Override
            public void run(final Promise<Integer>.Resolver resolver) {
                try {
                    updatesQuantity = updatesDAO.getUpdatesQuantity();
                    resolver.resolve(updatesQuantity);
                } catch (DatabaseAccessException e) {
                    e.printStackTrace();
                    resolver.except(e);
                }
            }
        }, true);
        promise.then(new Promise.Action<Integer, Void>() {
            @Override
            public Void action(final Integer data, final boolean success) {
                adapter.notifyDataSetChanged();
                return null;
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (castedDrawerLayout.isDrawerOpen(drawerMenu)) {
                    castedDrawerLayout.closeDrawer(drawerMenu);
                } else {
                    castedDrawerLayout.openDrawer(drawerMenu);
                }
                if (drawerToggle != null) {
                    drawerToggle.syncState();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }

    private enum DrawerMenuItem {

        UPDATES(R.drawable.ic_action_new, R.string.menu_updates),
        SEARCH(R.drawable.ic_action_search, R.string.menu_search),
        HISTORY(R.drawable.ic_action_time, R.string.menu_history),
        FAVORITE(R.drawable.ic_action_important, R.string.menu_favorite),
        LOCAL(R.drawable.ic_action_downloads, R.string.menu_local),
        DOWNLOAD_MANAGER(R.drawable.ic_download_manager, R.string.menu_download),
        SETTINGS(R.drawable.ic_action_settings, R.string.menu_settings);

        private int iconId;

        private int stringId;

        private boolean isSelected = false;

        private DrawerMenuItem(final int iconId, final int stringId) {
            this.iconId = iconId;
            this.stringId = stringId;
        }

        public void setSelected(final boolean isSelected) {
            this.isSelected = isSelected;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public int getIconId() {
            return iconId;
        }

        public int getStringId() {
            return stringId;
        }

    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {

        private DrawerMenuItem prevSelected = null;

        @Override
        public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
            if (prevSelected != null) {
                prevSelected.setSelected(false);
            }
            DrawerMenuItem item = DrawerMenuItem.values()[position];
            view.setSelected(true);
            item.setSelected(true);
            prevSelected = item;
            Intent intent = null;
            switch (item) {
                case UPDATES:
                    showMainFragment();
                    break;
                case SEARCH:
                    showRepositoryPickerFragment();
                    break;
                case HISTORY:
                    showHistoryFragment();
                    break;
                case FAVORITE:
                    showFavoriteMangaFragment();
                    break;
                case LOCAL:
                    showDownloadedMangaFragment();
                    break;
                case DOWNLOAD_MANAGER:
                    showDownloadManagerFragment();
                    break;
                case SETTINGS:
                    showSettingsFragment();
                    break;
            }
            syncToggle();
            closeDrawer();
        }

    }

    private void syncToggle() {
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
        ActionBar actionBar = getSupportActionBar();
        if (drawerToggle != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            drawerToggle.setDrawerIndicatorEnabled(true);
        } else {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    private void closeDrawer() {
        castedDrawerLayout.closeDrawer(drawerMenu);
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }

    private void showRepositoryPickerFragment() {
        RepositoryPickerFragment fragment = RepositoryPickerFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
        }
        isOnMainFragment = false;
    }

    private void showDownloadedMangaFragment() {
        Fragment fragment = DownloadedMangaFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
        }
        isOnMainFragment = false;
    }

    private void showFavoriteMangaFragment() {
        Fragment fragment = FavoritesFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
        }
        isOnMainFragment = false;
    }

    private void showDownloadManagerFragment() {
        Fragment fragment = DownloadManagerFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
        }
        isOnMainFragment = false;
    }

    private void showSettingsFragment() {
        Fragment fragment = SettingsFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
        }
        isOnMainFragment = false;
    }

    private void showHistoryFragment() {
        Fragment fragment = HistoryMangaFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
        }
        isOnMainFragment = false;
    }

    private void showMainFragment() {
        Fragment fragment = MainFragment.newInstance(firstLaunch);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(true);
        }
        isOnMainFragment = true;
    }

    public void changeUpdatesQuantity(final int updatesQuantity) {
        this.updatesQuantity = updatesQuantity;
        adapter.notifyDataSetChanged();
    }

    private class DrawerListAdapter extends ArrayAdapter<DrawerMenuItem> {

        private DrawerMenuItem[] objects;
        private int resourceId;
        private Context context;

        @Override
        public int getCount() {
            return objects.length;
        }

        public DrawerListAdapter(final Context context, final int resource, final DrawerMenuItem[] objects) {
            super(context, resource, objects);
            this.context = context;
            this.objects = objects;
            this.resourceId = resource;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(resourceId, parent, false); //attaching to parent == (false), because it attaching later by android
            }
            Object tag = view.getTag();
            ViewHolder holder = null;
            TextView text;
            ImageView icon;
            if (tag != null) {
                holder = (ViewHolder) tag;
                text = holder.text;
                icon = holder.icon;
            } else {
                holder = new ViewHolder();
                text = (TextView) view.findViewById(R.id.text);
                icon = (ImageView) view.findViewById(R.id.icon);
                holder.text = text;
                holder.icon = icon;
            }
            DrawerMenuItem item = objects[position];
            if (item.isSelected()) {
                view.setSelected(true);
            } else {
                view.setSelected(false);
            }
            if (item == DrawerMenuItem.UPDATES) {
                TextView quantityNew = (TextView) view.findViewById(R.id.quantity_new);
                quantityNew.setVisibility(View.VISIBLE);
                quantityNew.setText(updatesQuantity + "");
            } else {
                TextView quantityNew = (TextView) view.findViewById(R.id.quantity_new);
                quantityNew.setVisibility(View.INVISIBLE);
            }
            text.setText(context.getString(item.getStringId()));
            icon.setImageResource(item.getIconId());
            return view;
        }

        private class ViewHolder {
            public TextView text;
            public ImageView icon;
        }

    }

}