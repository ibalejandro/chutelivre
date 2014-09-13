package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Dome;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jme3.ui.Picture;

/**
 * test
 * @author normenhansen
 */
public class Main extends SimpleApplication {
    
    private BulletAppState bulletAppState;
    
    //Balón
    private Geometry ballGeom;
    private boolean isBallMoving;
    private RigidBodyControl ballRigid;
    
    //Chute
    private long powerTime = 0;
    private int power = 0;
    
    //Barrera
    private Node barrier = new Node();
    //private ArrayList<Node> playersInBarrier = new ArrayList<Node>();
    private int signBarrier = 1;
    private int barrierSpeed = 5;
    private RigidBodyControl barrierRigid;
    
    //Arco
    private Node goal = new Node();
    private Geometry scoreLimit;
    private boolean scored = false;
    
    //Indicadores
    private Geometry hBallGeom;
    private Geometry vBallGeom;
    private Geometry [] powerIndicatorGeom = new Geometry[11];
    private boolean horizontalSet = false;
    private boolean verticalSet = false;
    private boolean powerSet = false;
    private int indicatorSign = 1;
    
    //Arquero
    private Node goalKeeper;
    private int signKeeper = -1;
    private RigidBodyControl goalKeeperRigid;
    
    //Puntaje
    private BitmapText scoreText;
    private int goles = 0;
    
    //Límite del campo
    private Geometry stadiumLimit;
    
    //Fotos gol y perder
    private Picture goalPic;
    private Picture losePic;
    private boolean removeGoalPic = false;
    private boolean removeLosePic = false;
    private boolean losePicShown = false;
    
