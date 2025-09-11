package io.hackle.android

import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class HackleTest {

    @Test
    fun `user builder function should create user with id`() {
        val userId = "test_user_id"
        
        val result = Hackle.user(userId)
        
        expectThat(result.id).isEqualTo(userId)
    }

    @Test
    fun `user builder function with init block should create configured user`() {
        val userId = "test_user_id"
        
        val result = Hackle.user(userId) {
            property("age", 25)
            property("name", "test_name")
        }
        
        expectThat(result.id).isEqualTo(userId)
        expectThat(result.properties["age"]).isEqualTo(25)
        expectThat(result.properties["name"]).isEqualTo("test_name")
    }

    @Test
    fun `user builder function without id should create user with null id`() {
        val result = Hackle.user(null) {
            property("age", 30)
        }
        
        expectThat(result.id).isEqualTo(null)
        expectThat(result.properties["age"]).isEqualTo(30)
    }

    @Test
    fun `event builder function should create event with key`() {
        val eventKey = "test_event"
        
        val result = Hackle.event(eventKey)
        
        expectThat(result.key).isEqualTo(eventKey)
    }

    @Test
    fun `event builder function with init block should create configured event`() {
        val eventKey = "test_event"
        
        val result = Hackle.event(eventKey) {
            property("count", 5)
            property("category", "test")
        }
        
        expectThat(result.key).isEqualTo(eventKey)
        expectThat(result.properties["count"]).isEqualTo(5)
        expectThat(result.properties["category"]).isEqualTo("test")
    }

    @Test
    fun `User builder should create user with properties`() {
        val result = User.builder()
            .id("user123")
            .property("name", "John")
            .property("age", 25)
            .build()

        expectThat(result.id).isEqualTo("user123")
        expectThat(result.properties["name"]).isEqualTo("John")
        expectThat(result.properties["age"]).isEqualTo(25)
    }

    @Test
    fun `Event builder should create event with properties`() {
        val result = Event.builder("click")
            .property("button", "submit")
            .property("count", 1)
            .build()

        expectThat(result.key).isEqualTo("click")
        expectThat(result.properties["button"]).isEqualTo("submit")
        expectThat(result.properties["count"]).isEqualTo(1)
    }
}