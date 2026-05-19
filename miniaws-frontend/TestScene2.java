import io.github.sceneview.node.ModelNode;
import io.github.sceneview.SceneView;
import java.lang.reflect.Method;

public class TestScene2 {
    public void test(ModelNode node, SceneView view) {
        Object inst = node.getModelInstance();
        System.out.println(inst.getClass().getName());
    }
}
