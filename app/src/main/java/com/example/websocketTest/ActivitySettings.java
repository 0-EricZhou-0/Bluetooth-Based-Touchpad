package com.example.websocketTest;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

public class ActivitySettings extends AppCompatActivity {

    TabLayout tabContainer;
    ViewPager viewPager;
    TabItem generalSettingsTab;
    TabItem mappingTab;
    PageAdapter pageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        tabContainer = findViewById(R.id.tabContainer);
        viewPager = findViewById(R.id.viewPager);
        generalSettingsTab = findViewById(R.id.generalSettingsTab);
        mappingTab = findViewById(R.id.mappingTab);

        pageAdapter = new PageAdapter(getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
                getIntent().getBooleanExtra("changeDevicePermitted", true));
        viewPager.setAdapter(pageAdapter);

        tabContainer.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                Controls.SettingDetail.setCurrentSettingTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabContainer));

        Objects.requireNonNull(tabContainer.getTabAt(Controls.SettingDetail.getCurrentSettingTab())).select();

    }

    @Override
    public void onBackPressed() {
        Controls.updateAllSetting(true);
        finish();
    }
}
