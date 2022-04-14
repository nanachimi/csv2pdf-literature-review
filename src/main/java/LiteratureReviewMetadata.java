import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiteratureReviewMetadata {
    @CsvBindByName(column = "Authors", required = true)
    private String authors;

    @CsvBindByName(column = "Title", required = true)
    private String title;

    @CsvBindByName(column = "Publication")
    private String publication;

    @CsvBindByName(column = "Volume")
    private String volume;

    @CsvBindByName(column = "Number")
    private String number;

    @CsvBindByName(column = "Pages")
    private String pages;

    @CsvBindByName(column = "Year")
    private String year;

    @CsvBindByName(column = "Publisher")
    private String publisher;

    @CsvBindByName(column = "Abstract")
    private String summary;

    @CsvBindByName(column = "Keywords")
    private String keywords;

    @CsvBindByName(column = "Links")
    private String links;

    @CsvBindByName(column = "DOI")
    private String doi;
}
