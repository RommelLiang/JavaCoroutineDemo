package eg;

public class KotlinEqualsJava {

    class SuspendLambda<T> {
        Continuation createCoroutine(Continuation<T> completion) {
            return new SafeContinuation(completion);
        }
    }


    interface Continuation<T> {
    }

    class SafeContinuation<T> implements Continuation<T> {
        public Continuation<T> delegate;

        public SafeContinuation(Continuation<T> completion) {
            this.delegate = completion;
        }
    }
}
