package com.ccai.Concentration;

import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Log;
import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.engine.camera.SmoothCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.*;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.TiledSprite;
import org.andengine.entity.text.AutoWrap;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.ScrollDetector;
import org.andengine.input.touch.detector.SurfaceScrollDetector;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.adt.align.HorizontalAlign;
import org.andengine.util.adt.color.Color;
import org.andengine.util.debug.Debug;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * (c) 2013 Conan Cai
 *
 * @author Conan Cai
 * @email conan.cai@berkeley.edu
 */

/**
 * Uses AndEngine GLES2-AnchorCenter and PhysicsBox2DExtension - Nicolas Gramlich
 * https://github.com/nicolasgramlich/AndEngine/tree/GLES2-AnchorCenter
 * https://github.com/nicolasgramlich/AndEnginePhysicsBox2DExtension/tree/GLES2-AnchorCenter
 *
 * Android 2.2
 */

public class Concentration extends SimpleBaseGameActivity implements IOnSceneTouchListener, ScrollDetector.IScrollDetectorListener {

    // ===========================================================
    // Constants
    // ===========================================================
    private static int CAMERA_WIDTH;
    private static int CAMERA_HEIGHT;

    private int numCardGFXPairs = 0;
    private final Color gray = new Color(.5f,.5f,.5f);
    private Color background;
    private static final String TAG = "Concentration";

    // ===========================================================
    // Fields
    // ===========================================================
    private BuildableBitmapTextureAtlas mBitmapTextureAtlas;

    private int numCols = 4;
    private int numRows = 3;
    private int numPairsOnBoard = (numRows*numCols)/2;

    private HashMap<String, TiledTextureRegion> cardTextures = new HashMap<String, TiledTextureRegion>(numPairsOnBoard * 2);
    private ArrayList<TiledSprite> flipped = new ArrayList<TiledSprite>(2);
    private HashMap<TiledSprite, String> spriteToName = new HashMap<TiledSprite, String>();
    private HashMap<String,String> textInfo = new HashMap<String, String>();

    private boolean touchEnabled = true;
    private int tries;
    private int matched;

    // ===========================================================
    // Scene/Transitions
    // ===========================================================
    private HUD mHud;
    private Scene menuScene = null;
    private Scene levelScene = null;
    private Scene scoreScene = null;
    private Rectangle menuMask;
    private Rectangle levelMask;
    private Rectangle scoreMask;
    private HashMap<Scene,Rectangle> sceneToMask = new HashMap<Scene,Rectangle>();

    // ===========================================================
    // Text
    // ===========================================================
    private Font dFont;
    private Font dFontSmall;
    private Font hFont;
    private Font hFontSmall;
    private Text startText;
    private Text a;
    private Text p;
    private Text b;
    private Text triesText;

    // ===========================================================
    // Menu Scroll
    // ===========================================================
    private SurfaceScrollDetector mScrollDetector;
    private SmoothCamera mCamera;
    private float defaultCameraX;
    private float currCameraX;
    private ArrayList<Float> bounds = new ArrayList<Float>();
    private float maxCameraX;
    private long startTime;
    private float startX;

    // ===========================================================
    // Sound
    // ===========================================================
    private Sound correct;
    private Sound wrong;
    private Sound victory;

    // ===========================================================
    // Initialization
    // ===========================================================
    @Override
    public EngineOptions onCreateEngineOptions() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        CAMERA_WIDTH = metrics.widthPixels;
        CAMERA_HEIGHT = metrics.heightPixels;

