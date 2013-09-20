package org.orphanware;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;

public class App {

	private static byte[] bitMask = new byte[8];
	static {
		bitMask[7] = 1;
		for (int i=0;i<7;i++)
			bitMask[6-i] = (byte) (bitMask[7] << (i+1));
	}
	
	public static void main(String[] args) {


		if (args.length == 0) {

			printHelp();
			System.exit(0);
		}

		int argIndex = 0;
		String filePath = null;
		Boolean withLB = false;
		Boolean invertPixels = false;
		String outputFileName = "image";
		
		for (String arg : args) {

			if (arg.equals("-help")) {

				printHelp();
				System.exit(0);

			}

			if (arg.equals("-f")) {

				try {
					filePath = args[argIndex + 1];
				} catch (Exception e) {

					System.out.println("-f switch found but was not followed by file path");
					System.exit(1);
				}



			}

			if (arg.equals(("-lb"))) {

				withLB = true;
			}
			
			if (arg.equals(("-i"))) {

				invertPixels = true;
			}
			
			if (arg.equals(("-o"))) {

				try {
					outputFileName = args[argIndex + 1];
				} catch (Exception e) {

					System.out.println("-o switch found but was not followed by image name");
					System.exit(1);
				}
			}

			argIndex++;
		}

		if (filePath != null) {

			convertImage(filePath, withLB, invertPixels, outputFileName);
			System.exit(0);

		}



	}

	public static void convertImage(String filePath, Boolean withLB, 
				       Boolean invertPixels, String outputFileName) {

		try {
			BufferedImage img = ImageIO.read(new File(filePath));
			int imgHeight = img.getHeight();
			int imgWidth = img.getWidth();
			
			System.out.println("Height: " + imgHeight + ", Width: " + imgWidth);

			// Each pixel is one bit
			int rowWidthBytes = (int) Math.ceil(((double)imgWidth) / 8);
			
			System.out.println("Row width will be "+rowWidthBytes+" bytes.");
			
			byte[] imageBytes = new byte[imgHeight * rowWidthBytes];

			for (int y=0; y<imgHeight; y++) {
				for (int x=0; x<imgWidth; x++) {
					boolean bit = getPixel(img,x,y);
					if (bit)
						setPixel(imageBytes,x,y,rowWidthBytes);
				}
			}

			if ( invertPixels ) {
				// Set the width "leftover" bits on last byte of each row to 1
				for (int y=0;y<imgHeight;y++) {
					for (int x=imgWidth;x<rowWidthBytes*8;x++) {
						setPixel(imageBytes,x,y,rowWidthBytes);
					}
				}
				
				System.out.println("Pixels inverted!");
				for (int i = 0; i < imageBytes.length; i++) {
					imageBytes[i] ^= 0xFF;
				}
			}

			String byteAsString = Hex.encodeHexString(imageBytes);

			if (withLB) {
				
				char[] bytesAsCharArr = byteAsString.toCharArray();
				
				// Every byte represented by two characters in hex 
				int lineBreakCount = rowWidthBytes*2;

				System.out.println("Adding line break every: " + lineBreakCount + " characters");
				StringBuilder lineBrokenStr = new StringBuilder();
				for (int i = 0; i < bytesAsCharArr.length; i++) {

					if (i % lineBreakCount == 0) {
						lineBrokenStr.append("\n");
					}

					lineBrokenStr.append(bytesAsCharArr[i]);
				}

				byteAsString = lineBrokenStr.toString();
			}

			String imageTemplate = "~DG" + outputFileName + "," + imageBytes.length;
			imageTemplate       += "," + rowWidthBytes + "," + byteAsString;
			FileOutputStream fos = new FileOutputStream(outputFileName + ".grf");
			fos.write(imageTemplate.getBytes());
			fos.close();

			System.out.println("Finished!  Check for file \"" + outputFileName + ".grf\" in executing dir");


		} catch (FileNotFoundException ex) {
			System.out.println("Error.  No file found at path: " + filePath);
			System.exit(1);
		} catch (IOException ex) {
			System.out.println("Error.  No file found at path: " + filePath);
			System.exit(1);
		}
	}

	private static boolean getPixel(BufferedImage img, int x, int y) {
		int rgb = img.getRGB(x, y);
		
		// From http://stackoverflow.com/questions/4801366/convert-rgb-values-into-integer-pixel
		int r = (rgb>>16)&0x0ff;
		int g = (rgb>>8) &0x0ff;
		int b = (rgb)    &0x0ff;
		
		int grayscale = (r+g+b)/3;
		return grayscale >= 127;
	}

	private static void setPixel(byte[] imageBytes, int x, int y, int rowWidthBytes) {
		int bit = x%8;
		int byteIdx = y*rowWidthBytes + x/8;
		imageBytes[byteIdx] |= bitMask[bit];
	}
	
	
	
	public static void printHelp() {

		System.out.println("\nImage to Zebra GRF encoder.");
		System.out.println("Written by Arash Sharif");
		System.out.println("Released under MIT license @ http://opensource.org/licenses/MIT");
		System.out.println("-----------------------------------------------------------------------------------------");
		System.out.println("Basic Use: java -jar img2grf.jar -f {path to file}");
		System.out.println("-----------------------------------------------------------------------------------------");
		System.out.println("switches:\n");
		System.out.println("required:");
		System.out.println("-f \t-must be followed with path to the image you want to encode");
		System.out.println("optional:");
		System.out.println("-lb\t-tells encoder to insert line break at widths.  helps reading eye with naked eye.");
		System.out.println("-i \t-tells encoder to invert pixels");
		System.out.println("-o \t-must be followed by the name of the grf file (WITHOUT EXTENTION!). Used for encoding!");
		System.out.println("-----------------------------------------------------------------------------------------");
		System.out.println("Source found @ https://github.com/asharif/img2grf");
		System.out.println("-----------------------------------------------------------------------------------------");
		System.out.println("Supported image file formats on this platform:");
		for (String name : ImageIO.getReaderFormatNames())
			System.out.println(name);
	}
}
