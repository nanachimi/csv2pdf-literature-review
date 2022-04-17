import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Article {

    @EqualsAndHashCode.Include
    @CsvBindByName(column = "Authors", required = true)
    private String authors;

    @EqualsAndHashCode.Include
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

    @EqualsAndHashCode.Include
    @CsvBindByName(column = "Publisher")
    private String publisher;

    @CsvBindByName(column = "Abstract")
    private String summary;

    @CsvBindByName(column = "Keywords")
    private String keywords;

    @CsvBindByName(column = "Links")
    private String links;

    @CsvBindByName(column = "Comments")
    private String comments;

    @CsvBindByName(column = "ToExclude")
    private String toExclude;

}
