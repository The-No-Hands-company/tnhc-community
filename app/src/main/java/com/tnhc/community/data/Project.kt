package com.tnhc.community.data

data class Project(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val stage: String,
    val tags: List<String>,
    val focus: String,
    val affiliation: String,
    val isDemo: Boolean = true,
    val websiteUrl: String? = null,
    val repositoryUrl: String? = null,
    val statusAsOf: String? = null,
)

fun projectStageLabel(stage: String): String = when (stage) {
    "in_development" -> "In development"
    "released" -> "Released"
    "beta" -> "Beta"
    else -> stage.replace('_', ' ').replaceFirstChar(Char::uppercase)
}
