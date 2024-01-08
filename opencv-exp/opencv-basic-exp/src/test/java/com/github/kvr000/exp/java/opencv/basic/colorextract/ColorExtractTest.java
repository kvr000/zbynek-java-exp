package com.github.kvr000.exp.java.opencv.basic.colorextract;


import nu.pattern.OpenCV;
import org.apache.commons.io.IOUtils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;


public class ColorExtractTest
{
	public static final Imgcodecs imageCodecs = new Imgcodecs();

	static {
		OpenCV.loadShared();
	}

	@Test
	public void extractBlue() throws Exception
	{
		Mat image = loadResourceImage(ColorExtractTest.class, "PartBlueInput.jpeg");
		Mat imageTransparent = new Mat(image.rows(), image.cols(), CvType.CV_8UC4);
		Imgproc.cvtColor(image, imageTransparent, Imgproc.COLOR_BGR2BGRA);
		Scalar lowBlueScalar = hsvToScalar(155, 0.5f, 0.4f);
		Scalar highBlueScalar = hsvToScalar(310, 1.0f, 1.0f);
		Mat hsv = new Mat(image.rows(), image.cols(), CvType.CV_8UC4);
		Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
		Mat resultHsv = new Mat(image.rows(), image.cols(), CvType.CV_8UC4);
		Core.inRange(hsv, lowBlueScalar, highBlueScalar, resultHsv);
		Mat resultBgr = new Mat(image.rows(), image.cols(), CvType.CV_8UC4);
		imageTransparent.copyTo(resultBgr, resultHsv);
		imageCodecs.imwrite("target/extractBlue.png", resultBgr);
		//Runtime.getRuntime().exec(new String[]{ "open", "target/extractBlue.png" }).waitFor();
	}

	public static Mat loadResourceImage(Class clazz, String path) throws IOException
	{
		try (InputStream stream = ColorExtractTest.class.getResourceAsStream("PartBlueInput.jpeg")) {
			byte[] bytes = IOUtils.toByteArray(stream);
			Mat image = imageCodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
			return image;
		}
	}

	public static Scalar rgbToScalar(byte r, byte g, byte b)
	{
		Mat rgb = new Mat(1, 1, CvType.CV_8UC3); rgb.setTo(new Scalar(r&0xff, g&0xff, b&0xff));
		Mat hsv = new Mat(1, 1, CvType.CV_64FC3);
		Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV);
		Scalar scalar = new Scalar(hsv.get(0, 0));
		return scalar;
	}

	public static Scalar hsvToScalar(float hueDegrees, float saturation1, float value1)
	{
		Scalar scalar = new Scalar(hueDegrees/360*256, saturation1*255, value1*255);
		return scalar;
	}
}
