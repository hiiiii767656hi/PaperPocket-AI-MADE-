package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ModrinthSearchResponse(
    @Json(name = "hits") val hits: List<ModrinthProjectHit> = emptyList(),
    @Json(name = "offset") val offset: Int? = 0,
    @Json(name = "limit") val limit: Int? = 20,
    @Json(name = "total_hits") val totalHits: Int? = 0
)

@JsonClass(generateAdapter = true)
data class ModrinthProjectHit(
    @Json(name = "project_id") val projectId: String,
    @Json(name = "project_type") val projectType: String? = "plugin",
    @Json(name = "slug") val slug: String? = "",
    @Json(name = "author") val author: String? = "",
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = "",
    @Json(name = "categories") val categories: List<String>? = emptyList(),
    @Json(name = "display_categories") val displayCategories: List<String>? = emptyList(),
    @Json(name = "downloads") val downloads: Int? = 0,
    @Json(name = "follows") val follows: Int? = 0,
    @Json(name = "icon_url") val iconUrl: String? = null,
    @Json(name = "latest_version") val latestVersion: String? = null
)

@JsonClass(generateAdapter = true)
data class ModrinthVersionModel(
    @Json(name = "id") val id: String,
    @Json(name = "project_id") val projectId: String,
    @Json(name = "name") val name: String,
    @Json(name = "version_number") val versionNumber: String,
    @Json(name = "game_versions") val gameVersions: List<String>? = emptyList(),
    @Json(name = "loaders") val loaders: List<String>? = emptyList(),
    @Json(name = "files") val files: List<ModrinthVersionFile>? = emptyList(),
    @Json(name = "downloads") val downloads: Int? = 0
)

@JsonClass(generateAdapter = true)
data class ModrinthVersionFile(
    @Json(name = "url") val url: String,
    @Json(name = "filename") val filename: String,
    @Json(name = "primary") val primary: Boolean? = true,
    @Json(name = "size") val size: Long? = 0L
)
