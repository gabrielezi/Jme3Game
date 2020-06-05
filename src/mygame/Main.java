package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.SoftTextDialogInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.SoftTextDialogInputListener;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.*;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.controls.button.builder.ButtonBuilder;
import de.lessvoid.nifty.controls.textfield.builder.TextFieldBuilder;
import de.lessvoid.nifty.controls.textfield.filter.delete.TextFieldDeleteFilter;
import de.lessvoid.nifty.controls.textfield.filter.input.TextFieldInputCharFilter;
import de.lessvoid.nifty.controls.textfield.filter.input.TextFieldInputCharSequenceFilter;
import de.lessvoid.nifty.controls.textfield.filter.input.TextFieldInputFilter;
import de.lessvoid.nifty.controls.textfield.format.TextFieldDisplayFormat;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.DefaultScreenController;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Sample 7 - how to load an OgreXML model and play an animation,
 * using channels, a controller, and an AnimEventListener. */
public class Main extends SimpleApplication implements ActionListener, PhysicsCollisionListener, AnimEventListener{

    private BulletAppState bulletAppState;
    //character
    CharacterControl character;
    Node model;
    //temp vectors
    Vector3f walkDirection = new Vector3f();
    //animation
    AnimChannel animationChannel;
    AnimControl animationControl;
    float airTime = 0;
    //camera
    boolean left = false, right = false, up = false, down = false;
    ChaseCamera chaseCam;

    private static Box floor;
    private RigidBodyControl    floor_phy;
    /** Prepare Materials */
    Material mat_brick;
    Material floor_mat;

    /** dimensions used for bricks and wall */
    private static final float brickLength = 4f;
    private static final float brickWidth  = 4f;
    private static final float brickHeight = 6f;

    NiftyJmeDisplay niftyDisplay;
    Nifty nifty;

    private static String[] questionsArray = {"5*5", "6*7", "1*3"};
    static Map<String, Integer> aMap = new HashMap<String, Integer>();

    public static int questionsCounter = 0;
    public static float previousDistance = 0;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        aMap.put(questionsArray[0] , 25);
        aMap.put(questionsArray[1] , 42);
        aMap.put(questionsArray[2] , 3);
      //  System.out.println(questionsArray[0]);
       // System.out.println(aMap.get(questionsArray[0]));

        niftyDisplay = new NiftyJmeDisplay(
                assetManager, inputManager, audioRenderer, guiViewPort);
        nifty = niftyDisplay.getNifty();
        guiViewPort.addProcessor(niftyDisplay);
        flyCam.setDragToRotate(true);

        nifty.loadStyleFile("nifty-default-styles.xml");
        nifty.loadControlFile("nifty-default-controls.xml");

        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);


//        BitmapFont labelFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
//
//        BitmapText label = new BitmapText(labelFont);
//
//        label.setSize(1);
//
//        label.setText("lives: " + MyStartScreen.lives);
//
//        label.setLocalTranslation(0,5,0);

