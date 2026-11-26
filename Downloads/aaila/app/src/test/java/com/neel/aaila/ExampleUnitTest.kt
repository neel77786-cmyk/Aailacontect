package com.neel.aaila

import com.neel.aaila.data.model.BackendActionResult
import com.neel.aaila.data.repository.Phase1FoundationRepository
import com.neel.aaila.ui.screens.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login validation rejects empty family id and short password`() {
        val repo = Phase1FoundationRepository()
        val authVm = AuthViewModel(repo)

        authVm.onLoginFamilyIdChanged("")
        authVm.onLoginPasswordChanged("123")
        val result = authVm.submitLogin()

        assertFalse(result)
        assertNotNull(authVm.loginState.value.familyIdError)
        assertNotNull(authVm.loginState.value.passwordError)
    }

    @Test
    fun `sign in surfaces honest Phase 2 backend notice when valid credentials entered`() {
        val repo = Phase1FoundationRepository()
        val authVm = AuthViewModel(repo)

        authVm.onLoginFamilyIdChanged("SHARMA-101")
        authVm.onLoginPasswordChanged("FamilyPass99")
        val signedIn = authVm.submitLogin()

        assertFalse(signedIn)
        val notice = authVm.loginState.value.backendNotice
        assertNotNull(notice)
        assertTrue(notice!!.contains("Phase 2"))
    }

    @Test
    fun `admin setup validates matching passwords and marks preview ready`() {
        val repo = Phase1FoundationRepository()
        val authVm = AuthViewModel(repo)

        authVm.onFamilyNameChanged("The Sharma Family")
        authVm.onAdminNameChanged("Neel")
        authVm.onSetupFamilyIdChanged("SHARMA-01")
        authVm.onSetupPasswordChanged("SecurePass1")
        authVm.onSetupConfirmPasswordChanged("Mismatch1")

        assertFalse(authVm.validateAndSubmitAdminSetup())
        assertNotNull(authVm.adminSetupState.value.confirmPasswordError)

        authVm.onSetupConfirmPasswordChanged("SecurePass1")
        authVm.validateAndSubmitAdminSetup()

        assertTrue(authVm.adminSetupState.value.readyForPreview)
        assertEquals("The Sharma Family", repo.activeFamilySpace.value.familyName)
        assertEquals("SHARMA-01", repo.activeFamilySpace.value.familyId)
    }

    @Test
    fun `messaging repository marks Phase 1 preview messages honestly`() {
        val repo = Phase1FoundationRepository()
        val result = repo.sendPreviewMessage("thread_family_circle", "Hello family!", "Neel")
        assertTrue(result is BackendActionResult.BackendNotConfigured)
    }
}
