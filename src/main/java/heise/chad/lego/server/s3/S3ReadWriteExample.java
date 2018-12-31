package heise.chad.lego.server.s3;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import heise.chad.lego.brick.grid.BrickGrid;
import heise.chad.lego.brick.grid.BrickGridSplitter1;
import heise.chad.lego.brick.grid.BrickGridTransform;
import heise.chad.lego.brick.grid.BufferedImageBrickGridTransform;
import heise.chad.lego.color.grid.BufferedImageColorGridTransform;
import heise.chad.lego.color.grid.ColorColorGridTransform;
import heise.chad.lego.color.grid.LegoRectangleColorGridTransform;
import heise.chad.lego.color.measure.ColorMeasure;
import heise.chad.lego.color.measure.ExplodingEuclideanColorMeasure2;
import heise.chad.lego.color.palette.ColorPalette;
import heise.chad.lego.color.palette.LegoColorPalette;
import heise.chad.lego.color.transform.ColorPaletteColorTransform;

public class S3ReadWriteExample {

    public static void main(String[] args) {

        String accessKey = "";
        String secretKey = "";
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3Client s3Client = new AmazonS3Client(credentials);

        String bucketName = "lego-pictures";
        String objectKey = "before/GoldenDome.jpg";

        GetObjectRequest objRequest = new GetObjectRequest(bucketName, objectKey);
        S3Object object = s3Client.getObject(objRequest);
        InputStream objectData = object.getObjectContent();
        // Process the objectData stream.

        try {
            String imageFormat = "PNG"; // Use PNG not JPEG to avoid compression
                                        // artifacts

            ColorPalette palette = new LegoColorPalette();
            ColorMeasure colorMeasure = new ExplodingEuclideanColorMeasure2(.5, .7, .8, 20);
            Function<Color, Color> colorTransform = new ColorPaletteColorTransform(palette, colorMeasure);

            Function<BufferedImage, BrickGrid> fxn = new BufferedImageColorGridTransform()
                    .andThen(new LegoRectangleColorGridTransform(60))
                    .andThen(new ColorColorGridTransform(colorTransform))
                    .andThen(new BrickGridTransform())
                    .andThen(new BrickGridSplitter1());

            BufferedImage inputImage = ImageIO.read(objectData);
            BrickGrid brickGrid = fxn.apply(inputImage);
            BufferedImage outputImage = new BufferedImageBrickGridTransform().apply(brickGrid);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(outputImage, imageFormat, os);
            byte[] outputBytes = os.toByteArray();
            InputStream is = new ByteArrayInputStream(outputBytes);

            String outputKey = "after/output.png";
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(outputBytes.length);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, outputKey, is, metadata);
            s3Client.putObject(putObjectRequest);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
