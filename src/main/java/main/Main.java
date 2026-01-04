package main;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.HighGui;
import org.opencv.objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main {

	static {
		OpenCV.loadLocally();
	}

	public static void main(String[] args) {

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Please choose pictire");

		fileChooser.setCurrentDirectory(new File("."));

		int result = fileChooser.showOpenDialog(null);
		if (result != JFileChooser.APPROVE_OPTION) {
			System.out.println("No image selected.");
			return;
		}

		File selectedFile = fileChooser.getSelectedFile();
		String imagePath = selectedFile.getAbsolutePath();


		Mat image = Imgcodecs.imread(imagePath);
		if (image.empty()) {
			System.out.println("Image failed to load!");
			System.exit(0);
		}

		BufferedImage grayImg = detectAndDrawPlate(image);

		HighGui.imshow("NP detection result: (press any key to exit)", image);
		HighGui.imshow("Grayscale image", fromBufferedImage(grayImg));
		HighGui.waitKey(0);
		HighGui.destroyAllWindows();
		System.exit(0);
	}


	public static BufferedImage detectAndDrawPlate(Mat image) {

		BufferedImage grayImage;
		grayImage = Mat2BufferedImage(image);
		grayImage = RGBtoGray(grayImage);


		String resourcePath = "haarcascade_russian_plate_number.xml";
		CascadeClassifier plateDetector = null;
		File tempFile = null;

		try {
			// Get the InputStream for the resource.
			InputStream inputStream = Main.class.getResourceAsStream(resourcePath);
			if (inputStream == null) {
				// Handle case where resource is not found
				throw new RuntimeException("Resource not found in classpath: " + resourcePath);
			}
			// 2. Create a temporary file
			tempFile = File.createTempFile("plate_cascade", ".xml");
			tempFile.deleteOnExit(); 
			// Copy the contents from the InputStream to the temporary file
			Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			inputStream.close(); // Close the input stream

			// 3. Use the temporary file path for CascadeClassifier
			String tempXmlPath = tempFile.getAbsolutePath();
			plateDetector = new CascadeClassifier(tempXmlPath);

			if (!plateDetector.empty()) {
				System.out.println("Plate detector loaded successfully from: " + tempXmlPath);
			} else {
				throw new RuntimeException("CascadeClassifier failed to load the XML file from temp path.");
			}

		} catch (Exception e) {
			e.printStackTrace();
			// Handle the error (e.g., log it, exit the program)
			return null;
		}

		MatOfRect plateDetections = new MatOfRect();

		plateDetector.detectMultiScale(
				fromBufferedImage(grayImage),
				plateDetections,
				1.1,
				3,
				0,
				new Size(30, 30),
				new Size()
				);

		// 4. draw red outline
		int count = 0;
		/*Rect minimalRect = plateDetections.toArray()[0];
		for (Rect rect : plateDetections.toArray()) {
			if (minimalRect.area()>rect.area()){
				minimalRect = rect;
			}
		}*/
		for (Rect rect : plateDetections.toArray()) {

			double aspectRatio = (double) rect.width / rect.height;

			if (aspectRatio > 2.0 && aspectRatio < 5.5) {
				Imgproc.rectangle(
						image,
						new Point(rect.x, rect.y),
						new Point(rect.x + rect.width, rect.y + rect.height),
						new Scalar(0, 0, 255),
						3
						);
				count++;
			}
		}

		System.out.println("This image includes " + count + " NP regions");
		return grayImage;
	}
	
	public static BufferedImage Mat2BufferedImage(Mat matrix){
		MatOfByte mob=new MatOfByte();
		Imgcodecs.imencode(".jpg", matrix, mob);
		try {
			return ImageIO.read(new ByteArrayInputStream(mob.toArray()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public static Mat fromBufferedImage(BufferedImage img) {
		byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC1);
		mat.put(0, 0, pixels);
		return mat;
	}
	
	public static BufferedImage RGBtoGray(BufferedImage colorImage) {
		BufferedImage grayImage = new BufferedImage(colorImage.getWidth(), colorImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster colorRaster = colorImage.getRaster();	
		WritableRaster grayRaster = grayImage.getRaster();
		for (int col=0;col<colorImage.getWidth();col++) {
			for (int row=0;row<colorImage.getHeight();row++) {
				int[] pixel = colorRaster.getPixel(col, row, (int[])null);
				int red = pixel[0];
				int green = pixel[1];
				int blue = pixel[2];
				int gray = (int)Math.round(0.30*red + 0.59*green + 0.11*blue);
				grayRaster.setSample(col, row, 0, gray);
			}
		}
		return grayImage;
	}
}