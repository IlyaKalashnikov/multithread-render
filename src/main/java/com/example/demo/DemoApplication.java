package com.example.demo;

import com.drew.metadata.mov.atoms.Atom;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }



    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            System.setProperty("org.apache.pdfbox.rendering.UsePureJavaCMYKConversion", "true");
            PDDocument document = PDDocument.load(Paths.get("src/main/resources/Петр_гр_сн_н-ки_2020.pdf").toFile(),
                    MemoryUsageSetting.setupMainMemoryOnly().getPartitionedCopy(10));

            Splitter splitter = new Splitter();

            List<PDDocument> split = splitter.split(document);

            var map = new ConcurrentHashMap<Integer, byte[]>();
            for (int i = 0; i < split.size(); i++) {
                var doc = split.get(i);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    doc.save(baos);
                    doc.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                map.put(i, baos.toByteArray());
            }

            document.close();

            var exec = Executors.newCachedThreadPool();
            for(Map.Entry<Integer, byte[]> entry:map.entrySet()){
                exec.execute(() -> {
                    byte[] arr = entry.getValue();
                    System.out.println(Thread.currentThread() + "started rendering " + OffsetDateTime.now());
                    File preview = null;
                    File thumbnail = null;
                    PDDocument doc = null;
                    try {
                        doc = PDDocument.load(arr);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        preview = Files.createFile(Paths.get("src/output/w" + entry.getKey())).toFile();
                        thumbnail = Files.createFile(Paths.get("src/output/i" + entry.getKey())).toFile();

                        FileOutputStream previewFOS = new FileOutputStream(preview);
                        FileOutputStream thumbnailFOS = new FileOutputStream(thumbnail);

                        PDFRenderer renderer = new PDFRenderer(doc);

                        var renderedPreview = scalePdfPreview(renderer.renderImageWithDPI(0, 600),
                                1920);
                        ImageIOUtil.writeImage(renderedPreview, "png", previewFOS, 600);

                        var renderedThumbnail = scalePdfPreview(renderer.renderImageWithDPI(0, 600),
                                170);
                        ImageIOUtil.writeImage(renderedThumbnail, "png", thumbnailFOS, 600);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            doc.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println(Thread.currentThread() + "finished rendering at " + OffsetDateTime.now());
                    }
                });
            }
        };
    }

    public static BufferedImage scalePdfPreview(BufferedImage renderImage, int maxSize) {
        int height = renderImage.getHeight();
        int width = renderImage.getWidth();

        if (Math.max(height, width) > maxSize) {
            if (height > width) {
                float scaleCoefficient = (float) height / (float) maxSize;
                int newWidth = Math.round(width / scaleCoefficient);

                return toBufferedImage(renderImage.getScaledInstance(newWidth, maxSize, Image.SCALE_DEFAULT));
            } else if (width > height) {
                float scaleCoefficient = (float) width / (float) maxSize;
                int newHeight = Math.round(height / scaleCoefficient);

                return toBufferedImage(renderImage.getScaledInstance(maxSize, newHeight, Image.SCALE_DEFAULT));
            } else
                return toBufferedImage(renderImage.getScaledInstance(maxSize, maxSize, Image.SCALE_DEFAULT));
        }
        return renderImage;
    }

    private static BufferedImage toBufferedImage(Image scaledInstance) {
        if (scaledInstance.getClass().isInstance(BufferedImage.class)) {
            return (BufferedImage) scaledInstance;
        }
        BufferedImage bufferedImage = new BufferedImage(scaledInstance.getWidth(null),
                scaledInstance.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bufferedImage.createGraphics();
        bGr.drawImage(scaledInstance, 0, 0, null);
        bGr.dispose();

        return bufferedImage;
    }
}
