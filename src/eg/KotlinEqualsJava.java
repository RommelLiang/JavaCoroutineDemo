package eg;

public class KotlinEqualsJava {

    void demo() {
        /**首先创建一个匿名内部类
         * 对应挂起函数:
            suspend {
                println("A----------------${Thread.currentThread().name}")
            }
         **/
        SuspendLambda<String> suspendLambda = new SuspendLambda<String>(null) {
            @Override
            public BaseContinuationImpl<String> create(Continuation<String> continuation) {
                return new SuspendLambda<String>(continuation) {
                    @Override
                    void resumeWith() {

                    }

                    //实现create方法
                    @Override
                    public BaseContinuationImpl create(Continuation<String> continuation) {
                        return new SuspendLambda<String>(continuation) {

                            @Override
                            void resumeWith() {

                            }

                            @Override
                            public BaseContinuationImpl create(Continuation<String> continuation) {
                                return null;
                            }
                        };
                    }
                };
            }

            @Override
            void resumeWith() {

            }
        };
        /**再创建一个匿名内部类
         * 对应
         object : Continuation<Unit> {
            override val context: CoroutineContext
            get() = EmptyCoroutineContext

             override fun resumeWith(result: Result<Unit>) {
                println("B----------------${Thread.currentThread().name}")
             }
         }
         **/
        Continuation<String> completion = new Continuation<>() {
            @Override
            public void resumeWith() {

            }
        };
        //接着就是调用createCoroutine方法
        Continuation coroutine = suspendLambda.createCoroutine(completion);
        coroutine.resumeWith();
    }

    //SuspendLambda用来描述匿名挂起函数的实现
    abstract class SuspendLambda<T> extends BaseContinuationImpl<T> {
        Continuation<T> mContinuation;

        public SuspendLambda(Continuation<T> continuation) {
            mContinuation = continuation;
        }


        public Continuation createCoroutine(Continuation<T> completion) {
            //创建SafeContinuation
            return new SafeContinuation(createCoroutineUnintercepted(completion).intercepted());
        }

        public BaseContinuationImpl createCoroutineUnintercepted(Continuation<T> completion) {
            //调用create，该方法在BaseContinuationImpl中定义
            // 但是在demo中的匿名内部类中实现
            return create(completion);
        }

    }

    abstract class BaseContinuationImpl<T> extends Continuation<T> {
        public abstract BaseContinuationImpl create(Continuation<T> continuation);
        BaseContinuationImpl<T> intercepted() {
            return this;
        }
    }


    //假装它就是协程
    abstract class Continuation<T> {
        abstract void resumeWith();
    }

    class SafeContinuation<T> extends Continuation<T> {
        public Continuation<T> delegate;

        public SafeContinuation(Continuation<T> completion) {
            this.delegate = completion;
        }

        @Override
        public void resumeWith() {
            delegate.resumeWith();
        }

    }
}
