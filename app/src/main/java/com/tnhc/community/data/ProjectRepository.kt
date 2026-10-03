package com.tnhc.community.data

fun interface ProjectRepository {
    fun load(): List<Project>
}

object DemoProjectRepository : ProjectRepository {
    override fun load(): List<Project> = listOf(
        Project(
            "demo-orbit", "Orbit Workshop", "A small exploration game about finding your own way.",
            "Games", "Prototype", listOf("Game design", "Exploration", "3D"),
            "Experimenting with movement and a first playable environment. This fictional project shows how a game could share progress and invite feedback.", "TNHC",
        ),
        Project(
            "demo-workbench", "Open Workbench", "Everyday creative tools, built in the open.",
            "Tools", "Alpha", listOf("Open source", "Desktop", "Design"),
            "Exploring an approachable asset organiser for creators. This sample demonstrates a tool project; no download is available.", "TNHC",
        ),
        Project(
            "demo-seed", "Seed Engine", "A tiny engine with room for big experiments.",
            "Engines", "Research", listOf("Rendering", "C++", "Learning"),
            "Comparing rendering approaches and documenting the experiments. This sample is an engine research project, not an announced TNHC product.", "TNHC",
        ),
        Project(
            "demo-signal", "Signal Garden", "Playful connections between code and the physical world.",
            "Hardware", "Concept", listOf("Electronics", "Creative coding", "Sensors"),
            "Sketching a hands-on sensor experiment. This fictional entry illustrates how hardware can sit alongside games and software.", "TNHC",
        ),
    )
}

fun filterProjects(projects: List<Project>, query: String, category: String?): List<Project> {
    val term = query.trim()
    return projects.filter { project ->
        (category == null || project.category == category) &&
            (term.isEmpty() || (listOf(project.title, project.summary) + project.tags)
                .any { it.contains(term, ignoreCase = true) })
    }
}
