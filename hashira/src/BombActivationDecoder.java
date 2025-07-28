import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class BombActivationDecoder {

    static class Share {
        int x;
        BigInteger y;

        Share(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return String.format("(%d, %s)", x, y.toString());
        }
    }

    public static void main(String[] args) {
        System.out.println("🔐 Shamir Secret Sharing\n");
        processTestCase("testcase1.json", "Test Case 1");
        processTestCase("testcase2.json", "Test Case 2");
    }

    private static void processTestCase(String filename, String testName) {
        try {
            System.out.println("🔸 Processing " + testName + ":\n");

            String jsonContent = new String(Files.readAllBytes(Paths.get(filename)));
            Map<String, Object> jsonData = parseSimpleJson(jsonContent);
            Map<String, Object> keys = (Map<String, Object>) jsonData.get("keys");

            int n = parseInt(keys.get("n"));
            int k = parseInt(keys.get("k"));

            System.out.println("Total Shares (n): " + n);
            System.out.println("Minimum Required (k): " + k);
            System.out.println("Polynomial Degree: " + (k - 1) + "\n");

            List<Share> allShares = new ArrayList<>();

            System.out.println("📥 Decoding Shares:");
            for (String key : jsonData.keySet()) {
                if (!key.equals("keys")) {
                    try {
                        int x = Integer.parseInt(key);
                        Map<String, Object> shareData = (Map<String, Object>) jsonData.get(key);
                        int base = parseInt(shareData.get("base"));
                        String value = (String) shareData.get("value");
                        BigInteger y = new BigInteger(value, base);
                        allShares.add(new Share(x, y));
                        System.out.printf("  x=%d | base=%d | encoded='%s' | decoded=%s%n", x, base, value, y);
                    } catch (Exception e) {
                        System.err.println("  ⚠️ Error parsing share " + key + ": " + e.getMessage());
                    }
                }
            }

            allShares.sort(Comparator.comparingInt(s -> s.x));

            List<Share> selectedShares = allShares.subList(0, Math.min(k, allShares.size()));
            String selectedIds = selectedShares.stream().map(s -> String.valueOf(s.x)).reduce((a, b) -> a + ", " + b).orElse("");
            System.out.println("\n🔧 Using shares: [" + selectedIds + "]");

            BigInteger secret = interpolateConstantTerm(selectedShares);
            System.out.println("\n🧠 Reconstructed Secret (constant term): " + secret);

            if (allShares.size() > k) {
                System.out.println("\n🧪 Verifying with other combinations:");
                verifyWithCombinations(allShares, k, secret);
            }

            System.out.println("\n" + "=".repeat(50) + "\n");

        } catch (Exception e) {
            System.err.println("❌ Error in " + testName + ": " + e.getMessage());
        }
    }

    private static int parseInt(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) return Integer.parseInt((String) value);
        throw new IllegalArgumentException("Invalid number: " + value);
    }

    private static BigInteger interpolateConstantTerm(List<Share> shares) {
        Fraction result = new Fraction(BigInteger.ZERO, BigInteger.ONE);

        for (int i = 0; i < shares.size(); i++) {
            Fraction term = new Fraction(shares.get(i).y, BigInteger.ONE);

            for (int j = 0; j < shares.size(); j++) {
                if (i != j) {
                    BigInteger numerator = BigInteger.valueOf(-shares.get(j).x);
                    BigInteger denominator = BigInteger.valueOf(shares.get(i).x - shares.get(j).x);
                    term = term.multiply(new Fraction(numerator, denominator));
                }
            }

            result = result.add(term);
        }

        if (!result.denominator.equals(BigInteger.ONE)) {
            System.out.println("⚠️ Warning: Result is not a whole number. Result: " + result.numerator + "/" + result.denominator);
            return result.numerator.divide(result.denominator);
        }

        return result.numerator;
    }

    private static void verifyWithCombinations(List<Share> shares, int k, BigInteger expectedSecret) {
        List<List<Share>> combinations = generateCombinations(shares, k);
        int matches = 0;
        int checks = Math.min(combinations.size(), 5);

        for (int i = 1; i < checks; i++) {
            List<Share> combo = combinations.get(i);
            try {
                BigInteger reconstructed = interpolateConstantTerm(combo);
                boolean match = reconstructed.equals(expectedSecret);
                String comboIds = combo.stream().map(s -> String.valueOf(s.x)).reduce((a, b) -> a + "," + b).orElse("");
                System.out.println("  Shares [" + comboIds + "] → " + reconstructed + (match ? " ✅" : " ❌"));
                if (match) matches++;
            } catch (Exception e) {
                System.out.println("  ⚠️ Error in combination: " + e.getMessage());
            }
        }

        System.out.println("✔ Verification: " + (matches + 1) + "/" + checks + " matched the secret");
    }

    static class Fraction {
        BigInteger numerator;
        BigInteger denominator;

        Fraction(BigInteger num, BigInteger den) {
            if (den.equals(BigInteger.ZERO)) throw new ArithmeticException("Divide by zero");
            if (den.signum() < 0) {
                num = num.negate();
                den = den.negate();
            }
            BigInteger gcd = num.gcd(den);
            this.numerator = num.divide(gcd);
            this.denominator = den.divide(gcd);
        }

        Fraction add(Fraction other) {
            BigInteger num = numerator.multiply(other.denominator).add(other.numerator.multiply(denominator));
            BigInteger den = denominator.multiply(other.denominator);
            return new Fraction(num, den);
        }

        Fraction multiply(Fraction other) {
            return new Fraction(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
        }
    }

    private static Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> map = new HashMap<>();
        json = json.trim().replaceAll("\\s+", " ");
        json = json.substring(1, json.length() - 1);
        List<String> pairs = splitJsonPairs(json);

        for (String pair : pairs) {
            int colon = pair.indexOf(':');
            if (colon == -1) continue;
            String key = pair.substring(0, colon).trim().replaceAll("\"", "");
            String value = pair.substring(colon + 1).trim();

            if (value.startsWith("{")) {
                map.put(key, parseSimpleJson(value));
            } else {
                value = value.replaceAll("\"", "");
                map.put(key, value);
            }
        }
        return map;
    }

    private static List<String> splitJsonPairs(String input) {
        List<String> pairs = new ArrayList<>();
        int depth = 0, start = 0;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '{') depth++;
            else if (ch == '}') depth--;
            else if (ch == ',' && depth == 0) {
                pairs.add(input.substring(start, i).trim());
                start = i + 1;
            }
        }

        pairs.add(input.substring(start).trim());
        return pairs;
    }

    private static List<List<Share>> generateCombinations(List<Share> shares, int k) {
        List<List<Share>> result = new ArrayList<>();
        backtrack(shares, k, 0, new ArrayList<>(), result);
        return result;
    }

    private static void backtrack(List<Share> shares, int k, int start, List<Share> current, List<List<Share>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < shares.size(); i++) {
            current.add(shares.get(i));
            backtrack(shares, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
