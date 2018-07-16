import java.io.File;

public class Main {

    public static void main(String[] args) {
        File sourceFile, diffFile;

        if (args.length < 2) {
            System.err.println("Invalid parameters number. Should be 2.");
            return;
        } else {
            sourceFile = new File(args[0]);
            diffFile = new File(args[1]);
        }

        try {
            HtmlAnalyzer analyzer = new HtmlAnalyzer(sourceFile, diffFile);
            analyzer.searchById("make-everything-ok-button");
            System.out.println("result path " + analyzer.getResultPath());
        } catch (Exception e) {
            System.out.println("Error occured " + e.getMessage());
        }
    }
}
