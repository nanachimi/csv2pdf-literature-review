import com.opencsv.bean.CsvToBeanBuilder;
import lombok.SneakyThrows;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.pdfbox.pdmodel.PDPageContentStream.*;

public class LiteraturePdfGenerator {

    private static final float RATIO_JUSTIFIABLE_LINE = 0.3f;
    private final Map<String, Article> articlesToExclude = new HashMap<>();
    private final Map<String, Article> articlesWithoutLinks = new HashMap<>();
    private final Map<String, Article> articlesWithoutSummary = new HashMap<>();
    private int maxCharacterPerLineInSummary = 0;

    @SneakyThrows
    // @formatter:off
    public void generate(String sourceFileName, String outputFileName) {

        URL url = ClassLoader.getSystemResource(sourceFileName);
        FileReader reader = new FileReader(url.getFile());
        CsvToBeanBuilder<Article> builder = new CsvToBeanBuilder<>(reader);

        List<Article> literature = builder
                .withSeparator(';')
                .withType(Article.class)
                .build()
                .parse();

        PDDocument pdDocument = new PDDocument();

        float summaryFontSize = 11;
        float leading = 1.5f * summaryFontSize;

        for (Article article : literature) {

            boolean isToExclude = isNotBlank(article.getToExclude()) && article.getToExclude().equals("yes");

            if (isNotBlank(article.getSummary()) && !isToExclude) {
                PDPage pdPage = new PDPage();
                pdDocument.addPage(pdPage);
                PDRectangle mediabox = pdPage.getMediaBox();
                float margin = 90;
                float width = mediabox.getWidth() - 2 * margin;
                float startX = mediabox.getLowerLeftX() + margin;
                float startY = mediabox.getUpperRightY() - margin - 20;

                PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.PREPEND, true);
                contentStream.beginText();
                contentStream.setLeading(leading);
                contentStream.newLineAtOffset(startX, startY);

                PDFont titleFontName = PDType1Font.HELVETICA_BOLD;
                float titleFontSize = 14;
                List<String> titleLines = parseText(article.getTitle(), width, titleFontSize, titleFontName);
                contentStream.setFont(titleFontName, titleFontSize);
                for (String line : titleLines) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -leading);
                }

                contentStream.setLineWidth(0.6f);

                PDFont detailsFontName = PDType1Font.HELVETICA_OBLIQUE;
                float detailsFontSize = 11;
                List<String> authorsLines =
                        parseText(article.getAuthors(), width, detailsFontSize, detailsFontName);

                contentStream.setFont(detailsFontName, detailsFontSize);

                if (isNotBlank(authorsLines.get(0))) {
                    authorsLines.set(0, "Author(s): " + authorsLines.get(0));
                }

