package main;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

public class DetectMultiVehicles {
	static Main_v2 detector = new Main_v2();
	static ImageIcon img;
	static String img_dir, img_name;
	static JFrame jf;
	static int img_width, img_height;
	public static void main(String args[]) {
		jf = new JFrame();
		jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		jf.setLocationRelativeTo(null);

		File[] files = selectMultiImage();

		for (File file: files) {
			processSingleImg(file);
			resizeImg();
			jf.setSize(img_width, img_height+38);
			BufferedImage detected_img= detectSingleImg();
			saveResultImage(detected_img);
		}
		

	}



	public static void processSingleImg(File file) {
		if (file==null) {
			file = selectSingleImg();
		}
		openImageFile(file);
	}

	public static File selectSingleImg() {
		JFileChooser image_chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Image File", "jpg", "gif", "png", "jpeg", "svg", "tiff", "avif");
		image_chooser.setFileFilter(filter);
		int option = image_chooser.showOpenDialog(image_chooser);
		jf.add(image_chooser);
		if (option == JFileChooser.APPROVE_OPTION) {
			jf.remove(image_chooser);
			return image_chooser.getSelectedFile();

		} else {
			System.exit(0);
			return null;
		}

	}

	public static File[] selectMultiImage() {
		JFileChooser image_chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Image File", "jpg", "gif", "png", "jpeg", "svg", "tiff", "avif");
		image_chooser.setFileFilter(filter);
		image_chooser.setMultiSelectionEnabled(true);
		int option = image_chooser.showOpenDialog(image_chooser);
		jf.add(image_chooser);
		if (option == JFileChooser.APPROVE_OPTION) {
			jf.remove(image_chooser);
			return image_chooser.getSelectedFiles();

		} else {
			System.exit(0);
			return null;
		}

	}


	public static void openImageFile(File file) {
		System.out.println("Selected Image: " + file.getPath());
		img_dir=file.getAbsoluteFile().getParent();
		img_name = file.getName();
		img = new ImageIcon(file.getPath());
		System.out.println("Image dimension: "+ img.getIconWidth()+"x"+ img.getIconHeight());
		jf.setTitle(file.getName());
		jf.pack();
	}

	public static BufferedImage detectSingleImg() {
		if (img_width != 0 && img_height != 0) {
			Image scaled_img = img.getImage().getScaledInstance(img_width, img_height,  java.awt.Image.SCALE_DEFAULT);
			ImageIcon img2 = new ImageIcon(scaled_img);
			BufferedImage bufferedImg = new BufferedImage(img2.getIconWidth(), img2.getIconHeight(), BufferedImage.TYPE_3BYTE_BGR);
			bufferedImg.getGraphics().drawImage(img2.getImage(), 0, 0, null);
			Mat imgMat = fromBufferedImage(bufferedImg);
			BufferedImage detected_img = detectSingleVehicle(imgMat);
			return detected_img;
		}
		else
			return null;
	}
	public static void resizeImg() {
		if (img.getIconWidth()<=1344 && img.getIconHeight()<=756) {
			img_width = img.getIconWidth();
			img_height = img.getIconHeight();	
		}else {
			Double scale_ratio =  Math.min(1344/(double)img.getIconWidth(), 756/(double)img.getIconHeight());
			System.out.println(scale_ratio);
			img_width = (int)(scale_ratio * img.getIconWidth());
			img_height = (int)(scale_ratio * img.getIconHeight());
		}
	}

	public static void saveResultImage(BufferedImage detected_img) {
		String annotated_img_name = "detected-"+img_name;		
		File result = new File(img_dir+"/"+annotated_img_name);
		try {
			ImageIO.write(detected_img, "PNG", result);
			ImageIcon annotated_img = new ImageIcon(img_dir+"/"+annotated_img_name);
			Image annotated_scaled_img = annotated_img.getImage().getScaledInstance(img_width, img_height,  java.awt.Image.SCALE_DEFAULT);
			jf.add(new JLabel(new ImageIcon(annotated_scaled_img)));
			jf.setVisible(true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static BufferedImage detectSingleVehicle(Mat imgMat) {
		detector.detectLicensePlate(imgMat);
		return Mat2BufferedImage(imgMat);
	}

	public static Mat fromBufferedImage(BufferedImage img) {
		byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, pixels);
		return mat;
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
}
