package io.hackle.android.internal.invocator.invocation.handlers

import com.google.gson.GsonBuilder
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.sdk.common.Screen
import io.hackle.android.support.assertThrows
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNull
import strikt.assertions.isTrue

class ScreenInvocationHandlersTest {

    private lateinit var core: HackleAppCore
    private val gson = GsonBuilder().serializeNulls().create()

    @Before
    fun setup() {
        core = mockk(relaxUnitFun = true)
    }

    private fun request(command: String, parameters: Map<String, Any?>? = null): InvocationRequest {
        val map = mapOf<String, Any>(
            "_hackle" to mapOf(
                "command" to command,
                "parameters" to parameters,
                "browserProperties" to mapOf("url" to "https://hackle.io")
            )
        )
        return InvocationRequest.parse(gson.toJson(map))
    }

    // Screen

    @Test
    fun `SetCurrentScreenInvocationHandler - 화면을 설정한다`() {
        // given
        every { core.setCurrentScreen(any()) } answers { }
        val sut = SetCurrentScreenInvocationHandler(core)
        val params = mapOf<String, Any?>("screenName" to "Main", "className" to "MainActivity")

        // when
        val response = sut.invoke(request("setCurrentScreen", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
        verify(exactly = 1) {
            core.setCurrentScreen(Screen.builder("Main", "MainActivity").build())
        }
    }

    @Test
    fun `SetCurrentScreenInvocationHandler - screenName이 없으면 예외를 던진다`() {
        val sut = SetCurrentScreenInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("setCurrentScreen", mapOf("className" to "Foo")))
        }
    }

    @Test
    fun `SetCurrentScreenInvocationHandler - className이 없으면 예외를 던진다`() {
        val sut = SetCurrentScreenInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("setCurrentScreen", mapOf("screenName" to "Foo")))
        }
    }

    // DevTools

    @Test
    fun `ShowUserExplorerInvocationHandler - showUserExplorer를 호출한다`() {
        // given
        every { core.showUserExplorer() } answers { }
        val sut = ShowUserExplorerInvocationHandler(core)

        // when
        val response = sut.invoke(request("showUserExplorer"))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
        verify(exactly = 1) { core.showUserExplorer() }
    }

    @Test
    fun `HideUserExplorerInvocationHandler - hideUserExplorer를 호출한다`() {
        // given
        every { core.hideUserExplorer() } answers { }
        val sut = HideUserExplorerInvocationHandler(core)

        // when
        val response = sut.invoke(request("hideUserExplorer"))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
        verify(exactly = 1) { core.hideUserExplorer() }
    }
}
