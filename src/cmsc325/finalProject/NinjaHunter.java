/*
 * Filename:    NinjaHunter.java
 * Author:      Eduardo R. Rivas, Bryan J. VerHoven, KC
 * Class:       CMSC 325
 * Date:        4 Dec 2013
 * Assignment:  Final Project
 */
package cmsc325.finalProject;

/** import libraries */
import com.jme3.app.SimpleApplication;
import static com.jme3.app.SimpleApplication.INPUT_MAPPING_EXIT;
import com.jme3.app.state.AppState;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

/** NinjaHunter Main Class */
public class NinjaHunter extends SimpleApplication
        implements PhysicsCollisionListener {
    

    /** Set global variables */
    public static boolean isDebug = false;
    Material mat_brick;
    private RigidBodyControl bullet_phy;
    private static final Sphere sphere;
    Spatial[] ninja;
    RigidBodyControl[] ninjaBody;
    CollisionShape[] ninjaShape;
    int[] targetScore;
    private String lastA = null;
    private String lastB = null;
    private static final SphereCollisionShape sphereCollisionShape;
    private Spatial sceneModel;
    private BulletAppState bulletAppState;
    private RigidBodyControl landscape;
    private CharacterControl player;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;
    Nifty nifty;
    MyStartScreen screenControl;
    public boolean isRunning = false;
    public int score = 0;
    public int highscore;
    public static int levelTime = 300; //300 seconds = 5 minutes
    public static long start = 0;
    public int bulletsFired = 0;  
    public static File file;
    private AudioNode backgroundMusic;
    private AudioNode shot;
    private AudioNode hit;

    static {
      // Initialize the bullet geometry and collision
      sphere = new Sphere(16, 16, 0.2f, true, false);
      sphere.setTextureMode(Sphere.TextureMode.Projected);
      sphereCollisionShape = new SphereCollisionShape(0.2f);
      
      file = new File("high_score.txt");
    }

    /** Main method */
    public static void main(String[] args) {
        // Create an instance of the main game
        NinjaHunter ninjaHunterApplication = new NinjaHunter();

        // Setup the launch screen
        AppSettings ninjaHunterSettings = new AppSettings(true);
        ninjaHunterSettings.setResolution(1360, 768);
        ninjaHunterSettings.setSettingsDialogImage("Interface/Images/Ninja.jpg");
        ninjaHunterSettings.setTitle("NinjaHunter - Team Asteroids");
        ninjaHunterApplication.setSettings(ninjaHunterSettings);
        ninjaHunterApplication.setDisplayFps(isDebug);
        ninjaHunterApplication.setDisplayStatView(isDebug);
        
        

        
        
        // Start the launch screen
        ninjaHunterApplication.start();
    }

    /** Init App Method */
    public void simpleInitApp() {
        //disable flycam
        flyCam.setEnabled(false);
        
        highscore = readHighscore(); 

        
        // Initialize GUI
        NiftyJmeDisplay display = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, viewPort); //create jme-nifty-processor
        nifty = display.getNifty();
        nifty.addXml("Interface/xmlNameGoes.xml");
        nifty.gotoScreen("start");
        screenControl = (MyStartScreen) nifty.getScreen("start").getScreenController();
        stateManager.attach((AppState) screenControl);
        guiViewPort.addProcessor(display);
    }
    
    /** Loads the main game */
    public void LoadMainGame() { 
        // Set up Physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().addCollisionListener(this);
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        
        // Attach HUD screen to the stateManager
        MyStartScreen screenControl3 = (MyStartScreen) nifty.getScreen("hud").getScreenController();
        stateManager.attach((AppState) screenControl3);

        // set background color (sky)
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));

        // set move speed
        flyCam.setEnabled(true);
        flyCam.setMoveSpeed(100);

        initAudio();
        
        // main setup function calls
        setUpKeys();
        setUpLight();
        initMaterials();
        createNinjas();
        createScene();
        loadPlayer();
        initCrossHairs();
        
    }

    /** Add light to the scene */
    private void setUpLight() {
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);
        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
    }

    /** Set up the key/mouse input */
    private void setUpKeys() {
        // Delete the default Esc-to-close mapping
        inputManager.deleteMapping(INPUT_MAPPING_EXIT);
        
        // Add mappings for actions
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Escape", new KeyTrigger(KeyInput.KEY_ESCAPE));
        
        // Add action listeners
        inputManager.addListener(actionListener, "Left");
        inputManager.addListener(actionListener, "Right");
        inputManager.addListener(actionListener, "Up");
        inputManager.addListener(actionListener, "Down");
        inputManager.addListener(actionListener, "Jump");
        inputManager.addListener(actionListener, "shoot");
        inputManager.addListener(actionListener, "Escape");
    }

    /** Action responses */
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String binding, boolean keyPressed, float tpf) {
            if (isRunning) {
                if (binding.equals("Escape")) {
                    isRunning = false;
                    inputManager.setCursorVisible(true);
                    flyCam.setEnabled(false);
                    nifty.gotoScreen("pause");
                    MyStartScreen screenControl2 = (MyStartScreen) nifty.getScreen("pause").getScreenController();
                    stateManager.attach((AppState) screenControl2);
                }
                if (binding.equals("shoot") && !keyPressed) {
                    makeBullet();
                } else if (binding.equals("Left")) {
                    left = keyPressed;
                } else if (binding.equals("Right")) {
                  right = keyPressed;
                } else if (binding.equals("Up")) {
                  up = keyPressed;
                } else if (binding.equals("Down")) {
                  down = keyPressed;
                } else if (binding.equals("Jump")) {
                  player.jump();
                }
            }
        }
    };

    /** Update Loop! */
    @Override
    public void simpleUpdate(float tpf) {
        if (isRunning) {
            
            // play the ambient sound continuously
            backgroundMusic.play();
            
            flyCam.setEnabled(true);
            inputManager.setCursorVisible(false);
            Vector3f camDir = cam.getDirection().clone().multLocal(0.6f);
            Vector3f camLeft = cam.getLeft().clone().multLocal(0.4f);
            walkDirection.set(0, 0, 0);
            if (left)  { walkDirection.addLocal(camLeft); }
            if (right) { walkDirection.addLocal(camLeft.negate()); }
            if (up)    { walkDirection.addLocal(camDir); }
            if (down)  { walkDirection.addLocal(camDir.negate()); }
            player.setWalkDirection(walkDirection);
            cam.setLocation(player.getPhysicsLocation()); 
            
            // Update Timer
            if (System.currentTimeMillis() - start >= 1000) {
                // Every second, decrease level time by 1
                levelTime -= 1;
                start = System.currentTimeMillis();
            }
            
            // Exit game if 5 minutes has passed
            if (levelTime == 0) {
                System.exit(0);
            }
            
            // If the current score is greater than highscore, set the highscore
            if (score > highscore ) { highscore = increaseScore(score);}
            
            // Update HUD resources
            nifty.getCurrentScreen().findElementByName("score").getRenderer(TextRenderer.class).setText("Score: " + score + "      Local High Score :"+ highscore);
            nifty.getCurrentScreen().findElementByName("timeLeft").getRenderer(TextRenderer.class).setText("Elapsed Time: " + levelTime/60 + ":" + levelTime%60);
            nifty.getCurrentScreen().findElementByName("bulletsFired").getRenderer(TextRenderer.class).setText("Bullets Fired: " + bulletsFired + "/unlimited");
            nifty.getCurrentScreen().findElementByName("target0Score").getRenderer(TextRenderer.class).setText("Ninja0: " + targetScore[0]);
            nifty.getCurrentScreen().findElementByName("target1Score").getRenderer(TextRenderer.class).setText("Ninja1: " + targetScore[1]);
            nifty.getCurrentScreen().findElementByName("target2Score").getRenderer(TextRenderer.class).setText("Ninja2: " + targetScore[2]);
            nifty.getCurrentScreen().findElementByName("target3Score").getRenderer(TextRenderer.class).setText("Ninja3: " + targetScore[3]);
            nifty.getCurrentScreen().findElementByName("target4Score").getRenderer(TextRenderer.class).setText("Ninja4: " + targetScore[4]);
        }
    }
  
    /** Initialize the materials used in the scene */
    public void initMaterials() {
        mat_brick = new Material( 
        assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat_brick.setTexture("ColorMap", 
        assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"));
    }
  
    /** Create ninjas initially in the scene */
    public void createNinjas() {
        // Create arrays
        ninjaBody = new RigidBodyControl[10];
        ninja = new Spatial[10];
        ninjaShape = new CollisionShape[10];
        targetScore = new int[10];

        for (int i=0;i<5;i++) {
            ninja[i] = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
            ninja[i].setName("Ninja");
            ninja[i].setLocalTranslation((i*20.0f),30.0f,5.0f); //(i*20.0f),2.0f,(i*5.0f)
            ninja[i].rotate(0f,180*FastMath.DEG_TO_RAD,0f);
            ninja[i].scale(0.05f, 0.05f, 0.005f);

            // Set up collision detection for the ninja
            ninjaShape[i] = CollisionShapeFactory.createDynamicMeshShape((Spatial) ninja[i]);
            ninjaBody[i] = new RigidBodyControl(ninjaShape[i], 1f);
            ninja[i].addControl(ninjaBody[i]);

            // Attach ninja to scene and add to physics space
            rootNode.attachChild(ninja[i]);
            bulletAppState.getPhysicsSpace().add(ninja[i]);
            
            // Initialize the score variable
            targetScore[i] = 0;
        }
    }
  
    /** Create the scene */
    public void createScene() {
        // Load the scene and adjust its size
        sceneModel = assetManager.loadModel("Scenes/main.j3o");
        sceneModel.setLocalScale(2f);

        // Set up collision detection for the scene
        CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape((Node) sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        // Attach the scene and add to Physics space
        rootNode.attachChild(sceneModel);
        bulletAppState.getPhysicsSpace().add(landscape);
    }
  
    /** Initialize the player */
    public void loadPlayer() {
        // Set up collision detection for the player
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);
        player.setGravity(30);
        player.setPhysicsLocation(new Vector3f(40, 10, 55));

        // Add to physics space
        bulletAppState.getPhysicsSpace().add(player);
    }
  
    /** Set up the crosshairs */
    protected void initCrossHairs() {
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); 
        ch.setLocalTranslation(
        settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
        settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }
  
    /* Creates the bullet */
    public void makeBullet() {
        
        // play sound
        shot.playInstance();
            
        // Create a bullet geometry and attach to scene
        Geometry bullet_geo = new Geometry("bullet", sphere);
        bullet_geo.setMaterial(mat_brick);
        rootNode.attachChild(bullet_geo);

        // Position the bullet
        bullet_geo.setLocalTranslation(cam.getDirection().scaleAdd(3, cam.getLocation()));
        bullet_phy = new RigidBodyControl(1f);

        // Add bullet to physics space.
        bullet_geo.addControl(bullet_phy);
        bulletAppState.getPhysicsSpace().add(bullet_phy);
        cam.setLocation(player.getPhysicsLocation());

        // Shoots the bullet
        bullet_phy.setLinearVelocity(cam.getDirection().mult(100));
        
        // Increment bullets Fired resource by 1
        bulletsFired += 1;
        
        score -= 25;
    }
  
    /** Collision handler */
    public void collision(PhysicsCollisionEvent event) {
          try {
              // Declare variables local to the function
              final Spatial nodea = event.getNodeA();
              final Spatial nodeb = event.getNodeB();
              Spatial ninjaNode = null;
              int deadNinja = 0;

              // If one node is the Ninja and the other is the bullet...
              if ((nodea.getName() == "Ninja" && nodeb.getName() == "bullet")
                   || (nodea.getName() == "bullet" && nodeb.getName() == "Ninja")) {

                  // store the ninja node
                  if (nodea.getName() == "Ninja") {
                      ninjaNode = event.getNodeA();
                  }else {
                      ninjaNode = event.getNodeB();
                  }

                  // loop to check which ninja was hit
                  for (int z = 0;z<5;z++) {
                      if (ninjaNode.equals(ninja[z])) {
                          // which ninja died?
                          deadNinja = z;
                          
                          // increment score for that target!
                          targetScore[z] += 100;
                      }
                  }
                  
                  //increment score
                  score += 100;
                  
                  // play sound
                  hit.playInstance();
    

                  // remove ninja from physics and scene
                  rootNode.detachChild(ninjaNode);
                  bulletAppState.getPhysicsSpace().remove(ninjaNode);

                  // relocate ninja back in the air
                  relocateNinja(deadNinja);
              }

          } catch (Exception e) {
              // dreams computers compute when asked "do nothing"
          }
      };
    
   public void initAudio() {
        // hit a target
        hit = new AudioNode(assetManager, "Sounds/hit.ogg");
        hit.setPositional(false);
        hit.setLooping(false);
        hit.setVolume(2);
        rootNode.attachChild(hit);
        
        shot = new AudioNode(assetManager, "Sounds/shot.ogg");
        shot.setPositional(false);
        shot.setLooping(false);
        shot.setVolume(2);
        rootNode.attachChild(shot);

        // Background music - The Original Ninja Hunter Song
        backgroundMusic = new AudioNode(assetManager, "Sounds/ninjahunter.ogg", false);
        backgroundMusic.setPositional(false);
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(3);
        rootNode.attachChild(backgroundMusic);
    }

    // function to stop the ambient sound
    public void stopbackgroundMusic() {
        backgroundMusic.stop();
    }
  
    /** Function that respawns the dead ninja */
    public void relocateNinja(int ninjaArrayNumber) {
        //generate random number to spawn ninja at location
        Random r = new Random();
        int Low = 0;
        int High = 80;
        int randomLocation = r.nextInt(High-Low)+Low;

        // add ninja to scene again
        ninja[ninjaArrayNumber] = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        ninja[ninjaArrayNumber].setName("Ninja");
        ninja[ninjaArrayNumber].setLocalTranslation((randomLocation),30.0f,5.0f); //(i*20.0f),2.0f,(i*5.0f)
        ninja[ninjaArrayNumber].rotate(0f,180*FastMath.DEG_TO_RAD,0f);
        ninja[ninjaArrayNumber].scale(0.05f, 0.05f, 0.05f);
        //ninja[ninjaArrayNumber].
        rootNode.attachChild(ninja[ninjaArrayNumber]);

        // Set up collision detection for the ninja
        ninjaShape[ninjaArrayNumber] = CollisionShapeFactory.createDynamicMeshShape((Spatial) ninja[ninjaArrayNumber]);
       // ninjaShape[ninjaArrayNumber].
        
        ninjaBody[ninjaArrayNumber] = new RigidBodyControl(ninjaShape[ninjaArrayNumber], 1f);
        
        ninjaBody[ninjaArrayNumber].setAngularDamping(100f);
        ninja[ninjaArrayNumber].addControl(ninjaBody[ninjaArrayNumber]);

        
        // Attach the ninja and get Physics space
        bulletAppState.getPhysicsSpace().add(ninja[ninjaArrayNumber]);
    }
    

    // Read the current Highscore
    public int readHighscore() {
        
        int i = 0;
        
        try {
 	  // only do this if file exists
            if (!file.exists()) {
		return 0;
            }
 
	  Scanner scanner = new Scanner(file);
            try { if(scanner.hasNextInt()){
                    i = scanner.nextInt();
            }} catch (Exception e) { /* do nothing */}
            
	} catch (Exception e) { /* Do Nothing */}
        
        return i;
    }
    
    
    // Set the highscore file to the current score
    public Integer increaseScore(Integer amount) {
        try {
 	  // create highscore if needed
            if (!file.exists()) {
		file.createNewFile();
            }
 
	  FileWriter fw = new FileWriter(file.getAbsoluteFile());
	  BufferedWriter bw = new BufferedWriter(fw);
	  bw.write(amount.toString());
	  bw.close();
 
	} catch (IOException e) { /* Do Nothing */}
        
        return amount;
    }
    
    
  }




// TODO: (KC) (Optional) Enhancements 
// TODO: (KC) (Optional) Sound Effects

/* 
 * TODO: 2-5 page design document (See below. Assigned to All 3 of us)
 *  
 * Well-written word document describing: 
 *   - Your overall design
 *   - Your test plan, including test data and results
 *   - Your approach, lessons learned, design strengths, limitations and suggestions for future improvement and alternative approaches
 */


