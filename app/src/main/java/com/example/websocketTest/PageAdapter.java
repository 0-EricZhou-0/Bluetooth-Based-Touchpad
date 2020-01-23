package com.example.websocketTest;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

class PageAdapter extends FragmentPagerAdapter {

    private final int PAGE_COUNT = 3;
    private boolean changeDevicePermitted;
    PageAdapter(@NonNull FragmentManager fm, int behavior, boolean isPermitted) {
        super(fm, behavior);
        changeDevicePermitted = isPermitted;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position >= PAGE_COUNT) throw new IllegalArgumentException();
        switch (position) {
            case 0:
                return new ConnectionSettingFragment(changeDevicePermitted);
            case 1:
                return new GeneralSettingFragment();
            default:
                return new MappingFragment();
        }
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
