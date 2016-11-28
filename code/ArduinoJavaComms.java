import purejavacomm.CommPortIdentifier;
import purejavacomm.SerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;

//Klasse die de verbinding met de Arduino maakt

public class ArduinoJavaComms implements SerialPortEventListener {
    SerialPort port = null;

    private BufferedReader input = null;
    private static OutputStream output;

    private static final int TIME_OUT = 1000; // Port open timeout
    private static final String PORT_NAMES[] = {  // PORTS
            "COM10","COM9","COM8","COM7", "COM6", "COM5", "COM4", "COM3", "COM2", "COM1" // Windows only
    };
    
    private int[][] fysiekDambord;

    //main om te testen.
    public static void main(String[] args) {
        ArduinoJavaComms arduino = new ArduinoJavaComms();
        arduino.initialize();
    }

    //begint de communicatie
    public void initialize() {
        try {
            CommPortIdentifier portid = null;
            Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();

            while (portid == null && portEnum.hasMoreElements()) {
                portid = (CommPortIdentifier)portEnum.nextElement();
                if ( portid == null )
                    continue;

                System.out.println("Trying: " + portid.getName());
                for ( String portName: PORT_NAMES ) {
                    if ( portid.getName().equals(portName)
                            || portid.getName().contains(portName)) {  // CONTAINS
                        port = (SerialPort) portid.open("ArduinoJavaComms", TIME_OUT);
                        port.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN+SerialPort.FLOWCONTROL_XONXOFF_OUT); // FLOW-CONTROL
                        //open streams
                        input = new BufferedReader(
                                new InputStreamReader( port.getInputStream() ));
                        output = port.getOutputStream();

                        System.out.println( "Connected on port: " + portid.getName() );
                        
                        port.addEventListener(this);
                        port.notifyOnDataAvailable(true);
                    }
                }
            }
            while ( true) {
                try { Thread.sleep(100); } catch (Exception ex) { }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //handelt output van de arduino af die in de inputStream van Java staat.
    @Override
    public void serialEvent(SerialPortEvent event) {
        try {
            switch (event.getEventType() ) {
                case SerialPortEvent.DATA_AVAILABLE:
                    String inputLine = input.readLine();
                    if(inputLine.length()==1){
                        System.out.println(inputLine);
                    }
                    else if(inputLine.length() > 1){
                        System.out.println(inputLine);
                        if(inputLine.startsWith("f")){
                        	setFysiekDambord(inputLine);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //methode om een reeks codes naar de Arduino te sturen zodat de robotarm kan slaan.
    public void robotSlaat(int oldX, int oldY, int newX, int newY, int geslagenX, int geslagenY){
    	System.out.println("Proberen om met de robot te slaan...");
    	send(oldX, oldY);
    	send(newX, newY);
    	send(geslagenX, geslagenY);
    	send(10,10);
    }

    //methode om een reeks codes naar de Arduino te sturen zodat de robotarm kan schuiven.
    public void robotSchuift(int oldX, int oldY, int newX, int newY){
    	System.out.println("Proberen om met de robot te schuiven...");
    	send(oldX,oldY);
    	send(newX,newY);
    }
    
    public int naarCo�rdinaten(int dambordpositie){
    	//binnen het bord: 0 t/m 9
    	if(dambordpositie < 10)
    		return dambordpositie * 40 + 20;
    	//buiten het bord: 10
    	else
    		return 450;
    }
    
    //hulpmethode om co�rdinaten naar de Arduino te sturen
    public void send(int x, int y){
    	try{
    		String coords = "c " + naarCo�rdinaten(x) + " " + naarCo�rdinaten(y) + " ";
        	output.write(coords.getBytes());
        	output.flush();
        	System.out.println("Gelukt!");
    	}
    	catch(IOException|NullPointerException e){
            System.out.println("Er ging iets fout met het sturen van co�rdinaten, namelijk " + e);
        }
    }

    //returnt ontvangen data van het fysieke dambord
	public int[][] getFysiekDambord() {
		return fysiekDambord;
	}

	/*set fysieke dambord-data vanuit een verkregen string:
	//inputstring format: "f <getallen van 0 t/m 4, na iedere 10 cijfers komt een komma>"
	LEEG = 0
	ZWART = 1
	WIT = 2
	ZWARTEDAM = 3
	WITTEDAM = 4
	 */
	public void setFysiekDambord(String rauweInput) {
		int[][] updatedBord = new int[10][10];
		int x = 0;
		int y = 0;
		for(int i = 0; i < rauweInput.length(); i++){
			if(rauweInput.charAt(i) == '0' || rauweInput.charAt(i) == '1' || rauweInput.charAt(i) == '2' || rauweInput.charAt(i) == '3' || rauweInput.charAt(i) == '4'){
				updatedBord[y][x] = Character.getNumericValue(rauweInput.charAt(i));
				x = (x + 1) % 10;
			}
			if(rauweInput.charAt(i) == ','){
				y++;	
			}
			else{
				continue;
			}
		}
		this.fysiekDambord = updatedBord;
	}
}
