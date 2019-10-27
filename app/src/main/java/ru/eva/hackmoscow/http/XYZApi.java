package ru.eva.hackmoscow.http;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.eva.hackmoscow.model.Geodata;

public interface XYZApi {
    @GET("/hub/spaces/{spaceId}/search")
    Call<Geodata> getFeatures( @Path("spaceId") String spaceId, @Query("features.properties.id") String id, @Query("access_token") String token);
}
