package com.winlator.win32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MSLogFont {
  private byte charSet = 0;
  
  private byte clipPrecision = 0;
  
  private int escapement = 0;
  
  private String faceName = "Tahoma";
  
  private int height = -11;
  
  private byte italic = 0;
  
  private int orientation = 0;
  
  private byte outPrecision = 0;
  
  private byte pitchAndFamily = 34;
  
  private byte quality = 0;
  
  private byte strikeOut = 0;
  
  private byte underline = 0;
  
  private int weight = 400;
  
  private int width = 0;
  
  public MSLogFont fromByteArray(byte[] paramArrayOfbyte) {
    if (paramArrayOfbyte == null || paramArrayOfbyte.length < 92)
      return this; 
    ByteBuffer byteBuffer = ByteBuffer.wrap(paramArrayOfbyte).order(ByteOrder.LITTLE_ENDIAN);
    this.height = byteBuffer.getInt();
    this.width = byteBuffer.getInt();
    this.escapement = byteBuffer.getInt();
    this.orientation = byteBuffer.getInt();
    this.weight = byteBuffer.getInt();
    this.italic = byteBuffer.get();
    this.underline = byteBuffer.get();
    this.strikeOut = byteBuffer.get();
    this.charSet = byteBuffer.get();
    this.outPrecision = byteBuffer.get();
    this.clipPrecision = byteBuffer.get();
    this.quality = byteBuffer.get();
    this.pitchAndFamily = byteBuffer.get();
    StringBuilder stringBuilder = new StringBuilder();
    while (byteBuffer.remaining() > 0) {
      char c = byteBuffer.getChar();
      if (c == '\000')
        break; 
      stringBuilder.append(c);
    } 
    this.faceName = stringBuilder.toString();
    return this;
  }
  
  public String getFaceName() {
    return this.faceName;
  }
  
  public MSLogFont setFaceName(String paramString) {
    this.faceName = paramString;
    return this;
  }
  
  public MSLogFont setWeight(int paramInt) {
    this.weight = paramInt;
    return this;
  }
  
  public byte[] toByteArray() {
    ByteBuffer byteBuffer = ByteBuffer.allocate(92).order(ByteOrder.LITTLE_ENDIAN);
    byteBuffer.putInt(this.height);
    byteBuffer.putInt(this.width);
    byteBuffer.putInt(this.escapement);
    byteBuffer.putInt(this.orientation);
    byteBuffer.putInt(this.weight);
    byteBuffer.put(this.italic);
    byteBuffer.put(this.underline);
    byteBuffer.put(this.strikeOut);
    byteBuffer.put(this.charSet);
    byteBuffer.put(this.outPrecision);
    byteBuffer.put(this.clipPrecision);
    byteBuffer.put(this.quality);
    byteBuffer.put(this.pitchAndFamily);
    for (byte b = 0; b < this.faceName.length(); b++)
      byteBuffer.putChar(this.faceName.charAt(b)); 
    return byteBuffer.array();
  }
}


/* Location:              D:\Distributives\ApkDecompile\dex-tools-v2.4\Winlator_10.0_hotfix-dex2jar.jar!\com\winlator\win32\MSLogFont.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */