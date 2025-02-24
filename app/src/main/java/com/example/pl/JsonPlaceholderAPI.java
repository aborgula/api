package com.example.pl;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface JsonPlaceholderAPI{
    @GET("users")
    Call<List<User>> getUsers();
}
