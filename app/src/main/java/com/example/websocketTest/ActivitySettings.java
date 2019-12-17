package com.example.websocketTest;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class ActivitySettings extends AppCompatActivity {

    private static SparseArray<Controls.TaskDetail> currentMappings;
    private static ArrayList<Controls.SettingDetail> currentSettings;

    static ArrayList<Controls.SettingDetail> getCurrentSettings() {
        return currentSettings;
    }

    static SparseArray<Controls.TaskDetail> getCurrentMappings() {
        return currentMappings;
    }

    SparseBooleanArray currentGeneralSetting = Controls.getCurrentSettingStatus();

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

        pageAdapter = new PageAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(pageAdapter);

        tabContainer.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabContainer));

        currentMappings = Controls.getCurrentMapping();
        currentSettings = Controls.getCurrentSetting();
    }

    @Override
    public void onBackPressed() {
        Controls.setCurrentSettingStatus(currentGeneralSetting);
        Controls.remapping(currentMappings);
        finish();
    }
}
