package eg;

public class KotlinEqualsJava {

    void demo() {
        SuspendLambda<String> suspendLambda = new SuspendLambda<String>(null) {
            @Override
            public BaseContinuationImpl<String> create(Continuation<String> continuation) {
                return new SuspendLambda<String>(continuation) {
                    @Override
                    void resumeWith() {

                    }

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
        Continuation<String> completion = new Continuation<>() {
            @Override
            public void resumeWith() {

            }
        };
        BaseContinuationImpl coroutine = suspendLambda.createCoroutine(completion);
        coroutine.resumeWith();
    }

    abstract class SuspendLambda<T> extends BaseContinuationImpl<T> {
        Continuation<T> mContinuation;

        public SuspendLambda(Continuation<T> continuation) {
            mContinuation = continuation;
        }

        public abstract BaseContinuationImpl create(Continuation<T> continuation);

        public BaseContinuationImpl createCoroutine(Continuation<T> completion) {
            return new SafeContinuation(createCoroutineUnintercepted(completion).intercepted());
        }

        public BaseContinuationImpl createCoroutineUnintercepted(Continuation<T> completion) {
            return create(completion);
        }

    }

    abstract class BaseContinuationImpl<T> extends Continuation<T> {
        BaseContinuationImpl<T> intercepted() {
            return this;
        }
    }


    abstract class Continuation<T> {
        abstract void resumeWith();
    }

    class SafeContinuation<T> extends BaseContinuationImpl<T> {
        public Continuation<T> delegate;

        public SafeContinuation(Continuation<T> completion) {
            this.delegate = completion;
        }

        @Override
        public void resumeWith() {

        }
    }
}
