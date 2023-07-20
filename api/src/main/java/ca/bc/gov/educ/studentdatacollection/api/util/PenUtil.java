package ca.bc.gov.educ.studentdatacollection.api.util;

import java.util.ArrayList;
import java.util.List;

public class PenUtil {

    private PenUtil() {}

    public static boolean validCheckDigit(final String pen) {
        if (pen.length() != 9 || !pen.matches("-?\\d+(\\.\\d+)?")) {
            return false;
        }
        final List<Integer> odds = new ArrayList<>();
        final List<Integer> evens = new ArrayList<>();
        for (int i = 0; i < pen.length() - 1; i++) {
            final int number = Integer.parseInt(pen.substring(i, i + 1));
            if (i % 2 == 0) {
                odds.add(number);
            } else {
                evens.add(number);
            }
        }

        final int sumOdds = odds.stream().mapToInt(Integer::intValue).sum();

        final StringBuilder fullEvenStringBuilder = new StringBuilder();
        for (final int i : evens) {
            fullEvenStringBuilder.append(i);
        }

        final List<Integer> listOfFullEvenValueDoubled = new ArrayList<>();
        final String fullEvenValueDoubledString = Integer.toString(Integer.parseInt(fullEvenStringBuilder.toString()) * 2);
        for (int i = 0; i < fullEvenValueDoubledString.length(); i++) {
            listOfFullEvenValueDoubled.add(Integer.parseInt(fullEvenValueDoubledString.substring(i, i + 1)));
        }

        final int sumEvens = listOfFullEvenValueDoubled.stream().mapToInt(Integer::intValue).sum();

        final int finalSum = sumEvens + sumOdds;

        final String penCheckDigit = pen.substring(8, 9);

        return ((finalSum % 10 == 0 && penCheckDigit.equals("0")) || ((10 - finalSum % 10) == Integer.parseInt(penCheckDigit)));
    }
}
