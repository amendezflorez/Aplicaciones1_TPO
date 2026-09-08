package com.example.rondaapp.data.network;

import com.example.rondaapp.data.model.AuthResponse;
import com.example.rondaapp.data.model.Favorite;
import com.example.rondaapp.data.model.FavoriteResponse;
import com.example.rondaapp.data.model.LoginBody;
import com.example.rondaapp.data.model.OtpRequestBody;
import com.example.rondaapp.data.model.OtpVerifyBody;
import com.example.rondaapp.data.model.PublicationResponse;
import com.example.rondaapp.data.model.SavedSearch;
import com.example.rondaapp.data.model.SavedSearchResponse;
import com.example.rondaapp.data.model.SimpleResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface unificada para los servicios de la API REST de Ronda.
 */
public interface ApiService {

    // --- Endpoints de Autenticación ---

    @POST("auth/otp/request")
    Call<SimpleResponse> requestOtp(@Body OtpRequestBody body);

    @POST("auth/otp/resend")
    Call<SimpleResponse> resendOtp(@Body OtpRequestBody body);

    @POST("auth/otp/verify")
    Call<AuthResponse> verifyOtp(@Body OtpVerifyBody body);

    @POST("auth/login")
    Call<AuthResponse> loginWithPassword(@Body LoginBody body);

    // --- Endpoints de Publicaciones (Home) ---

    @GET("publications")
    Call<PublicationResponse> getPublications(
            @Query("search") String search,
            @Query("category") String category,
            @Query("condition") String condition,
            @Query("minPrice") Double minPrice,
            @Query("maxPrice") Double maxPrice,
            @Query("zone") String zone,
            @Query("sortBy") String sortBy,
            @Query("page") int page,
            @Query("limit") int limit
    );

    // --- Endpoints de Favoritos (Feature 11) ---

    @POST("favorites")
    Call<Favorite> addFavorite(@Body Favorite favorite);

    @GET("favorites")
    Call<FavoriteResponse> getFavorites(@Query("userId") String userId);

    @DELETE("favorites/{id}")
    Call<SimpleResponse> deleteFavorite(@Path("id") int favoriteId);

    // --- Endpoints de Búsquedas Guardadas ---

    @POST("saved-searches")
    Call<SavedSearch> addSavedSearch(@Body SavedSearch savedSearch);

    @GET("saved-searches")
    Call<SavedSearchResponse> getSavedSearches(@Query("userId") String userId);

    @DELETE("saved-searches/{id}")
    Call<SimpleResponse> deleteSavedSearch(@Path("id") int searchId);
}
