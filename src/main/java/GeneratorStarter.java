public class GeneratorStarter {

    public static void main(String[] args) {
        // Start Generator
        String sourceFileName = "source.csv";
        String outputFileName = "output.pdf";
        LiteraturePdfGenerator generator = new LiteraturePdfGenerator();
        generator.generate(sourceFileName, outputFileName);
    }
}
