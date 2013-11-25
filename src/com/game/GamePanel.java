package com.game;
import javax.swing.JPanel;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.*;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable, KeyListener{
	
	private static final long serialVersionUID = 1L;
	//fields
	public static int WIDTH = 400;
	public static int HEIGHT= 400;
	private Thread thread;
	private boolean running;
	
	private BufferedImage image;
	private Graphics2D g;
	
	private int FPS = 30;
	private double averageFPS;
	public static Player player;
	public static ArrayList<Bullet> bullets;
	public static ArrayList<Enemy> enemies;
	public static ArrayList<PowerUp> powerups;
	public static ArrayList<Explosion> explosions;
	
	private long waveStartTimer;
	private long waveStartTimerDiff;
	private long waveNumber;
	private boolean waveStart;
	private int waveDelay = 2000;
	private boolean playerHitDuringWave = false;
	
	//constructor
	public GamePanel(){
		super();
		setPreferredSize(new Dimension(WIDTH,HEIGHT));
		setFocusable(true);
		requestFocus();
	}
	
	public void addNotify(){
		super.addNotify();
		if(thread == null){
			thread = new Thread(this);
			thread.start();
		}
		addKeyListener(this);
	}

	
	public void run() {
		
		running = true;
		
		image = new BufferedImage(WIDTH,HEIGHT, BufferedImage.TYPE_INT_RGB);
		g = (Graphics2D) image.getGraphics();
		g.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		player = new Player();
		bullets = new ArrayList<Bullet>();
		enemies = new ArrayList<Enemy>();
		powerups = new ArrayList<PowerUp>();
		explosions = new ArrayList<Explosion>();
		
		waveStartTimer = 0;
		waveStartTimerDiff = 0;
		 waveNumber = 0;
		 waveStart = true;
		
		long startTime;
		long URDTimeMillis;
		long waitTime;
		long totalTime = 0;
		
		int frameCount=0;
		int maxFrameCount=30;
		long targetTime = 1000/FPS;
		
		
		
		//Game LOOP
		while(running){
			
			startTime = System.nanoTime();
			
			gameUpdate();
			gameRender();
			gameDraw();
			
			URDTimeMillis = (System.nanoTime()-startTime) / 1000000;
			waitTime = targetTime-URDTimeMillis;
			
			try{
				Thread.sleep(waitTime);
			}catch(Exception e){	
			}
			
			totalTime += System.nanoTime() - startTime;
			frameCount++;
			if(frameCount == maxFrameCount){
				averageFPS = 1000.0/ ((totalTime/frameCount)/1000000);
				frameCount = 0;
				totalTime = 0;
			}
		}
	}
	
	private void gameUpdate(){
		
		//new wave
		if(waveStartTimer == 0 && enemies.size() ==  0){
			waveNumber++;
			waveStart = false;
			waveStartTimer = System.nanoTime();
		}else{
			waveStartTimerDiff = (System.nanoTime() - waveStartTimer) /1000000;
			if(waveStartTimerDiff > waveDelay){
				waveStart = true;
				waveStartTimer = 0;
				waveStartTimerDiff = 0;
			}
		}
		
		//create enemies
		if(waveStart && enemies.size() == 0){
			createNewEnemies();
			if (waveNumber !=1){
			    if(!playerHitDuringWave){
			    	player.addScore(50);
			    	playerHitDuringWave = false;
			    }
			}
		}
		
		//player update 
		player.update();
		//bullet update
		for(int i = 0;i<bullets.size();i++){
			boolean remove = bullets.get(i).update();
			if(remove){
				bullets.remove(i);
				i--;
			}
		}
		
		//enemy update 
		for(int i = 0;i<enemies.size();i++){
			 enemies.get(i).update();
		}
		
		
		//powerup update
		for(int i = 0;i<powerups.size();i++){
			boolean remove = powerups.get(i).update();
			if(remove){
				powerups.remove(i);
				i--;
			}
		}
		
		//explosion update
		for(int i = 0;i<explosions.size();i++){
			boolean remove = explosions.get(i).update();
			if(remove){
				explosions.remove(i);
				i--;
			}
		}
		
		
		//bullet-enemy collision
		for(int i = 0;i<bullets.size();i++){
			Bullet b = bullets.get(i);
			for(int j = 0;j<enemies.size();j++){
				Enemy e = enemies.get(j);
				
				if (Distance(b,e) < b.getr() + e.getr()){
					e.hit();
					bullets.remove(i);
					i--;
					break;
				}
			}
		}
		//check dead enemies
		for(int j = 0;j<enemies.size();j++){
				if(enemies.get(j).isDead()){
					Enemy e = enemies.get(j);
					
					//chance for powerup
					double rand = Math.random();
					if(rand < 0.001) powerups.add(new PowerUp(1,e.getx(),e.gety()));
					else if(rand < 0.120) powerups.add(new PowerUp(2,e.getx(),e.gety()));
					else if(rand < 0.020) powerups.add(new PowerUp(3,e.getx(),e.gety()));
					player.addScore((e.getType() + e.getRank())*5);
					enemies.remove(j);
					j--;
					e.explode();
					explosions.add(new Explosion(e.getx(), e.gety(),e.getr(),e.getr()+20));
				}
		}
		
		//player-enemy collision
		if(!player.isRecovering()){
			for(int j = 0;j<enemies.size();j++){
				Enemy e = enemies.get(j);
				if (Distance(player,e) < player.getr() + e.getr()){
					player.loselife();
					playerHitDuringWave = true;
				}
				
			}
		}
		
		//player-powerup collision
		if(!player.isRecovering()){
			for(int j = 0;j<powerups.size();j++){
				PowerUp p = powerups.get(j);
				if (Distance(player,p) < player.getr() + p.getr()){
					
					
					//collected  power up
					if(p.getType() ==1){
						player.gainLife();
					}
					if(p.getType() ==2){
						player.increasePower(1);
					}
					if(p.getType() ==3){
						player.increasePower(2);
					}
					
					powerups.remove(j);
					j--;
					
				}
				
			}
		}
		
		
	}
	
	//Draws to Graphics
	private void gameRender(){
		
		//draw background
		g.setColor(new Color(0,100,255));
		g.fillRect(0, 0, WIDTH, HEIGHT);
		
		//draw player
		player.draw(g);
		//draw bullets
		for(int i = 0;i<bullets.size();i++){
			bullets.get(i).draw(g);
		}
		//draw enemy
		for(int i = 0;i<enemies.size();i++){
			 enemies.get(i).draw(g);
		}
		
		//draw powerup
		for(int i = 0;i<powerups.size();i++){
			 powerups.get(i).draw(g);
		}
		
		//draw explosions
		for(int i = 0;i<explosions.size();i++){
			 explosions.get(i).draw(g);
		}
		
		//draw wave number
		if(waveStartTimer != 0){
			g.setFont(new Font("Century Gothic", Font.PLAIN, 18));
			String s = "- W A V E    " + waveNumber + "   -";
			int length = (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
			int alpha = (int) (255+Math.sin(3.14* waveStartTimerDiff/waveDelay));
			if(alpha > 255) alpha = 255;
			g.setColor(new Color(255,255,255,alpha));
			g.drawString(s, WIDTH / 2 - length/2, HEIGHT /2);
		}
		//draw player lives
		for(int i = 0;i < player.getLives();i++){
			g.setColor(Color.WHITE);
			g.fillOval(10 + (20 * i), 10, 2*player.getr(), 2*player.getr());
			g.setStroke(new BasicStroke(3));
			g.setColor(Color.WHITE.darker());
			g.drawOval(10 + (20 * i), 10, 2*player.getr(), 2*player.getr());
			g.setStroke(new BasicStroke(1));
		}
		
		
		//draw player power
		g.setColor(Color.YELLOW);
		g.fillRect(10,30, player.getPower()*8,  8);
		g.setColor(Color.YELLOW.darker());
		g.setStroke(new BasicStroke(2));
		for(int i = 0; i < player.getRequiredPower();i++){
			g.drawRect(10+8*i, 30, 8, 8);
		}
		g.setStroke(new BasicStroke(1));
		
		//draw player score
		g.setColor(Color.WHITE);
		g.setFont(new Font("Century Gothic", Font.PLAIN, 14));
		g.drawString("Score:  "+ player.getScore(), WIDTH -100, 20);
	}
	//Draws completed image to the screen
	private void gameDraw(){
		Graphics g2 = this.getGraphics();
		g2.drawImage(image,0,0, null);
		g2.dispose();
	}

	private void createNewEnemies(){
		enemies.clear();
		Enemy e;
		if(waveNumber == 1){
			for(int i = 0; i<4;i++){
				enemies.add(new Enemy(1,1));
			}
		}
		if(waveNumber == 2){
			for(int i = 0; i<8;i++){
				enemies.add(new Enemy(1,1));
			}
			enemies.add(new Enemy(1,2));
			enemies.add(new Enemy(1,2));
		}
		if(waveNumber == 3){
			enemies.add(new Enemy(1,3));
			enemies.add(new Enemy(1,3));
			enemies.add(new Enemy(1,4));
		}
	}
	
	
	public void keyPressed(KeyEvent key) {
		int keyCode = key.getKeyCode();
		if(keyCode ==KeyEvent.VK_LEFT){
			player.setLeft(true);
		}
		if(keyCode ==KeyEvent.VK_RIGHT){
			player.setRight(true);
		}
		if(keyCode ==KeyEvent.VK_UP){
			player.setUp(true);
		}
		if(keyCode ==KeyEvent.VK_DOWN){
			player.setDown(true);
		}
		if(keyCode ==KeyEvent.VK_Z){
			player.setFiring(true);
		}
	}

	
	public void keyReleased(KeyEvent key) {
		int keyCode = key.getKeyCode();
		if(keyCode ==KeyEvent.VK_LEFT){
			player.setLeft(false);
		}
		if(keyCode ==KeyEvent.VK_RIGHT){
			player.setRight(false);
		}
		if(keyCode ==KeyEvent.VK_UP){
			player.setUp(false);
		}
		if(keyCode ==KeyEvent.VK_DOWN){
			player.setDown(false);
		}
		if(keyCode ==KeyEvent.VK_Z){
			player.setFiring(false);
		}
		
	}

	
	public void keyTyped(KeyEvent key) {
		
		
	}

	public double Distance(Bullet b, Enemy e){
		double dx = b.getx() - e.getx();
		double dy = b.gety() - e.gety();
		return  Math.sqrt(dx * dx + dy *dy);
	}
	public double Distance(Player p, Enemy e){
		double dx = p.getx() - e.getx();
		double dy = p.gety() - e.gety();
		return  Math.sqrt(dx * dx + dy *dy);
	}
	public double Distance(Player p, PowerUp e){
		double dx = p.getx() - e.getx();
		double dy = p.gety() - e.gety();
		return  Math.sqrt(dx * dx + dy *dy);
	}
	
}
