package pl.tupsonik.pacjack;
import android.app.Activity;
import android.os.Bundle;
public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setFlags(1024,1024); setContentView(new GameView(this));}
}
