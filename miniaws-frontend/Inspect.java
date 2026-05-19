import io.github.sceneview.node.ModelNode;
import java.lang.reflect.Method;
public class Inspect {
    public static void main(String[] args) {
        for (Method m : io.github.sceneview.SceneView.class.getMethods()) {
            String name = m.getName().toLowerCase();
            if (name.contains("transp") || name.contains("back") || name.contains("env") || name.contains("sky") || name.contains("shadow")) {
                System.out.println(m.getName());
            }
        }
    }
}
