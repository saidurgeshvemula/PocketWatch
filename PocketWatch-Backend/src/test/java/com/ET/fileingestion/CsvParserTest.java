package com.ET.fileingestion;

import com.ET.fileingestion.dto.ParsedTransaction;
import com.ET.fileingestion.parser.CsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CsvParserTest {

    private CsvParser csvParser;

    @BeforeEach
    void setUp() {
        csvParser = new CsvParser();
    }

    @Test
    void parse_hdfcFormat_shouldExtractTransactions() throws IOException {
        String csv = """
                Date,Narration,Chq./Ref.No.,Withdrawal Amt.,Deposit Amt.,Closing Balance
                10/01/2025,SWIGGY ORDER #1234,REF001,800.00,,45200.00
                11/01/2025,SALARY CREDIT,REF002,,50000.00,95200.00
                12/01/2025,UBER TRIP,REF003,250.00,,94950.00
                """;

        List<ParsedTransaction> result = csvParser.parse(toStream(csv));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getDescription()).isEqualTo("SWIGGY ORDER #1234");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(result.get(0).getRawType()).isEqualTo("DR");
        assertThat(result.get(1).getRawType()).isEqualTo("CR");
        assertThat(result.get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    void parse_sbiFormat_shouldExtractTransactions() throws IOException {
        String csv = """
                Txn Date,Description,Ref No./Cheque No.,Debit,Credit,Balance
                15/01/2025,ATM WITHDRAWAL,CHQ001,2000.00,,30000.00
                16/01/2025,NEFT CREDIT,CHQ002,,15000.00,45000.00
                """;

        List<ParsedTransaction> result = csvParser.parse(toStream(csv));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDescription()).isEqualTo("ATM WITHDRAWAL");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(result.get(1).getRawType()).isEqualTo("CR");
    }

    @Test
    void parse_amountWithCommas_shouldParseCorrectly() throws IOException {
        String csv = """
                Date,Narration,Chq./Ref.No.,Withdrawal Amt.,Deposit Amt.,Closing Balance
                10/01/2025,AMAZON PURCHASE,REF001,"1,24,999.00",,500000.00
                """;

        List<ParsedTransaction> result = csvParser.parse(toStream(csv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("124999.00"));
    }

    @Test
    void parse_emptyFile_shouldReturnEmptyList() throws IOException {
        String csv = "Date,Narration,Chq./Ref.No.,Withdrawal Amt.,Deposit Amt.,Closing Balance\n";

        List<ParsedTransaction> result = csvParser.parse(toStream(csv));

        assertThat(result).isEmpty();
    }

    @Test
    void parse_rowWithMissingDate_shouldBeSkipped() throws IOException {
        String csv = """
                Date,Narration,Chq./Ref.No.,Withdrawal Amt.,Deposit Amt.,Closing Balance
                10/01/2025,VALID TRANSACTION,REF001,500.00,,10000.00
                ,BAD ROW NO DATE,REF002,300.00,,9700.00
                """;

        List<ParsedTransaction> result = csvParser.parse(toStream(csv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("VALID TRANSACTION");
    }

    @Test
    void parse_allDatesFormats_shouldBeParsed() throws IOException {
        String csv = """
                Date,Narration,Chq./Ref.No.,Withdrawal Amt.,Deposit Amt.,Closing Balance
                10/01/2025,FORMAT DD/MM/YYYY,REF001,100.00,,9900.00
                """;

        List<ParsedTransaction> result = csvParser.parse(toStream(csv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionDate()).isNotNull();
        assertThat(result.get(0).getTransactionDate().getYear()).isEqualTo(2025);
        assertThat(result.get(0).getTransactionDate().getMonthValue()).isEqualTo(1);
        assertThat(result.get(0).getTransactionDate().getDayOfMonth()).isEqualTo(10);
    }

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
