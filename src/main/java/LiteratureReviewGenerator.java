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
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class LiteratureReviewGenerator {

  private final List<String> articlesWithoutSummary = new ArrayList<>();
  private final List<String> articlesWithoutLinks = new ArrayList<>();
  private int maxCharacterPerLineInSummary = 0;

  @SneakyThrows
  // @formatter:off
  public void generate(String sourceFileName, String outputFileName) {

    URL url = ClassLoader.getSystemResource(sourceFileName);
    FileReader reader = new FileReader(url.getFile());
    CsvToBeanBuilder<LiteratureReviewMetadata> builder = new CsvToBeanBuilder<>(reader);

    var literature =
        builder.withSeparator(';').withType(LiteratureReviewMetadata.class).build().parse();

    PDDocument pdDocument = new PDDocument();

    float summaryFontSize = 12;
    float leading = 1.5f * summaryFontSize;

    for (LiteratureReviewMetadata article : literature) {
      PDPage pdPage = new PDPage();
      PDRectangle mediabox = pdPage.getMediaBox();
      float margin = 90;
      float width = mediabox.getWidth() - 2 * margin;
      float startX = mediabox.getLowerLeftX() + margin;
      float startY = mediabox.getUpperRightY() - margin - 20;

      pdDocument.addPage(pdPage);
      PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage);
      contentStream.beginText();
      contentStream.setLeading(leading);
      contentStream.newLineAtOffset(startX, startY);

      PDFont titleFontName = PDType1Font.HELVETICA_BOLD;
      float titleFontSize = 13;
      List<String> titleLines = parseText(article.getTitle(), width, titleFontSize, titleFontName);
      contentStream.setFont(titleFontName, titleFontSize);
      for (String line : titleLines) {
        contentStream.showText(line);
        contentStream.newLineAtOffset(0, -leading);
      }

      contentStream.setLineWidth(0.6f);

      PDFont authorsFontName = PDType1Font.HELVETICA_OBLIQUE;
      float authorsFontSize = 11;
      List<String> authorsLines =
          parseText(article.getAuthors(), width, authorsFontSize, authorsFontName);

      contentStream.setFont(authorsFontName, authorsFontSize);

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
        List<String> pubs = parseText(publicationInfos, width, authorsFontSize, authorsFontName);
        for (String line : pubs) {
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
        String line = summaryLines.get(i);
        if (maxCharacterPerLineInSummary < line.length()) {
          maxCharacterPerLineInSummary = line.length();
          System.out.println("max character per line = " + maxCharacterPerLineInSummary);
        }
        float charSpacing = 0;
        if (line.length() > 1) {
          float size = summaryFontSize * summaryFontName.getStringWidth(line) / 1000;
          float free = width - size;
          if (free > 0) {
            charSpacing = free / (line.length() - 1);
          }
        }
        //Do not justify the last lines and the line smaller than 30% of the longest line
        if (i < summaryLines.size() - 1
            && line.length() > maxCharacterPerLineInSummary * (1 - 0.3)) {
          contentStream.setCharacterSpacing(charSpacing);
        } else {
          System.out.println("number of characters of current line = " + line.length());
        }
        contentStream.showText(line);
        contentStream.newLineAtOffset(0, -leading);
      }

      contentStream.endText();
      contentStream.close();
    }

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
}
