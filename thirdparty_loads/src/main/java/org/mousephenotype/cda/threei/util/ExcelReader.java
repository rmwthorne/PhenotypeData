/*******************************************************************************
 * Copyright © 2015 EMBL - European Bioinformatics Institute
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 ******************************************************************************/

package org.mousephenotype.cda.threei.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.ArrayList;

@Component
public class ExcelReader {

    private FileInputStream excelFile;
    private Workbook workbook;
    private Sheet datatypeSheet;
    private Iterator<Row> rowIterator;
    private Iterator<Sheet> sheetIterator;
    private ArrayList<String> columnHeadings;
    private String[] lastRowRead;
    private int numberOfRowsRead;
    private int numberOfMiceProcessed;
    private int currentSheet;
    private int nSheets;

    public ExcelReader(String inFilename) {
        try {
            this.excelFile = new FileInputStream(new File(inFilename));
            this.workbook = new XSSFWorkbook(excelFile);
            this.sheetIterator = workbook.iterator();
            this.datatypeSheet = sheetIterator.next();
            this.rowIterator = datatypeSheet.iterator();

            setColumnHeadings();
            reset();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Set column headings
    private void setColumnHeadings() {
        this.columnHeadings = new ArrayList<String>();
        if (this.rowIterator.hasNext()) {
            Row currentRow = this.rowIterator.next();
            Iterator<Cell> cellIterator = currentRow.iterator();
            int nColumns = 0;
            while (cellIterator.hasNext()) {
                Cell currentCell = cellIterator.next();
                nColumns++;
                if (currentCell.getCellTypeEnum() == CellType.STRING) {
                    String value = currentCell.getStringCellValue();
                    this.columnHeadings.add(value);
                } else {
                    this.columnHeadings.add("Column" + nColumns + "isNotString");
                }
            }
        }
    }

    public ArrayList<String> getColumnHeadings() {
        return this.columnHeadings;
    }

    public int getNumberOfColumns() {
        return this.columnHeadings.size();
    }

    // Returns next row in spreadsheet. returns null if no rows left
    public String[] getRow() {
        // Need wrapper because row iterator can return rows that had values
        // in them but were deleted.
        // Set any cells in row that are null to NonStringNonNumericValue
        // However, if all cells are null then this is an empty row and we
        // want to discard it.
        int nColumns = this.getNumberOfColumns();
        String[] resultRow;
        while (true) {
            resultRow = localGetRow();
            if (resultRow == null) {
                return resultRow;
            }
            
            int nNullCells = 0;
            for (int col=0; col<nColumns; col++) {
            	if (resultRow[col] == null || resultRow[col].equals("NonStringNonNumericValue")) {
                    nNullCells += 1;
            		resultRow[col] = "NonStringNonNumericValue";
            	}
            }
            if (nNullCells < nColumns) {
                break;
            }
        }
        numberOfRowsRead++;
        this.lastRowRead = resultRow;
        return resultRow;
    }
    
    // Return an ArrayList of consecutive rows referring to the same mouse ID
    public ArrayList<String[]> getRowsForMouse(){
        ArrayList<String[]> resultsForMouse = new ArrayList<String[]>();

        int nRowsForMouse = 0;
        String lastMouseId = "";
        int mouseIdCol = this.columnHeadings.indexOf("MOUSE_ID");
        
        while (this.lastRowRead != null) {
            String[] resultRow = this.lastRowRead;
            if (0 == nRowsForMouse || lastMouseId.equals(resultRow[mouseIdCol])) {
                resultsForMouse.add(resultRow);
                nRowsForMouse++;
                lastMouseId = resultRow[mouseIdCol];
                getRow();
            } else {
                break;
            }
        }
        if (resultsForMouse.size() > 0) {
            numberOfMiceProcessed++;
        }
        return resultsForMouse;
    }

    // Wrapper around hasNext to check if either buffer for last row read or
    // the row iterator have values
    public boolean hasNext() {
        return (sheetIterator.hasNext() || rowIterator.hasNext());
    }

    // Return number of rows read so far
    public int getNumberOfRowsRead() {
        return this.numberOfRowsRead;
    }

    // Return the number of mice processed
    public int getNumberOfMiceProcessed() {
        return this.numberOfMiceProcessed;
    }

    // Reset reader
    public void reset() {
        sheetIterator = workbook.iterator();
        datatypeSheet = sheetIterator.next();
        rowIterator = datatypeSheet.iterator();
        // Call getRow twice - the first time to discard the column headings
        getRow();
        numberOfRowsRead = 0;
        numberOfMiceProcessed = 0;
        getRow();
    }

    private String[] localGetRow() {
        if (!rowIterator.hasNext()) {
            // Check if there is another sheet
            if (this.sheetIterator.hasNext()) {
                datatypeSheet = sheetIterator.next();
                rowIterator = datatypeSheet.iterator();
            } else {
                // Return null if there is no other sheet
                this.lastRowRead = null;
                return null;
            }
            if (!this.rowIterator.hasNext()) {
                this.lastRowRead = null;
                return null;
            }
        }
        
        Row currentRow = this.rowIterator.next();
        Iterator<Cell> cellIterator = currentRow.iterator();
        int nColumns = this.getNumberOfColumns();

        // Get row details
        String[] resultRow = new String[nColumns];
        for (int col=0; col < nColumns; col++) {
            if (cellIterator.hasNext()) {
                Cell currentCell = cellIterator.next();
                int colIndex = currentCell.getColumnIndex();
                //getCellTypeEnum shown as deprecated for version 3.15
                //getCellTypeEnum ill be renamed to getCellType starting from version 4.0
             if (currentCell.getCellTypeEnum() == CellType.STRING) {
                    resultRow[colIndex] = "" + currentCell.getStringCellValue();
                } else if (currentCell.getCellTypeEnum() == CellType.NUMERIC) {
                    if (DateUtil.isCellDateFormatted(currentCell)) {
                        resultRow[colIndex] = "" + currentCell.getDateCellValue();
                    } else {
                    resultRow[colIndex] = currentCell.getNumericCellValue() + "";
                    }
                } else {
                    resultRow[colIndex] = "NonStringNonNumericValue";
                }
            }
        }
        return resultRow;
    }
}
