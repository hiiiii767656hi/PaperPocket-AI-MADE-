package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface PaperApiService {
    @GET("v2/projects/paper")
    suspend fun getProjectInfo(): PaperProjectResponse

    @GET("v2/projects/paper/versions/{version}")
    suspend fun getVersionInfo(
        @Path("version") version: String
    ): PaperVersionResponse

    @GET("v2/projects/paper/versions/{version}/builds/{build}")
    suspend fun getBuildInfo(
        @Path("version") version: String,
        @Path("build") build: Int
    ): PaperBuildResponse
}
