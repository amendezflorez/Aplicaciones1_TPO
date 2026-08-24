package com.ronda.app.data.network;

import com.ronda.app.data.model.AuthResponse;
import com.ronda.app.data.model.LoginBody;
import com.ronda.app.data.model.OtpRequestBody;
import com.ronda.app.data.model.OtpVerifyBody;
import com.ronda.app.data.model.SimpleResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Endpoints de autenticación consumidos por la app.
 * Ajustar los paths ("auth/...") según lo que exponga finalmente la API_Rest del backend.
 */
public interface ApiService {

    @POST("auth/otp/request")
    Call<SimpleResponse> requestOtp(@Body OtpRequestBody body);

    @POST("auth/otp/resend")
    Call<SimpleResponse> resendOtp(@Body OtpRequestBody body);

    @POST("auth/otp/verify")
    Call<AuthResponse> verifyOtp(@Body OtpVerifyBody body);

    @POST("auth/login")
    Call<AuthResponse> loginWithPassword(@Body LoginBody body);
}
