package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.uaa.misgastosapp.data.UserEntity
import com.uaa.misgastosapp.data.repository.AuthRepository
import com.uaa.misgastosapp.utils.GoogleSignInHelper
import com.uaa.misgastosapp.utils.SecureSessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val application: Application = mockk(relaxed = true)
    private val sessionManager: SecureSessionManager = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val googleSignInHelper: GoogleSignInHelper = mockk()

    private val sampleUser = UserEntity(
        id = 1,
        email = "test@test.com",
        password = "salt:hash",
        name = "Test",
        createdAt = "2026-01-01T00:00:00"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { sessionManager.isLoggedIn() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AuthViewModel(application, sessionManager, authRepository, googleSignInHelper)

    @Test
    fun `login exitoso marca isLoggedIn en true`() = runTest {
        val viewModel = createViewModel()
        coEvery { authRepository.login("test@test.com", "Password1") } returns sampleUser

        var success = false
        viewModel.login("test@test.com", "Password1", onSuccess = { success = true }, onError = { fail("no debería fallar: $it") })

        assertTrue(success)
        assertTrue(viewModel.isLoggedIn.value)
    }

    @Test
    fun `login con campos vacios no llega a llamar al repositorio`() = runTest {
        val viewModel = createViewModel()
        var errorMsg: String? = null

        viewModel.login("", "", onSuccess = { fail("no debería tener éxito") }, onError = { errorMsg = it })

        assertEquals("Por favor completa todos los campos", errorMsg)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `login fallido reporta el error y no marca sesion activa`() = runTest {
        val viewModel = createViewModel()
        coEvery { authRepository.login(any(), any()) } throws Exception("Credenciales inválidas")

        var errorMsg: String? = null
        viewModel.login("test@test.com", "incorrecta", onSuccess = { fail("no debería tener éxito") }, onError = { errorMsg = it })

        assertEquals("Credenciales inválidas", errorMsg)
        assertFalse(viewModel.isLoggedIn.value)
    }

    @Test
    fun `register valida que las contraseñas coincidan antes de llamar al repositorio`() = runTest {
        val viewModel = createViewModel()
        var errorMsg: String? = null

        viewModel.register(
            "Test", "test@test.com", "testuser", "Password1", "Password2",
            onSuccess = { fail("no debería tener éxito") },
            onError = { errorMsg = it }
        )

        assertEquals("Las contraseñas no coinciden", errorMsg)
        coVerify(exactly = 0) { authRepository.register(any(), any(), any(), any()) }
    }

    @Test
    fun `register exitoso marca isLoggedIn en true`() = runTest {
        val viewModel = createViewModel()
        coEvery { authRepository.register("Test", "test@test.com", "testuser", "Password1") } returns Unit

        var success = false
        viewModel.register(
            "Test", "test@test.com", "testuser", "Password1", "Password1",
            onSuccess = { success = true },
            onError = { fail("no debería fallar: $it") }
        )

        assertTrue(success)
        assertTrue(viewModel.isLoggedIn.value)
    }

    @Test
    fun `signInWithGoogle exitoso delega en el repositorio y marca sesion activa`() = runTest {
        val viewModel = createViewModel()
        val credential: GoogleIdTokenCredential = mockk {
            every { id } returns "usuario@gmail.com"
            every { displayName } returns "Usuario Google"
        }
        coEvery { googleSignInHelper.signIn(any(), any()) } coAnswers {
            firstArg<(GoogleIdTokenCredential) -> Unit>().invoke(credential)
        }
        coEvery { authRepository.loginOrRegisterWithGoogle("usuario@gmail.com", "Usuario Google") } returns
            sampleUser.copy(id = 2, email = "usuario@gmail.com", name = "Usuario Google")

        var success = false
        viewModel.signInWithGoogle(onSuccess = { success = true }, onError = { fail("no debería fallar: $it") })

        assertTrue(success)
        assertTrue(viewModel.isLoggedIn.value)
    }

    @Test
    fun `signInWithGoogle fallido reporta el error de Credential Manager`() = runTest {
        val viewModel = createViewModel()
        coEvery { googleSignInHelper.signIn(any(), any()) } coAnswers {
            secondArg<(String) -> Unit>().invoke("No se encontraron cuentas de Google en el dispositivo")
        }

        var errorMsg: String? = null
        viewModel.signInWithGoogle(onSuccess = { fail("no debería tener éxito") }, onError = { errorMsg = it })

        assertEquals("No se encontraron cuentas de Google en el dispositivo", errorMsg)
        assertFalse(viewModel.isLoggedIn.value)
    }

    @Test
    fun `deleteAccount exitoso limpia la sesion activa`() = runTest {
        val viewModel = createViewModel()
        coEvery { authRepository.login(any(), any()) } returns sampleUser
        viewModel.login("test@test.com", "Password1", onSuccess = {}, onError = { fail(it) })
        assertTrue(viewModel.isLoggedIn.value)

        coEvery { authRepository.deleteAccount() } returns Unit
        var success = false
        viewModel.deleteAccount(onSuccess = { success = true }, onError = { fail("no debería fallar: $it") })

        assertTrue(success)
        assertFalse(viewModel.isLoggedIn.value)
    }
}
