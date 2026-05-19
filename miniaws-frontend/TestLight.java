import io.github.sceneview.node.LightNode;
import io.github.sceneview.SceneView;
public class TestLight {
    public void test(SceneView view) {
        LightNode light = new LightNode(view.getEngine(), com.google.android.filament.LightManager.Type.DIRECTIONAL);
        view.addChild(light);
    }
}
