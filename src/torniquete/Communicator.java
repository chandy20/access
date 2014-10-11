/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package torniquete;

import gnu.io.*;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TooManyListenersException;

public class Communicator implements SerialPortEventListener {

    //passed from main GUI
    GUI2 window = null;

    //for containing the ports that will be found
    private Enumeration ports = null;
    //map the port names to CommPortIdentifiers
    private HashMap portMap = new HashMap();

    //this is the object that contains the opened port
    private CommPortIdentifier selectedPortIdentifier = null;
    private SerialPort serialPort = null;

    //input and output streams for sending and receiving data
    private InputStream input = null;
    private OutputStream output = null;

    //just a boolean flag that i use for enabling
    //and disabling buttons depending on whether the program
    //is connected to a serial port or not
    private boolean bConnected = false;

    //the timeout value for connecting with the port
    final static int TIMEOUT = 2000;

    //some ascii values for for certain things
    final static int SPACE_ASCII = 32;
    final static int DASH_ASCII = 45;
    final static int NEW_LINE_ASCII = 10;

    //a string for recording what goes on in the program
    //this string is written to the GUI
    String logText = "";

//    public Communicator(GUI window) {
////        this.window = window;
//    }
    public Communicator(GUI2 window) {
        this.window = window;
    }

    //search for all the serial ports
    //pre: none
    //post: adds all the found ports to a combo box on the GUI
    public void searchForPorts() {
        ports = CommPortIdentifier.getPortIdentifiers();

        while (ports.hasMoreElements()) {
            CommPortIdentifier curPort = (CommPortIdentifier) ports.nextElement();

            //get only serial ports
            if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                System.out.println("curPort.getName():"+curPort.getName());
                if(curPort.getName().isEmpty()==false)
                {
                    
                    window.cboxPorts.addItem(curPort.getName());
                    portMap.put(curPort.getName(), curPort);
                }
                
            }
        }
    }

    //connect to the selected port in the combo box
    //pre: ports are already found by using the searchForPorts method
    //post: the connected comm port is stored in commPort, otherwise,
    //an exception is generated
    public void connect() {
        String selectedPort = (String) window.cboxPorts.getSelectedItem();
        selectedPortIdentifier = (CommPortIdentifier) portMap.get(selectedPort);

        CommPort commPort = null;

        try {
            //the method below returns an object of type CommPort
            commPort = selectedPortIdentifier.open("TigerControlPanel", TIMEOUT);
            //the CommPort object can be casted to a SerialPort object
            serialPort = (SerialPort) commPort;

            //for controlling GUI elements
            setConnected(true);

            //logging
            logText = selectedPort + " opened successfully.";
//            window.txtLog.setForeground(Color.black);
//            window.txtLog.append(logText + "\n");

            //CODE ON SETTING BAUD RATE ETC OMITTED
            //XBEE PAIR ASSUMED TO HAVE SAME SETTINGS ALREADY
            //enables the controls on the GUI if a successful connection is made
            window.keybindingController.toggleControls();
        } catch (PortInUseException e) {
            logText = selectedPort + " is in use. (" + e.toString() + ")";

//            window.txtLog.setForeground(Color.RED);
//            window.txtLog.append(logText + "\n");
        } catch (Exception e) {
            logText = "Failed to open " + selectedPort + "(" + e.toString() + ")";
//            window.txtLog.append(logText + "\n");
//            window.txtLog.setForeground(Color.RED);
        }
    }

    //open the input and output streams
    //pre: an open port
    //post: initialized intput and output streams for use to communicate data
    public boolean initIOStream() {
        //return value for whather opening the streams is successful or not
        boolean successful = false;

        try {
            //
            input = serialPort.getInputStream();
            output = serialPort.getOutputStream();
//            writeData(0, 0);
            getcounter();
            successful = true;
            return successful;
        } catch (IOException e) {
            logText = "I/O Streams failed to open. (" + e.toString() + ")";
//            window.txtLog.setForeground(Color.red);
//            window.txtLog.append(logText + "\n");
            return successful;
        }
    }

    //starts the event listener that knows whenever data is available to be read
    //pre: an open serial port
    //post: an event listener for the serial port that knows when data is recieved
    public void initListener() {
        try {
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        } catch (TooManyListenersException e) {
            logText = "Too many listeners. (" + e.toString() + ")";
//            window.txtLog.setForeground(Color.red);
//            window.txtLog.append(logText + "\n");
        }
    }

    //disconnect the serial port
    //pre: an open serial port
    //post: clsoed serial port
    public void disconnect() {
        //close the serial port
        try {
//            writeData(0, 0);
            
            serialPort.removeEventListener();
            serialPort.close();
            input.close();
            output.close();
            setConnected(false);
            window.keybindingController.toggleControls();

            logText = "Disconnected.";
//            window.txtLog.setForeground(Color.red);
//            window.txtLog.append(logText + "\n");
            
        } catch (Exception e) {
            logText = "Failed to close " + serialPort.getName() + "(" + e.toString() + ")";
//            window.txtLog.setForeground(Color.red);
//            window.txtLog.append(logText + "\n");
        }
    }

    final public boolean getConnected() {
        return bConnected;
    }

    public void setConnected(boolean bConnected) {
        this.bConnected = bConnected;
    }

    String entrada = "";
    int continuar = 2;

    //what happens when data is received
    //pre: serial event is triggered
    //post: processing on the data it reads
    public void serialEvent(SerialPortEvent evt) {
        if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                byte singleData = (byte) input.read();

                if (singleData != NEW_LINE_ASCII) {
                    if (continuar == 2) {
                        logText = new String(new byte[]{singleData});
//                    System.out.println("Llego "+logText);
                        entrada += logText;
//                    window.txtLog.append(logText);

                        if (logText.equals("E")) {
                            System.out.println("Tam: " + entrada.length());
//                            window.txtLog.append("\n");
                            finalizarLlegada();
                        }
                    }else
                        continuar++;

                }

//                else
//                {
//                    
//                    
//                }
            } catch (Exception e) {
                logText = "Failed to read data. (" + e.toString() + ")";
//                window.txtLog.setForeground(Color.red);
//                window.txtLog.append(logText + "\n");
            }
        } else {
//            finalizarLlegada();
        }
    }

    //method that can be called to send data
    //pre: open serial port
    //post: data sent to the other device
    public void writeData(int leftThrottle, int rightThrottle,String mensaje) {
        try {
            System.out.println("Escribiendo");
//            output.write(leftThrottle);
//            output.flush();
//            //this is a delimiter for the data
//            output.write(DASH_ASCII);
//            output.flush();
//            
//            output.write(rightThrottle);
//            output.flush();
//            //will be read as a byte so it is a space key
//            output.write(SPACE_ASCII);
//            output.flush();

            String EN = "S006000000E20";
            System.out.println("Mensaje: "+mensaje);
//            System.out.println("Checksum: "+calcularChecksum(EN, EN.length()));
//          output.write(Byte.parseByte(EN));
            for (int i = 0; i < mensaje.length(); i++) {
                char c = mensaje.charAt(i);
                int ascii = (int) c;
                output.write(ascii);

            }
            output.flush();

        } catch (Exception e) {
            logText = "Failed to write data. (" + e.toString() + ")";
//            window.txtLog.setForeground(Color.red);
//            window.txtLog.append(logText + "\n");
        }
    }

    private void finalizarLlegada() {

        System.out.println("Llego: " + entrada);
        System.out.println("Llego: " + entrada.substring(0, 13));
        
        TorniqueteDAO dao = new TorniqueteDAO();
        if(entrada.equals("S011000000000E")){
            dao.salida();
//            window.txtLog.append("Saliendo");
            getcounter();
        }else if(entrada.equals("S010000000000E")){
            dao.entrada();
//            window.txtLog.append("Entrando");
            getcounter();
        }else if(entrada.substring(0, 13).equals("S006000000012"))
        {
//            window.txtLog.append("Obteniendo contadores");
            String y,x;
            y=entrada.substring(13, 19);
            x=entrada.substring(19, 25);
            System.out.println("Entrada:"+y);
            System.out.println("Salida:"+x);
            int yy=Integer.parseInt(y);
            int xx=Integer.parseInt(x);
            System.out.println("Entrada:"+yy);
            System.out.println("Salida:"+xx);
//            window.lblEntrada.setText(yy+"");
//            window.lblSalida.setText(xx+"");
            
        }
        entrada = "";
        continuar=0;
    }
    public void permitirentrar()
    {
        String mensaje="S009000000E2F";
        writeData(TIMEOUT, TIMEOUT, mensaje);
    }
    public void getcounter()
    {
        String mensaje="S006000000E20";
        writeData(TIMEOUT, TIMEOUT, mensaje);
    }
    private char calcularChecksum(String str, int nLength) {
        char uRet = 0;
        for (int i = 0; i < nLength; i++) {
            uRet ^= (char) str.charAt(i);
        }
        return uRet;
    }
}
