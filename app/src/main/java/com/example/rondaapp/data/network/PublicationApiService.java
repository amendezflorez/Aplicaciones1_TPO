package com.example.rondaapp.data.network;

import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.model.SimpleResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Interfaz de Retrofit para operaciones específicas de publicaciones.
 */
public interface PublicationApiService {

    @GET("publications/{id}")
    Call<Publication> getPublicationDetail(@Path("id") int id);

    // Acciones de Interesado
    @POST("publications/{id}/offers")
    Call<SimpleResponse> makeOffer(@Path("id") int id, @Body Object offerData);

    @POST("publications/{id}/favorites")
    Call<SimpleResponse> saveToFavorites(@Path("id") int id);

    // Acciones de Vendedor
    @PUT("publications/{id}")
    Call<Publication> updatePublication(@Path("id") int id, @Body Object updateData);

    @DELETE("publications/{id}")
    Call<SimpleResponse> deletePublication(@Path("id") int id);

    @POST("publications/{id}/pause")
    Call<SimpleResponse> pausePublication(@Path("id") int id);
}
