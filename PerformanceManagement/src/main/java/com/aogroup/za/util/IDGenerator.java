package com.aogroup.za.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class IDGenerator {

    private static final Logger logger = Logger.getLogger("IDGenerator.class");

    private long prevTime;
    private int charOneIndex = 0;
    private int charTwoIndex = 0;

    private char firstChar = 0;
    private char secondCharDebug = 0;

    private static String strPrepender;
    private Random random;
    private static List<Character> firstCharList;
    private static List<Character> secondCharList;

    final static char[] DigitTens = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',};

    final static char[] DigitOnes = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',};

    private final static char[] digits = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};

    private IDGenerator() {
        prevTime = 0;
        firstCharList = new ArrayList<>();
        secondCharList = new ArrayList<>();
        random = new Random();
        refreshCharacterList(firstCharList);
        refreshCharacterList(secondCharList);
    }

    public synchronized static String MvisaRRNGenerate() {
        char year = new SimpleDateFormat("Y").format(new Date()).toCharArray()[3];
        String dayInYear = new SimpleDateFormat("D").format(new Date());
        String hourInDay = new SimpleDateFormat("H").format(new Date());
        //System.out.println("Mvisa RRN = " + year + dayInYear + hourInDay + IDGenerator.getInstance("qp").getRRN().substring(3, 9));
        return year + dayInYear + hourInDay + IDGenerator.getInstance("qp").getRRN().substring(3, 9);

    }

    public static IDGenerator getInstance(String prepender) {
        if (strPrepender == null) {
            strPrepender = prepender.toUpperCase();
        }
        return RRNGeneratorHolder.INSTANCE;
    }

    private static class RRNGeneratorHolder {

        private static final IDGenerator INSTANCE = new IDGenerator();
    }

    public synchronized String getRandomCharacterRRN(long time) {
        StringBuilder rrn = new StringBuilder(strPrepender);

        char secondChar = 0;

        if (time == prevTime) {
            if (!secondCharList.isEmpty()) {
                secondChar = secondCharList.remove(random.nextInt(secondCharList.size()));
                if (secondCharDebug == secondChar) {
                    //System.out.println(secondChar);
                }
                secondCharDebug = secondChar;
            } else {
                if (!firstCharList.isEmpty()) {
                    firstChar = firstCharList.remove(random.nextInt(firstCharList.size()));

                    // refresh secondCharList
                    refreshCharacterList(secondCharList);

                    // get secondChar
                    secondChar = secondCharList.remove(random.nextInt(secondCharList.size()));
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        logger.info("IDGenerator: getRRN - Error has occured." + ex.getMessage().toUpperCase());
                    }

                    time = System.currentTimeMillis() / 1000;

                    // refresh firstCharList
                    refreshCharacterList(firstCharList);

                    // get firstChar
                    firstChar = firstCharList.remove(random.nextInt(firstCharList.size()));

                    // refresh secondCharList
                    refreshCharacterList(secondCharList);

                    // get secondChar
                    secondChar = secondCharList.remove(random.nextInt(secondCharList.size()));
                }
            }
        } else {
            // refresh firstCharList
            refreshCharacterList(firstCharList);

            // get firstChar
            firstChar = firstCharList.remove(random.nextInt(firstCharList.size()));

            // refresh secondCharList
            refreshCharacterList(secondCharList);

            // get secondChar
            secondChar = secondCharList.remove(random.nextInt(secondCharList.size()));
        }

        prevTime = time;

        rrn.append(firstChar).append(toString(time, 34).toUpperCase()).append(secondChar);

        return rrn.toString();
    }

    public synchronized String getRandomCharacterRRN() {

        long time = System.currentTimeMillis() / 1000;

        return getRandomCharacterRRN(time);

    }

    public synchronized String getRRN(long time) {
        StringBuilder rrn = new StringBuilder(strPrepender);

        if (time == prevTime) {
            charOneIndex++;
            if (charOneIndex >= digits.length) {
                charOneIndex = 0;
                charTwoIndex++;
                if (charTwoIndex >= digits.length) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        logger.info("RRNGenerator: getRRN - Error has occured." + ex.getMessage().toUpperCase());
                    }
                    charOneIndex = 0;
                    charTwoIndex = 0;
                    return getRRN();
                }
            }
        } else {
            charOneIndex = 0;
            charTwoIndex = 0;
        }

        prevTime = time;

//        rrn.append(Long.toHexString(time).toUpperCase()).append(digits[char2]).append(digits[char1]);
        rrn.append(digits[charTwoIndex]).append(toString(time, 34).toUpperCase()).append(digits[charOneIndex]);
