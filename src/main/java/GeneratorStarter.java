public class GeneratorStarter {

    public static void main(String[] args) {
        // Start Generator
        String sourceFileName = "source.csv";
        String outputFileName = "output.pdf";
        LiteratureReviewGenerator generator = new LiteratureReviewGenerator();
        generator.generate(sourceFileName, outputFileName);
    }
}