                for (String line : authorsLines) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -leading);
                }

                // Parse and write publisher infos if available
                if (isNotBlank(article.getPublisher()) && isNotBlank(article.getYear())) {
                    String publicationInfos =
                            "Publisher: " + article.getPublisher() + " in " + article.getYear();
                    List<String> pubs = parseText(publicationInfos, width, detailsFontSize, detailsFontName);
                    for (String line : pubs) {
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -leading);
                    }
                }

                // Parse and write keywords if available
                if (isNotBlank(article.getKeywords())) {
                    String keywords =
                            "Keywords: " + article.getKeywords()
                                    .replace(",", ";")
                                    .replace("\t", "")
                                    .replace("\n", "");

                    List<String> words = parseText(keywords, width, detailsFontSize, detailsFontName);

                    for (String line : words) {
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -leading);
                    }
                }

                contentStream.newLine();

                PDFont summaryFontName = PDType1Font.HELVETICA;
                List<String> summaryLines =
                        parseText(article.getSummary(), width, summaryFontSize, summaryFontName);
                contentStream.setFont(summaryFontName, summaryFontSize);
                int i = 0;
                for (; i < summaryLines.size(); i++) {

                    String currentLine = summaryLines.get(i);

                    //Update the current max character per line
                    if (maxCharacterPerLineInSummary < currentLine.length()) {
                        maxCharacterPerLineInSummary = currentLine.length();
                        //System.out.println("current max character per line = " + maxCharacterPerLineInSummary);
                    }
                    float charSpacing = 0;
                    if (currentLine.length() > 1) {
                        float size = summaryFontSize * summaryFontName.getStringWidth(currentLine) / 1000;
                        float free = width - size;
                        if (free > 0) {
                            charSpacing = free / (currentLine.length() - 1);
                        }
                    }
                    //Do not justify the last lines and the line smaller than 30% of the longest line
                    if (i < summaryLines.size() - 1
                            && currentLine.length() > maxCharacterPerLineInSummary * (1 - RATIO_JUSTIFIABLE_LINE)) {
                        contentStream.setCharacterSpacing(charSpacing);
                    }
/*                    else {
                        System.out.println("length of non-justified line = " + line.length());
                    }*/
                    contentStream.showText(currentLine);
                    contentStream.newLineAtOffset(0, -leading);
                }

                //Adding comments if available
                if (isNotBlank(article.getComments())) {
                    List<String> articles = parseText(article.getComments(), width, summaryFontSize, summaryFontName);
                    for (String line : articles) {
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -leading);
                    }
                }

                contentStream.endText();
                contentStream.close();

            } else {
                articlesWithoutSummary.put(article.getTitle(), article);
            }

            if (isBlank(article.getLinks())) {
                articlesWithoutLinks.put(article.getTitle(), article);
            }

            if (isToExclude) {
                articlesToExclude.put(article.getTitle(), article);
            }
        }

        //Article without summary which are not to exclude
        List<Article> articlesWithoutSummaryList = new ArrayList<>(articlesWithoutSummary.values());
        List<Article> articlesToExcludeList = new ArrayList<>(articlesToExclude.values());
        List<Article> articlesWithoutLinksList = new ArrayList<>(articlesWithoutLinks.values());
        List<Article> articlesWithoutSummaryButWithLinksWhichAreNotToExcluded = articlesWithoutSummaryList
                .stream()
                .filter(element -> !articlesToExcludeList.contains(element))
                .filter(element -> !articlesWithoutLinksList.contains(element))
                .collect(Collectors.toList());

        System.out.printf("### %s Articles to read but without abstract%s", articlesWithoutSummaryButWithLinksWhichAreNotToExcluded.size(), " ###\n");
        articlesWithoutSummaryButWithLinksWhichAreNotToExcluded.forEach(this::printArticleOnCli);

        System.out.printf("### %s Articles to read without full access%s", articlesWithoutLinksList.size(), " ###\n");
        articlesWithoutLinksList.forEach(this::printArticleOnCli);

        System.out.printf("### %s Articles to exclude from the list%s", articlesToExcludeList.size(), " ###\n");
        articlesToExcludeList.forEach(this::printArticleOnCli);

        pdDocument.save(outputFileName);
        pdDocument.close();
    }

    @SneakyThrows
    private List<String> parseText(String text, float width, float fontSize, PDFont pdfFont) {
        List<String> lines = new ArrayList<>();
        for (String current : text.split("\n")) {
            int lastSpace = -1;
            while (current.length() > 0) {
                int spaceIndex = current.indexOf(' ', lastSpace + 1);
                if (spaceIndex < 0) spaceIndex = current.length();
                String subString = current.substring(0, spaceIndex);
                float size = fontSize * pdfFont.getStringWidth(subString) / 1000;
                //System.out.printf("'%s' - %f of %f\n", subString, size, width);
                if (size > width) {
                    if (lastSpace < 0) lastSpace = spaceIndex;
                    subString = current.substring(0, lastSpace);
                    lines.add(subString);
                    current = current.substring(lastSpace).trim();
                    //System.out.printf("'%s' is line\n", subString);
                    lastSpace = -1;
                } else if (spaceIndex == current.length()) {
                    lines.add(current);
                    //System.out.printf("'%s' is line\n", current);
                    current = "";
                } else {
                    lastSpace = spaceIndex;
                }
            }
        }
        return lines;
    }

    private void printArticleOnCli(Article article) {
        System.out.println("Title: " + article.getTitle());
        System.out.println("Author(s): " + article.getAuthors());
        if (isNotBlank(article.getPublisher()) && isNotBlank(article.getYear()) && isNotBlank(article.getPublication())) {
            System.out.println("Publisher: " + article.getPublisher() + "; " + article.getPublication() + "; " + article.getYear() );
        } else if (isNotBlank(article.getPublisher()) && isNotBlank(article.getYear())) {
            System.out.println("Publisher: " + article.getPublisher() + "; " + article.getYear());
        } else if (isNotBlank(article.getYear()) && isNotBlank(article.getPublication())) {
            System.out.println("Publisher: "   + article.getPublication() + "; " +  article.getYear());
        } else if (isNotBlank(article.getPublisher()) && isNotBlank(article.getPublication())) {
            System.out.println("Publisher: " + article.getPublisher() + "; " + article.getPublication());
        }
        if (isNotBlank(article.getComments())) {
            System.out.println("Comments: " + article.getComments());
        }
        System.out.println();
    }

}
