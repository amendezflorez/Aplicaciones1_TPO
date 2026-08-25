package com.example.rondaapp.data.network;

import com.example.rondaapp.data.model.PublicationResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
public interface ApiService {
    @GET("api/publications")
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
}
