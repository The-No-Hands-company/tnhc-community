package com.tnhc.community.data

import org.junit.Assert.*
import org.junit.Test

class ProjectRepositoryTest {
    private val projects = listOf(
        Project("a", "Orbit", "Explore a quiet world", "Games", "Prototype", listOf("Kotlin"), "Movement", "TNHC"),
        Project("b", "Workbench", "Tools for creators", "Tools", "Alpha", listOf("Desktop"), "Editing", "Community"),
    )

    @Test fun canonicalProjectStagesHaveReadableLabels() {
        assertEquals("Released", projectStageLabel("released"))
        assertEquals("Beta", projectStageLabel("beta"))
        assertEquals("In development", projectStageLabel("in_development"))
    }

    @Test fun searchIgnoresCaseAndSurroundingWhitespace() {
        assertEquals(listOf("a"), filterProjects(projects, "  ORbIt  ", null).map { it.id })
    }
    @Test fun searchFindsTagsAndSummary() {
        assertEquals(listOf("a"), filterProjects(projects, "kotlin", null).map { it.id })
        assertEquals(listOf("b"), filterProjects(projects, "creators", null).map { it.id })
    }
    @Test fun categoryAndTextMustBothMatch() {
        assertTrue(filterProjects(projects, "Orbit", "Tools").isEmpty())
        assertEquals(listOf("b"), filterProjects(projects, "", "Tools").map { it.id })
    }
    @Test fun clearingFiltersRestoresCatalogueOrder() {
        assertEquals(listOf("a", "b"), filterProjects(projects, "  ", null).map { it.id })
    }
    @Test fun emptyAndUnknownSearchesHaveNoResults() {
        assertTrue(filterProjects(emptyList(), "", null).isEmpty())
        assertTrue(filterProjects(projects, "nonexistent", null).isEmpty())
    }
    @Test fun demoIdsAreUniqueAndResolveToTheSameProject() {
        val demo = DemoProjectRepository.load()
        assertTrue(demo.isNotEmpty())
        assertEquals(demo.size, demo.map { it.id }.toSet().size)
        demo.forEach { assertEquals(it, demo.find { item -> item.id == it.id }) }
    }
}
