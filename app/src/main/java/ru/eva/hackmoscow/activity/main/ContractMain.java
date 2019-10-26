package ru.eva.hackmoscow.activity.main;


import android.app.Activity;
import android.content.pm.PackageManager;

import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.venues3d.VenueService;

import androidx.annotation.NonNull;

class ContractMain {
    interface View {
        void initializeView();

        void initializeMap();

        void showToast(String text);

        void initSuccess();

        void initError(String message);

        void initResult();
    }

    interface Presenter {
        void checkPermission(Activity activity);

        void checkMapPermission(PackageManager packageManager, Activity activity);

        boolean onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, Activity activity);

        void checkMapInitError(OnEngineInitListener.Error error);

        void checkMapInitResult(VenueService.InitStatus result);
    }

    interface Repository {
    }
}