    //Cámara
    private boolean leftCam = false;
    private boolean rightCam = false;
    private boolean followCam = false;
    private boolean initCam = true;
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);
    
        //Poner para que la cámara se mueva más rapidito
        flyCam.setMoveSpeed(100f);
        cam.setLocation(new Vector3f(0f, 13f, 120f));
        flyCam.setEnabled(false);
        
        isBallMoving = false;
        
        initField();
        initBall();
        initKeys();
        initBarrier();
        initGoal();
        initIndicators();
        initGoalKeeper();
        initScore();
        initStadiumLimit();
        initTribune();
    }

    @Override
    public void simpleUpdate(float tpf) {
        //Saber si hay gol
        if (!scored) {
            if (!canScore() && isBallMoving && !losePicShown) {
                //Perdí reset.
                losePicShown = true;
                losePic = new Picture("HUD Picture");
                losePic.setImage(assetManager, "Textures/lose2.jpg", true);
                losePic.setWidth(settings.getWidth()/2);
                losePic.setHeight(settings.getHeight()/2);
                losePic.setPosition(settings.getWidth()/4, settings.getHeight()/4);
                guiNode.attachChild(losePic);
                new LoseMessage().start();
                //newRound(true);
            }
            else {
                Sphere ballClone = new Sphere(32, 32, 1f);
                Geometry ballCloneGeom = new Geometry("BallClone", ballClone);
                ballCloneGeom.setLocalTranslation(ballGeom.getLocalTranslation().x,
                                                  ballGeom.getLocalTranslation().y,
                                                  ballGeom.getLocalTranslation().z);
                CollisionResults results = new CollisionResults();
                BoundingVolume bv = scoreLimit.getWorldBound();
                ballCloneGeom.collideWith(bv, results);
                if (results.size() > 0) {
                    scored = true;
                    System.out.println("GOLAZOOOOO");
                    goalPic = new Picture("HUD Picture");
                    goalPic.setImage(assetManager, "Textures/golazo2.jpg", true);
                    goalPic.setWidth(settings.getWidth()/2);
                    goalPic.setHeight(settings.getHeight()/2);
                    goalPic.setPosition(settings.getWidth()/4, settings.getHeight()/4);
                    guiNode.attachChild(goalPic);
                    new GoalMessage().start();
                }
            }
        }
        
        
        // Mover la barrera
        if (barrier.getLocalTranslation().x >= 25f) signBarrier = -1;
        if (barrier.getLocalTranslation().x <= -37f) signBarrier = 1;
        //barrier.move(barrierSpeed * sign * tpf, 0 * tpf, 0 * tpf);
        //barrierRigid.setPhysicsLocation(new Vector3f(barrier
          //                      .getLocalTransform().getTranslation().x, 0, 0));
        barrierRigid.setLinearVelocity(new Vector3f(barrierSpeed * signBarrier, 0f, 0f));
        
        // Mover el balón
        /*if (isBallMoving) {
            ballGeom.rotate(5f * tpf, 5f * tpf, 5f * tpf);
            System.out.println("Chuto con power" + power);
            ballGeom.move(0 * tpf, 0 * tpf, -5 * (power + 1) * tpf);
        }*/
        if (powerTime != 0) power = computePower(System.currentTimeMillis());
        //System.out.println("Power: " + power);
        
        
        //Movimiento de la cámara
        if (isBallMoving) {
            if (initCam) {
                cam.setLocation(new Vector3f(0f, 0f, 0f));
                cam.lookAt(new Vector3f(0f, 0f, -1f), new Vector3f(0f, 1f, 0f));
                cam.setLocation(new Vector3f(0f, 13f, 120f));
            }
            else if (followCam) {
               cam.setLocation(new Vector3f(ballGeom.getLocalTranslation().x,
                                         ballGeom.getLocalTranslation().y,
                                         ballGeom.getLocalTranslation().z + 50));
               cam.lookAt(new Vector3f(0f, 0f, -80f), new Vector3f(0f, 1f, 0f)); 
            }
            else if (rightCam) {
               cam.setLocation(new Vector3f(95f, 20f, 0f));
               cam.lookAt(ballGeom.getLocalTranslation(), new Vector3f(0f, 1f, 0f)); 
            }
            else if (leftCam) {
               cam.setLocation(new Vector3f(-95f, 20f, 0f));
               cam.lookAt(ballGeom.getLocalTranslation(), new Vector3f(0f, 1f, 0f)); 
            }
        }
        
        
        // Movimiento de indicadores
        if (!horizontalSet) {
            if (hBallGeom.getLocalTranslation().x >= 4.5f) indicatorSign = -1;
            if (hBallGeom.getLocalTranslation().x <= -4.5f) indicatorSign = 1;
            hBallGeom.move(barrierSpeed * indicatorSign * tpf, 0 * tpf, 0 * tpf);
        }
        else if (!verticalSet) {
            if (vBallGeom.getLocalTranslation().y >= 9.5f) indicatorSign = -1;
            if (vBallGeom.getLocalTranslation().y <= 0.5f) indicatorSign = 1;
            vBallGeom.move(0 * tpf, barrierSpeed * indicatorSign * tpf, 0 * tpf);
        }
        else if (!powerSet) {
            /*float deltaZ = 2f * power;
            powerIndicatorGeom.setLocalTranslation(new Vector3f(0f,
                                                                0f, 
                                                                70f - deltaZ));
            powerIndicatorGeom.setLocalScale(0.7f + (deltaZ/10f));*/
            for (int i = 0; i < 11; i++) {
                if (i <= power) {
                    rootNode.attachChild(powerIndicatorGeom[i]);
                }
                else {
                    rootNode.detachChild(powerIndicatorGeom[i]);
                }
            }
        }
        else if (!isBallMoving) { //Todos están seteados entonces chuto
            float xCoord, yCoord, zCoord = -3f;
            xCoord = hBallGeom.getLocalTranslation().x;
            //Se le resta 0.5 para poder q exista el tiro rastrero
            yCoord = vBallGeom.getLocalTranslation().y - 0.5f;
            ballRigid.setLinearVelocity(new Vector3f(xCoord, yCoord, zCoord)
                                                            .mult((power+2)));
            isBallMoving = true;
        }
        
        //Movimiento del arquero
        if (goalKeeper.getLocalTranslation().x >= 13f) signKeeper = -1;
        if (goalKeeper.getLocalTranslation().x <= -13f) signKeeper = 1;
        goalKeeperRigid.setLinearVelocity(new Vector3f(barrierSpeed * signKeeper, 0f, 0f));
        goalKeeperRigid.setPhysicsRotation(Matrix3f.ZERO);
        
        //Remover foto de gol
        if (removeGoalPic) {
            guiNode.detachChild(goalPic);
            removeGoalPic = false;
            incrementScore();
            newRound(false);
        }
        //Remover foto de perder
        if (removeLosePic) {
            losePicShown = false;
            guiNode.detachChild(losePic);
            removeLosePic = false;
            newRound(true);
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    /**
     * SánchezVanegas V2.0.
     * @param currentTime Tiempo actual con el que se calcula la diferencia de
     * tiempo con respecto al tiempo que se presionó la tecla de chute.
     * @return Potencia entre 0 - 10
     */
    private int computePower(long currentTime) {
        if (powerTime == 0) return 0;
        long totalMillis = (currentTime - powerTime)*barrierSpeed;
        long actualSeconds = totalMillis / 1000;
        if ((actualSeconds/10) % 2 == 0) {
            return ((int) actualSeconds % 10);
        }
        else {
            return ((int) (10 - (actualSeconds % 10)));
        }
    }
    
    private void initKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Kick", new KeyTrigger(KeyInput.KEY_SPACE));
        
        inputManager.addListener(actionListener, "Kick", "Left", "Right", "Up",
                                                                        "Down");
    }
    
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Kick")) {
                if (!keyPressed) {
                    /*isBallMoving = !isBallMoving;
                    powerTime = System.currentTimeMillis() - powerTime;
                    System.out.println("Me demoré " + powerTime + " millis");
                    //Modificamos la variable kick = true;*/
                    //powerTime = 0;
                    //ballRigid.setLinearVelocity(new Vector3f(0f, 1f, -1f).mult(3*(power+1)));
                    if (!horizontalSet) horizontalSet = true;
                    else if (!verticalSet) {
                        verticalSet = true;
                        powerTime = System.currentTimeMillis();
                    }
                    else if (!powerSet) {
                        powerSet = true;
                        powerTime = 0;
                    }
                }
                else {
                    //powerTime = System.currentTimeMillis();
                }
            }
            if (name.equals("Left")) {
                leftCam = true;
                rightCam = false;
                followCam = false;
                initCam = false;
            }
            if (name.equals("Right")) {
                leftCam = false;
                rightCam = true;
                followCam = false;
                initCam = false;
            }
            if (name.equals("Up")) {
                leftCam = false;
                rightCam = false;
                followCam = true;
                initCam = false;
            }
            if (name.equals("Down")) {
                leftCam = false;
                rightCam = false;
                followCam = false;
                initCam = true;
            }
        }
    };
            
    private void initBall() {
        Sphere ball = new Sphere(32, 32, 1f);
        ballGeom = new Geometry("Ball", ball);
        
        Material mat = new Material(assetManager,
                                    "Common/MatDefs/Misc/Unshaded.j3md");
        
        Texture ballTexture = assetManager.loadTexture("Textures/ball2.jpg");
        mat.setTexture("ColorMap", ballTexture);
        
        ballGeom.setMaterial(mat);
        //playerNode.attachChild(ballGeom);
        rootNode.attachChild(ballGeom);
        
        ballGeom.setLocalTranslation(new Vector3f(0.0f, 5f, 80f));
        
        ballRigid = new RigidBodyControl(1f);
        ballGeom.addControl(ballRigid);
        ballRigid.setFriction(1f);
        //ballRigid.setLinearDamping(0.1f);
        ballRigid.setAngularDamping(0.8f);
        ballRigid.setRestitution(0.5f);
        
        bulletAppState.getPhysicsSpace().add(ballRigid);
        //ballRigid.setLinearVelocity(new Vector3f(0f, 1f, -1f).mult(20));
    }
    
    private void initField() {
        Box field = new Box(45f, 0f, 90f);
        Geometry fieldGeometry = new Geometry("Field", field);
        
        Material mat = new Material(assetManager,
                                    "Common/MatDefs/Misc/Unshaded.j3md");
        Texture fieldTexture = assetManager.loadTexture("Textures/grass.jpg");
        mat.setTexture("ColorMap", fieldTexture);
        
        fieldGeometry.setMaterial(mat);
        
        rootNode.attachChild(fieldGeometry);
        
        RigidBodyControl fieldRigid = new RigidBodyControl(0f);
        fieldGeometry.addControl(fieldRigid);
        fieldRigid.setFriction(1f);
        fieldRigid.setRestitution(0.5f);
        
        bulletAppState.getPhysicsSpace().add(fieldRigid);
    }
    
    private void initBarrier() {
        for (int i = 0; i < 3; i++) {
            Node player = (Node) assetManager
                                          .loadModel("Models/Oto/Oto.mesh.xml");
            //player.setLocalScale(0.5f);
            player.setLocalTranslation(6.5f * i, 4.8f, 0f);
            Material mat = new Material(assetManager,
                                    "Common/MatDefs/Misc/Unshaded.j3md");
            Texture fieldTexture = assetManager.loadTexture("Textures/brick.jpg");
            mat.setTexture("ColorMap", fieldTexture);
            player.setMaterial(mat);
            barrier.attachChild(player);
            
            //RigidBodyControl barrierRigid = new RigidBodyControl(1f);
            //player.addControl(barrierRigid);
            //barrierRigid.setFriction(1f);
            //barrierRigid.setRestitution(0.5f);

            //bulletAppState.getPhysicsSpace().add(barrierRigid);
        }
        //barrierRigid = new RigidBodyControl(0f);
        //barrier.addControl(barrierRigid);
        //CollisionShapeFactory.createDynamicMeshShape(barrier);
        rootNode.attachChild(barrier);
        //barrierRigid = new RigidBodyControl(CollisionShapeFactory
          //                                      .createDynamicMeshShape(barrier), 0);
        barrierRigid = new RigidBodyControl(99999999f);
        barrier.addControl(barrierRigid);
        
        bulletAppState.getPhysicsSpace().add(barrierRigid);
    }
    
    private void initGoal() {
        // Materiales para malla y postes
        Material netMat = new Material(assetManager, 
                                    "Common/MatDefs/Misc/Unshaded.j3md");
        Texture netTexture = assetManager.loadTexture("Textures/net.png");
        netMat.setTexture("ColorMap", netTexture);
        netMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        
        Material poleMat = new Material(assetManager, 
                                    "Common/MatDefs/Misc/Unshaded.j3md");
        Texture poleTexture = assetManager.loadTexture("Textures/pole.jpg");
        poleMat.setTexture("ColorMap", poleTexture);
        
        //Parte izquierda
        Box left = new Box(0.1f, 6.5f, 5f);
        Geometry leftGeom = new Geometry("leftGoal", left);
        leftGeom.setMaterial(netMat);
        leftGeom.setLocalTranslation(new Vector3f(-15f, 6.5f, 0f));
        leftGeom.setQueueBucket(Bucket.Transparent);
        goal.attachChild(leftGeom);
        //leftGeom.addControl(new RigidBodyControl(0f));
        //bulletAppState.getPhysicsSpace().add(leftGeom);
        
        //Parte derecha
        Box right = new Box(0.1f, 6.5f, 5f);
        Geometry rightGeom = new Geometry("rightGoal", right);
        rightGeom.setMaterial(netMat);
        rightGeom.setLocalTranslation(new Vector3f(15f, 6.5f, 0f));
        rightGeom.setQueueBucket(Bucket.Transparent);
        goal.attachChild(rightGeom);
        //rightGeom.addControl(new RigidBodyControl(0f));
        //bulletAppState.getPhysicsSpace().add(rightGeom);
        
        //Parte fondo
        Box back = new Box(15f, 6.5f, 0.1f);
        Geometry backGeom = new Geometry("backGoal", back);
        backGeom.setMaterial(netMat);
        backGeom.setLocalTranslation(new Vector3f(0.0f, 6.5f, -5f));
        backGeom.setQueueBucket(Bucket.Transparent);
        goal.attachChild(backGeom);
        //backGeom.addControl(new RigidBodyControl(0f));
        //bulletAppState.getPhysicsSpace().add(backGeom);
        
        //Parte techo
        Box roof = new Box(15f, 0.1f, 5f);
        Geometry roofGeom = new Geometry("roofGoal", roof);
        roofGeom.setMaterial(netMat);
        roofGeom.setLocalTranslation(new Vector3f(0.0f, 13f, 0f));
        roofGeom.setQueueBucket(Bucket.Transparent);
        goal.attachChild(roofGeom);
        //roofGeom.addControl(new RigidBodyControl(0f));
        //bulletAppState.getPhysicsSpace().add(roofGeom);
        
        //Palo izquierdo
        Cylinder leftPole = new Cylinder(32, 32, 0.5f, 13f);
        Geometry leftPoleGeom = new Geometry("leftPole", leftPole);
        leftPoleGeom.setMaterial(poleMat);
        leftPoleGeom.rotate((float)((90 * Math.PI) / 180), 0f, 0f);
        leftPoleGeom.setLocalTranslation(new Vector3f(-15f, 6.5f, 5f));
        goal.attachChild(leftPoleGeom);
        //leftPoleGeom.addControl(new RigidBodyControl(0f));
        //bulletAppState.getPhysicsSpace().add(leftPoleGeom);
        
        //Palo derecho
        Cylinder rightPole = new Cylinder(32, 32, 0.5f, 13f);
        Geometry rightPoleGeom = new Geometry("rightPole", rightPole);
        rightPoleGeom.setMaterial(poleMat);
        rightPoleGeom.rotate((float)((90 * Math.PI) / 180), 0f, 0f);
        rightPoleGeom.setLocalTranslation(new Vector3f(15f, 6.5f, 5f));
        goal.attachChild(rightPoleGeom);
        //rightPoleGeom.addControl(new RigidBodyControl(0f));
        //bulletAppState.getPhysicsSpace().add(rightPoleGeom);
        
        //Palo arriba (Travesaño)
        Cylinder roofPole = new Cylinder(32, 32, 0.5f, 31f);
        Geometry roofPoleGeom = new Geometry("roofPole", roofPole);
        roofPoleGeom.setMaterial(poleMat);
        roofPoleGeom.rotate(0f, (float)((90 * Math.PI) / 180), 0f);
        roofPoleGeom.setLocalTranslation(new Vector3f(0f, 13f, 5f));
        goal.attachChild(roofPoleGeom);
        //roofPoleGeom.addControl(new RigidBodyControl(0f));
        //bulletAppState.getPhysicsSpace().add(roofPoleGeom);
        
        
        goal.setLocalTranslation(0, 0, -80);
        goal.addControl(new RigidBodyControl(0f));
        bulletAppState.getPhysicsSpace().add(goal);
        rootNode.attachChild(goal);
        
        //Inicializamos el límite de gol
        Box score = new Box(14f, 6f, 4.8f);
        scoreLimit = new Geometry("scoreLimit", score);
        Material scoreMat = new Material(assetManager,
                                        "Common/MatDefs/Misc/Unshaded.j3md");
        Texture texture = assetManager.loadTexture("Textures/transparent.png");
        scoreMat.setTexture("ColorMap", texture);
        scoreMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        scoreLimit.setMaterial(scoreMat);
        scoreLimit.setQueueBucket(Bucket.Transparent); 
        scoreLimit.setLocalTranslation(0f, 6f, -80.2f);
        rootNode.attachChild(scoreLimit);
        
    }
    
    private void initIndicators() {
        Box horizontal = new Box(5f, 0.5f, 0.5f);
        Geometry horizontalGeom = new Geometry("horizontal", horizontal);
        horizontalGeom.setLocalTranslation(new Vector3f(0.0f, 0.5f, 85f));
        Material materialHorizontal = new Material(assetManager, 
                                       "Common/MatDefs/Misc/Unshaded.j3md");
        materialHorizontal.setColor("Color", ColorRGBA.Yellow);
        horizontalGeom.setMaterial(materialHorizontal);
        //playerNode.attachChild(horizontalGeom);
        rootNode.attachChild(horizontalGeom);
        
        Box vertical = new Box(0.5f, 5f, 0.5f);
        Geometry verticalGeom = new Geometry("horizontal", vertical);
        verticalGeom.setLocalTranslation(new Vector3f(5.5f, 5f, 85f));
        Material materialVertical = new Material(assetManager, 
                                       "Common/MatDefs/Misc/Unshaded.j3md");
        materialVertical.setColor("Color", ColorRGBA.Green);
        verticalGeom.setMaterial(materialVertical);
        //playerNode.attachChild(verticalGeom);
        rootNode.attachChild(verticalGeom);
        
        
        Material mat = new Material(assetManager,
                                    "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        //Bolita horizontal
        Sphere horizontalBall = new Sphere(32, 32, 0.5f);
        hBallGeom = new Geometry("horizontalBall", horizontalBall);
        hBallGeom.setLocalTranslation(new Vector3f(-4.5f, 0.5f, 86f));
        hBallGeom.setMaterial(mat);
        //playerNode.attachChild(hBallGeom);
        rootNode.attachChild(hBallGeom);
        
        //Bolita vertical
        Sphere verticalBall = new Sphere(32, 32, 0.5f);
        vBallGeom = new Geometry("verticalBall", verticalBall);
        vBallGeom.setLocalTranslation(new Vector3f(5.5f, 0.5f, 86f));
        vBallGeom.setMaterial(mat);
        //playerNode.attachChild(vBallGeom);
        rootNode.attachChild(vBallGeom);
        
        //Power indicator
        
        /*Material powerMat = new Material(assetManager,
                                    "Common/MatDefs/Misc/Unshaded.j3md");
        powerMat.setColor("Color", ColorRGBA.Red);
        Dome powerSemi = new Dome(Vector3f.ZERO, 32, 32, 0.7f, false);
        powerIndicatorGeom = new Geometry("powerIndicator", powerSemi);
        powerIndicatorGeom.setLocalTranslation(new Vector3f(0f, 0f, 70f));
        powerIndicatorGeom.setMaterial(powerMat);
        rootNode.attachChild(powerIndicatorGeom);*/
        
        for (int i = 0; i < 11; i++) {
            float deltaZ = 3f * i;
            Material powerMat = new Material(assetManager,
                                    "Common/MatDefs/Misc/Unshaded.j3md");
            powerMat.setColor("Color", ColorRGBA.Red);
            Dome powerSemi = new Dome(Vector3f.ZERO, 32, 32, 0.7f + deltaZ/37f,
                                                                        false);
            powerIndicatorGeom[i] = new Geometry("powerIndicator", powerSemi);
            powerIndicatorGeom[i].setLocalTranslation(new Vector3f(0f, 0f, 
                                                                70f - deltaZ));
            
            powerIndicatorGeom[i].setMaterial(powerMat);
            if (i == 0) rootNode.attachChild(powerIndicatorGeom[i]);
        }
    }
    
    private void initGoalKeeper() {
        goalKeeper = (Node) assetManager
                                    .loadModel("Models/Oto/Oto.mesh.xml");
        //player.setLocalScale(0.5f);
        goalKeeper.setLocalTranslation(new Vector3f(0f, 4.8f, -70f));
        Material mat = new Material(assetManager,
                              "Common/MatDefs/Misc/Unshaded.j3md");
        Texture texture = assetManager.loadTexture("Textures/keeper.jpg");
        mat.setTexture("ColorMap", texture);
        goalKeeper.setMaterial(mat);
        rootNode.attachChild(goalKeeper);
        goalKeeperRigid = new RigidBodyControl(99999999f);
        goalKeeper.addControl(goalKeeperRigid);
        
        bulletAppState.getPhysicsSpace().add(goalKeeperRigid);
    }
    
    private void initScore() {
        scoreText = new BitmapText(guiFont, false);          
        //scoreText.setSize(guiFont.getCharSet().getRenderedSize());
        scoreText.setSize(40f);
        scoreText.setColor(ColorRGBA.Yellow);
        scoreText.setText("Goles: " + goles); //Está en 0 inicialmente
        scoreText.setLocalTranslation(settings.getWidth() - 200, settings.getHeight()/2 + 350, 0);
        guiNode.attachChild(scoreText);
    }
    
    private void initStadiumLimit() {
        //Inicializamos el límite de la cancha
        Box stadium = new Box(45f, 999f, 95f);
        stadiumLimit = new Geometry("stadiumLimit", stadium);
        Material stadiumMat = new Material(assetManager,
                                        "Common/MatDefs/Misc/Unshaded.j3md");
        Texture texture = assetManager.loadTexture("Textures/transparent.png");
        stadiumMat.setTexture("ColorMap", texture);
        stadiumMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        stadiumLimit.setMaterial(stadiumMat);
        stadiumLimit.setQueueBucket(Bucket.Transparent); 
        stadiumLimit.setLocalTranslation(0f, 998f, 0f);
        rootNode.attachChild(stadiumLimit);
    }
    
    private void incrementScore() {
        goles++;
        scoreText.setText("Goles: " + goles);
        //Aumentar dificultad
        barrierSpeed += 1;
    }
    
    private void newRound(boolean reset) {
        powerTime = 0;
        power = 0;
        scored = false;
        horizontalSet = false;
        verticalSet = false;
        powerSet = false;
        isBallMoving = false;
        ballRigid.setLinearVelocity(Vector3f.ZERO);
        ballRigid.setPhysicsLocation(new Vector3f(0.0f, 5f, 80f));
        cam.setLocation(new Vector3f(0f, 0f, 0f));
        cam.lookAt(new Vector3f(0f, 0f, -1f), new Vector3f(0f, 1f, 0f));
        cam.setLocation(new Vector3f(0f, 13f, 120f));
        
        if (reset) {
            scoreText.setText("Goles: 0");
            goles = 0;
            barrierSpeed = 5;
        }
    }
    
    private boolean canScore() {
        //Preguntamos colision con caja del campo
        Sphere ballClone = new Sphere(32, 32, 1f);
        Geometry ballCloneGeom = new Geometry("BallClone", ballClone);
        ballCloneGeom.setLocalTranslation(ballGeom.getLocalTranslation().x,
                                          ballGeom.getLocalTranslation().y,
                                          ballGeom.getLocalTranslation().z);
        CollisionResults results = new CollisionResults();
        BoundingVolume bv = stadiumLimit.getWorldBound();
        ballCloneGeom.collideWith(bv, results);
        if (results.size() == 0) return false;
        
        //preguntamos si el balón está quieto
        if (ballRigid.getLinearVelocity().x == 0f &&
            ballRigid.getLinearVelocity().y == 0f &&
            ballRigid.getLinearVelocity().z == 0f) return false;
        return true;
    }
    
    
    private class GoalMessage extends Thread {
        
        @Override
        public void run() {
            try {
                this.sleep(3000);
            } catch (InterruptedException ex) {
                System.err.println("Error: " + ex.getMessage());
            }
            removeGoalPic = true;
            /*guiNode.detachChild(goalPic);
            incrementScore();
            newRound(false);*/
	}
    }
    
    private class LoseMessage extends Thread {
        
        @Override
        public void run() {
            try {
                this.sleep(3000);
            } catch (InterruptedException ex) {
                System.err.println("Error: " + ex.getMessage());
            }
            removeLosePic = true;
            /*guiNode.detachChild(goalPic);
            incrementScore();
            newRound(false);*/
	}
    }

    private void initTribune() {
        //Material tribunas
        Material yellowMat = new Material(assetManager,
                                           "Common/MatDefs/Misc/Unshaded.j3md");
        Texture texture1 = assetManager.loadTexture("Textures/tribune.jpg");
        yellowMat.setTexture("ColorMap", texture1);
        
        Material blueMat = new Material(assetManager,
                                           "Common/MatDefs/Misc/Unshaded.j3md");
        Texture texture2 = assetManager.loadTexture("Textures/tribune2.jpg");
        blueMat.setTexture("ColorMap", texture2);
        
        Material sandMat = new Material(assetManager,
                                           "Common/MatDefs/Misc/Unshaded.j3md");
        Texture texture3 = assetManager.loadTexture("Textures/athletics.jpg");
        sandMat.setTexture("ColorMap", texture3);
        
        Material sandMat2 = new Material(assetManager,
                                           "Common/MatDefs/Misc/Unshaded.j3md");
        Texture texture4 = assetManager.loadTexture("Textures/athletics2.jpg");
        sandMat2.setTexture("ColorMap", texture4);
        
        //Tribuna izquierda
        Box leftTribune = new Box(1f, 100f, 150f);
        Geometry leftTribuneGeom = new Geometry("LeftTribune", leftTribune);
        leftTribuneGeom.setMaterial(yellowMat);
        leftTribuneGeom.setLocalTranslation(-100f, -30f, 0f);
        rootNode.attachChild(leftTribuneGeom);
        
        //Tribuna derecha
        Box rightTribune = new Box(1f, 100f, 150f);
        Geometry rightTribuneGeom = new Geometry("RightTribune", rightTribune);
        rightTribuneGeom.setMaterial(yellowMat);
        rightTribuneGeom.setLocalTranslation(100f, -30f, 0f);
        rootNode.attachChild(rightTribuneGeom);
        
        //Tribuna norte
        Box northTribune = new Box(100f, 100f, 1f);
        Geometry northTribuneGeom = new Geometry("NorthTribune", northTribune);
        northTribuneGeom.setMaterial(blueMat);
        northTribuneGeom.setLocalTranslation(0f, -30f, -150f);
        rootNode.attachChild(northTribuneGeom);
        
        //Tribuna sur
        Box southTribune = new Box(100f, 100f, 1f);
        Geometry southTribuneGeom = new Geometry("SouthTribune", southTribune);
        southTribuneGeom.setMaterial(blueMat);
        southTribuneGeom.setLocalTranslation(0f, -30f, 150f);
        rootNode.attachChild(southTribuneGeom);
        
        //Arena
        Box sandLeft = new Box(30f, 1f, 150f);
        Geometry sandLeftGeom = new Geometry("sandLeft", sandLeft);
        sandLeftGeom.setMaterial(sandMat);
        sandLeftGeom.setLocalTranslation(-70f, -2f, 0f);
        rootNode.attachChild(sandLeftGeom);
        
        RigidBodyControl sandLeftRigid = new RigidBodyControl(0f);
        sandLeftGeom.addControl(sandLeftRigid);
        sandLeftRigid.setFriction(1f);
        sandLeftRigid.setRestitution(0.5f);
        
        bulletAppState.getPhysicsSpace().add(sandLeftRigid);
        
        Box sandRight = new Box(30f, 1f, 150f);
        Geometry sandRightGeom = new Geometry("sandRight", sandRight);
        sandRightGeom.setMaterial(sandMat);
        sandRightGeom.setLocalTranslation(70f, -2f, 0f);
        rootNode.attachChild(sandRightGeom);
        
        RigidBodyControl sandRightRigid = new RigidBodyControl(0f);
        sandRightGeom.addControl(sandRightRigid);
        sandRightRigid.setFriction(1f);
        sandRightRigid.setRestitution(0.5f);
        
        bulletAppState.getPhysicsSpace().add(sandRightRigid);
        
        Box sandNorth = new Box(100f, 1f, 35f);
        Geometry sandNorthGeom = new Geometry("sandNorth", sandNorth);
        sandNorthGeom.setMaterial(sandMat2);
        sandNorthGeom.setLocalTranslation(0f, -2.01f, -120f);
        rootNode.attachChild(sandNorthGeom);
        
        RigidBodyControl sandNorthRigid = new RigidBodyControl(0f);
        sandNorthGeom.addControl(sandNorthRigid);
        sandNorthRigid.setFriction(1f);
        sandNorthRigid.setRestitution(0.5f);
        
        bulletAppState.getPhysicsSpace().add(sandNorthRigid);
        
        Box sandSouth = new Box(100f, 1f, 35f);
        Geometry sandSouthGeom = new Geometry("sandSouth", sandSouth);
        sandSouthGeom.setMaterial(sandMat2);
        sandSouthGeom.setLocalTranslation(0f, -2.01f, 120f);
        rootNode.attachChild(sandSouthGeom);
        
        RigidBodyControl sandSouthRigid = new RigidBodyControl(0f);
        sandSouthGeom.addControl(sandSouthRigid);
        sandSouthRigid.setFriction(1f);
        sandSouthRigid.setRestitution(0.5f);
        
        bulletAppState.getPhysicsSpace().add(sandSouthRigid);
    }
}
