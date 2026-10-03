package com.tnhc.community

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tnhc.community.data.DemoProjectRepository
import com.tnhc.community.data.ProjectRepository
import com.tnhc.community.ui.CommunityApp
import com.tnhc.community.ui.TnhcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommunityAppTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun allDestinationsOpenAndProjectBackReturnsToCatalogue() {
        compose.setContent { TnhcTheme { CommunityApp() } }
        listOf("Community", "Messages", "Profile", "Home", "Projects").forEach {
            compose.onNodeWithTag("tab-$it").performClick().assertIsSelected()
        }
        compose.onNodeWithTag("project-demo-orbit").performClick()
        compose.onNodeWithText("Current focus").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("project-search").assertIsDisplayed()
    }

    @Test fun noMatchesCanBeCleared() {
        compose.setContent { TnhcTheme { CommunityApp() } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-search").performTextInput("nothingmatches")
        compose.onNodeWithText("No matching projects").assertIsDisplayed()
        compose.onNodeWithText("Clear filters").performClick()
        compose.onNodeWithTag("project-demo-orbit").assertExists()
    }

    @Test fun selectedProjectAndSearchSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TnhcTheme { CommunityApp() } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-search").performTextInput("Orbit")
        compose.onNodeWithText("Games").performClick()
        compose.onNodeWithTag("project-demo-orbit").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Current focus").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("project-search").assertTextContains("Orbit")
        compose.onNodeWithText("Games").assertIsSelected()
    }

    @Test fun retryRecoversFromRepositoryFailure() {
        var failed = true
        val repository = ProjectRepository {
            if (failed) error("Synthetic failure") else DemoProjectRepository.load()
        }
        compose.setContent { TnhcTheme { CommunityApp(repository) } }
        compose.onNodeWithText("Projects could not load").assertIsDisplayed()
        compose.runOnIdle { failed = false }
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Build something\ntogether.").assertIsDisplayed()
    }

    @Test fun emptyRepositoryExplainsState() {
        compose.setContent { TnhcTheme { CommunityApp(ProjectRepository { emptyList() }) } }
        compose.onNodeWithText("No projects yet").assertIsDisplayed()
    }

    @Test fun restoredDetailCanRetryWithoutLosingSelectedProject() {
        var failed = false
        val repository = ProjectRepository {
            if (failed) error("Synthetic failure") else DemoProjectRepository.load()
        }
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TnhcTheme { CommunityApp(repository) } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-demo-orbit").performClick()
        compose.runOnIdle { failed = true }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Projects could not load").assertIsDisplayed()
        compose.runOnIdle { failed = false }
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Current focus").assertExists()
    }

    @Test fun systemBackClosesDetailsThenReturnsHome() {
        compose.setContent { TnhcTheme { CommunityApp() } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-demo-orbit").performClick()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("project-search").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("tab-Home").assertIsSelected()
    }

    @Test fun missingRestoredProjectCanReturnToCatalogue() {
        var removed = false
        val repository = ProjectRepository {
            DemoProjectRepository.load().filter { !removed || it.id != "demo-orbit" }
        }
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TnhcTheme { CommunityApp(repository) } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-demo-orbit").performClick()
        compose.runOnIdle { removed = true }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Project unavailable").assertIsDisplayed()
        compose.onNodeWithText("Go back").performClick()
        compose.onNodeWithTag("project-search").assertIsDisplayed()
    }
}
