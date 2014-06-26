package maze3d.beta;

import static javax.media.opengl.GL.*;
import static javax.media.opengl.GL2.*;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_AMBIENT;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_DIFFUSE;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_LIGHT1;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_LIGHTING;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_POSITION;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureIO;

import java.awt.Point;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/*
 * 3D bludiste Azaltovic Jan ai3 / 2
*/

public class AppMaze3D implements GLEventListener, MouseListener, MouseMotionListener, KeyListener {

	private static final int FPS = 60;
	private GL2 gl;
	private GLAutoDrawable glDrawable;
	private GLU glu;

	private int width, height, x, dx, ox, y, dy, oy;

	private boolean dopredu, dozadu, doleva, doprava, nahoru, dolu;
	private float posX = 2.5f, posY = 0.3f, posZ = 1.5f;
	private int newposX, newposZ;
	private float eyeX = 0, eyeY = 0, eyeZ = 0.0f;
	private float cenX = 0.0f, cenY = 0.0f, cenZ = 0.0f;
	private float upX = 0.0f, upY = 0.0f, upZ = 0.0f;
	private float azimut = 180f;
	private float zenit = 0f;

	private Texture[] texture = new Texture[3];
	private File[] textureFileName = { new File("floor.jpg"), new File("7.png"), new File("sun_diffuse.jpg") };
	private float[] textureTop = new float[3];
	private float[] textureBottom = new float[3];
	private float[] textureLeft = new float[3];
	private float[] textureRight = new float[3];

	private List<Cuboid> cuboids;
	
	private double timeLeftBeforeLightStateChange;

	private static boolean isLightOn;

	private static Clip clip;
	private static Clip clipShock;

	static File fileSoundSrc = new File("hbNoDie.wav");
	static File fileSoundSrcShock = new File("electric.wav");

	private final static int C_LENGTH = 1;
	private final static int C_WIDTH = 1;
	private final static int C_HEIGHT = 2;

