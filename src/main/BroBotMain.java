package main;

import javax.swing.*;
import java.lang.*;



public class BroBotMain {

    public static void main(String[] args) {
        
        // initialize bot config to straight up
        
        // start GUI
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // create app frame
                BroBotFrame frame = new BroBotFrame();
                // build controller
                BroBotApplicationController appController = new BroBotApplicationController(frame);
                
            }
        });

    }

}