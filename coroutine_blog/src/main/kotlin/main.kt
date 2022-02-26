import kotlin.coroutines.*

fun main() {

    val coroutineContext =
        FirstContext() + EmptyCoroutineContext + SecondContext() + ThirdContext() + FourthContext()
    val createCoroutine = suspend {
        println("A------------")
        "B"
    }.createCoroutine(object : Continuation<String> {
        override val context: CoroutineContext
            get() = coroutineContext

        override fun resumeWith(result: Result<String>) {
            println("${result.getOrNull()}----------------")
            context[FirstContext]?.first()
            context[SecondContext]?.second()
            context[ThirdContext]?.third()
            context[FourthContext]?.fourth()
        }
    })
    createCoroutine.resume(Unit)

    println(coroutineContext)
}

class FirstContext : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<FirstContext>

    fun first() {
        println("第一个上下文------------------")
    }
}

class SecondContext : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<SecondContext>

    fun second() {
        println("第二个上下文------------------")
    }
}

class ThirdContext : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ThirdContext>

    fun third() {
        println("第三个上下文------------------")
    }
}

class FourthContext : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<FourthContext>

    fun fourth() {
        println("第四个上下文------------------")
    }
}