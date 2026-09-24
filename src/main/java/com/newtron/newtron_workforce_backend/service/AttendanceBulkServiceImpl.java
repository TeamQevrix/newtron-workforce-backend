package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.dto.AttendanceBulkErrorDto;
import com.newtron.newtron_workforce_backend.dto.AttendanceBulkResponseDto;
import com.newtron.newtron_workforce_backend.entity.Attendance;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.entity.WorkOrder;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.AttendanceStatus;
import com.newtron.newtron_workforce_backend.enums.WorkOrderStatus;
import com.newtron.newtron_workforce_backend.repository.AttendanceRepository;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import com.newtron.newtron_workforce_backend.repository.WorkOrderRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceBulkServiceImpl implements AttendanceBulkService {

    private final AttendanceRepository attendanceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public AttendanceBulkResponseDto importAttendance(MultipartFile file, User currentUser) {
        if (file == null || file.isEmpty()) {
            return buildErrorResponse(0, "File is missing or empty.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElse(null);
        if (company == null) {
            return buildErrorResponse(0, "Authenticated user does not own a valid company.");
        }

        List<AttendanceBulkErrorDto> errors = new ArrayList<>();
        List<Attendance> validAttendances = new ArrayList<>();
        Set<String> excelDuplicatesTracker = new HashSet<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                return buildErrorResponse(0, "Excel file has no data rows.");
            }

            // Validate header
            Row headerRow = rowIterator.next();
            if (!isValidHeader(headerRow)) {
                return buildErrorResponse(1, "Invalid or missing headers. Expected: Date, Work Order Number, Worker ID, Status, Remark");
            }

            int rowNumber = 1; // Start from 1, header was 1, next is 2 but we'll use 2 for first data row. Let's use standard Excel row numbers (1-indexed).
            int excelRowIndex = 1;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                excelRowIndex++;
                
                // Skip empty rows
                if (isRowEmpty(row)) {
                    continue;
                }

                String dateStr = getCellValue(row.getCell(0));
                String workOrderNumber = getCellValue(row.getCell(1));
                String workerIdStr = getCellValue(row.getCell(2));
                String statusStr = getCellValue(row.getCell(3));
                String remarkStr = getCellValue(row.getCell(4));

                boolean hasError = false;

                // 1. Validate Date
                LocalDate date = null;
                if (dateStr == null || dateStr.isBlank()) {
                    errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Date is required."));
                    hasError = true;
                } else {
                    try {
                        date = LocalDate.parse(dateStr);
                        if (date.isAfter(LocalDate.now())) {
                            errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Date cannot be in the future."));
                            hasError = true;
                        }
                    } catch (DateTimeParseException e) {
                        errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Invalid date format. Expected YYYY-MM-DD."));
                        hasError = true;
                    }
                }

                // 2. Validate Work Order
                WorkOrder workOrder = null;
                if (workOrderNumber == null || workOrderNumber.isBlank()) {
                    errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Work Order Number is required."));
                    hasError = true;
                } else {
                    workOrder = workOrderRepository.findByWorkOrderNumber(workOrderNumber).orElse(null);
                    if (workOrder == null) {
                        errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Work Order not found."));
                        hasError = true;
                    } else if (!workOrder.getCompany().getId().equals(company.getId())) {
                        errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Work Order does not belong to your company."));
                        hasError = true;
                    } else if (workOrder.getStatus() != WorkOrderStatus.ACTIVE) {
                        errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Work Order is not ACTIVE. Current status: " + workOrder.getStatus()));
                        hasError = true;
                    }
                }

                // 3. Validate Worker ID
                WorkerProfile workerProfile = null;
                if (workerIdStr == null || workerIdStr.isBlank()) {
                    errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Worker ID is required."));
                    hasError = true;
                } else {
                    try {
                        // We expect the workerIdStr to be a double representation if formatted as numeric or just string
                        long parsedWorkerId = Double.valueOf(workerIdStr).longValue();
                        workerProfile = workerProfileRepository.findById(parsedWorkerId).orElse(null);
                        if (workerProfile == null) {
                            errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Worker Profile not found."));
                            hasError = true;
                        } else if (workOrder != null && !workerProfile.getUser().getId().equals(workOrder.getWorker().getId())) {
                            errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Worker ID does not match the assigned Worker for this Work Order."));
                            hasError = true;
                        }
                    } catch (NumberFormatException e) {
                        errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Invalid Worker ID format. Must be numeric."));
                        hasError = true;
                    }
                }

                // 4. Validate Status
                AttendanceStatus status = null;
                if (statusStr == null || statusStr.isBlank()) {
                    errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Status is required."));
                    hasError = true;
                } else {
                    try {
                        status = AttendanceStatus.valueOf(statusStr.trim());
                    } catch (IllegalArgumentException e) {
                        errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Invalid Status. Must be exactly PRESENT, ABSENT, or HALF_DAY."));
                        hasError = true;
                    }
                }

                // 5. Validate Remark
                String remark = remarkStr != null ? remarkStr.trim() : null;
                if (remark != null && remark.length() > 255) {
                    errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Remark exceeds 255 characters limit."));
                    hasError = true;
                }

                // 6. Check Excel internal duplicates
                if (date != null && workOrderNumber != null && !workOrderNumber.isBlank()) {
                    String duplicateKey = workOrderNumber + "_" + date.toString();
                    if (!excelDuplicatesTracker.add(duplicateKey)) {
                        errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Duplicate Work Order and Date found within the same Excel file."));
                        hasError = true;
                    }
                }

                // 7. Check Database duplicates
                if (date != null && workOrder != null) {
                    if (attendanceRepository.existsByWorkOrderIdAndAttendanceDate(workOrder.getId(), date)) {
                        errors.add(new AttendanceBulkErrorDto(excelRowIndex, "Attendance already exists for this Work Order on the given date."));
                        hasError = true;
                    }
                }

                if (!hasError) {
                    Attendance attendance = Attendance.builder()
                            .workOrder(workOrder)
                            .attendanceDate(date)
                            .status(status)
                            .remark(remark)
                            .build();
                    validAttendances.add(attendance);
                }
            }

        } catch (Exception e) {
            log.error("Error parsing Excel file", e);
            return buildErrorResponse(0, "Error reading Excel file. Please ensure it is a valid .xlsx format.");
        }

        if (!errors.isEmpty()) {
            return AttendanceBulkResponseDto.builder()
                    .errors(errors)
                    .build();
        }

        if (validAttendances.isEmpty()) {
            return buildErrorResponse(0, "File contains no valid attendance records.");
        }

        attendanceRepository.saveAll(validAttendances);

        return AttendanceBulkResponseDto.builder()
                .importedCount(validAttendances.size())
                .build();
    }

    private AttendanceBulkResponseDto buildErrorResponse(int row, String reason) {
        return AttendanceBulkResponseDto.builder()
                .errors(Collections.singletonList(new AttendanceBulkErrorDto(row, reason)))
                .build();
    }

    private boolean isValidHeader(Row row) {
        if (row == null || row.getLastCellNum() < 4) return false;
        
        String col0 = getCellValue(row.getCell(0));
        String col1 = getCellValue(row.getCell(1));
        String col2 = getCellValue(row.getCell(2));
        String col3 = getCellValue(row.getCell(3));
        // Remark is optional, so col4 might be missing, but headers usually exist
        // Checking first 4 strictly
        
        if (col0 == null || !col0.toLowerCase().contains("date")) return false;
        if (col1 == null || !col1.toLowerCase().contains("work order")) return false;
        if (col2 == null || !col2.toLowerCase().contains("worker id")) return false;
        if (col3 == null || !col3.toLowerCase().contains("status")) return false;
        
        return true;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Convert date back to string in YYYY-MM-DD
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellValue(cell);
                if (val != null && !val.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
