/*
 * Copyright (c) 2011-2019 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.eva.hackmoscow.activity.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.Map.Animation;
import com.here.android.mpa.mapping.MapGesture.OnGestureListener;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.PositionIndicator;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RouteOptions.TransportMode;
import com.here.android.mpa.routing.RouteOptions.Type;
import com.here.android.mpa.search.Address;
import com.here.android.mpa.venues3d.BaseLocation;
import com.here.android.mpa.venues3d.CombinedRoute;
import com.here.android.mpa.venues3d.DeselectionSource;
import com.here.android.mpa.venues3d.Level;
import com.here.android.mpa.venues3d.RoutingController;
import com.here.android.mpa.venues3d.RoutingController.RoutingControllerListener;
import com.here.android.mpa.venues3d.Space;
import com.here.android.mpa.venues3d.SpaceLocation;
import com.here.android.mpa.venues3d.Venue;
import com.here.android.mpa.venues3d.VenueController;
import com.here.android.mpa.venues3d.VenueInfo;
import com.here.android.mpa.venues3d.VenueMapFragment;
import com.here.android.mpa.venues3d.VenueMapFragment.VenueListener;
import com.here.android.mpa.venues3d.VenueRouteOptions;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import ru.eva.hackmoscow.R;
import ru.eva.hackmoscow.activity.login.LoginActivity;
import ru.eva.hackmoscow.controller.VenueFloorsController;
import ru.eva.hackmoscow.helper.DialogHelper;
import ru.eva.hackmoscow.helper.SQLiteHandler;
import ru.eva.hackmoscow.helper.SessionManager;

public class MainActivity extends FragmentActivity
        implements VenueListener, OnGestureListener, RoutingControllerListener, View.OnClickListener, ContractMain.View {

    private Map m_map = null;
    private VenueMapFragment m_mapFragment = null;
    private AtomicBoolean m_initCompleted = new AtomicBoolean(false);
    private MapMarker mapMarker;

    private Button fromToButton;
    private ImageButton hideRoutingButton;
    private View m_routeInfoLayout;
    private SearchView searchView;

    private BaseLocation startLocation = null;
    private BaseLocation endLocation = null;

    private boolean m_is_routing_mode = false;

    private VenueController m_currentVenue;

    ContractMain.Presenter mPresenter;

    private SQLiteHandler db;
    private SessionManager session;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }
        HashMap<String, String> user = db.getUserDetails();

        String name = user.get("name");
        String email = user.get("email");

        mPresenter = new PresenterMain(this);
        mPresenter.checkPermission(this);
    }


    @Override
    public void initializeView() {
        m_mapFragment = (VenueMapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        m_routeInfoLayout = findViewById(R.id.m_route_info_layout);
        fromToButton = findViewById(R.id.from_to_button);
        hideRoutingButton = findViewById(R.id.hide_routing_button);

        hideRoutingButton.setOnClickListener(this);
        fromToButton.setOnClickListener(this);

        searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mPresenter.checkInitComplete(m_initCompleted, query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        mPresenter.checkMapPermission(getPackageManager(), this);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.from_to_button:
                if (!m_is_routing_mode) {
                    return;
                }
                mPresenter.calculateCenterScreenPoint(m_map, this);
                break;

            case R.id.hide_routing_button:
                m_routeInfoLayout.setVisibility(View.GONE);
                m_is_routing_mode = false;
                m_mapFragment.getRoutingController().hideRoute();
                m_map.removeMapObject(mapMarker);
                fromToButton.setText("FROM");
                searchView.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void initializeMap() {
        m_mapFragment.init(error -> mPresenter.checkMapInitError(error), result -> mPresenter.checkMapInitResult(result));
    }

    @Override
    public void initSuccess() {
        m_map = m_mapFragment.getMap();
        m_map.setCenter(new GeoCoordinate(49.196261, -123.004773, 0.0), Animation.NONE);
    }

    @Override
    public void initError(String message) {
        DialogHelper.getInstance().showAlertDialog(this, message);
    }

    @Override
    public void initResult() {
        m_mapFragment.addListener(this);
        m_mapFragment.setFloorChangingAnimation(true);
        m_mapFragment.setVenueEnteringAnimation(true);
        m_mapFragment.setVenuesInViewportCallback(true);
        m_mapFragment.getRoutingController().addListener(this);
        m_mapFragment.getMapGesture().addOnGestureListener(this, 0, false);
        m_initCompleted.set(true);
        VenueFloorsController m_floorsController = new VenueFloorsController(this, m_mapFragment, findViewById(R.id.floorListView), R.layout.floor_item, R.id.floorName, R.id.floorGroundSep);
        PositioningManager positioningManager = PositioningManager.getInstance();
        positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK_INDOOR);
        PositionIndicator positionIndicator = m_mapFragment.getPositionIndicator();
        positionIndicator.setVisible(true);
    }

    private void logoutUser() {
        session.setLogin(false);
        db.deleteUsers();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (mPresenter.onRequestPermissionResult(requestCode, permissions, grantResults, this))
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }


    public void calculateRoute() {
        if ((startLocation == null) || (endLocation == null)) {
            showToast("you have to set start and stop point");
            return;
        }
        VenueRouteOptions venueRouteOptions = new VenueRouteOptions();
        RouteOptions options = venueRouteOptions.getRouteOptions();

        options.setRouteType(getRouteTypeFromChip());
        ;
        options.setTransportMode(getTransportModeFromChip());
        options.setRouteCount(1);
        venueRouteOptions.setRouteOptions(options);
        RoutingController routingController = m_mapFragment.getRoutingController();
        routingController.calculateCombinedRoute(startLocation, endLocation, venueRouteOptions);
    }

    private TransportMode getTransportModeFromChip() {
        if (((Chip) findViewById(R.id.car_chip)).isChecked())
            return TransportMode.CAR;
        else if (((Chip) findViewById(R.id.pedestrian_chip)).isChecked())
            return TransportMode.PEDESTRIAN;
        else
            return TransportMode.PUBLIC_TRANSPORT;
    }

    private Type getRouteTypeFromChip() {
        if (((Chip) findViewById(R.id.fastest_chip)).isChecked())
            return Type.FASTEST;
        else if (((Chip) findViewById(R.id.shortest_chip)).isChecked())
            return Type.SHORTEST;
        else
            return Type.BALANCED;
    }

    @Override
    public void openVenueAsync(String venueId) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);

        VenueInfo result = m_mapFragment.selectVenueAsync("DM_" + venueId);
        String textResult = "NOT IN THE INDEX FILE";
        if (result != null) {
            textResult = "TRYING TO OPEN...";
            m_map.setCenter(result.getBoundingBox().getCenter(), Animation.NONE, 17, 0, 1);
        }

        showToast("Open venue:" + textResult);
    }

    @Override
    public void onVenueTapped(Venue venue, float x, float y) {
        m_currentVenue = m_mapFragment.getVenueController(venue);
        m_map.pixelToGeo(new PointF(x, y));
        m_currentVenue.getVenue().getId();
        m_mapFragment.selectVenue(venue);
    }

    @Override
    public void addToRoute(BaseLocation location) {
        if (fromToButton.getText().toString().equals("FROM")) {
            startLocation = location;
            fromToButton.setText("TO");
            mapMarker = new MapMarker(location.getGeoCoordinate());
            m_map.addMapObject(mapMarker);
        } else if (fromToButton.getText().toString().equals("TO")) {
            endLocation = location;
            fromToButton.setText("FROM");
            calculateRoute();
        }
    }

    @Override
    public void onSpaceSelected(Venue venue, Space space) {
        if (!m_is_routing_mode) {
            onSpaceSelectedMapMode(space);
            return;
        }
        addToRoute(new SpaceLocation(space, m_mapFragment.getVenueController(venue)));
    }

    @SuppressLint("DefaultLocale")
    @Override
    public boolean onTapEvent(PointF p) {
        return false;
    }


    private void onSpaceSelectedMapMode(Space space) {

        String spaceName = space.getContent().getName();
        String parentCategory = space.getContent().getParentCategoryId();
        String placeCategory = space.getContent().getPlaceCategoryId();
        showToast("Space:" + spaceName + ",\nparent category: " + parentCategory + ", \nplace category: " + placeCategory);

        Address address = space.getContent().getAddress();
        if (address != null) {
            System.out.println("Space address: " + address.getStreet() + " " + address.getPostalCode() + " " + address.getCity());
            System.out.println("Space floor: " + address.getFloorNumber() + " place cat: " + space.getContent().getPlaceCategoryId());
        }
    }

    @Override
    public void onBackPressed() {
        VenueController controller = m_mapFragment
                .getVenueController(m_mapFragment.getSelectedVenue());
        if (controller == null) {
            super.onBackPressed();
        } else {
            if (controller.getSelectedSpace() == null) {
                m_mapFragment.deselectVenue();
                if (m_currentVenue != null) {
                    m_currentVenue = null;
                }
            } else {
                controller.deselectSpace();
            }
        }
    }

    @Override
    public void onVenueSelected(Venue venue) {
        m_currentVenue = m_mapFragment.getVenueController(venue);

        String venueId = venue.getId();
        String venueName = venue.getContent().getName();
        showToast(venueId + ": " + venueName);
    }

    @Override
    public boolean onDoubleTapEvent(PointF point) {
        return false;
    }

    @Override
    public boolean onLongPressEvent(PointF point) {
        int visibility = m_routeInfoLayout.getVisibility();
        if (visibility == View.VISIBLE) {
            return false;
        }
        searchView.setVisibility(View.GONE);
        m_routeInfoLayout.setVisibility(View.VISIBLE);
        startLocation = null;
        endLocation = null;
        m_is_routing_mode = true;
        m_map.setCenter(point, Animation.BOW, m_map.getZoomLevel(), 0, 1);
        return false;
    }

    private boolean DisplayRoute(CombinedRoute route) {
        RoutingController routingController = m_mapFragment.getRoutingController();
        routingController.showRoute(route);
        return true;
    }

    @Override
    public void onCombinedRouteCompleted(CombinedRoute route) {
        boolean result = false;
        RoutingController routingController = m_mapFragment.getRoutingController();

        if (route.getRouteSections().size() > 0) {
            result = DisplayRoute(route);
        }

        if (!result) {
            routingController.hideRoute();
        }

        String textResult = (result ? "The route is built" : "Route built fail");
        showToast(textResult);
        m_map.removeMapObject(mapMarker);
    }

    @Override
    public void onVenueDeselected(Venue venue, DeselectionSource source) {
    }

    @Override
    public void onSpaceDeselected(Venue venue, Space space) {
    }

    @Override
    public void onFloorChanged(Venue venue, Level oldLevel, Level newLevel) {
    }

    @Override
    public void onVenueVisibleInViewport(Venue venue, boolean visible) {
    }

    @Override
    public void onLongPressRelease() {
    }

    @Override
    public boolean onMapObjectsSelected(List<ViewObject> arg0) {
        return false;
    }

    @Override
    public void onMultiFingerManipulationEnd() {
    }

    @Override
    public void onMultiFingerManipulationStart() {
    }

    @Override
    public void onPanEnd() {
    }

    @Override
    public void onPanStart() {
    }

    @Override
    public void onPinchLocked() {
    }

    @Override
    public boolean onPinchZoomEvent(float scaleFactor, PointF point) {
        return false;
    }

    @Override
    public boolean onRotateEvent(float angle) {
        return false;
    }

    @Override
    public void onRotateLocked() {
    }

    @Override
    public boolean onTiltEvent(float angle) {
        return false;
    }

    @Override
    public boolean onTwoFingerTapEvent(PointF arg0) {
        return false;
    }


}
