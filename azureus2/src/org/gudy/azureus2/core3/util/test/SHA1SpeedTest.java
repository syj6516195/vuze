/*
 * Created on Mar 12, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.util.test;

import java.nio.*;

import org.gudy.azureus2.core3.util.SHA1;
import org.gudy.azureus2.core3.util.SHA1Az;

/**
 */
public class SHA1SpeedTest {
  
  private static final int BUFF_MAX_SIZE = 4 * 1024 * 1024;
  
  private static final int[] LOOPS = {20000, 10000, 4000, 3000, 2000, 1200};
  private static final int[] TESTS = {   16,    64,  256,  512, 1024, 2048};
  
	public static void main(String[] args) {
    
    SHA1 sha1Jmule = new SHA1();
    SHA1Az sha1Gudy = new SHA1Az();
    
    ByteBuffer dBuffer = ByteBuffer.allocateDirect( BUFF_MAX_SIZE );
    ByteBuffer fBuffer = ByteBuffer.allocateDirect( 20 );
    
    fBuffer.order(ByteOrder.BIG_ENDIAN);
    
    for (int i=0; i < BUFF_MAX_SIZE; i++) {
      byte b = (byte)(Math.random() * 255);
      dBuffer.put( b );
    }
    
    //allow time for setting thread to high-priority
    try { Thread.sleep(10000); } catch (Exception ignore) {}
    

    for (int t=0; t < TESTS.length; t++) {
      
      int buffsize = TESTS[t] * 1024;
      dBuffer.limit( buffsize );
      
      int loops = LOOPS[t];
    
    	String info = " [" + buffsize/1024 + "KB, " + loops + "x] = ";
      
    	float totalMBytes = ((float)buffsize / (1024 * 1024)) * loops;
    
    	System.out.print("JMule SHA1 ");
    	long jds = System.currentTimeMillis();
    	for (int i=0; i < loops; i++) {
    		dBuffer.position(0);
        dBuffer.limit( buffsize );
        fBuffer.position(0);
        sha1Jmule.update( dBuffer );
    		sha1Jmule.finalDigest( fBuffer );
    	}
    	long jde = System.currentTimeMillis();
  
    	long jdt = (jde - jds);
    	float jdspeed = totalMBytes / (jdt / 1000);
      
    	System.out.println(info + jdt + " ms @ " + jdspeed + " MB/s");

    
    	System.out.print("Gudy SHA1-1");
    	long gds = System.currentTimeMillis();
    	for (int i=0; i < loops; i++) {
    		dBuffer.position(0);
        dBuffer.limit( buffsize );
    		sha1Gudy.digest1( dBuffer );
    	}
    	long gde = System.currentTimeMillis();
    
    	long gdt = (gde - gds);
    	float gdspeed = totalMBytes / (gdt / 1000);
    	System.out.println(info + gdt + " ms @ " + gdspeed + " MB/s");
      
      
      System.out.print("Gudy SHA1-2");
      long g2ds = System.currentTimeMillis();
      for (int i=0; i < loops; i++) {
        dBuffer.position(0);
        dBuffer.limit( buffsize );
        sha1Gudy.digest2( dBuffer );
      }
      long g2de = System.currentTimeMillis();
    
      long g2dt = (g2de - g2ds);
      float g2dspeed = totalMBytes / (g2dt / 1000);
      System.out.println(info + g2dt + " ms @ " + g2dspeed + " MB/s");
      
      
      System.out.println();
    
    }
    
    System.out.println("DONE");
    
    
	}

}
