===========================
Conan Cai
conan.cai@berkeley.edu
===========================

===========Build===========
 - Android SDK API 19, min API level 8
 - Andengine GLES2-AnchorCenter
	https://github.com/nicolasgramlich/AndEngine/tree/GLES2-AnchorCenter
 - Andengine PhysicsBox2DExtension
	https://github.com/nicolasgramlich/AndEnginePhysicsBox2DExtension/tree/GLES2-AnchorCenter
 - Built successfully on IntelliJ Idea 13.0 and confirmed working on HTC One X Android 4.4
	results may vary with other configs. 

======Changing Icon========
 - Replace "icon.png" in res/drawable
 - Change title of app inside AndroidManifest

=====Adding new images=====
 - Images are stored in /assets/gfx
 - Images should be in .PNG format and follow the current naming scheme, ie 0-a and 0-b are matching pairs, 1-a and 1-b are matching pairs, and so on
 - Images are split vertically; the left side is the "front" of the card while the right side is the "back" of the card.
 - Add as many image pairs as needed, but try to keep images small. ie 300x300
 - Update the config file accordingly. (width,height,gfxpairs,card information)

======Changing Config======
 - config.txt is located in /res/raw
 - Follow current syntax key:value
 - '#' is comment symbol. Lines starting with '#' are ignored.
 - Empty lines are ignored.
 - One key:value per line
 - Do not change keys, only edit their values.
 - values:
 	- width_card: this is the width of the card in px. So this value should be 1/2 of the width of the card images stored in /assets/gfx since
	  those images are double in width to show both front and back.
	- height_card: height of each card in px.
	- gfx_pairs: this is the number of image pairs that are stored in /assets/gfx. You can use a lower number than what is actually in the folder
	  but setting a number higher than available images will cause an error.
	- background_color: background color that will be used. Input is a RGB triple seperated by commas.
	- text: this is where text in the game is defined.
	- card information: this is where each card's caption is defined. 

======Troubleshooting======
 - If the screen does not display properly after adding new text or images, likely a memory error has occured. Try to reduce the size of images or shorten
   text. If that does not work, increase the size of the texture bitmap and try again. Bitmap dimensions should be in powers of 2.
	- Images: increase mBitTextureAtlas size
	  	161-this.mBitmapTextureAtlas = new BuildableBitmapTextureAtlas(this.getTextureManager(), 1024, 2048, TextureOptions.BILINEAR);
	- Text: increase hFont/hfontSmall/dFont/dFontSmall texture sizes
		151-this.hFont = FontFactory.createFromAsset(this.getFontManager(), this.getTextureManager(), 1024, 512, this.getAssets(), "fnt/handwritten.ttf", CAMERA_WIDTH/6, true, Color.WHITE_ARGB_PACKED_INT);
        	152-this.hFontSmall = FontFactory.createFromAsset(this.getFontManager(), this.getTextureManager(), 512, 256, this.getAssets(), "fnt/handwritten.ttf", CAMERA_WIDTH/14, true, Color.WHITE_ARGB_PACKED_INT);
        	153-this.dFont = FontFactory.create(this.getFontManager(), this.getTextureManager(), 512, 512, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), CAMERA_WIDTH/6, Color.WHITE_ARGB_PACKED_INT);
        	154-this.dFontSmall = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), CAMERA_WIDTH/14, Color.WHITE_ARGB_PACKED_INT);
 - Any other error make sure things are spelled correctly in config.txt and there are the correct number of images inside /assets/gfx.
 - No other known bugs except for the occasional glitch. Sometimes the camera will be off center when swiping. I'm not sure how to replicate it but it rarely happens.