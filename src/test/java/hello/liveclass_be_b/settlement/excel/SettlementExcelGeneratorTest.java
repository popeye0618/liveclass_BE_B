package hello.liveclass_be_b.settlement.excel;

import hello.liveclass_be_b.creator.entity.Creator;
import hello.liveclass_be_b.settlement.entity.Settlement;
import hello.liveclass_be_b.settlement.enums.SettlementStatus;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementExcelGenerator 테스트")
class SettlementExcelGeneratorTest {

    private final SettlementExcelGenerator settlementExcelGenerator = new SettlementExcelGenerator();

    @Test
    @DisplayName("정산 내역으로 엑셀 파일을 생성한다")
    void generate() throws Exception {
        // given
        Settlement settlement = createSettlement(SettlementStatus.PAID);
        ReflectionTestUtils.setField(settlement, "id", 1L);

        // when
        byte[] content = settlementExcelGenerator.generate(List.of(settlement));

        // then
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet("settlements");
            Row headerRow = sheet.getRow(0);
            Row dataRow = sheet.getRow(1);

            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("정산 ID");
            assertThat(headerRow.getCell(12).getStringCellValue()).isEqualTo("상태");
            assertThat(dataRow.getCell(0).getNumericCellValue()).isEqualTo(1);
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("creator-1");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("김강사");
            assertThat(dataRow.getCell(3).getStringCellValue()).isEqualTo("2025-03");
            assertThat(dataRow.getCell(4).getNumericCellValue()).isEqualTo(260000);
            assertThat(dataRow.getCell(8).getNumericCellValue()).isEqualTo(30000);
            assertThat(dataRow.getCell(12).getStringCellValue()).isEqualTo("PAID");
            assertThat(dataRow.getCell(13).getStringCellValue()).isEqualTo("2025-04-01T00:00:00+09:00");
            assertThat(dataRow.getCell(14).getStringCellValue()).isEqualTo("2025-04-02T00:00:00+09:00");
            assertThat(dataRow.getCell(15).getStringCellValue()).isEqualTo("2025-04-03T00:00:00+09:00");
        }
    }

    @Test
    @DisplayName("정산 내역이 없어도 헤더만 있는 엑셀 파일을 생성한다")
    void generateWhenEmpty() throws Exception {
        // when
        byte[] content = settlementExcelGenerator.generate(List.of());

        // then
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet("settlements");

            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("정산 ID");
            assertThat(sheet.getRow(1)).isNull();
        }
    }

    private Settlement createSettlement(SettlementStatus status) {
        return Settlement.builder()
                .creator(createCreator())
                .settlementMonth("2025-03")
                .totalSalesAmount(260000L)
                .totalRefundAmount(110000L)
                .netSalesAmount(150000L)
                .feeRate(20)
                .platformFeeAmount(30000L)
                .settlementAmount(120000L)
                .salesCount(4)
                .cancelCount(2)
                .status(status)
                .calculatedAt(OffsetDateTime.parse("2025-04-01T00:00:00+09:00"))
                .confirmedAt(OffsetDateTime.parse("2025-04-02T00:00:00+09:00"))
                .paidAt(OffsetDateTime.parse("2025-04-03T00:00:00+09:00"))
                .build();
    }

    private Creator createCreator() {
        return Creator.builder()
                .id("creator-1")
                .name("김강사")
                .build();
    }
}
