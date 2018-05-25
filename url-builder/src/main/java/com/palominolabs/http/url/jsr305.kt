package com.palominolabs.http.url

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class NotThreadSafe

@MustBeDocumented
@kotlin.annotation.Target(AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.BINARY)
annotation class ThreadSafe
