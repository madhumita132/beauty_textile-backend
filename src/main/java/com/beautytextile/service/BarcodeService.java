package com.beautytextile.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

/** Generates Code 128 barcode images and sequential barcode values (BT1001...). */
@Service
public class BarcodeService {

    private static final String PREFIX = "BT";
    private static final int START = 1000;

    /** Build the next barcode value from the last product's barcode. */
    public String nextBarcode(String lastBarcode) {
        if (lastBarcode == null || !lastBarcode.startsWith(PREFIX)) {
            return PREFIX + (START + 1);
        }
        try {
            int n = Integer.parseInt(lastBarcode.substring(PREFIX.length()));
            return PREFIX + (n + 1);
        } catch (NumberFormatException e) {
            return PREFIX + (START + 1);
        }
    }

    /** Generate a PNG Code 128 barcode for the given value. */
    public byte[] generatePng(String value, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 4);
            BitMatrix matrix = new Code128Writer().encode(value, BarcodeFormat.CODE_128, width, height, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate barcode for " + value, e);
        }
    }
}
