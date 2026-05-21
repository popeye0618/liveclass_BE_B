package hello.liveclass_be_b.settlement.excel;

import hello.liveclass_be_b.settlement.entity.Settlement;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class SettlementExcelGenerator {

    private static final ZoneOffset KST = ZoneOffset.of("+09:00");
    private static final String SHEET_NAME = "settlements";
    private static final String[] HEADERS = {
            "정산 ID",
            "크리에이터 ID",
            "크리에이터명",
            "정산 연월",
            "총 판매 금액",
            "총 환불 금액",
            "순 판매 금액",
            "수수료율",
            "플랫폼 수수료",
            "정산 예정 금액",
            "판매 건수",
            "취소 건수",
            "상태",
            "정산 계산 일시",
            "정산 확정 일시",
            "지급 완료 일시"
    };

    public byte[] generate(List<Settlement> settlements) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);
            CellStyle headerStyle = createHeaderStyle(workbook);

            createHeaderRow(sheet, headerStyle);
            createDataRows(sheet, settlements);
            resizeColumns(sheet);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("정산 엑셀 파일 생성에 실패했습니다.", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createDataRows(Sheet sheet, List<Settlement> settlements) {
        for (int i = 0; i < settlements.size(); i++) {
            Settlement settlement = settlements.get(i);
            Row row = sheet.createRow(i + 1);

            setCellValue(row, 0, settlement.getId());
            setCellValue(row, 1, settlement.getCreator().getId());
            setCellValue(row, 2, settlement.getCreator().getName());
            setCellValue(row, 3, settlement.getSettlementMonth());
            setCellValue(row, 4, settlement.getTotalSalesAmount());
            setCellValue(row, 5, settlement.getTotalRefundAmount());
            setCellValue(row, 6, settlement.getNetSalesAmount());
            setCellValue(row, 7, settlement.getFeeRate());
            setCellValue(row, 8, settlement.getPlatformFeeAmount());
            setCellValue(row, 9, settlement.getSettlementAmount());
            setCellValue(row, 10, settlement.getSalesCount());
            setCellValue(row, 11, settlement.getCancelCount());
            setCellValue(row, 12, settlement.getStatus().name());
            setCellValue(row, 13, formatDateTime(settlement.getCalculatedAt()));
            setCellValue(row, 14, formatDateTime(settlement.getConfirmedAt()));
            setCellValue(row, 15, formatDateTime(settlement.getPaidAt()));
        }
    }

    private void resizeColumns(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void setCellValue(Row row, int index, Long value) {
        if (value == null) {
            return;
        }

        row.createCell(index).setCellValue(value);
    }

    private void setCellValue(Row row, int index, Integer value) {
        if (value == null) {
            return;
        }

        row.createCell(index).setCellValue(value);
    }

    private void setCellValue(Row row, int index, String value) {
        if (value == null) {
            return;
        }

        row.createCell(index).setCellValue(value);
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.withOffsetSameInstant(KST)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
