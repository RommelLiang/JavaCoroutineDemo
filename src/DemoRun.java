import coroutine.CoroutineInterface;
import coroutine.SuspendAnonymous;

public class DemoRun {
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
}
