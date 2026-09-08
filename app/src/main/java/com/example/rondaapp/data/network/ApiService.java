package com.example.rondaapp.data.network;

import com.example.rondaapp.data.model.AuthResponse;
import com.example.rondaapp.data.model.CreatePublicationBody;
import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.model.PublicationStatusBody;
import com.example.rondaapp.data.model.LoginBody;
import com.example.rondaapp.data.model.OtpRequestBody;
import com.example.rondaapp.data.model.OtpVerifyBody;
import com.example.rondaapp.data.model.PublicationResponse;
import com.example.rondaapp.data.model.SimpleResponse;
import com.example.rondaapp.data.model.UpdateProfileBody;
import com.example.rondaapp.data.model.UserProfile;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
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

    // --- Endpoints de Perfil y Reputación ---

    /** Perfil de un usuario: datos personales, reputación y publicaciones activas. */
    @GET("users/{id}")
    Call<UserProfile> getUserProfile(@Path("id") String userId);

    /** Edita los datos personales del perfil propio. */
    @PUT("users/{id}")
    Call<UserProfile> updateUserProfile(@Path("id") String userId, @Body UpdateProfileBody body);

    // --- Endpoints de Publicaciones (Home) ---

    // --- Endpoints de Publicar / Mis publicaciones (punto 5) ---

    /** Crea una publicación con sus fotos en base64. */
    @POST("publications")
    Call<Publication> createPublication(@Body CreatePublicationBody body);

    /** Todas las publicaciones del usuario, en cualquier estado. */
    @GET("users/{id}/publications")
    Call<PublicationResponse> getMyPublications(@Path("id") String userId);

    /** Pausar, reactivar o marcar vendida. */
    @PATCH("publications/{id}/status")
    Call<Publication> updatePublicationStatus(@Path("id") int publicationId,
                                              @Body PublicationStatusBody body);

    // --- Endpoints de Publicaciones (Home) ---

    @GET("publications")
    Call<PublicationResponse> getPublications(
            @Query("search") String search,
            @Query("category") String category,
            @Query("condition") String condition,
            @Query("minPrice") Double minPrice,
            @Query("maxPrice") Double maxPrice,
            @Query("zone") String zone,
            @Query("nearZone") String nearZone,
            @Query("sortBy") String sortBy,
            @Query("page") int page,
            @Query("limit") int limit
    );
}
