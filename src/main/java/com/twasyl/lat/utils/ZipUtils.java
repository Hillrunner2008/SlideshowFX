package com.twasyl.lat.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final Logger LOGGER = Logger.getLogger(ZipUtils.class.getName());

    public static void unzip(File archive, File destination) throws IOException {
        if(archive == null) throw new IllegalArgumentException("The ZIP file can not be null");
        if(!archive.exists()) throw new IllegalArgumentException("The ZIP file does not exist");
        if(destination == null) throw new IllegalArgumentException("The destination can not be null");

        if(!destination.exists()) destination.mkdirs();

        ZipInputStream zipReader = new ZipInputStream(new FileInputStream(archive));
        ZipEntry zipEntry;
        File extractedFile;
        FileOutputStream extractedFileOutputStream;

        // Unzip
        LOGGER.fine("Extracting file " + archive.toURI().toASCIIString());

        while((zipEntry = zipReader.getNextEntry()) != null) {
            extractedFile = new File(destination, zipEntry.getName());

            LOGGER.fine("Extracting file: " + extractedFile.getAbsolutePath());

            if(zipEntry.isDirectory()) extractedFile.mkdirs();
            else {
                // Ensure to create the parents directories
                extractedFile.getParentFile().mkdirs();

                int length;
                byte[] buffer = new byte[1024];

                extractedFileOutputStream = new FileOutputStream(extractedFile);

                while((length = zipReader.read(buffer)) > 0) {
                    extractedFileOutputStream.write(buffer, 0, length);
                }

                extractedFileOutputStream.flush();
                extractedFileOutputStream.close();
            }
        }

        LOGGER.fine("Extraction done");

        zipReader.closeEntry();
        zipReader.close();
    }

    public static void zip(File fileToZip, File destination) throws IOException {
        if(fileToZip == null) throw new IllegalArgumentException("The file to zip can not be null");
        if(!fileToZip.exists()) throw new IllegalArgumentException("The file to zip does not exist");
        if(destination == null) throw new IllegalArgumentException("The destination can not be null");

        List<File> filesToZip = new ArrayList<>();
        makeFileList(filesToZip, fileToZip);

        FileInputStream fileInput = null;
        ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(destination));
        ZipEntry entry;
        String entryName;
        byte[] buffer  = new byte[1024];
        int length;

        String prefixToDelete;
        if(fileToZip.isDirectory()) {
            prefixToDelete = fileToZip.getAbsolutePath() + File.separator;
        } else {
            prefixToDelete = "";
        }

        for(File file : filesToZip) {
            LOGGER.fine("Compressing file: " + file.getAbsolutePath());

            entryName = file.getAbsolutePath().substring(prefixToDelete.length(), file.getAbsolutePath().length());
            LOGGER.finest("Entry name: " + entryName);

            entry = new ZipEntry(entryName);
            zipOutput.putNextEntry(entry);

            try {
                fileInput = new FileInputStream(file);

                while((length = fileInput.read(buffer)) > 0) {
                    zipOutput.write(buffer, 0, length);
                }
            } finally {
                fileInput.close();
            }
        }

        zipOutput.closeEntry();
        zipOutput.flush();
        zipOutput.close();

        LOGGER.fine("File compressed");
    }

    private static void makeFileList(List<File> list, File file) {

        if(file.isFile()) {
            list.add(file);
        } else {
            for(File subFile : file.listFiles()) {
                makeFileList(list, subFile);
            }
        }
    }
}
