package com.g42.platform.gms.common.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ExcelService {
    public static <T> byte[] exportToExcel(List<T> data, String[] headers,Function<T,Object[]> rowMapper){
        try {
        Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Sheet sheet = workbook.createSheet("Data");

        //header
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        //write data
        int rowIdx = 1;
        for (T item : data) {
            Row row = sheet.createRow(rowIdx++);
            Object[] cellValue = rowMapper.apply(item);

            for (int j = 0; j < cellValue.length; j++) {
                Cell cell = row.createCell(j);
                Object value = cellValue[j];

                //auto recognize data to correct writing
                if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                }else if (value instanceof Boolean) {
                    cell.setCellValue((Boolean) value);
                }else if (value != null) {
                    cell.setCellValue(value.toString());
                }
            }
        }
        workbook.write(outputStream);
        return outputStream.toByteArray();



        }catch (Exception e){
        throw new RuntimeException(e.getMessage());
        }
    }
    public static <T> List<T> importFromExcel(MultipartFile file, Function<Row, T> rowMapper){
        List<T> resultList = new ArrayList<>();
        try (InputStream is = file.getInputStream();

             Workbook workbook = new XSSFWorkbook(is)){
            Sheet sheet = workbook.getSheetAt(0);
            boolean isFirstRow = true;

            for (Row row : sheet) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
                if (row.getCell(0) == null||row.getCell(0).getCellType() == CellType.BLANK) {
                    continue;
                }
                T item = rowMapper.apply(row);
                if (item != null) {
                    resultList.add(item);
                }
            }
return  resultList;
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
}