//        rootNode.attachChild(label);

        nifty.addScreen("hud", new ScreenBuilder("hud") {{
            controller(new DefaultScreenController());
            setupKeys();
            createLight();
            createSky();
            initFloor();
            initWalls();

            createCharacter();
            setupChaseCamera();
            setupAnimationController();
            layer(new LayerBuilder("top"){{
                childLayoutVertical();
                panel(new PanelBuilder("panel_top_left") {{
                    childLayoutVertical();
                    height("70%");
                    width("70%");

                    text(new TextBuilder("TextStart") {{
                        text("lives: " + MyStartScreen.lives + ", score: " + MyStartScreen.score);
                        font("Interface/Fonts/Default.fnt");
                        onActiveEffect(new EffectBuilder("textSize"){{
//                                effectValue("100");
//                                effectParameter("factor", "10");
//                                effectParameter("startSize", "2");
//                                effectParameter("endSize", "2");
//                                effectParameter("textSize", "1");
                        }});
                        wrap(true);
                        height("100%");
                        width("100%");
                        textHAlignLeft();
                    }});
                }});
            }});
        }}.build(nifty));
        nifty.gotoScreen("hud");
    }

    private static void createMyScreen(final Nifty nifty) {
        questionsCounter = (int)(questionsArray.length * Math.random());
        int answer = aMap.get(questionsArray[questionsCounter]);
        MyStartScreen.setAnswer(answer);

        nifty.addScreen("start", new ScreenBuilder("start") {{
            controller(new MyStartScreen());
            layer(new LayerBuilder("foreground") {{
                childLayoutVertical();

                panel(new PanelBuilder("panel_mid") {{
                    childLayoutCenter();
                    alignCenter();
                    height("75%");
                    width("75%");

                    text(new TextBuilder() {{
                        text(questionsArray[questionsCounter] + "=");
                        font("Interface/Fonts/Default.fnt");
                        onActiveEffect(new EffectBuilder("textSize"){{
                                effectValue("100");
                                effectParameter("factor", "10");
                                effectParameter("startSize", "2");
                                effectParameter("endSize", "2");
                                effectParameter("textSize", "1");
                        }});
                        wrap(true);
                        height("100%");
                        width("100%");
                    }});
                }});

                panel(new PanelBuilder("panel_bottom") {{
                    childLayoutHorizontal();
                    alignCenter();
                    height("25%");
                    width("75%");

                    panel(new PanelBuilder("panel_bottom_left") {{
                        childLayoutCenter();
                        valignCenter();
                        height("50%");
                        width("50%");

                        control(new TextFieldBuilder("TextField", ""){{
                            alignCenter();
                            valignCenter();
                            height("50%");
                            width("50%");
                            visibleToMouse(true);
                        }});
                    }});

                    panel(new PanelBuilder("panel_bottom_right") {{
                        childLayoutCenter();
                        valignCenter();
                        height("50%");
                        width("50%");

                        control(new ButtonBuilder("StartButton", "Submit") {{
                            alignCenter();
                            valignCenter();
                            height("50%");
                            width("50%");
                            visibleToMouse(true);
                            interactOnClick("submit(hud)");
                        }});

                    }});
                }}); // panel added
            }});
            layer(new LayerBuilder("top"){{
                childLayoutVertical();
                panel(new PanelBuilder("panel_top_left") {{
                    childLayoutVertical();
                    height("70%");
                    width("70%");

                    text(new TextBuilder("Text") {{
                        text("lives: " + MyStartScreen.lives + ", score: " + MyStartScreen.score);
                        font("Interface/Fonts/Default.fnt");
                        onActiveEffect(new EffectBuilder("textSize"){{
//                                effectValue("100");
//                                effectParameter("factor", "10");
//                                effectParameter("startSize", "2");
//                                effectParameter("endSize", "2");
//                                effectParameter("textSize", "1");
                        }});
                        wrap(true);
                        height("100%");
                        width("100%");
                        textHAlignLeft();
                    }});
                }});
            }});
        }}.build(nifty));
    }
    private void createSky() {
        rootNode.attachChild(SkyFactory.createSky(assetManager,
                "Textures/Sky/Bright/BrightSky.dds",
                SkyFactory.EnvMapType.CubeMap));
    }

    private void setupAnimationController() {
        animationControl = model.getControl(AnimControl.class);
        animationControl.addListener(this);
        animationChannel = animationControl.createChannel();
    }

    private void setupChaseCamera() {
        flyCam.setEnabled(false);
        chaseCam = new ChaseCamera(cam, model, inputManager);
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    private void createCharacter() {
        CapsuleCollisionShape capsule = new CapsuleCollisionShape(3f, 4f);
        character = new CharacterControl(capsule, 0.01f);
        model = (Node) assetManager.loadModel("Models/Oto/OtoOldAnim.j3o");
        //model.rotate(0,1.5f,0);
        model.addControl(character);
        //character.setPhysicsLocation(new Vector3f(-140, 40, -80));
        character.setPhysicsLocation(new Vector3f(0, 0, 0));
        rootNode.attachChild(model);
        getPhysicsSpace().add(character);
    }

    private void createLight() {
        Vector3f direction = new Vector3f(-0.1f, -0.7f, -1).normalizeLocal();
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(direction);
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);
    }
    private void setupKeys() {
        inputManager.addMapping("wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(this, "wireframe");
        inputManager.addMapping("CharLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("CharRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("CharUp", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("CharDown", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("CharSpace", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "CharLeft");
        inputManager.addListener(this, "CharRight");
        inputManager.addListener(this, "CharUp");
        inputManager.addListener(this, "CharDown");
        inputManager.addListener(this, "CharSpace");
    }

    public void initWalls(){

        Box box = new Box(brickLength,brickHeight,brickWidth);
        Spatial wall = new Geometry("Box", box );
        mat_brick = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat_brick.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"));
        wall.setMaterial(mat_brick);
        wall.setLocalTranslation(new Vector3f(5, 0, 10));
        wall.addControl(new RigidBodyControl(2f));
        rootNode.attachChild(wall);
        this.getPhysicsSpace().add(wall);

        Spatial wall2 = new Geometry("Box", box );
        wall2.setMaterial(mat_brick);
        wall2.setLocalTranslation(new Vector3f(-11, 0, 10));
        wall2.addControl(new RigidBodyControl(2f));
        rootNode.attachChild(wall2);
        this.getPhysicsSpace().add(wall2);

        Spatial wall3 = new Geometry("Box", box );
        wall3.setMaterial(mat_brick);
        wall3.setLocalTranslation(new Vector3f(5, 0, 18));
        //wall3.rotate(0, FastMath.HALF_PI,0);
        wall3.addControl(new RigidBodyControl(2f));
        rootNode.attachChild(wall3);
        this.getPhysicsSpace().add(wall3);

        Spatial wall4 = new Geometry("Box", box );
        wall4.setMaterial(mat_brick);
        wall4.setLocalTranslation(new Vector3f(-3, 0, 26));
        //wall4.rotate(0,1.5f,0);
        wall4.addControl(new RigidBodyControl(1.5f));
        rootNode.attachChild(wall4);
        this.getPhysicsSpace().add(wall4);

    }

    public void initFloor() {
        floor = new Box(100f, 0.1f, 100f);
        floor.scaleTextureCoordinates(new Vector2f(10, 10));
        floor_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key3 = new TextureKey("Textures/Terrain/splat/grass.jpg");
        key3.setGenerateMips(true);
        Texture tex3 = assetManager.loadTexture(key3);
        tex3.setWrap(Texture.WrapMode.Repeat);
        floor_mat.setTexture("ColorMap", tex3);

        Geometry floor_geo = new Geometry("Floor", floor);
        floor_geo.setMaterial(floor_mat);
       // floor_geo.setLocalTranslation(new Vector3f(-140, 32, -10));
        floor_geo.setLocalTranslation(new Vector3f(0, 0, 90));
        this.rootNode.attachChild(floor_geo);
        /* Make the floor physical with mass 0.0f! */
        floor_phy = new RigidBodyControl(0.0f);
        floor_geo.addControl(floor_phy);
        bulletAppState.getPhysicsSpace().add(floor_phy);
    }

    @Override
    public void simpleUpdate(float tpf) {
        Vector3f camDir = cam.getDirection().clone().multLocal(0.2f); //zingsnio dydis
        Vector3f camLeft = cam.getLeft().clone().multLocal(0.2f);
        camDir.y = 0;
        camLeft.y = 0;
        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        if (!character.onGround()) {
            airTime = airTime + tpf;
        } else {
            airTime = 0;
        }
        if (walkDirection.length() == 0) {
            if (!"stand".equals(animationChannel.getAnimationName())) {
                animationChannel.setAnim("stand", 1f);
            }
        } else {
            character.setViewDirection(walkDirection);
            if (airTime > .3f) {
                if (!"stand".equals(animationChannel.getAnimationName())) {
                    animationChannel.setAnim("stand");
                }
            } else if (!"Walk".equals(animationChannel.getAnimationName())) {
                animationChannel.setAnim("Walk", 0.7f);
                animationChannel.setSpeed(1.2f);
            }
        }
        character.setWalkDirection(walkDirection);

        float distance = model.getWorldTranslation().distance( new Vector3f(0, 0, 0));
        if((int)distance % 20 == 0 && distance != previousDistance) {
            createMyScreen(nifty);
            nifty.gotoScreen("start");
        }
        previousDistance = distance;
    }
//    public static void setEnabled(boolean enabled) {
//        Main.setEnabled(enabled);
//        auto_con=enemy.getControl(AutomaticVehicleControl.class);
//        if(!enabled){
//
//            input.setEnabled(enabled);
//            physics.setEnabled(enabled);
//            physics.getPhysicsSpace().removeCollisionListener(this);
//            auto_con.setEnabled(enabled);
//            stateManager.getState(GameObjects.class).setEnabled(enabled);
//        }
//        else if(enabled){
//            input.setEnabled(enabled);
//            physics.setEnabled(enabled);
//            physics.getPhysicsSpace().addCollisionListener(this);
//            auto_con.setEnabled(enabled);
//            stateManager.getState(GameObjects.class).setEnabled(enabled);
//        }
//    }
    @Override
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
    }

    @Override
    public void onAnimChange(AnimControl animControl, AnimChannel animChannel, String s) {

    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
    }

    @Override
    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("CharLeft")) {
            if (value) {
                left = true;
            } else {
                left = false;
            }
        } else if (binding.equals("CharRight")) {
            if (value) {
                right = true;
            } else {
                right = false;
            }
        } else if (binding.equals("CharUp")) {
            if (value) {
                up = true;

            } else {
                up = false;
            }
        } else if (binding.equals("CharDown")) {
            if (value) {
                down = true;
            } else {
                down = false;
            }
        } else if (binding.equals("CharSpace")) {
            character.jump();
        }

    }
}