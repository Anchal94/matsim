package playground.ciarif.carpooling;

import org.matsim.core.controler.Controler;

public class CarPoolingControler extends Controler {
	
	public CarPoolingControler (String[] args){
		super(args);
	}
	protected void loadControlerListeners() {
		
		super.loadControlerListeners();
		
		// the scoring function processes facility loads
		this.addControlerListener(new CarPoolingListener());
		
	}
	public static void main (final String[] args) { 
		Controler controler = new CarPoolingControler(args);
		controler.run();
	}
}