        mCamera = new SmoothCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT, 2500, 0, 0);
        defaultCameraX = mCamera.getCenterX();
        currCameraX = defaultCameraX;

        EngineOptions options =  new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), mCamera);
        options.getAudioOptions().setNeedsSound(true);
        return options;
    }

    /**
     * Load font and resources for game here. Card images should be stored in /assets/gfx folder. Files should follow naming convention (NUM)-a.png and (NUM)-b.png for prevention and hazard, respectively.
     * Text for each card is stored in /res/raw/info.txt. Each line is a description, in the same order its corresponding image file. Line 0 corresponds to img 0-a.png. Line 1 corresponds to img 0-b.png.
     * Line 2 corresponds to img 1-a.png...and so on.
     * */
    @Override
    public void onCreateResources() {
        //FONT CREATION//////////
        this.hFont = FontFactory.createFromAsset(this.getFontManager(), this.getTextureManager(), 1024, 512, this.getAssets(), "fnt/handwritten.ttf", CAMERA_WIDTH/6, true, Color.WHITE_ARGB_PACKED_INT);
        this.hFontSmall = FontFactory.createFromAsset(this.getFontManager(), this.getTextureManager(), 512, 512, this.getAssets(), "fnt/handwritten.ttf", CAMERA_WIDTH/14, true, Color.WHITE_ARGB_PACKED_INT);
        this.dFont = FontFactory.create(this.getFontManager(), this.getTextureManager(), 512, 512, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), CAMERA_WIDTH/6, Color.WHITE_ARGB_PACKED_INT);
        this.dFontSmall = FontFactory.create(this.getFontManager(), this.getTextureManager(), 512, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), CAMERA_WIDTH/14, Color.WHITE_ARGB_PACKED_INT);
        this.hFont.load();
        this.hFontSmall.load();
        this.dFont.load();
        this.dFontSmall.load();

        //LOAD FROM CONFIG///////
        BufferedReader br = null;
        try{
            int id = getResources().getIdentifier("config", "raw", this.getPackageName());
            br = new BufferedReader(new InputStreamReader(getResources().openRawResource(id)));
            String curr;
            while ((curr = br.readLine()) != null){
                if(curr.startsWith("#") || curr.equals("")){
                    continue;
                }
                String key = curr.split(":")[0];
                String info = curr.split(":")[1];
                textInfo.put(key, info);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        } finally {
            try{
                if(br != null)br.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        String bg = textInfo.get("background_color");
        String red = bg.split(",")[0];
        String green = bg.split(",")[1];
        String blue = bg.split(",")[2];
        background = new Color(Float.parseFloat(red)/255f, Float.parseFloat(green)/255f, Float.parseFloat(blue)/255f);
        numCardGFXPairs = Integer.parseInt(textInfo.get("gfx_pairs"));

        //LOAD IMAGES////////////
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
        this.mBitmapTextureAtlas = new BuildableBitmapTextureAtlas(this.getTextureManager(), 2048, 2048, TextureOptions.BILINEAR);
        for(int i = 0; i < numCardGFXPairs; i++){
            cardTextures.put(i+"-a", BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, i+"-a.png", 2, 1));
            cardTextures.put(i+"-b", BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, i+"-b.png", 2, 1));
        } try {
            this.mBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 0, 1));
            this.mBitmapTextureAtlas.load();
        } catch (ITextureAtlasBuilder.TextureAtlasBuilderException e) {
            Debug.e(e);
        }

        //LOAD SOUND/////////////
        SoundFactory.setAssetBasePath("sfx/");
        try {
            this.correct = SoundFactory.createSoundFromAsset(this.mEngine.getSoundManager(), this, "correct.ogg");
            this.wrong = SoundFactory.createSoundFromAsset(this.mEngine.getSoundManager(), this, "wrong.ogg");
            this.victory = SoundFactory.createSoundFromAsset(this.mEngine.getSoundManager(), this, "victory.ogg");
        } catch (final IOException e) {
            Debug.e(e);
        }

        /////////////////////////
        a = new Text(CAMERA_WIDTH/2, ((CAMERA_HEIGHT/6)*5)+((CAMERA_HEIGHT/18)*2.25f), hFontSmall, "abcdefghijklmnopqrstuvwxyz.", this.getVertexBufferObjectManager());
        p = new Text(CAMERA_WIDTH/2, ((CAMERA_HEIGHT/6)*5)+((CAMERA_HEIGHT/18)*1.5f), dFontSmall, "abcdefghijklmnopqrstuvwxyz.", this.getVertexBufferObjectManager());
        b = new Text(CAMERA_WIDTH/2, ((CAMERA_HEIGHT/6)*5)+((CAMERA_HEIGHT/18)*.75f), hFontSmall, "abcdefghijklmnopqrstuvwxyz.", this.getVertexBufferObjectManager());

        menuMask = new Rectangle(CAMERA_WIDTH/2, CAMERA_HEIGHT/2, CAMERA_WIDTH, CAMERA_HEIGHT, this.getVertexBufferObjectManager());
        levelMask = new Rectangle(CAMERA_WIDTH/2, CAMERA_HEIGHT/2, CAMERA_WIDTH, CAMERA_HEIGHT, this.getVertexBufferObjectManager());
        scoreMask = new Rectangle(CAMERA_WIDTH/2, CAMERA_HEIGHT/2, CAMERA_WIDTH, CAMERA_HEIGHT, this.getVertexBufferObjectManager());
        menuMask.setScale(0);
        levelMask.setScale(0);
        scoreMask.setScale(0);
    }

    @Override
    public Scene onCreateScene() {
        menuScene = new Scene();
        //HUD////////////////
        Rectangle r1 = new Rectangle(CAMERA_WIDTH/2,CAMERA_HEIGHT/2,CAMERA_WIDTH,CAMERA_HEIGHT/9,this.getVertexBufferObjectManager());
        startText = new Text(CAMERA_WIDTH/2, r1.convertSceneCoordinatesToLocalCoordinates(CAMERA_WIDTH/2,CAMERA_HEIGHT/2)[1], hFont, "abcdefghijklmnopqrstuvwxyz", this.getVertexBufferObjectManager()){
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if(pSceneTouchEvent.isActionUp() && !pSceneTouchEvent.isActionCancel()){
                    mCamera.setCenter(defaultCameraX, CAMERA_HEIGHT/2);
                    generateLevel();
                    sceneTransition(menuScene, levelScene);
                }
                return true;
            }
        };
        startText.setText(textInfo.get("start"));
        r1.attachChild(startText);
        r1.setColor(gray);
        mHud = new HUD();
        mHud.attachChild(r1);
        mCamera.setHUD(mHud);

        //TEXT SETUP/////////////
        final Text titleText = new Text(CAMERA_WIDTH/2, (CAMERA_HEIGHT/6)*5, hFont, textInfo.get("title"), this.getVertexBufferObjectManager());
        final Text infoText = new Text(CAMERA_WIDTH/2, CAMERA_HEIGHT/6*4, dFontSmall, textInfo.get("info"), this.getVertexBufferObjectManager());
        final Text creditsText = new Text(CAMERA_WIDTH/2 + CAMERA_WIDTH, (CAMERA_HEIGHT/5)*4, dFontSmall, textInfo.get("credits")+":\nConan Cai\nconan.cai@berkeley.edu\n\n"+textInfo.get("art")+": Jennifer Kotler", this.getVertexBufferObjectManager());
        final Text helpText = new Text(CAMERA_WIDTH/2 + CAMERA_WIDTH, (CAMERA_HEIGHT/4), dFontSmall, textInfo.get("help_text"),
                new TextOptions(AutoWrap.WORDS, CAMERA_WIDTH-100, HorizontalAlign.CENTER, Text.LEADING_DEFAULT), this.getVertexBufferObjectManager());
        final Rectangle selector = new Rectangle(CAMERA_WIDTH/2, (CAMERA_HEIGHT/9)*2, CAMERA_WIDTH, CAMERA_HEIGHT/9, this.getVertexBufferObjectManager());
        selector.setColor(gray);
        Text easy = new Text(CAMERA_WIDTH/2, (CAMERA_HEIGHT/9)*3, dFont, textInfo.get("easy"), this.getVertexBufferObjectManager()){
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                numCols = 3;
                numRows = 2;
                selector.setPosition(CAMERA_WIDTH/2, (CAMERA_HEIGHT/9)*3);
                return true;
            }
        };
        Text medium = new Text(CAMERA_WIDTH/2, (CAMERA_HEIGHT/9)*2, dFont, textInfo.get("medium"), this.getVertexBufferObjectManager()){
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                numCols = 4;
                numRows = 3;
                selector.setPosition(CAMERA_WIDTH/2, (CAMERA_HEIGHT/9)*2);
                return true;
            }
        };
        Text hard = new Text(CAMERA_WIDTH/2, (CAMERA_HEIGHT/9), dFont, textInfo.get("hard"), this.getVertexBufferObjectManager()){
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                numCols = 5;
                numRows = 4;
                selector.setPosition(CAMERA_WIDTH/2, (CAMERA_HEIGHT/9));
                return true;
            }
        };

        //SCALING////////////////
        easy.setScale(0.75f);
        medium.setScale(0.75f);
        hard.setScale(0.75f);
        if(titleText.getWidth() > CAMERA_WIDTH-100){
            titleText.setScale(1 / (titleText.getWidth() / (CAMERA_WIDTH - 100)));
        } if(startText.getWidth() > CAMERA_WIDTH-100){
            startText.setScale(1/(startText.getWidth()/(CAMERA_WIDTH-100)));
        } if(creditsText.getWidth() > CAMERA_WIDTH-100){
            creditsText.setScale(1 / (creditsText.getWidth() / (CAMERA_WIDTH - 100)));
        }
        float ratio = Float.parseFloat(textInfo.get("width_card")) / Float.parseFloat(textInfo.get("height_card"));
        float rWidth = (CAMERA_WIDTH / 9) * 5;
        float rHeight = (CAMERA_HEIGHT / 5) * 3;
        float[] dims;
        float[] case1 = new float[]{rWidth, rWidth/ratio};
        float[] case2 = new float[]{rHeight*ratio, rHeight};
        if(case1[1]>rHeight){
            dims = case2;
        } else if(case2[0]>rWidth){
            dims = case1;
        } else{
            dims = case1;
        }

        //MENU CARDS/////////
        for(int i = 0; i < numCardGFXPairs+2; i++){
            bounds.add(defaultCameraX + CAMERA_WIDTH*i);
        }
        maxCameraX = defaultCameraX + CAMERA_WIDTH*(numCardGFXPairs+1);
        for(int i = 0; i < numCardGFXPairs; i++){
            TiledSprite ts1 = new TiledSprite(((CAMERA_WIDTH/5)*2)+((2+i)*(CAMERA_WIDTH)), ((CAMERA_HEIGHT/3)*2), dims[0], dims[1], cardTextures.get(i+"-a"), this.getVertexBufferObjectManager());
            TiledSprite ts2 = new TiledSprite(((CAMERA_WIDTH/5)*3)+((2+i)*(CAMERA_WIDTH)), ((CAMERA_HEIGHT/3)), dims[0], dims[1], cardTextures.get(i+"-b"), this.getVertexBufferObjectManager());
            Text t1 = new Text(((CAMERA_WIDTH/5)*2)+((2+i)*(CAMERA_WIDTH)), ((CAMERA_HEIGHT/18)*17), dFontSmall, textInfo.get(i+"-a"), new TextOptions(AutoWrap.WORDS, dims[0]+50, HorizontalAlign.CENTER, Text.LEADING_DEFAULT), this.getVertexBufferObjectManager());
            Text t2 = new Text(((CAMERA_WIDTH/5)*3)+((2+i)*(CAMERA_WIDTH)), ((CAMERA_HEIGHT/18)), dFontSmall, textInfo.get(i+"-b"), new TextOptions(AutoWrap.WORDS, dims[0]+50, HorizontalAlign.CENTER, Text.LEADING_DEFAULT), this.getVertexBufferObjectManager());
            menuScene.attachChild(ts2);
            menuScene.attachChild(ts1);
            menuScene.attachChild(t1);
            menuScene.attachChild(t2);
        }

        //FINAL SETUP////////
        menuScene.getBackground().setColor(background);
        menuScene.attachChild(titleText);
        menuScene.attachChild(infoText);
        menuScene.attachChild(creditsText);
        menuScene.attachChild(helpText);
        menuScene.attachChild(selector);
        menuScene.attachChild(easy);
        menuScene.attachChild(medium);
        menuScene.attachChild(hard);
        menuScene.registerTouchArea(startText);
        menuScene.registerTouchArea(easy);
        menuScene.registerTouchArea(medium);
        menuScene.registerTouchArea(hard);
        menuScene.attachChild(menuMask);
        sceneToMask.put(menuScene, menuMask);
        this.mScrollDetector = new SurfaceScrollDetector(this);
        this.mScrollDetector.setEnabled(true);
        menuScene.setOnSceneTouchListener(this);

        a = new Text(CAMERA_WIDTH/2, ((CAMERA_HEIGHT/6)*5)+((CAMERA_HEIGHT/18)*2.25f), hFontSmall, "abcdefghijklmnopqrstuvwxyz.", this.getVertexBufferObjectManager());
        p = new Text(CAMERA_WIDTH/2, ((CAMERA_HEIGHT/6)*5)+((CAMERA_HEIGHT/18)*1.5f), dFontSmall, "doesnotprevents", this.getVertexBufferObjectManager());
        b = new Text(CAMERA_WIDTH/2, ((CAMERA_HEIGHT/6)*5)+((CAMERA_HEIGHT/18)*.75f), hFontSmall, "abcdefghijklmnopqrstuvwxyz.", this.getVertexBufferObjectManager());

        menuMask = new Rectangle(CAMERA_WIDTH/2, CAMERA_HEIGHT/2, CAMERA_WIDTH, CAMERA_HEIGHT, this.getVertexBufferObjectManager());
        levelMask = new Rectangle(CAMERA_WIDTH/2, CAMERA_HEIGHT/2, CAMERA_WIDTH, CAMERA_HEIGHT, this.getVertexBufferObjectManager());
        scoreMask = new Rectangle(CAMERA_WIDTH/2, CAMERA_HEIGHT/2, CAMERA_WIDTH, CAMERA_HEIGHT, this.getVertexBufferObjectManager());
        menuMask.setScale(0);
        levelMask.setScale(0);
        scoreMask.setScale(0);

        return this.menuScene;
    }

    // ===========================================================
    // Menu Scroll
    // ===========================================================
    @Override
    public void onScrollStarted(final ScrollDetector pScrollDetector, final int pPointerID, final float pDistanceX, final float pDistanceY) {
        this.startTime = System.currentTimeMillis();
        this.startX = mCamera.getCenterX();
        if(mCamera.getCenterX() - pDistanceX >= defaultCameraX - CAMERA_WIDTH/4 && mCamera.getCenterX() - pDistanceX <= maxCameraX + CAMERA_WIDTH/4){
            this.mCamera.offsetCenter(-pDistanceX, 0);
        }
    }

    @Override
    public void onScroll(final ScrollDetector pScrollDetector, final int pPointerID, final float pDistanceX, final float pDistanceY) {
        if(mCamera.getCenterX() - pDistanceX >= defaultCameraX - CAMERA_WIDTH/4 && mCamera.getCenterX() - pDistanceX <= maxCameraX + CAMERA_WIDTH/4){
            this.mCamera.offsetCenter(-pDistanceX, 0);
        }
    }

    @Override
    public void onScrollFinished(final ScrollDetector pScrollDetector, final int pPointerID, final float pDistanceX, final float pDistanceY) {
        //MOMENTUM SCROLL////
        float velocity = (mCamera.getCenterX() - this.startX)/(System.currentTimeMillis()-this.startTime);
        System.out.println(velocity);
        if(velocity >= 1.0f){
            float newX = Math.min(currCameraX + CAMERA_WIDTH, maxCameraX);
            mCamera.setCenter(newX, CAMERA_HEIGHT/2);
            currCameraX = newX;
        } else if(velocity <= -1.0f){
            float newX = Math.max(currCameraX - CAMERA_WIDTH, defaultCameraX);
            mCamera.setCenter(newX, CAMERA_HEIGHT/2);
            currCameraX = newX;
        } else{
            float min = Float.MAX_VALUE;
            float closest = mCamera.getCenterX();
            for (float v : bounds) {
                final float diff = Math.abs(v - mCamera.getCenterX());
                if (diff < min) {
                    min = diff;
                    closest = v;
                }
            }
            mCamera.setCenter(closest, CAMERA_HEIGHT/2);
            currCameraX = closest;
        } if(currCameraX == defaultCameraX){
            startText.setText(textInfo.get("start"));
        } else if(currCameraX == defaultCameraX + CAMERA_WIDTH){
            startText.setText(textInfo.get("help"));
        } else{
            startText.setText(textInfo.get("prevents"));
        }
    }

    @Override
    public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
        this.mScrollDetector.onTouchEvent(pSceneTouchEvent);
        return true;
    }

    // ===========================================================
    // Gameplay
    // ===========================================================
    /**
     * Generates a new level scene. Randomly picks from loaded textures and populates grid.
     */
    private void generateLevel(){
        touchEnabled = true;
        flipped.clear();
        tries = 0;
        matched = 0;
        numPairsOnBoard = (numCols* numRows)/2;

        levelScene = new Scene();
        levelMask.detachSelf();
        a.detachSelf();
        p.detachSelf();
        b.detachSelf();
        levelScene.attachChild(levelMask);
        levelScene.attachChild(a);
        levelScene.attachChild(p);
        levelScene.attachChild(b);
        sceneToMask.put(levelScene, levelMask);
        levelScene.getBackground().setColor(background);

        a.setText("");
        p.setText(textInfo.get("lets_start"));
        b.setText("");

        final float rWidth = CAMERA_WIDTH / numCols;
        final float rHeight = (CAMERA_HEIGHT - (CAMERA_HEIGHT/6)) / numRows;

        //Scale cards to max size while keeping aspect ratio intact.
        float ratio = Float.parseFloat(textInfo.get("width_card")) / Float.parseFloat(textInfo.get("height_card"));
        float[] dims;
        float[] case1 = new float[]{rWidth, rWidth/ratio};
        float[] case2 = new float[]{rHeight*ratio, rHeight};
        if(case1[1]>rHeight){
            dims = case2;
        } else if(case2[0]>rWidth){
            dims = case1;
        } else{
            dims = case1;
        }
        //Populate game board randomly
        ArrayList<String> randomPos = new ArrayList<String>(numPairsOnBoard *2);
        ArrayList<Integer> randomPair = new ArrayList<Integer>(numCardGFXPairs);
        for(int i = 0; i < numCardGFXPairs; i++){
            randomPair.add(i);
        }
        Collections.shuffle(randomPair);
        for(int i = 0; i < numPairsOnBoard; i++){
            int pair = randomPair.get(i%randomPair.size());
            randomPos.add(pair + "-a");
            randomPos.add(pair + "-b");
        }
        for(String name:randomPos){
            System.out.println(name);
        }
        Collections.shuffle(randomPos);
        for(int i = 0; i < numCols; i++){
            for(int j = 0; j < numRows; j++){
                String cardName = randomPos.get(i*numRows+j);
                TiledSprite sprite = new TiledSprite((i*rWidth)+(rWidth/2), (j*rHeight)+(rHeight/2), dims[0], dims[1], cardTextures.get(cardName), this.getVertexBufferObjectManager()){
                    final TiledSprite s = this;
                    @Override
                    public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float X, float Y){
                        if (pSceneTouchEvent.isActionDown() && touchEnabled){
                            touchEnabled = false;
                            flipped.add(s);
                            levelScene.unregisterTouchArea(s);
                            flip(s);
                        }
                        return true;
                    };
                };
                sprite.setCurrentTileIndex(1);
                sprite.setScale(0.97f);
                levelScene.registerTouchArea(sprite);
                levelScene.attachChild(sprite);
                spriteToName.put(sprite, cardName);
            }
        }
        levelMask.setZIndex(Integer.MAX_VALUE);
        levelScene.sortChildren();
    }

    /**
     * Called when two tiles are flipped. Determines if two tiles are pairs. If so, removed from play. Else, tiles are returned to the board.
     */
    private void matchOrReset(){
        if(flipped.size() >= 2){
            tries++;
            boolean match = false;
            TiledSprite one = flipped.get(0);
            TiledSprite two = flipped.get(1);
            String oneIndex = spriteToName.get(one).split("-")[0];
            String oneType = spriteToName.get(one).split("-")[1];
            String twoIndex = spriteToName.get(two).split("-")[0];
            String twoType = spriteToName.get(two).split("-")[1];
            if((oneType.equals("a")&&twoType.equals("b")) || (oneType.equals("b")&&twoType.equals("a"))){
                if(oneIndex.equals(twoIndex)){
                    match = true;
                }
            }
            if(match){
                //do scoring stuff
                matched++;
                correct.play();
                matchAnimation();
                if(matched == numPairsOnBoard){
                    levelScene.registerUpdateHandler(new TimerHandler(1.75f, new ITimerCallback() {
                        @Override
                        public void onTimePassed(TimerHandler pTimerHandler) {
                        generateScore();
                        sceneTransition(levelScene, scoreScene);
                        victory.play();
                        levelScene.unregisterUpdateHandler(pTimerHandler);
                        }
                    }));
                } else{
                    flipped.clear();
                }
            } else{
                wrong.play();
                a.setColor(Color.RED_ARGB_PACKED_INT);
                p.setText(textInfo.get("does_not_prevent"));
                b.setColor(Color.RED_ARGB_PACKED_INT);
                unflip();
                flipped.clear();
            }
        } else{
            touchEnabled = true;
        }
    }

    // ===========================================================
    // Scoring
    // ===========================================================
    /**
     * Creates the score scene when all pairs have been matched.
     */
    private void generateScore(){
        if(scoreScene != null){
            triesText.setText(Integer.toString(tries));
        } else{
            scoreScene = new Scene();
            final Text t1 = new Text(CAMERA_WIDTH/2, (CAMERA_HEIGHT/6)*5, dFont, textInfo.get("you_took"), this.getVertexBufferObjectManager());
            triesText = new Text(CAMERA_WIDTH/2, (CAMERA_HEIGHT/6)*4, hFont, "0123456789", this.getVertexBufferObjectManager());
            triesText.setText(Integer.toString(tries));
            final Text t2 = new Text(CAMERA_WIDTH/2, (CAMERA_HEIGHT/6)*3, dFont, textInfo.get("turns"), this.getVertexBufferObjectManager());
            final Text replayText = new Text(CAMERA_WIDTH/2, CAMERA_HEIGHT/6, hFont, textInfo.get("replay"), this.getVertexBufferObjectManager()){
                @Override
                public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                    if(pSceneTouchEvent.isActionUp()){
                        generateLevel();
                        sceneTransition(scoreScene, levelScene);
                    }
                    return true;
                }
            };

            scoreScene.attachChild(triesText);
            scoreScene.attachChild(t1);
            scoreScene.attachChild(t2);
            scoreScene.attachChild(replayText);
            scoreScene.attachChild(scoreMask);
            scoreScene.registerTouchArea(replayText);
            sceneToMask.put(scoreScene, scoreMask);
            scoreScene.getBackground().setColor(background);
        }
    }
    // ===========================================================
    // Animations
    // ===========================================================
    /**
     *
     * @param card the card to animate a "flip"
     *
     * Animates a flip
     */
    private void flip(TiledSprite card){
        final TiledSprite c = card;
        final String cardName = spriteToName.get(c);
        final String type = cardName.split("-")[1];
        p.setText("");
        c.registerEntityModifier(
            new ScaleModifier(0.2f, 0.97f, 0, 0.97f, 0.92f) {
                @Override
                protected void onModifierFinished(IEntity pItem) {
                    c.setCurrentTileIndex(0);
                    c.registerEntityModifier(
                        new ScaleModifier(0.2f, 0, 0.97f, .92f, 0.97f) {
                            @Override
                            protected void onModifierFinished(IEntity pItem) {
                                if(type.equals("a")){
                                    if(a.getText().equals("")){
                                        a.setText(textInfo.get(cardName));
                                    } else{
                                        b.setText(textInfo.get(cardName));
                                    }
                                } else{
                                    if(b.getText().equals("")){
                                        b.setText(textInfo.get(cardName));
                                    } else{
                                        a.setText(textInfo.get(cardName));
                                    }
                                }
                                matchOrReset();
                            }
                        }
                    );
                }
            }
        );
    }

    /**
     * Animate a card unflipping
     */
    private void unflip(){
        Text[] text = new Text[]{a,p,b};
        for(TiledSprite ts : flipped){
            final TiledSprite t = ts;
            levelScene.registerTouchArea(t);
            t.registerEntityModifier(
                new DelayModifier(1.24f){
                    @Override
                    protected void onModifierFinished(IEntity pItem) {
                        t.registerEntityModifier(
                            new ScaleModifier(0.2f, 0.97f, 0, 0.97f, 0.92f) {
                                @Override
                                protected void onModifierFinished(IEntity pItem) {
                                    t.setCurrentTileIndex(1);
                                    t.registerEntityModifier(new ScaleModifier(0.2f, 0, 0.97f, .92f, 0.97f));
                                }
                            }
                        );
                    }
                }
            );
        }
        for(Text txt : text){
            final Text t = txt;
            t.registerEntityModifier(new SequenceEntityModifier(
                new DelayModifier(1.25f),
                new ScaleModifier(0.3f, 1f, 0)
            ){
                @Override
                protected void onModifierFinished(IEntity pItem) {
                    t.setText("");
                    t.setScale(1);
                    t.setColor(Color.WHITE_ARGB_PACKED_INT);
                    touchEnabled = true;
                }
            });
        }
    }

    private void matchAnimation(){
        a.setColor(Color.GREEN_ABGR_PACKED_INT);
        p.setText(textInfo.get("prevents"));
        b.setColor(Color.GREEN_ARGB_PACKED_INT);
        Text[] text = new Text[]{a,p,b};
        for(TiledSprite t:flipped){
            t.registerEntityModifier(new SequenceEntityModifier(
                new DelayModifier(1.26f),
                new ScaleModifier(0.2f, 0.97f, 0, 0.97f, 0.92f)
            ));
        }
        for(Text txt : text){
            final Text t = txt;
            t.registerEntityModifier(new SequenceEntityModifier(
                new DelayModifier(1.25f),
                new ScaleModifier(0.3f, 1f, 0)
            ){
                @Override
                protected void onModifierFinished(IEntity pItem) {
                    t.setText("");
                    t.setScale(1);
                    t.setColor(Color.WHITE_ARGB_PACKED_INT);
                    touchEnabled = true;
                }
            });
        }
    }

    // ===========================================================
    // Utilities
    // ===========================================================
    /**
     *
     * @param mix Color to be mixed with
     * @return returns a randomly generated color.
     */
    public static Color generateRandomColor(Color mix) {
        float red = (float)Math.random();
        float green = (float)Math.random();
        float blue = (float)Math.random();

        // mix the color
        if (mix != null) {
            red = (red + mix.getRed()) / 2;
            green = (green + mix.getGreen()) / 2;
            blue = (blue + mix.getBlue()) / 2;
        }

        Color color = new Color(red, green, blue);
        return color;
    }

    @Override
    public void onBackPressed(){
        Scene curr = mEngine.getScene();
        if(curr == menuScene){
            if(mCamera.getCenterX() != defaultCameraX){
                mCamera.setCenter(defaultCameraX, CAMERA_HEIGHT/2);
                startText.setText(textInfo.get("start"));
                currCameraX = defaultCameraX;
            }
            else{
                finish();
            }
        }
        else if(curr == levelScene){
            sceneTransition(levelScene, menuScene);
        }
        else if(curr == scoreScene){
            sceneTransition(scoreScene, menuScene);
        }
    }

    /**
     *
     * @param from Scene that will be transitioned from
     * @param to Scene that will be transitioned to
     *
     * Creates scene transition to mask scene changes.
     */
    private void sceneTransition(final Scene from, final Scene to){
        if(!to.equals(menuScene)){
            mHud.setVisible(false);
        }
        final Color maskColor = generateRandomColor(gray);
        final Rectangle fromMask = sceneToMask.get(from);
        final Rectangle toMask = sceneToMask.get(to);
        fromMask.setColor(maskColor);
        toMask.setColor(maskColor);
        fromMask.setScale(0);
        toMask.setScale(1);
        fromMask.registerEntityModifier(new ScaleModifier(.35f,0,1){
            @Override
            protected void onModifierFinished(IEntity pItem) {
                mEngine.setScene(to);
                toMask.registerEntityModifier(new ScaleModifier(.35f,1,0));
                if(to.equals(menuScene)){
                    mHud.setVisible(true);
                }
            }
        });
    }
}