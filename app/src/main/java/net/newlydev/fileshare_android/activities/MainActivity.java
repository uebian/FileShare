package net.newlydev.fileshare_android.activities;

import android.content.*;
import android.content.pm.*;
import android.os.*;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.preference.*;

import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;

import com.google.android.gms.ads.*;

import java.io.*;

import net.newlydev.fileshare_android.*;
import net.newlydev.fileshare_android.fragments.*;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import net.newlydev.fileshare_android.R;

public class MainActivity extends mActivity {
    public boolean waiting;
    public ProgressBar pb;
    public ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AdView adview = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adview.loadAd(adRequest);
        final Fragment statusFragment = new MainFragment();
        final Fragment aboutFragment = new AboutFragment();
        final Fragment settingFragment = new SettingFragment();
        final FragmentManager fragmentManager = getSupportFragmentManager();
        waiting = false;
        Toolbar toolbar = findViewById(R.id.toolbar_normal);
        lv = findViewById(R.id.activity_main_list);
        pb = findViewById(R.id.activity_main_waitingprogressbar);
        pb.setVisibility(View.GONE);
        ArrayAdapter<String> aa = new ArrayAdapter<String>(this, R.layout.list_text);
        aa.add("首页");
        aa.add("设置");
        aa.add("关于");
        lv.setAdapter(aa);

        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
        File varfile = new File(getFilesDir(), "var");
        if (!varfile.exists()) {
            fragmentManager.beginTransaction().replace(R.id.activity_main_content, settingFragment).commit();
            try {
                varfile.createNewFile();

                DataOutputStream varos = new DataOutputStream(new FileOutputStream(varfile));
                varos.writeInt(info.versionCode);
                varos.close();
            } catch (Exception e) {
            }
            Toast.makeText(this, "首次使用，请先设置", Toast.LENGTH_SHORT).show();
            lv.setItemChecked(1, true);
        } else {
            try {
                DataInputStream varis = new DataInputStream(new FileInputStream(varfile));
                if (varis.readInt() < info.versionCode) {
                    varis.close();
                    varfile.delete();
                    varfile.createNewFile();
                    DataOutputStream varos = new DataOutputStream(new FileOutputStream(varfile));
                    varos.writeInt(info.versionCode);
                    varos.close();
                } else {
                    varis.close();
                }
            } catch (Exception e) {
            }
            fragmentManager.beginTransaction().replace(R.id.activity_main_content, statusFragment).commit();
            lv.setItemChecked(0, true);
        }
        final DrawerLayout mDrawerLayout = findViewById(R.id.activity_main_dl);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open, R.string.close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
                switch (p3) {
                    case 0:
                        fragmentManager.beginTransaction().replace(R.id.activity_main_content, statusFragment).commit();
                        break;
                    case 1:
                        fragmentManager.beginTransaction().replace(R.id.activity_main_content, settingFragment).commit();
                        break;
                    case 2:
                        fragmentManager.beginTransaction().replace(R.id.activity_main_content, aboutFragment).commit();
                        break;
                }
                mDrawerLayout.closeDrawers();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!waiting) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Toast.makeText(this, "起始目录已更新", Toast.LENGTH_SHORT).show();
            if (getContentResolver().getPersistedUriPermissions().size() > 0) {
                getContentResolver().releasePersistableUriPermission(getContentResolver().getPersistedUriPermissions().get(0).getUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            getContentResolver().takePersistableUriPermission(data.getData(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            //PreferenceManager.getDefaultSharedPreferences(this).edit().putString("uriPath", data.getData().toString()).apply();
            Session.sessions.clear();
        }
    }

}
