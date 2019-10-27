package ru.eva.hackmoscow.activity.main;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.venues3d.BaseLocation;
import com.here.android.mpa.venues3d.VenueService;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import ru.eva.hackmoscow.OnGeodataRecieved;
import ru.eva.hackmoscow.model.Feature;
import ru.eva.hackmoscow.model.Geodata;

class ContractMain {
    interface View {
        void initializeView();

        void initializeMap();

        void showToast(String text);

        void initSuccess();

        void initError(String message);

        void initResult();

        void openVenueAsync(String query);

        void addToRoute(BaseLocation outdoorLocation);

        void setMarkers(List<Feature> featureList);
    }

    interface Presenter {
        void checkPermission(Activity activity);

        void checkMapPermission(PackageManager packageManager, Activity activity);

        boolean onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, Activity activity);

        void checkMapInitError(OnEngineInitListener.Error error);

        void checkMapInitResult(VenueService.InitStatus result);

        void checkInitComplete(AtomicBoolean m_initCompleted, String query);

        void calculateCenterScreenPoint(Map m_map, Context context);

        void getGeodata(String spaceId, String id);
    }

    interface Repository {
        void getGeodata(String spaceId, String id, OnGeodataRecieved onGeodataRecieved);
    }
}
