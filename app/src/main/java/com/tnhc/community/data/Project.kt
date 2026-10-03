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
)
