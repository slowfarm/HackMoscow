package ru.eva.hackmoscow.activity.main;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.eva.hackmoscow.AppController;
import ru.eva.hackmoscow.OnGeodataRecieved;
import ru.eva.hackmoscow.model.Geodata;

public class RepositoryMain implements ContractMain.Repository {
    @Override
    public void getGeodata(String spaceId, String id, OnGeodataRecieved onGeodataRecieved) {
        AppController.xyzApi().getFeatures("9dzb3GUZ", "1568168", "AOckCZiXQ2GJE9o3UrKPPgA").enqueue(new Callback<Geodata>() {
            @Override
            public void onResponse(Call<Geodata> call, Response<Geodata> response) {
                onGeodataRecieved.onResponse(response.body());
            }

            @Override
            public void onFailure(Call<Geodata> call, Throwable t) {
                onGeodataRecieved.onFailure(t);
            }
        });
    }
}
