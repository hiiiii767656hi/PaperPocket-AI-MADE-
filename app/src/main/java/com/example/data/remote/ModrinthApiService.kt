package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ModrinthApiService {
    @GET("v2/search")
    suspend fun searchPlugins(
        @Query("query") query: String = "",
        @Query("facets") facets: String = "[[\"project_type:plugin\"]]",
        @Query("index") index: String = "downloads",
        @Query("limit") limit: Int = 20
    ): ModrinthSearchResponse

    @GET("v2/project/{id_or_slug}/version")
    suspend fun getProjectVersions(
        @Path("id_or_slug") idOrSlug: String,
        @Query("loaders") loaders: String? = null
    ): List<ModrinthVersionModel>
}
