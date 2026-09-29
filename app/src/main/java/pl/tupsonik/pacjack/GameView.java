package pl.tupsonik.pacjack;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public class GameView extends View {
 Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 final int N=13;
 final int[][] maze={
  {1,1,1,1,1,1,1,1,1,1,1,1,1},
  {1,0,0,0,1,0,0,0,1,0,0,0,1},
  {1,0,1,0,1,0,1,0,1,0,1,0,1},
  {1,0,1,0,0,0,1,0,0,0,1,0,1},
  {1,0,1,1,1,0,1,1,1,0,1,0,1},
  {1,0,0,0,0,0,0,0,0,0,0,0,1},
  {1,1,1,0,1,1,1,0,1,1,1,0,1},
  {1,0,0,0,1,0,0,0,1,0,0,0,1},
  {1,0,1,0,1,0,1,0,1,0,1,0,1},
  {1,0,1,0,0,0,1,0,0,0,1,0,1},
  {1,0,1,1,1,0,1,1,1,0,1,0,1},
  {1,0,0,0,0,0,0,0,0,0,0,0,1},
  {1,1,1,1,1,1,1,1,1,1,1,1,1}};
 boolean[][] dots=new boolean[N][N];
 ArrayList<int[]> ghosts=new ArrayList<>();
 Random random=new Random();
 SharedPreferences prefs;
 long coins;
 int px=1,py=1,score,level=1,bet=10;
 boolean gameOver=false, won=false;
 float cell,ox,oy;

 public GameView(Context c){
  super(c); prefs=c.getSharedPreferences("pacjack",0);
  coins=prefs.getLong("coins",500); reset(); setFocusable(true);
 }
 void save(){prefs.edit().putLong("coins",coins).apply();}
 void reset(){
  px=1;py=1;score=0;gameOver=false;won=false;ghosts.clear();
  ghosts.add(new int[]{11,11}); ghosts.add(new int[]{11,1}); ghosts.add(new int[]{1,11});
  for(int y=0;y<N;y++)for(int x=0;x<N;x++) dots[y][x]=maze[y][x]==0;
 }
 void drawText(Canvas c,String s,float x,float y,float size,int color){
  p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(size);p.setColor(color);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);
 }
 @Override protected void onDraw(Canvas c){
  int W=getWidth(),H=getHeight(); c.drawColor(Color.rgb(7,8,18));
  cell=Math.min((W-24f)/N,(H-220f)/N);ox=(W-cell*N)/2f;oy=128;
  drawText(c,"PAC JACK",W/2f,36,28,Color.WHITE);
  drawText(c,"🪙 "+coins,W/2f,67,18,Color.rgb(255,210,45));
  drawText(c,"LEVEL "+level+"   •   SCORE "+score,W/2f,94,12,Color.LTGRAY);
  drawText(c,"STAKE  "+bet+"   |   WIN x"+(level+1),W/2f,112,12,Color.rgb(100,220,255));
  for(int y=0;y<N;y++)for(int x=0;x<N;x++){
   float l=ox+x*cell,t=oy+y*cell;
   if(maze[y][x]==1){p.setColor(Color.rgb(36,43,91));c.drawRoundRect(l+1,t+1,l+cell-1,t+cell-1,7,7,p);}
   else if(dots[y][x]){p.setColor(Color.rgb(255,214,70));c.drawCircle(l+cell/2,t+cell/2,3,p);}
  }
  p.setColor(Color.rgb(255,211,35));c.drawCircle(ox+(px+.5f)*cell,oy+(py+.5f)*cell,cell*.34f,p);
  p.setColor(Color.rgb(7,8,18));c.drawCircle(ox+(px+.64f)*cell,oy+(py+.38f)*cell,2,p);
  p.setColor(Color.rgb(255,67,90));for(int[]g:ghosts)c.drawCircle(ox+(g[0]+.5f)*cell,oy+(g[1]+.5f)*cell,cell*.30f,p);
  // controls / stakes
  p.setColor(Color.rgb(20,23,43));c.drawRoundRect(14,H-105,W-14,H-14,24,24,p);
  drawText(c,"←",W*.17f,H-52,28,Color.WHITE);drawText(c,"↑",W*.33f,H-52,28,Color.WHITE);
  drawText(c,"↓",W*.45f,H-24,25,Color.WHITE);drawText(c,"→",W*.61f,H-52,28,Color.WHITE);
  drawText(c,"BET -",W*.76f,H-60,13,Color.rgb(255,110,110));
  drawText(c,"BET +",W*.91f,H-60,13,Color.rgb(100,230,150));
  if(gameOver){
   p.setColor(Color.argb(235,0,0,0));c.drawRect(0,0,W,H,p);
   drawText(c,won?"JACKPOT!":"BUST!",W/2f,H/2f-35,34,won?Color.rgb(255,215,45):Color.rgb(255,80,90));
   drawText(c,won?("+"+(bet*(level+1))+" COINS"):"Tap to spin again",W/2f,H/2f+5,18,Color.WHITE);
  }
 }
 void move(int dx,int dy){
  if(gameOver)return;
  int nx=px+dx,ny=py+dy;
  if(nx<0||nx>=N||ny<0||ny>=N||maze[ny][nx]==1)return;
  px=nx;py=ny;
  if(dots[py][px]){dots[py][px]=false;score+=10;coins+=2;save();}
  for(int[]g:ghosts)if(g[0]==px&&g[1]==py){gameOver=true;won=false;coins=Math.max(0,coins-bet);save();}
  if(px==11&&py==11){coins+=bet*(level+1);level++;won=true;gameOver=true;save();}
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  if(e.getAction()!=MotionEvent.ACTION_UP)return true;
  float x=e.getX(),y=e.getY();int W=getWidth(),H=getHeight();
  if(gameOver){reset();return true;}
  if(y>H-120){
   if(x<W*.27)move(-1,0);
   else if(x<W*.40)move(0,-1);
   else if(x<W*.54)move(0,1);
   else if(x<W*.68)move(1,0);
   else if(x<W*.84){bet=Math.max(10,bet-10);}
   else {bet=Math.min(100,bet+10);}
  } else if(y>oy&&y<oy+cell*N){
   float dx=x-(ox+(px+.5f)*cell),dy=y-(oy+(py+.5f)*cell);
   if(Math.abs(dx)>Math.abs(dy))move(dx>0?1:-1,0);else move(0,dy>0?1:-1);
  }
  invalidate();return true;
 }
}
