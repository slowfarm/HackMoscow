package ru.eva.hackmoscow.activity.main;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.venues3d.OutdoorLocation;
import com.here.android.mpa.venues3d.VenueService;
import com.here.odnp.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import ru.eva.hackmoscow.OnGeodataRecieved;
import ru.eva.hackmoscow.model.Feature;
import ru.eva.hackmoscow.model.Geodata;

public class PresenterMain implements ContractMain.Presenter, OnGeodataRecieved {

    private ContractMain.View mView;
    private ContractMain.Repository mRepository;

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    PresenterMain(ContractMain.View mView) {
        this.mView = mView;
        this.mRepository = new RepositoryMain();
    }

    @Override
    public void checkPermission(Activity activity) {
        if (hasPermissions(activity, REQUIRED_SDK_PERMISSIONS)) {
            mView.initializeView();
        } else {
            ActivityCompat.requestPermissions(activity, REQUIRED_SDK_PERMISSIONS,
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    @Override
    public void checkMapPermission(PackageManager packageManager, Activity activity) {
        String intentName = "";
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            intentName = bundle.getString("INTENT_NAME");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.getClass().toString(), "Failed to find intent name, NameNotFound: " + e.getMessage());
        }

        String diskCacheRoot = activity.getFilesDir().getPath() + File.separator + ".isolated-here-maps";

        if (com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(diskCacheRoot, intentName)) {
            mView.initializeMap();
        }
    }

    @Override
    public boolean onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, Activity activity) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            for (int index = 0; index < permissions.length; index++) {
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[index])) {
                        mView.showToast("Required permission " + permissions[index] + " not granted. " + "Please go to settings and turn on for sample app");
                    } else {
                        mView.showToast("Required permission " + permissions[index] + " not granted");
                    }
                }
            }
            mView.initializeView();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void checkMapInitError(OnEngineInitListener.Error error) {
        if (error == OnEngineInitListener.Error.NONE) {
            mView.initSuccess();
        } else {
            String message = "Error : " + error.name() + "\n\n" + error.getDetails();
            mView.initError(message);
        }
    }

    @Override
    public void checkMapInitResult(VenueService.InitStatus result) {
        if (result == VenueService.InitStatus.ONLINE_SUCCESS || result == VenueService.InitStatus.OFFLINE_SUCCESS) {
            mView.initResult();
        }
    }

    @Override
    public void checkInitComplete(AtomicBoolean m_initCompleted, String query) {
        if (m_initCompleted.get()) {
            mView.openVenueAsync(query);
        } else {
            mView.showToast("Initialization is incomplete, please, check logs");
        }
    }

    @Override
    public void calculateCenterScreenPoint(Map m_map, Context context) {
        int mWidth = context.getResources().getDisplayMetrics().widthPixels;
        int mHeight = context.getResources().getDisplayMetrics().heightPixels;
        PointF p = new PointF(mWidth / 2, mHeight / 2);
        GeoCoordinate touchLocation = m_map.pixelToGeo(p);
        mView.addToRoute(new OutdoorLocation(touchLocation));
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void getGeodata(String spaceId, String id) {
        mRepository.getGeodata(spaceId, id, this);
    }


    @Override
    public void onResponse(Geodata geodata) {
        if (geodata != null) {
            List<Feature> featureList = new ArrayList<>();
            featureList.add(geodata.getFeatures().get(0));
            for (int i = 1; i < geodata.getFeatures().size(); i++) {
                if (geodata.getFeatures().get(i).getGeometry().getCoordinates().get(0).equals(geodata.getFeatures().get(i - 1).getGeometry().getCoordinates().get(0)))
                    continue;
                featureList.add(geodata.getFeatures().get(i));
            }
            mView.setMarkers(featureList);
        }
    }

    @Override
    public void onFailure(Throwable t) {
        mView.showToast(t.getMessage());
    }
}
