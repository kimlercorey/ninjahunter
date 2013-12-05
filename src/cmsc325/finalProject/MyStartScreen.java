/*
 * Filename:    MyStartScreen.java
 * Author:      Eduardo R. Rivas, Bryan J. VerHoven, KC
 * Class:       CMSC 325
 * Date:        4 Dec 2013
 * Assignment:  Final Project
 */

package cmsc325.finalProject;
 
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
 
public class MyStartScreen extends AbstractAppState implements ScreenController {
 
    Nifty nifty;
    private Screen screen;
    private NinjaHunter app;
 
    public MyStartScreen() {
    }
 
    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }
 
    public void onStartScreen() {
        //Unused
    }
 
    public void onEndScreen() {
        //Unused
    }
 
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (NinjaHunter) app;
    }
 
    // Launch game after clicking on start button
    public void startGame(String nextScreen) {
        nifty.gotoScreen(nextScreen);
        app.LoadMainGame();
        app.isRunning = true;
    }
    
    /* Update player name when player button is clicked
    public void playerName(String nameOfPlayer) {
        //reset all defaults
        app.displayPlayerName = nameOfPlayer;
        app.money = 1000;
        app.luckyCharm = 0.0;
        app.colorCoinState = false;
        app.imageName = "table.jpg";
        app.initTableMaterial();
        // only reset coin if executed from pause screen
        if (nifty.getCurrentScreen().getScreenId().equals("pause")) {
            app.resetCoin = true;
            app.initCoin();
        }
    }*/
    
    // Resume game when player clicks on Resume
    public void resumeGame(String nextScreen) {
        nifty.gotoScreen(nextScreen);
        app.isRunning = true;
    }
 
    // Quit game if player clicks on Quit
    public void quitGame() {
        //app.stopAmbientAudio();
        app.stop();
    }
    
    /* Change screen when player clicks on shop
    public void enterShop(String nextScreen) {
        nifty.gotoScreen(nextScreen);
    }*/
    
    /* Change screen when player clicks on exit shop
    public void exitShop(String nextScreen) {
        nifty.gotoScreen(nextScreen);
    }*/
}
