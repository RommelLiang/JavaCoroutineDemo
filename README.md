### Kotlin协程
> 本质上，协程是轻量级的线程

这就是[Kotlin官方对](https://www.kotlincn.net/docs/reference/coroutines/basics.html)协程的定义。

但是这等于什么都没说，它究竟是什么？从一个Android开发者的眼光来看：可以把它理解为时一个线程调度API——就是一个Kotlin的语言特性，或者说编程思想。Kotlin通过它为我们封装了一套线程Api。（虽然在线程调度是它的主要使用场景，但是事实上远不止如此）。这时，可能你就要反驳了：不对啊，我用了那么多次协程，根本就没有线程啊！不要慌，我们今天就把Android中Kotlin协程的面具给扒下来。看看它到底是个什么东西。

### 什么是协程
阅读本小节一定要了解Kotlin高阶函数！！！！
相关推荐：

* [官方文档](https://www.kotlincn.net/docs/reference/functions.html)
* [Kotlin中的高阶函数、内置高阶函数和内联](https://juejin.cn/post/7065982112767148068)
* [lambda表达式与Kotlin高阶函数](https://juejin.cn/post/6844903910327451662)

或者你干脆把高阶函数或者Lambda表达式，当做是一个匿名内部类，里面的代码块都会放在类里面的一个方法里。

在开始之前，首先回想一下Kotlin协程的使用方法:

```
GlobalScope.launch {
	doSomething()
}
```
就是这么简单。但实际上它只是个`CoroutineScope`（协程作用域我们下文再讲）的扩展函数，实际上最终是通过调用createCoroutineUnintercepted(R,Continuation)来实现的。调用链很简单，这里就不展开。

```
package kotlin.coroutines.intrinsics

public actual fun <R, T> (suspend R.() -> T).createCoroutineUnintercepted(
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> {
    val probeCompletion = probeCoroutineCreated(completion)
    return if (this is BaseContinuationImpl)
        create(receiver, probeCompletion)
    else {
        createCoroutineFromSuspendFunction(probeCompletion) {
            (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, it)
        }
    }
}
```

但是这个方法稍微有点“复杂”，我们换一个简单的方式创建协程。使用最原始的方式创建：

```
public fun <T> (suspend () -> T).createCoroutine(
    completion: Continuation<T>
): Continuation<Unit> =
 SafeContinuation(createCoroutineUnintercepted(completion).intercepted(), COROUTINE_SUSPENDED)
```
这是一个**挂起函数的扩展函数**。如果你对挂起函数和扩展函数不太理解，建议你先阅读[Kotlin中的高阶函数、内置高阶函数和内联](https://juejin.cn/post/7065982112767148068)和[lambda表达式与Kotlin高阶函数](https://juejin.cn/post/6844903910327451662)，或者直接看[官方文档](https://www.kotlincn.net/docs/reference/functions.html)。

接下来我们看如何用这个函数实现一个协程：

```
fun main(args: Array<String>) {
    suspend {
        println("A----------------${Thread.currentThread().name}")
    }.createCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Unit>) {
            println("B----------------${Thread.currentThread().name}")
        }
    }).resume(Unit)
    println("C----------------${Thread.currentThread().name}")
}

out：
A----------------main
B----------------main
C----------------main
```

非常简单，创建一个匿名函数，调用createCoroutine方法并传入一个匿名内部类Continuation。最后，调用resume方法，协程就启动了。但是，通过ABC三个打印输出，这好像和Android中的不太一样。不过这不是本小节的重点。这里我们只关注协程的创建。

#### 协程的创建流程
在开始看协程的创建之前，我们先来聊聊高阶函数。如果你了解高阶函数，你一定知道它的字节码实现就是匿名内部类，你直接把它当做是一个匿名内部类就好了！当然，挂起函数也是：

```
package kotlin.coroutines.jvm.internal

// Suspension lambdas inherit from this class
internal abstract class SuspendLambda(
    public override val arity: Int,
    completion: Continuation<Any?>?
) : ContinuationImpl(completion), FunctionBase<Any?>, SuspendFunction {
    constructor(arity: Int) : this(arity, null)

    public override fun toString(): String =
        if (completion == null)
            Reflection.renderLambdaToString(this) // this is lambda
        else
            super.toString() // this is continuation
}

// State machines for named suspend functions extend from this class
internal abstract class ContinuationImpl(
    completion: Continuation<Any?>?,
    private val _context: CoroutineContext?
) : BaseContinuationImpl(completion) {
    constructor(completion: Continuation<Any?>?) : this(completion, completion?.context)

    public override val context: CoroutineContext
        get() = _context!!

    @Transient
    private var intercepted: Continuation<Any?>? = null

    public fun intercepted(): Continuation<Any?> =
        intercepted
            ?: (context[ContinuationInterceptor]?.interceptContinuation(this) ?: this)
                .also { intercepted = it }

    protected override fun releaseIntercepted() {
        val intercepted = intercepted
        if (intercepted != null && intercepted !== this) {
            context[ContinuationInterceptor]!!.releaseInterceptedContinuation(intercepted)
        }
        this.intercepted = CompletedContinuation // just in case
    }
}
```

上面代码在`ContinuationImpl.kt`中，注意看SuspendLambda的注释Suspension lambdas inherit from this class（Lambda的挂起函数继承该类）。没错，我们的` suspend {...}`就继承于此。你可以在它的toString方法里打上断点，然后运行`println("${suspend {}}")`就能看到断点被执行到了。而它的父类`ContinuationImpl `。注释就不用翻译了吧！它们最终继承自`BaseContinuationImpl`。现在还没到分析它的时候，但请记住：`BaseContinuationImpl `非常重要！但是请留意它们的继承关系：

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/de6c40e28bcd45f1b0b465c42973e249~tplv-k3u1fbpfcp-zoom-1.image)

也可以理解为这就是` suspend {...}`，毕竟它是个匿名内部类嘛。

好了，马上开始分析createCoroutine方法。它是一个扩展函数，通过上面对` suspend {...}`解释，该方法可以概括为：**SuspendLambda的扩展函数，接收一个Continuation<T>实例，并返回一个Continuation<Unit>**。如果你不理解扩展函数，那么你可以粗暴的把它和下面的Java代码画等号：

```
class SuspendLambda<T> {
	Continuation createCoroutine(Continuation<T> completion) {
            return new SafeContinuation(completion);
       }
}
```
就是一个SuspendLambda实例调用它自己的方法创建一个新的Continuation并返回了。而这个`SafeContinuation`，它也继承自`Continuation`。

那么整个逻辑就是：ContinuationA(挂起函数)调用自己的一个方法，该方法接受一个ContinuationB(就是通过匿名内部类方式创建的completion)，然后再创建一个ContinuationC(SafeContinuation)并将其返回。紧接着我们会使用resume方法启动SafeContinuation：

```
public inline fun <T> Continuation<T>.resume(value: T): Unit =
    resumeWith(Result.success(value))
```

它也是一个扩展函数，并且调用了Continuation的resumeWith方法。这个Continuation就是ContinuationC(SafeContinuation)，那么它对应的实现就是SafeContinuation里的resumeWith，也就是下面SafeContinuation代码的关键代码一处了：

```
internal actual class SafeContinuation<in T>
internal actual constructor(
    private val delegate: Continuation<T>,
    initialResult: Any?
) : Continuation<T>, CoroutineStackFrame {
   //...
   //关键代码一
    public actual override fun resumeWith(result: Result<T>) {
        while (true) { // lock-free loop
            val cur = this.result // atomic read
            when {
                cur === UNDECIDED -> if (RESULT.compareAndSet(this, UNDECIDED, result.value)) return
                cur === COROUTINE_SUSPENDED -> if (RESULT.compareAndSet(this, COROUTINE_SUSPENDED, RESUMED)) {
                    delegate.resumeWith(result)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }
	//...
}
```
还记得createCoroutine方法里是如何创建SafeContinuation的吗？`SafeContinuation(createCoroutineUnintercepted(completion).intercepted(), COROUTINE_SUSPENDED)`。这里的COROUTINE_SUSPENDED就和when表达式对应上了。`SafeContinuation. resumeWith`最终会调用`delegate.resumeWith(result)`。这个delegate，是通过`createCoroutineUnintercepted(completion).intercepted()`创建的，它是一个代理，也是一个Continuation。接着看createCoroutineUnintercepted，它也是一个(suspend () -> T)的扩展函数，那么实际上就相当于是SuspendLambda的扩展函数，道理和`createCoroutine`:

```
@SinceKotlin("1.3")
public actual fun <T> (suspend () -> T).createCoroutineUnintercepted(
    completion: Continuation<T>
): Continuation<Unit> {
    val probeCompletion = probeCoroutineCreated(completion)
    return if (this is BaseContinuationImpl)
        create(probeCompletion)
    else
        createCoroutineFromSuspendFunction(probeCompletion) {
            (this as Function1<Continuation<T>, Any?>).invoke(it)
        }
}
```

代码实现如上，需要注意的是，该方法是用`expect`[关键字](https://www.kotlincn.net/docs/reference/keyword-reference.html)修饰的，是平台相关的。上面的代码就是[具体实现](https://sourcegraph.com/github.com/JetBrains/kotlin@master/-/blob/libraries/stdlib/jvm/src/kotlin/coroutines/intrinsics/IntrinsicsJvm.kt)。

继续看代码：首先调用了probeCoroutineCreated:

```
internal fun <T> probeCoroutineCreated(completion: Continuation<T>): Continuation<T> {
    return completion
```

代码很简单。接着看里面的判断语句`(this is BaseContinuationImpl)`。this就是`suspend {}`，当然，它就是个`BaseContinuationImpl`。那么` create(probeCompletion)`调用的就是`BaseContinuationImpl`里定义的了：

```
public open fun create(completion: Continuation<*>): Continuation<Unit> {
        throw UnsupportedOperationException("create(Continuation) has not been overridden")
    }

public open fun create(value: Any?, completion: Continuation<*>): Continuation<Unit> {
        throw UnsupportedOperationException("create(Any?;Continuation) has not been overridden")
    }
```
只有定义，没有实现啊！！让我们把

```
suspend {
     println("A----------------${Thread.currentThread().name}")
}
```

反编译为Java。代码太多了，截个图贴上：

![image.png](https://raw.githubusercontent.com/RommelLiang/JavaCoroutineDemo/main/img/611645678760_.pic_hd.jpg)

这里实现了create和invokeSuspend方法。invokeSuspend 方法里就有我们的println代码，很明显，该方法就是执行协程体力代码的地方。create方法也很简单，就是创建了个新的var2（Continuation实例）返回了。var2和它所在的实例一样，也是一个SuspendLambda。紧接着就是调用 var2的intercepted方法，根据上文中的继承关系，这个方法的实现在`ContinuationImpl`中：

```
public fun intercepted(): Continuation<Any?> =
        intercepted
            ?: (context[ContinuationInterceptor]?.interceptContinuation(this) ?: this)
                .also { intercepted = it }
```

先忽略这个context上下文，这里可以简单的理解为就是返回了this，也就是var2。那么，SafeContinuation的`delegate`就是它了。

如果你对上面的创建流程还是不是太清楚，我用Java代码把它描述一遍，源码详情请看：[Github-KotlinEqualsJava](https://github.com/RommelLiang/JavaCoroutineDemo/blob/main/src/eg/KotlinEqualsJava.java)（注意，这只是一个流程的粗略描述！！）。这里由于篇幅原因就不贴代码了。

到此，创建流程就结束了，接下来就是启动了。

#### 协程的启动流程
启动流程上面已经提到过是通过扩展函数resume实现的

```
public inline fun <T> Continuation<T>.resume(value: T): Unit =
    resumeWith(Result.success(value))
```
最终它会调用SafeContinuation.resumeWith方法，这在前面已经见过了。

接着就是继续看SafeContinuation.resumeWith的代码了。回到delegate.resumeWith(result)。而delegate，也就是var2，本身也是个SuspendLambda。那么理所当然，它的resumeWith调用它的间接父类`BaseContinuationImpl. resumeWith`方法，具体实现如下：

```
public final override fun resumeWith(result: Result<Any?>) {
        // This loop unrolls recursion in current.resumeWith(param) to make saner and shorter stack traces on resume
        var current = this
        var param = result
        while (true) {
            probeCoroutineResumed(current)
            with(current) {
            	   //注释一
                val completion = completion!! 
                    try {
                        //注释二
                        val outcome = invokeSuspend(param)
                        if (outcome === COROUTINE_SUSPENDED) return
                        Result.success(outcome)
                    } catch (exception: Throwable) {
                        Result.failure(exception)
                    }
                releaseIntercepted() // this state machine instance is terminating
                //注释三
                if (completion is BaseContinuationImpl) {
                    current = completion
                    param = outcome
                } else {
			 //注释四
                    completion.resumeWith(outcome)
                    return
                }
            }
        }
}
```

首先看注释一，这里的completion来自构造函数，实际就是经过在我们调用最开始`suspend{  }.createCoroutin(Continuation)`代码中的Continuation，也就是那个匿名内部类。

接着看注释二，此处执行了invokeSuspend，注意当前这个BaseContinuationImpl实力，它就是开头的`suspend{  }`，就是那个挂起函数。而在上文中，我们提到过它反编译成Java后的代码，这次我们把invokeSuspend给贴出来：

```
int label;

@Nullable
public final Object invokeSuspend(@NotNull Object $result) {
    Object var4 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
    switch(this.label) {
        case 0:
            ResultKt.throwOnFailure($result);
            StringBuilder var10000 = (new StringBuilder()).append("A----------------");
            Thread var10001 = Thread.currentThread();
            Intrinsics.checkNotNullExpressionValue(var10001, "Thread.currentThread()");
            String var2 = var10000.append(var10001.getName()).toString();
            boolean var3 = false;
            System.out.println(var2);
            return Unit.INSTANCE;
        default:
            throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
        }
}
```

重点在switch，label只有定义没有赋值所以它为0。可以看到，invokeSuspend就是执行挂起函数里面的代码块的地方。

接下来看注释三，completion是我们创建的匿名内部类，是直接通过Continuation创建的，就是一个Continuation直接实现。并不是一个BaseContinuationImpl，所以代码会执行到注释四。

注释四处就更简单了，直接执行resumeWith方法，也就是执行了我们开头的`println("B----------------${Thread.currentThread().name}")`

到这一步，协程就执行完毕了。

#### 用Java实现一个伪协程流程

如果上面的一大堆流转把你绕晕了，多看几遍源码就好了。打上断点一步一步追踪就行了。但是为了从全局了解它的运转流程。这里用Java代码演示一下协程的运转流程(伪)。
首先定义一个协程接口：

```
public interface CoroutineInterface {
    public void resumeWith();
}
```

接着定义个抽象类，用来指代挂起函数：
```
public abstract class SuspendAnonymous implements CoroutineInterface {
    public CoroutineInterface mCoroutineInterface;

    public SuspendAnonymous(CoroutineInterface coroutineInterface) {
        this.mCoroutineInterface = coroutineInterface;
    }

    public abstract void invokeSuspend();

    public static CoroutineInterface creatCoroutune(SuspendAnonymous suspendAnonymous){
        return new CoroutineInterface() {
            @Override
            public void resumeWith() {
                suspendAnonymous.resumeWith();
            }
        };
    }
}
```

最后，在main函数里运行：

```
public static void main(String[] args) {
	 //创建匿名内部类(指代挂起函数)
        SuspendAnonymous suspendAnonymous = new SuspendAnonymous(new CoroutineInterface() {
        	//创建匿名内部类，指代createCoroutine的Continuation参数
            @Override
            public void resumeWith() {
                System.out.println("B-------------");
            }
        }) {
		//（挂起函数）实现resumeWith
            @Override
            public void resumeWith() {
                invokeSuspend();
                mCoroutineInterface.resumeWith();
            }

		//（挂起函数）实现invokeSuspend
		//用来指代挂起函数代码块
            public void invokeSuspend() {
                System.out.println("A-------------");
            }
        };
        CoroutineInterface coroutineInterface = SuspendAnonymous.creatCoroutune(suspendAnonymous);
        coroutineInterface.resumeWith();
        System.out.println("C-------------");
}

out：
A-------------
B-------------
C-------------
```

极度精简后的流程大致就是这么个意思。Github上有[详细代码](https://github.com/RommelLiang/JavaCoroutineDemo/blob/main/src/DemoRun.java)

看到这里你一定会恍然大悟，就这？这不就是一个接口回调吗？搞这么复杂，那到底是怎么执行异步任务的呢？别着急，马上就开始讲协程的上下文和调度器。但在此之前先总结一下协程是什么：

#### 概括总结协程是什么？
协程没有脱离线程，它也是运行在线程中的；它并不是一个什么新奇的东西，而是一种编程思想。可以狭隘的讲是Kotlin为了解决某个问题而提供的一整套封装好的Api，从上文中的分析就不难发现，它甚至就只是一个接口回调！。而所要解决的问题就是解决并发问题，让并发任务更简单（像使用同步代码一样使用并发）。


### 协程的上下文和调度器








