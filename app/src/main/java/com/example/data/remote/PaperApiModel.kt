package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaperProjectResponse(
    @Json(name = "project_id") val projectId: String,
    @Json(name = "project_name") val projectName: String,
    @Json(name = "version_groups") val versionGroups: List<String>,
    @Json(name = "versions") val versions: List<String>
)

@JsonClass(generateAdapter = true)
data class PaperVersionResponse(
    @Json(name = "project_id") val projectId: String,
    @Json(name = "version") val version: String,
    @Json(name = "builds") val builds: List<Int>
)

@JsonClass(generateAdapter = true)
data class PaperBuildResponse(
    @Json(name = "project_id") val projectId: String,
    @Json(name = "version") val version: String,
    @Json(name = "build") val build: Int,
    @Json(name = "time") val time: String? = null,
    @Json(name = "changes") val changes: List<PaperChange>? = null,
    @Json(name = "downloads") val downloads: PaperDownloads? = null
)

@JsonClass(generateAdapter = true)
data class PaperChange(
    @Json(name = "commit") val commit: String,
    @Json(name = "summary") val summary: String,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class PaperDownloads(
    @Json(name = "application") val application: PaperDownloadFile? = null
)

@JsonClass(generateAdapter = true)
data class PaperDownloadFile(
    @Json(name = "name") val name: String,
    @Json(name = "sha256") val sha256: String
)