	private int[][] maze = { 	{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
								{ 1, 1, 0, 1, 0, 0, 0, 0, 1, 1 },
								{ 1, 0, 0, 1, 0, 1, 1, 0, 1, 1 },
								{ 1, 0, 1, 1, 0, 0, 1, 0, 1, 1 },
								{ 1, 0, 0, 0, 1, 0, 1, 0, 0, 0 },
								{ 1, 0, 1, 1, 0, 0, 1, 1, 1, 1 },
								{ 1, 0, 0, 0, 0, 1, 0, 0, 0, 1 },
								{ 1, 1, 0, 1, 1, 1, 0, 1, 0, 1 },
								{ 1, 0, 0, 0, 0, 0, 0, 1, 0, 1 },
								{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
							};

	public void init(GLAutoDrawable drawable) {
		glDrawable = drawable;
		gl = glDrawable.getGL().getGL2();
		glu = new GLU();

		gl.glEnable(GL2.GL_DEPTH_TEST);

		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE);

		try {
			texture[0] = TextureIO.newTexture(textureFileName[0], false);
			texture[1] = TextureIO.newTexture(textureFileName[1], false);
			texture[2] = TextureIO.newTexture(textureFileName[2], false);

			// linear filter for larger
			gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			// linear filter for smaller
			gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

			// tex coords, coz they switch vertically
			TextureCoords[] textureCoords = { texture[0].getImageTexCoords(), texture[1].getImageTexCoords(), texture[2].getImageTexCoords() };
			textureTop[0] = textureCoords[0].top();
			textureBottom[0] = textureCoords[0].bottom();
			textureLeft[0] = textureCoords[0].left();
			textureRight[0] = textureCoords[0].right();

			textureTop[1] = textureCoords[1].top();
			textureBottom[1] = textureCoords[1].bottom();
			textureLeft[1] = textureCoords[1].left();
			textureRight[1] = textureCoords[1].right();

			// not used anymore
			textureTop[2] = textureCoords[2].top();
			textureBottom[2] = textureCoords[2].bottom();
			textureLeft[2] = textureCoords[2].left();
			textureRight[2] = textureCoords[2].right();

		} catch (GLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// max red, max alfa
		float[] lightAmbientValue = { 1.0f, 0.0f, 0.0f, 1.0f };
		float[] lightDiffuseValue = { 1.0f, 0.0f, 0.0f, 1.0f };
		
		// Diffuse light location xyz + out of scene light pos
		float lightDiffusePosition[] = { 10.0f, 6f, 10.0f, 6.0f };

		gl.glLightfv(GL_LIGHT1, GL_AMBIENT, lightAmbientValue, 0);
		gl.glLightfv(GL_LIGHT1, GL_DIFFUSE, lightDiffuseValue, 0);
		gl.glLightfv(GL_LIGHT1, GL_POSITION, lightDiffusePosition, 0);

		gl.glEnable(GL_LIGHT1);
		
		gl.glDisable(GL_LIGHTING);
		isLightOn = false;

		// flashing counter
		timeLeftBeforeLightStateChange = (Math.random() + 10);
		
		// bludiste vykresleni list predkompilace, fps++
		gl.glNewList(1, GL_COMPILE);
		drawMaze(maze, gl);
		gl.glEndList();
	}

	public void display(GLAutoDrawable drawable) {

		glDrawable = drawable;

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL2.GL_DEPTH_TEST);

		gl.glFrontFace(GL2.GL_CCW);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		glu.gluPerspective(45, width / (float) height, 0.1f, 20000.0f);

		zenit -= dy;

		// reset zenit if +-90
		if (zenit > 90) {

			zenit = 90;
		}
		
		if (zenit < -90) {
			zenit = -90;
		}
		
		// reset azimut if over
		azimut += dx;
		azimut = azimut % 360;

		// reset zmenu mysi
		dy = 0;
		dx = 0;

		if (dopredu) {

			posZ -= (float) ((Math.cos(azimut * Math.PI / 180) * Math.cos(zenit * Math.PI / 180))) / 50;
			posX += (float) ((Math.sin(azimut * Math.PI / 180) * Math.cos(zenit * Math.PI / 180))) / 50;

			checkCD();

			// pokus s zjistovanim pozice podle azimutu, funguje pokud kamera je v rozmezi +-45stupnu od primeho uhlu kdy jdu ke stene..
			// jenze potrebuju checkovat +-90, tak sem vymyslel jiny zpusob, i kdyz ne zrovna presny.. viz checkCD()

			//   _ _ x
			//  |_|_|
			//  |_|_|
			// z

			//					// leva horni shora
			//					if (posZ - newposZ < 0.5 && (azimut > 90 && azimut < 270 || azimut < -135 && azimut > -225)) {
			//						posZ = newposZ;
			//						System.out.println("x: " + posX + " z: " + posZ);
			//					}
			//
			//					// leva horni zleva
			//					if (posX - newposX < 0.5 && (azimut > 0 && azimut < 180 || azimut < -225 && azimut > -315)) {
			//						posX = newposX;
			//						System.out.println("x: " + posX + " z: " + posZ);
			//					}
			//
			//					// prava horni shora
			//					if (posX - newposX > 0.5 && posZ - newposZ < 0.5 && (azimut > 135 && azimut < 225 || azimut < -135 && azimut > -225)) {
			//						posZ = newposZ;
			//						System.out.println("x: " + posX + " z: " + posZ);
			//					}
			//
			//					// prava horni zprava
			//					if (posX - newposX > 0.5 && posZ - newposZ < 0.5 && (azimut > 225 && azimut < 315 || azimut > -45 && azimut < -135)) {
			//						posX = newposX + 1;
			//						System.out.println("x: " + posX + " z: " + posZ);
			//					}
			//
			//					// leva dolni zleva
			//					if (posX - newposX < 0.5 && posZ - newposZ > 0.5 && (azimut > 45 && azimut < 135 || azimut < -225 && azimut > -315)) {
			//						posX = newposX;
			//						System.out.println("x: " + posX + " z: " + posZ);
			//					}
			//
			//					// leva dolni zespodu
			//					if (posX - newposX < 0.5 && posZ - newposZ > 0.5 && (azimut > 315 && azimut < 360 || azimut > -45 && azimut < 45 || azimut < -315 && azimut < 45)) {
			//						posZ = newposZ + 1;
			//						System.out.println("x: " + posX + " z: " + posZ);
			//					}
			//
			//					// prava dolni zprava
			//					if (posX - newposX > 0.5 && posZ - newposZ > 0.5 && (azimut > 225 && azimut < 315 || azimut < -45 && azimut > -135)) {
			//						posX = newposX + 1;
			//						System.out.println("x: " + posX + " z: " + posZ);
			//					}
			//
			//					// prava dolni zespodu
			//					if (posX - newposX > 0.5 && posZ - newposZ > 0.5 && (azimut > 315 && azimut < 360 || azimut > -45 && azimut < 45 || azimut < -315 && azimut < 45)) {
			//						posZ = newposZ + 1;
			//						System.out.println("x: " + posX + " z: " + posZ);
			//
			//					}

		}

		if (dozadu) {
			posZ += (float) ((Math.cos(azimut * Math.PI / 180) * Math.cos(zenit * Math.PI / 180))) / 50;
			posX -= (float) ((Math.sin(azimut * Math.PI / 180) * Math.cos(zenit * Math.PI / 180))) / 50;

			checkCD();
		}
		if (doleva) {
			posZ -= (float) ((Math.cos(azimut * Math.PI / 180 - Math.PI / 2))) / 50;
			posX += (float) ((Math.sin(azimut * Math.PI / 180 - Math.PI / 2))) / 50;

			checkCD();
		}
		if (doprava) {
			posZ += (float) ((Math.cos(azimut * Math.PI / 180 - Math.PI / 2))) / 50;
			posX -= (float) ((Math.sin(azimut * Math.PI / 180 - Math.PI / 2))) / 50;
			
			checkCD();
		}
		
		if (nahoru) {
			posY += 0.1;
		}
		
		if (dolu) {
			posY -= 0.1;
		}

		eyeZ = -(float) (Math.cos(azimut * Math.PI / 180) * Math.cos(zenit * Math.PI / 180));
		eyeX = (float) (Math.sin(azimut * Math.PI / 180) * Math.cos(zenit * Math.PI / 180));
		eyeY = (float) (Math.sin(zenit * Math.PI / 180));

		upZ = -(float) (Math.cos(azimut * Math.PI / 180) * Math.cos(zenit * Math.PI / 180 + Math.PI / 2));
		upX = (float) (Math.sin(azimut * Math.PI / 180) * Math.cos(zenit * Math.PI / 180 + Math.PI / 2));
		upY = (float) (Math.sin(zenit * Math.PI / 180 + Math.PI / 2));

		cenX = posX + eyeX * 100;
		cenY = posY + eyeY * 100;
		cenZ = posZ + eyeZ * 100;

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		glu.gluLookAt(posX, posY, posZ, cenX, cenY, cenZ, upX, upY, upZ);

		// blikani svetla
		flashLight();

		texture[1].disable(gl);
		
		drawAxes(gl);

		texture[0].enable(gl);
		texture[0].bind(gl);

		// kreslit bludiste
		gl.glCallList(1);
		
		texture[1].enable(gl);
		texture[1].bind(gl);

		drawFloor();
		//System.out.println(azimut);
	}

	private void checkCD() {
		// seriznu at muzu pouzit coords kamery k loopovani maze pole
		newposZ = (int) posZ;
		newposX = (int) posX;

		// abych byl v matici check..
		if (newposZ >= 0 && newposZ < maze.length && newposX >= 0 && newposX < maze[0].length) {
			
			// prochazim polem podle camera coords
			if (maze[newposZ][newposX] == 1) {

				if (posZ >= newposZ && posZ <= newposZ + 1) {
					//System.out.println("kolize");
					try {
						AudioInputStream ais = AudioSystem.getAudioInputStream(fileSoundSrcShock);
						clipShock = AudioSystem.getClip();
						clipShock.open(ais);
						clipShock.loop(0);
						clipShock.start();

						System.out.println("starting media - electric");
					} catch (Exception ex) {}
					
					if (posZ - newposZ < 0.5) {
						posZ-=0.05f;
					} else {
						posZ+=0.05f;
					}
				}
				
				if (posX >= newposX && posX <= newposX + 1) {
					try {
						AudioInputStream ais = AudioSystem.getAudioInputStream(fileSoundSrcShock);
						clipShock = AudioSystem.getClip();
						clipShock.open(ais);
						clipShock.loop(0);
						clipShock.start();

						System.out.println("starting media - electric");
					} catch (Exception ex) {}
					if (posX - newposX < 0.5) {
						posX-=0.05f;
					} else {
						posX+=0.05f;
					}
				}
			}
		}
	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public void dispose(GLAutoDrawable arg0) {}
	
	public static void main(String[] args) {
		JFrame f = new JFrame("Maze3D");

		GLProfile profile = GLProfile.get(GLProfile.GL2);
		GLCapabilities capabilities = new GLCapabilities(profile);

		GLCanvas canvas = new GLCanvas(capabilities);
		AppMaze3D ren = new AppMaze3D();
		canvas.addGLEventListener(ren);
		canvas.addMouseListener(ren);
		canvas.addMouseMotionListener(ren);
		canvas.addKeyListener(ren);
		canvas.setSize(1024, 768);

		f.add(canvas);

		final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);

		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				new Thread() {
					@Override
					public void run() {
						if (animator.isStarted())
							animator.stop();
						System.exit(0);
					}
				}.start();
			}
		});

		f.pack();
		f.setLocationRelativeTo(null);
		// schovam kurzor
		f.setCursor(f.getToolkit().createCustomCursor(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "null"));

		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(fileSoundSrc);
			clip = AudioSystem.getClip();
			clip.open(ais);
			clip.loop(2);
			clip.start();

			System.out.println("starting media - heartbeat");

		} catch (Exception ex) {};

		f.setVisible(true);
		canvas.requestFocus();
		animator.start();
	}
	
	// vytvorim si cuboidy at mam info o strukture bludiste + vykreslim strukturu..
	private void drawMaze(int[][] maze, GL2 gl) {

		for (int row = 0; row < maze.length; row++) {
			for (int col = 0; col < maze[0].length; col++) {

				if (maze[row][col] == 1) {

					// vsude zacinam vlevo nahore krom podlahy bo nebudu koukat na spodek podlahy..
					// bottom
					Vec3D topLeftBot = new Vec3D(C_LENGTH * col, 0, C_WIDTH * row);
					Vec3D botLeftBot = new Vec3D(C_LENGTH * col, 0, C_WIDTH * row + C_WIDTH);
					Vec3D botRightBot = new Vec3D(C_LENGTH * col + C_LENGTH, 0, C_WIDTH * row + C_WIDTH);
					Vec3D topRightBot = new Vec3D(C_LENGTH * col + C_LENGTH, 0, C_WIDTH * row);

					Rect squareBot = new Rect(topLeftBot, botLeftBot, botRightBot, topRightBot);

					// top
					Vec3D topLeftTop = new Vec3D(C_LENGTH * col, C_HEIGHT, C_WIDTH * row);
					Vec3D botLeftTop = new Vec3D(C_LENGTH * col, C_HEIGHT, C_WIDTH * row + C_WIDTH);
					Vec3D botRightTop = new Vec3D(C_LENGTH * col + C_LENGTH, C_HEIGHT, C_WIDTH * row + C_WIDTH);
					Vec3D topRightTop = new Vec3D(C_LENGTH * col + C_LENGTH, C_HEIGHT, C_WIDTH * row);

					Rect squareTop = new Rect(topLeftTop, botLeftTop, botRightTop, topRightTop);

					// front
					Vec3D topLeftFro = new Vec3D(C_LENGTH * col, C_HEIGHT, C_WIDTH * row + C_WIDTH);
					Vec3D botLeftFro = new Vec3D(C_LENGTH * col, 0, C_WIDTH * row + C_WIDTH);
					Vec3D botRightFro = new Vec3D(C_LENGTH * col + C_LENGTH, 0, C_WIDTH * row + C_WIDTH);
					Vec3D topRightFro = new Vec3D(C_LENGTH * col + C_LENGTH, C_HEIGHT, C_WIDTH * row + C_WIDTH);

					Rect squareFro = new Rect(topLeftFro, botLeftFro, botRightFro, topRightFro);

					// back stejny jak top akorat sem odebral width u kazdyho vec a prohodil sem navzajem levy a pravy vektory kvuli texture
					Vec3D topLeftBac = new Vec3D(C_LENGTH * col + C_LENGTH, C_HEIGHT, C_WIDTH * row);
					Vec3D botLeftBac = new Vec3D(C_LENGTH * col + C_LENGTH, 0, C_WIDTH * row);
					Vec3D botRightBac = new Vec3D(C_LENGTH * col, 0, C_WIDTH * row);
					Vec3D topRightBac = new Vec3D(C_LENGTH * col, C_HEIGHT, C_WIDTH * row);

					Rect squareBac = new Rect(topLeftBac, botLeftBac, botRightBac, topRightBac);

					// left
					Vec3D topLeftLef = new Vec3D(C_LENGTH * col, C_HEIGHT, C_WIDTH * row);
					Vec3D botLeftLef = new Vec3D(C_LENGTH * col, 0, C_WIDTH * row);
					Vec3D botRightLef = new Vec3D(C_LENGTH * col, 0, C_WIDTH * row + C_WIDTH);
					Vec3D topRightLef = new Vec3D(C_LENGTH * col, C_HEIGHT, C_WIDTH * row + C_WIDTH);

					Rect squareLef = new Rect(topLeftLef, botLeftLef, botRightLef, topRightLef);

					// right zas prohodim left a right
					Vec3D topLeftRig = new Vec3D(C_LENGTH * col + C_LENGTH, C_HEIGHT, C_WIDTH * row + C_WIDTH);
					Vec3D botLeftRig = new Vec3D(C_LENGTH * col + C_LENGTH, 0, C_WIDTH * row + C_WIDTH);
					Vec3D botRightRig = new Vec3D(C_LENGTH * col + C_LENGTH, 0, C_WIDTH * row);
					Vec3D topRightRig = new Vec3D(C_LENGTH * col + C_LENGTH, C_HEIGHT, C_WIDTH * row);

					Rect squareRig = new Rect(topLeftRig, botLeftRig, botRightRig, topRightRig);

					Cuboid cuboid = new Cuboid(squareBot, squareTop, squareFro, squareBac, squareLef, squareRig);

					// bottom
					gl.glBegin(GL_QUADS);
					gl.glNormal3i(0, -1, 0);
					gl.glTexCoord2f(textureLeft[0], textureTop[0]);
					gl.glVertex3i(cuboid.getBottom().getTopLeft().getX(), cuboid.getBottom().getTopLeft().getY(), cuboid.getBottom().getTopLeft().getZ());
					gl.glTexCoord2f(textureLeft[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getBottom().getBotLeft().getX(), cuboid.getBottom().getBotLeft().getY(), cuboid.getBottom().getBotLeft().getZ());
					gl.glTexCoord2f(textureRight[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getBottom().getBotRight().getX(), cuboid.getBottom().getBotRight().getY(), cuboid.getBottom().getBotRight().getZ());
					gl.glTexCoord2f(textureRight[0], textureTop[0]);
					gl.glVertex3i(cuboid.getBottom().getTopRight().getX(), cuboid.getBottom().getTopRight().getY(), cuboid.getBottom().getTopRight().getZ());
					gl.glEnd();

					// top
					gl.glBegin(GL_QUADS);
					gl.glNormal3i(0, 1, 0);
					gl.glTexCoord2f(textureLeft[0], textureTop[0]);
					gl.glVertex3i(cuboid.getTop().getTopLeft().getX(), cuboid.getTop().getTopLeft().getY(), cuboid.getTop().getTopLeft().getZ());
					gl.glTexCoord2f(textureLeft[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getTop().getBotLeft().getX(), cuboid.getTop().getBotLeft().getY(), cuboid.getTop().getBotLeft().getZ());
					gl.glTexCoord2f(textureRight[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getTop().getBotRight().getX(), cuboid.getTop().getBotRight().getY(), cuboid.getTop().getBotRight().getZ());
					gl.glTexCoord2f(textureRight[0], textureTop[0]);
					gl.glVertex3i(cuboid.getTop().getTopRight().getX(), cuboid.getTop().getTopRight().getY(), cuboid.getTop().getTopRight().getZ());
					gl.glEnd();

					// front
					gl.glBegin(GL_QUADS);
					gl.glNormal3i(0, 0, 1);
					gl.glTexCoord2f(textureLeft[0], textureTop[0]);
					gl.glVertex3i(cuboid.getFront().getTopLeft().getX(), cuboid.getFront().getTopLeft().getY(), cuboid.getFront().getTopLeft().getZ());
					gl.glTexCoord2f(textureLeft[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getFront().getBotLeft().getX(), cuboid.getFront().getBotLeft().getY(), cuboid.getFront().getBotLeft().getZ());
					gl.glTexCoord2f(textureRight[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getFront().getBotRight().getX(), cuboid.getFront().getBotRight().getY(), cuboid.getFront().getBotRight().getZ());
					gl.glTexCoord2f(textureRight[0], textureTop[0]);
					gl.glVertex3i(cuboid.getFront().getTopRight().getX(), cuboid.getFront().getTopRight().getY(), cuboid.getFront().getTopRight().getZ());
					gl.glEnd();

					// back
					gl.glBegin(GL_QUADS);
					gl.glNormal3i(0, 0, -1);
					gl.glTexCoord2f(textureLeft[0], textureTop[0]);
					gl.glVertex3i(cuboid.getBack().getTopLeft().getX(), cuboid.getBack().getTopLeft().getY(), cuboid.getBack().getTopLeft().getZ());
					gl.glTexCoord2f(textureLeft[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getBack().getBotLeft().getX(), cuboid.getBack().getBotLeft().getY(), cuboid.getBack().getBotLeft().getZ());
					gl.glTexCoord2f(textureRight[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getBack().getBotRight().getX(), cuboid.getBack().getBotRight().getY(), cuboid.getBack().getBotRight().getZ());
					gl.glTexCoord2f(textureRight[0], textureTop[0]);
					gl.glVertex3i(cuboid.getBack().getTopRight().getX(), cuboid.getBack().getTopRight().getY(), cuboid.getBack().getTopRight().getZ());
					gl.glEnd();

					// left
					gl.glBegin(GL_QUADS);
					gl.glNormal3i(-1, 0, 0);
					gl.glTexCoord2f(textureLeft[0], textureTop[0]);
					gl.glVertex3i(cuboid.getLeft().getTopLeft().getX(), cuboid.getLeft().getTopLeft().getY(), cuboid.getLeft().getTopLeft().getZ());
					gl.glTexCoord2f(textureLeft[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getLeft().getBotLeft().getX(), cuboid.getLeft().getBotLeft().getY(), cuboid.getLeft().getBotLeft().getZ());
					gl.glTexCoord2f(textureRight[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getLeft().getBotRight().getX(), cuboid.getLeft().getBotRight().getY(), cuboid.getLeft().getBotRight().getZ());
					gl.glTexCoord2f(textureRight[0], textureTop[0]);
					gl.glVertex3i(cuboid.getLeft().getTopRight().getX(), cuboid.getLeft().getTopRight().getY(), cuboid.getLeft().getTopRight().getZ());
					gl.glEnd();

					// right
					gl.glBegin(GL_QUADS);
					gl.glNormal3i(1, 0, 0);
					gl.glTexCoord2f(textureLeft[0], textureTop[0]);
					gl.glVertex3i(cuboid.getRight().getTopLeft().getX(), cuboid.getRight().getTopLeft().getY(), cuboid.getRight().getTopLeft().getZ());
					gl.glTexCoord2f(textureLeft[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getRight().getBotLeft().getX(), cuboid.getRight().getBotLeft().getY(), cuboid.getRight().getBotLeft().getZ());
					gl.glTexCoord2f(textureRight[0], textureBottom[0]);
					gl.glVertex3i(cuboid.getRight().getBotRight().getX(), cuboid.getRight().getBotRight().getY(), cuboid.getRight().getBotRight().getZ());
					gl.glTexCoord2f(textureRight[0], textureTop[0]);
					gl.glVertex3i(cuboid.getRight().getTopRight().getX(), cuboid.getRight().getTopRight().getY(), cuboid.getRight().getTopRight().getZ());
					gl.glEnd();

					cuboids = new ArrayList<>();
					cuboids.add(cuboid);

				}
			}
		}

		texture[0].disable(gl);
	}
	
	// podlaha ze ctvercu
	private void drawFloor() {
		gl.glBegin(GL_QUADS);
		gl.glNormal3f(0, 1, 0);
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				gl.glTexCoord2f(textureLeft[1], textureBottom[1]);
				gl.glVertex3f(j, 0, i);
				gl.glTexCoord2f(textureRight[1], textureBottom[1]);
				gl.glVertex3f(j, 0, i + 1f);
				gl.glTexCoord2f(textureRight[1], textureTop[1]);
				gl.glVertex3f(j + 1f, 0, i + 1f);
				gl.glTexCoord2f(textureLeft[1], textureTop[1]);
				gl.glVertex3f(j + 1f, 0, i);
			}
		}
		gl.glEnd();
	}
	
	private void flashLight() {
		timeLeftBeforeLightStateChange -= 1;

		if (timeLeftBeforeLightStateChange <= 0) {
			timeLeftBeforeLightStateChange = (Math.random() + 10);

			if (isLightOn) {
				isLightOn = !isLightOn;
			} else {
				isLightOn = !isLightOn;
			}
		}

		if (isLightOn) {
			gl.glDisable(GL_LIGHTING);
		} else {
			gl.glEnable(GL_LIGHTING);
		}
	}

	private void drawAxes(GL2 gl) {

		gl.glBegin(GL2.GL_LINES);

		// x
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glVertex3f(0f, 0f, 0f);
		gl.glVertex3f(100f, 0f, 0f);

		// y
		gl.glVertex3f(0f, 0f, 0f);
		gl.glVertex3f(0f, 100f, 0f);

		// z
		gl.glVertex3f(0f, 0f, 0f);
		gl.glVertex3f(0f, 0f, 100f);

		gl.glEnd();

	}

	public void keyTyped(KeyEvent e) {}
	
	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {}

	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_W:
			dopredu = false;
			break;
		case KeyEvent.VK_S:
			dozadu = false;
			break;
		case KeyEvent.VK_A:
			doleva = false;
			break;
		case KeyEvent.VK_D:
			doprava = false;
			break;
		case KeyEvent.VK_E:
			nahoru = false;
			break;
		case KeyEvent.VK_Q:
			dolu = false;
			break;
		}
	}

	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_W:
			dopredu = true;
			break;
		case KeyEvent.VK_S:
			dozadu = true;
			break;
		case KeyEvent.VK_A:
			doleva = true;
			break;
		case KeyEvent.VK_D:
			doprava = true;
			break;
		case KeyEvent.VK_E:
			nahoru = true;
			break;
		case KeyEvent.VK_Q:
			dolu = true;
			break;
		}
	}

	public void mousePressed(MouseEvent e) {
		x = e.getX();
		y = e.getY();
		dx = 0;
		dy = 0;
		ox = x;
		oy = y;
	}

	public void mouseMoved(MouseEvent e) {
		x = e.getX();
		y = e.getY();
		dx += x - ox;
		ox = x;
		oy = y;
	}

	public void mouseDragged(MouseEvent e) {
		x = e.getX();
		y = e.getY();
		dx += x - ox;
		dy += y - oy;
		ox = x;
		oy = y;
	}
}