//        //System.out.println(rrn.toString() + ": " + time);
        try {
            Thread.sleep(1);
        } catch (InterruptedException ex) {
            logger.info("RRNGenerator: getRRN - Error has occured." + ex.getMessage().toUpperCase());
        }
        return rrn.toString().substring(0, 12);
    }

    public synchronized String getRRN() {

        long time = System.currentTimeMillis();

        return getRRN(time);

    }

    private void refreshCharacterList(List charList) {
        charList.clear();
        for (char c : digits) {
            charList.add(c);
        }
    }

    /**
     * Returns a string representation of the first argument in the radix
     * specified by the second argument.
     * <p>
     * <p>
     * If the radix is smaller than {@code Character.MIN_RADIX} or larger than
     * {@code Character.MAX_RADIX}, then the radix {@code 10} is used instead.
     * <p>
     * <p>
     * If the first argument is negative, the first element of the result is the
     * ASCII minus sign {@code '-'} (<code>'&#92;u002d'</code>). If the first
     * argument is not negative, no sign character appears in the result.
     * <p>
     * <p>
     * The remaining characters of the result represent the magnitude of the
     * first argument. If the magnitude is zero, it is represented by a single
     * zero character {@code '0'} (<code>'&#92;u0030'</code>); otherwise, the
     * first character of the representation of the magnitude will not be the
     * zero character. The following ASCII characters are used as digits:
     * <p>
     * <blockquote> {@code 0123456789abcdefghijklmnopqrstuvwxyz}
     * </blockquote>
     * <p>
     * These are <code>'&#92;u0030'</code> through <code>'&#92;u0039'</code> and
     * <code>'&#92;u0061'</code> through <code>'&#92;u007a'</code>. If
     * {@code radix} is
     * <var>N</var>, then the first <var>N</var> of these characters are used as
     * radix-<var>N</var> digits in the order shown. Thus, the digits for
     * hexadecimal (radix 16) are {@code 0123456789abcdef}. If uppercase letters
     * are desired, the {@link String#toUpperCase()} method may be called on the
     * result:
     * <p>
     * <blockquote> {@code Long.toString(n, 16).toUpperCase()}
     * </blockquote>
     *
     * @param i a {@code long} to be converted to a string.
     * @param radix the radix to use in the string representation.
     * @return a string representation of the argument in the specified radix.
     * @see Character#MAX_RADIX
     * @see Character#MIN_RADIX
     */
    public static String toString(long i, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            radix = 10;
        }
        if (radix == 10) {
            return toString(i);
        }
        char[] buf = new char[65];
        int charPos = 64;
        boolean negative = (i < 0);

        if (!negative) {
            i = -i;
        }

        while (i <= -radix) {
            buf[charPos--] = digits[(int) (-(i % radix))];
            i = i / radix;
        }
        buf[charPos] = digits[(int) (-i)];

        if (negative) {
            buf[--charPos] = '-';
        }

        return new String(buf, charPos, (65 - charPos));
    }

    /**
     * Returns a {@code String} object representing the specified {@code long}.
     * The argument is converted to signed decimal representation and returned
     * as a string, exactly as if the argument and the radix 10 were given as
     * arguments to the {@link
     * #toString(long, int)} method.
     *
     * @param i a {@code long} to be converted.
     * @return a string representation of the argument in base&nbsp;10.
     */
    public static String toString(long i) {
        if (i == Long.MIN_VALUE) {
            return "-9223372036854775808";
        }
        int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
        char[] buf = new char[size];
        getChars(i, size, buf);
        return new String(buf);
    }

    // Requires positive x
    static int stringSize(long x) {
        long p = 10;
        for (int i = 1; i < 19; i++) {
            if (x < p) {
                return i;
            }
            p = 10 * p;
        }
        return 19;
    }

    /**
     * Places characters representing the integer i into the character array
     * buf. The characters are placed into the buffer backwards starting with
     * the least significant digit at the specified index (exclusive), and
     * working backwards from there.
     * <p>
     * Will fail if i == Long.MIN_VALUE
     */
    static void getChars(long i, int index, char[] buf) {
        long q;
        int r;
        int charPos = index;
        char sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i > Integer.MAX_VALUE) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
            i = q;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int) i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16 + 3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buf[--charPos] = digits[r];
            i2 = q2;
            if (i2 == 0) {
                break;
            }
        }
        if (sign != 0) {
            buf[--charPos] = sign;
        }
    }
}
