import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Chatbot {
    static IntentsData intentsData;
    static Map<String, List<String>> tagToPatterns = new HashMap<>();
    static Map<String, List<String>> tagToResponses = new HashMap<>();
    static Set<String> vocabulary = new HashSet<>();

    public static void main(String[] args) throws IOException {
        loadIntents("intents.json");
        Scanner scanner = new Scanner(System.in);

        System.out.println("🤖 Chatbot is ready! Type 'exit' to quit.");
        while (true) {
            System.out.print("You: ");
            String input = scanner.nextLine().toLowerCase();
            if (input.equals("exit")) break;
            String tag = predictIntent(input);
            String response = getResponse(tag);
            System.out.println("Bot: " + response);
        }
        scanner.close();
    }

    static void loadIntents(String filename) throws IOException {
        Reader reader = new FileReader(filename);
        Gson gson = new Gson();
        intentsData = gson.fromJson(reader, IntentsData.class);
        for (Intent intent : intentsData.intents) {
            tagToPatterns.put(intent.tag, intent.patterns);
            tagToResponses.put(intent.tag, intent.responses);
            for (String pattern : intent.patterns) {
                vocabulary.addAll(Arrays.asList(pattern.toLowerCase().split("\\s+")));
            }
        }
    }

    static String predictIntent(String input) {
        Map<String, Integer> inputVector = bagOfWords(input);
        String bestTag = null;
        double bestScore = -1;

        for (String tag : tagToPatterns.keySet()) {
            double score = 0;
            for (String pattern : tagToPatterns.get(tag)) {
                Map<String, Integer> patternVector = bagOfWords(pattern);
                score += cosineSimilarity(inputVector, patternVector);
            }
            score /= tagToPatterns.get(tag).size();
            if (score > bestScore) {
                bestScore = score;
                bestTag = tag;
            }
        }
        return bestTag != null ? bestTag : "unknown";
    }

    static Map<String, Integer> bagOfWords(String text) {
        Map<String, Integer> vector = new HashMap<>();
        for (String word : text.toLowerCase().split("\\s+")) {
            if (vocabulary.contains(word)) {
                vector.put(word, vector.getOrDefault(word, 0) + 1);
            }
        }
        return vector;
    }

    static double cosineSimilarity(Map<String, Integer> v1, Map<String, Integer> v2) {
        Set<String> allWords = new HashSet<>(v1.keySet());
        allWords.addAll(v2.keySet());

        double dot = 0, norm1 = 0, norm2 = 0;
        for (String word : allWords) {
            int a = v1.getOrDefault(word, 0);
            int b = v2.getOrDefault(word, 0);
            dot += a * b;
            norm1 += a * a;
            norm2 += b * b;
        }

        return (norm1 == 0 || norm2 == 0) ? 0 : dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    static String getResponse(String tag) {
        if (tagToResponses.containsKey(tag)) {
            List<String> responses = tagToResponses.get(tag);
            return responses.get(new Random().nextInt(responses.size()));
        }
        return "I'm not sure how to respond to that.";
    }
}

