package com.secpro.platform.monitoring.agent.utils.io;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;


public class LinedInputStream
{
   private InputStream in;
   private byte[] b;
   private int CACHESIZE = 1024*1024;

   public LinedInputStream( InputStream in )
   {
       this.in = in;       
   }

   public LinedInputStream( InputStream in ,int cachesize )
   {
       this.in = in;
       this.CACHESIZE = cachesize;
   }
   
   public byte[] readLine() throws IOException
   {
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       
       int size = 0;
       boolean ifCRLF = false;
       
       while( true )
       {           
          
           fill();
           int read = 0; 
           size = b.length;   
           
           for( int i=0;i<size;i++)
           {
               read++;
               if( b[i] == '\n')
               {                   
                   ifCRLF = true;
                   break;
               }
               if( b[i] != '\r' )
               {
                   baos.write(b[i]);
               }
           }
           
           
           b = Arrays.copyOfRange(b,read,size);           
           if( ifCRLF )
               break;
       }

       byte[] b = baos.toByteArray();
       baos.close();
       baos = null;
       return b;
   }
   
   public void close() throws IOException
   {
       in.close();       
   }

   private void fill() throws IOException
   {       
       ByteArrayOutputStream baos = new ByteArrayOutputStream();

       if( b != null && b.length > 0 )
       {
           baos.write( b,0,b.length );
       }

       int size = in.available();       
       
       if( size > 0 )
       {
           if( size > CACHESIZE )
           {
               
               size = CACHESIZE;
           }
           byte[] _b = new byte[size];
           size = in.read(_b);
           baos.write(_b,0,size);           
           _b = null;                   
       }

       b = baos.toByteArray();
       baos = null;   
   }
   public static void main(String[] args) throws Exception{
           File f =new File("d:\\1387867150177");
           InputStream is=new FileInputStream(f);
          /* LinedInputStream lis=new LinedInputStream(is);
           String line="";
           while((line=lis.readLine())!=null){
                 //  System.out.println(new String(line.getBytes("gbk"),"gbk"));
                   System.out.println(line);
                   if(line.contains("# ")){
                           return ;
                   }
           }*/
           String line="";
           BufferedReader br=new BufferedReader(new InputStreamReader(is,"utf-8"));
           while((line=br.readLine())!=null){
                           System.out.println(line);
                        //   System.out.println(new String(line.getBytes("gbk"),"utf-8"));
                           if(line.contains("# ")){
                                   return ;
                           }
                   }
   }
}