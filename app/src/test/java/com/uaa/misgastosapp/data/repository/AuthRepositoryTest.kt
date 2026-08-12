package com.uaa.misgastosapp.data.repository

import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.UserDao
import com.uaa.misgastosapp.data.UserEntity
import com.uaa.misgastosapp.utils.SecureSessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private val userDao: UserDao = mockk()
    private val sessionManager: SecureSessionManager = mockk(relaxed = true)
    private val appDatabase: AppDatabase = mockk(relaxed = true)
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        repository = AuthRepository(userDao, sessionManager, appDatabase)
    }

    @Test
    fun `register lanza excepcion si el email ya existe`() = runTest {
        coEvery { userDao.getUserByEmail("test@test.com") } returns
            UserEntity(id = 1, email = "test@test.com", password = "salt:hash", name = "Test", createdAt = "2026-01-01T00:00:00")

        try {
            repository.register("Test", "test@test.com", "testuser", "Password1")
            fail("Deberia lanzar excepcion")
        } catch (e: Exception) {
            assertEquals("El email ya está registrado", e.message)
        }
        coVerify(exactly = 0) { userDao.insert(any()) }
    }

    @Test
    fun `register hashea la contraseña, guarda el email en minusculas e inicia sesion local`() = runTest {
        coEvery { userDao.getUserByEmail(any()) } returns null
        val userSlot = slot<UserEntity>()
        coEvery { userDao.insert(capture(userSlot)) } returns 42L

        repository.register("Test", "Test@Test.com", "testuser", "Password1")

        val stored = userSlot.captured
        assertEquals("test@test.com", stored.email)
        assertNotEquals("Password1", stored.password)
        assertTrue("el hash debe guardarse como 'salt:hash'", stored.password.contains(":"))

        coVerify {
            sessionManager.saveUserSession(
                userId = 42,
                email = "test@test.com",
                name = "Test",
                username = "testuser",
                accessToken = any()
            )
        }
    }

    @Test
    fun `login acepta la contraseña correcta y rechaza una incorrecta`() = runTest {
        // se registra primero para obtener un hash real generado por el propio repository (los
        // metodos de hash/verificacion son privados, asi que no se pueden invocar directamente).
        coEvery { userDao.getUserByEmail(any()) } returns null
        val userSlot = slot<UserEntity>()
        coEvery { userDao.insert(capture(userSlot)) } returns 1L
        repository.register("Test", "test@test.com", "testuser", "Password1")
        val storedUser = userSlot.captured.copy(id = 1)

        coEvery { userDao.getUserByEmail("test@test.com") } returns storedUser

        val result = repository.login("test@test.com", "Password1")
        assertEquals(storedUser.email, result.email)

        try {
            repository.login("test@test.com", "OtraPassword1")
            fail("Deberia lanzar excepcion con la contraseña incorrecta")
        } catch (e: Exception) {
            assertEquals("Credenciales inválidas", e.message)
        }
    }

    @Test
    fun `login lanza excepcion si el usuario no existe`() = runTest {
        coEvery { userDao.getUserByEmail("noexiste@test.com") } returns null

        try {
            repository.login("noexiste@test.com", "Password1")
            fail("Deberia lanzar excepcion")
        } catch (e: Exception) {
            assertEquals("Credenciales inválidas", e.message)
        }
    }

    @Test
    fun `loginOrRegisterWithGoogle crea un usuario local nuevo si el email no existe`() = runTest {
        coEvery { userDao.getUserByEmail("nuevo@gmail.com") } returns null
        val userSlot = slot<UserEntity>()
        coEvery { userDao.insert(capture(userSlot)) } returns 7L

        val result = repository.loginOrRegisterWithGoogle("Nuevo@Gmail.com", "Nuevo Usuario")

        assertEquals("nuevo@gmail.com", userSlot.captured.email)
        assertEquals(7, result.id)
        coVerify {
            sessionManager.saveUserSession(
                userId = 7,
                email = "nuevo@gmail.com",
                name = "Nuevo Usuario",
                username = "nuevo",
                accessToken = any()
            )
        }
    }

    @Test
    fun `loginOrRegisterWithGoogle reutiliza el usuario local existente por email`() = runTest {
        val existing = UserEntity(id = 5, email = "existe@gmail.com", password = "a:b", name = "Ya existe", createdAt = "2026-01-01T00:00:00")
        coEvery { userDao.getUserByEmail("existe@gmail.com") } returns existing

        val result = repository.loginOrRegisterWithGoogle("existe@gmail.com", "Otro nombre")

        assertEquals(5, result.id)
        coVerify(exactly = 0) { userDao.insert(any()) }
    }

    @Test
    fun `deleteAccount borra toda la base de datos local y cierra la sesion`() = runTest {
        repository.deleteAccount()

        verify { appDatabase.clearAllTables() }
        verify { sessionManager.logout() }
    }

    @Test
    fun `logout cierra la sesion local`() {
        repository.logout()

        verify { sessionManager.logout() }
    }
}
