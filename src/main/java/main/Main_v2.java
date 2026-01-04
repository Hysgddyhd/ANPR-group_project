package main;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class Main_v2 extends JFrame {

	static {
		OpenCV.loadLocally();
	}

	private JLabel originalLabel;
	private JLabel resultLabel;


	public Main_v2() {
		setTitle("License Plate Detection System");
		setSize(1100, 550);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);


		JButton selectBtn = new JButton("Select Image");
		selectBtn.addActionListener(e -> chooseAndDetect());

		JPanel topPanel = new JPanel();
		topPanel.add(selectBtn);


		originalLabel = new JLabel("Original Image", JLabel.CENTER);
		resultLabel   = new JLabel("Detection Result", JLabel.CENTER);

		originalLabel.setBorder(
				BorderFactory.createTitledBorder("Original Image"));
		resultLabel.setBorder(
				BorderFactory.createTitledBorder("Detected License Plate"));

		JPanel imagePanel = new JPanel(new GridLayout(1, 2, 10, 10));
		imagePanel.add(originalLabel);
		imagePanel.add(resultLabel);

		add(topPanel, BorderLayout.NORTH);
		add(imagePanel, BorderLayout.CENTER);
	}


	private void chooseAndDetect() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select Vehicle Image");

		if (chooser.showOpenDialog(this)
				!= JFileChooser.APPROVE_OPTION) return;

		File file = chooser.getSelectedFile();
		Mat original = Imgcodecs.imread(file.getAbsolutePath());

		if (original.empty()) {
			JOptionPane.showMessageDialog(
					this, "Failed to load image!");
			return;
		}

		Mat result = original.clone();

		long start = System.currentTimeMillis();
		detectLicensePlate(result);
		long end = System.currentTimeMillis();

		System.out.println(
				"Detection time: " + (end - start) + " ms");

		originalLabel.setIcon(
				new ImageIcon(matToImage(original)));
		resultLabel.setIcon(
				new ImageIcon(matToImage(result)));
	}


	void detectLicensePlate(Mat image) {

		Mat gray = new Mat();
		//convert rgb image to grayscale
		// Split the BGR image into separate channels
		ArrayList<Mat> channels = new ArrayList<>();
		Core.split(image, channels);

		Mat b = channels.get(0);
		Mat g = channels.get(1);
		Mat r = channels.get(2);

		// Calculate weighted average: 0.3*r + 0.59*g + 0.11*b
		Core.addWeighted(r, 0.3, g, 0.59, 0.0, gray);
		Core.addWeighted(gray, 1.0, b, 0.11, 0.0, gray);

		//blur, eliminate noise
		Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

		//equalize histogram
		Imgproc.equalizeHist(gray, gray);
		//binarize
		Mat binary = new Mat();
		Imgproc.threshold(
				gray, binary, 0, 255,
				Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
		//morphological closing
		Mat kernel = Imgproc.getStructuringElement(
				Imgproc.MORPH_RECT, new Size(3, 3));
		Imgproc.morphologyEx(
				binary, binary,
				Imgproc.MORPH_CLOSE, kernel);

		// 6. Haar Cascade
		String resourcePath = "haarcascade_russian_plate_number.xml";
		CascadeClassifier detector = null;
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
			detector = new CascadeClassifier(tempXmlPath);

			if (!detector.empty()) {
				System.out.println("Plate detector loaded successfully from: " + tempXmlPath);
			} else {
				throw new RuntimeException("CascadeClassifier failed to load the XML file from temp path.");
			}

		} catch (Exception e) {
			e.printStackTrace();
			// Handle the error (e.g., log it, exit the program)
			System.exit(0);;
		}

		MatOfRect plates = new MatOfRect();


		detector.detectMultiScale(
				gray,
				plates,
				1.1,
				5,
				0,
				new Size(40, 40),
				new Size()
				);


		Rect bestPlate = null;
		double maxArea = 0;

		for (Rect rect : plates.toArray()) {

			double ratio = (double) rect.width / rect.height;
			double area  = rect.width * rect.height;

			if (ratio > 1.8 && ratio < 6.0) {
				if (area > maxArea) {
					maxArea = area;
					bestPlate = rect;
				}
			}
		}

		if (bestPlate != null) {
			Imgproc.rectangle(
					image,
					new Point(bestPlate.x, bestPlate.y),
					new Point(bestPlate.x + bestPlate.width,
							bestPlate.y + bestPlate.height),
					new Scalar(0, 0, 255),
					3
					);
			System.out.println("License plate detected.");
		} else {
			System.out.println("No license plate detected.");
		}
	}


	private Image matToImage(Mat mat) {

		Mat rgb = new Mat();

		if (mat.channels() == 3) {
			Imgproc.cvtColor(
					mat, rgb,
					Imgproc.COLOR_BGR2RGB);
		} else {
			rgb = mat;
		}

		byte[] data = new byte[
		                       rgb.rows() * rgb.cols() * rgb.channels()];
		rgb.get(0, 0, data);

		BufferedImage image = new BufferedImage(
				rgb.cols(),
				rgb.rows(),
				BufferedImage.TYPE_3BYTE_BGR);

		image.getRaster().setDataElements(
				0, 0,
				rgb.cols(), rgb.rows(),
				data);

		return image.getScaledInstance(
				originalLabel.getWidth(),
				originalLabel.getHeight(),
				Image.SCALE_SMOOTH);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() ->
		new Main_v2().setVisible(true));
	}
}
