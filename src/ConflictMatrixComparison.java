import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class ConflictMatrixComparison {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Please enter the path to the henshin file: ");
        String henshinFilePath = scanner.nextLine();

        System.out.print("Please enter the path to the ChatGPT file: ");
        String chatGptFilePath = scanner.nextLine();

        try {
            Map<String, List<Boolean>> henshinMatrix = readMatrix(henshinFilePath, "binary");
            Map<String, List<Boolean>> chatGptMatrix = readMatrix(chatGptFilePath, "binary");

            List<String> rules = new ArrayList<>(henshinMatrix.keySet()); // Assuming both files have the same rules in the same order.

            System.out.println("\nConflicts found in both files:");
            System.out.println("-------------------------------------------------");
            int conflictsInBoth = printConflicts(henshinMatrix, chatGptMatrix, rules, true);
            System.out.println("\nTotal number of conflicts found in both: " + conflictsInBoth + ".");
            System.out.println("-------------------------------------------------");

            System.out.println("\nConflicts found only in Henshin:");
            System.out.println("-------------------------------------------------");
            int conflictsOnlyInHenshin = printConflicts(henshinMatrix, chatGptMatrix, rules, false);
            System.out.println("\nTotal number of conflicts found only in Henshin: " + conflictsOnlyInHenshin+ ".");
            System.out.println("-------------------------------------------------");

            System.out.println("\nConflicts found only in ChatGPT:");
            System.out.println("-------------------------------------------------");
            int conflictsOnlyInChatGPT = printConflicts(chatGptMatrix, henshinMatrix, rules, false);
            System.out.println("\nTotal number of conflicts found only in ChatGPT: " + conflictsOnlyInChatGPT+ ".");
            System.out.println("-------------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }

        scanner.close();
    }

    private static Map<String, List<Boolean>> readMatrix(String filePath, String keyword) throws IOException {
        Map<String, List<Boolean>> matrix = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        boolean sectionFound = false;

        for (String line : lines) {
            if (sectionFound) {
                if (line.trim().isEmpty()) break;
                String[] parts = line.split("\\|");
                String ruleName = parts[1].trim();
                List<Boolean> conflicts = Arrays.stream(parts[0].trim().split("\\s+"))
                        .map(num -> num.equals("1"))
                        .collect(Collectors.toList());
                matrix.put(ruleName, conflicts);
            }
            if (line.trim().contains(keyword)) {
                sectionFound = true;
            }
        }

        return matrix;
    }

    private static int printConflicts(Map<String, List<Boolean>> matrix1, Map<String, List<Boolean>> matrix2, List<String> rules, boolean common) {
        int count = 0;
        for (String rule : rules) {
            List<Boolean> conflicts1 = matrix1.get(rule);
            List<Boolean> conflicts2 = matrix2.get(rule);

            for (int j = 0; j < conflicts1.size(); j++) {
                if (common && conflicts1.get(j) && conflicts2.get(j)) {
                    System.out.println(rule + " -> " + rules.get(j));
                    count++;
                } else if (!common && conflicts1.get(j) && !conflicts2.get(j)) {
                    System.out.println(rule + " -> " + rules.get(j));
                    count++;
                }
            }
        }
        return count;
    }
}