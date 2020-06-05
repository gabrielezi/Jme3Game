package mygame;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import org.w3c.dom.Text;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class MyStartScreen extends AbstractAppState implements ScreenController {

    private static int answer;
    public static int lives = 5;
    public static int score = 0;
    private Nifty nifty;
    private Application app;
    private Screen screen;

    public static void setAnswer(int answer1){
        answer = answer1;
    }

    public MyStartScreen() {
        /** You custom constructor, can accept arguments */
    }

    public void startGame(String nextScreen) {
        nifty.gotoScreen(nextScreen);  // switch to another screen
    }

    public void quitGame() {
        app.stop();
    }

    public void submit(String nextScreen){
        try{
            TextField input = nifty.getCurrentScreen().findNiftyControl("TextField", TextField.class);
            int inputText = Integer.parseInt(input.getRealText());
            if(inputText == answer) {
                score = score + 5;
                updateStats();
                nifty.gotoScreen(nextScreen);
            }
            else{
                lives--;
                updateStats();
            }
        }catch (NullPointerException | NumberFormatException e){
            lives--;
            updateStats();
        };
    }

    public void updateStats(){
        Element element = (Element) nifty.getCurrentScreen().findElementById("Text");
        element.getRenderer(TextRenderer.class).setText("lives: " + lives + ", score: " + score);

        Element element1 = (Element) nifty.getScreen("hud").findElementById("TextStart");
        element1.getRenderer(TextRenderer.class).setText("lives: " + lives + ", score: " + score);
    }

    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    public void onStartScreen() {
    }

    public void onEndScreen() {
    }

    /** jME3 AppState methods */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.app = app;
    }

    @Override
    public void update(float tpf) {
        //if (screen.getScreenId().equals("hud"))
        try {
            Element niftyElement = nifty.getCurrentScreen().findElementByName("score");
            // Display the time-per-frame -- this field could also display the score etc...
            niftyElement.getRenderer(TextRenderer.class).setText((int)(tpf*100000) + "");
        }catch (NullPointerException a){

        }
    }
}
