import coroutine.CoroutineInterface;
import coroutine.SuspendAnonymous;

public class DemoRun {
    public static void main(String[] args) {
        CoroutineInterface completion = new CoroutineInterface() {
            @Override
            public void resumeWith() {
                System.out.println("A-------------");
            }
        };
        SuspendAnonymous suspendAnonymous = new SuspendAnonymous(completion) {

            @Override
            public void resumeWith() {
                invokeSuspend();
                mCoroutineInterface.resumeWith();
            }

            public void invokeSuspend() {
                System.out.println("B-------------");
            }
        };
        CoroutineInterface coroutineInterface = SuspendAnonymous.creatCoroutune(suspendAnonymous);
        coroutineInterface.resumeWith();
    }
}
