package coroutine;

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
