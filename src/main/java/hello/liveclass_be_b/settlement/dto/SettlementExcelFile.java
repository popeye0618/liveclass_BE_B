package hello.liveclass_be_b.settlement.dto;

public record SettlementExcelFile(
        String fileName,
        byte[] content
) {
}
