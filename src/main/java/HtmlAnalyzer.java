
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HtmlAnalyzer {

    //private static Logger LOGGER = LoggerFactory.getLogger(HtmlAnalyzer.class);
    private static final int MAX_SEARCHED_ELEMENTS = 25;

    private static List<WeightedTokenEntry> searchTokens = new ArrayList<>(10);

    static {
        searchTokens.add(new WeightedTokenEntry("id", 0.9));
        searchTokens.add(new WeightedTokenEntry("class", 0.3));
        searchTokens.add(new WeightedTokenEntry("href", 0.7));
        searchTokens.add(new WeightedTokenEntry("title", 0.5));
        searchTokens.add(new WeightedTokenEntry("onclick", 0.8));
    }

    private Document source;
    private Document diff;
    private int searchedElementsCounter;

    private XMLSearchResult matchedSearchResult;

    public HtmlAnalyzer(File sourceFile, File diffFile) {
        try {
            source = Jsoup.parse(sourceFile, "UTF-8");
            diff = Jsoup.parse(diffFile, "UTF-8");
            searchedElementsCounter = 0;
            matchedSearchResult = new XMLSearchResult();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void searchById(String exactSourceId) {
        Element searchElement;
        if ((searchElement = source.getElementById(exactSourceId)) != null) {
            buildSearchInstance(searchElement);
            searchElement(searchElement.parent());
        } else {
            //log
        }
    }

    private void buildSearchInstance(final Element sourceElement) {
        searchTokens.forEach(e -> e.setValue(sourceElement.attr(e.getToken())));
    }

    public void searchElement(Element parentElement) {
        Elements siblingElements = diff.getElementsByClass(parentElement.className());
        if (!siblingElements.isEmpty()) {
            siblingElements.forEach(element -> findUsingParent(element));
        } else {
            diff.children().forEach(element -> findInFullDocument(element));
        }
    }

    private void findUsingParent(Element parentElement) {
        if (parentElement != null && searchedElementsCounter < MAX_SEARCHED_ELEMENTS) {
            parentElement.children().forEach(child -> {
                searchedElementsCounter++;
                setNewSearchResult(getSearchResult(child));
            });
            findUsingParent(parentElement.parent());
        } else {
            return;
        }
    }

    private void findInFullDocument(Element element) {
        if (searchedElementsCounter < MAX_SEARCHED_ELEMENTS) {
            setNewSearchResult(getSearchResult(element));
            element.children().forEach(child -> findInFullDocument(child));
        } else {
            return;
        }
    }

    private void setNewSearchResult(XMLSearchResult newResult) {
        if (newResult.score > matchedSearchResult.getScore()) {
            matchedSearchResult = newResult;

        }
    }

    private XMLSearchResult getSearchResult(Element element) {

        Map<String, Double> calculations = searchTokens.stream()
                .collect(Collectors.toMap(WeightedTokenEntry::getToken, e -> {
                    double res = 0;
                    for (String s : element.attr(e.getToken()).split(" ")) {
                        if (e.getValue().contains(s))
                            res += e.getWeight();
                    }
                    return res;
                }));
        return new XMLSearchResult(calculations, element);
    }

    public XMLSearchResult getMatchedSearchResult() {
        return matchedSearchResult;
    }

    public String getResultPath() {
        LinkedList<String> tagsQueue = new LinkedList<>();
        addNode(tagsQueue, matchedSearchResult.sourceElement);
        StringBuilder sb = new StringBuilder();
        while (!tagsQueue.isEmpty()) {
            sb.append(tagsQueue.removeLast() + " > ");
        }
        String result = sb.toString();
        return result.substring(0, result.length() - 2);
    }

    private void addNode(LinkedList<String> tagsQueue, Element element) {
        if (element.parent() == null)
            return;
        tagsQueue.add(element.tagName());
        addNode(tagsQueue, element.parent());
    }

    public class XMLSearchResult {
        private double score;
        private Map<String, Double> calculations;
        private Element sourceElement;

        public XMLSearchResult() {
            this.score = 0;
            this.calculations = new HashMap<>();
        }

        public XMLSearchResult(Map<String, Double> calculations, Element sourceElement) {
            this.sourceElement = sourceElement;
            this.calculations = calculations;
            this.score = calculateScore();
        }

        private double calculateScore() {
            return calculations.entrySet().stream().mapToDouble(e -> e.getValue()).sum();
        }

        public double getScore() {
            return score;
        }
    }

    public static class WeightedTokenEntry {
        private String token;
        private double weight;
        private String value;

        public WeightedTokenEntry(String token, double weight) {
            this.token = token;
            this.weight = weight;
        }

        public String getToken() {
            return token;
        }

        public double getWeight() {
            return weight;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
