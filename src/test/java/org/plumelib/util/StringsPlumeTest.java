package org.plumelib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.checkerframework.common.value.qual.ArrayLen;
import org.junit.jupiter.api.Test;
import org.plumelib.util.StringsPlume.VersionNumberComparator;

/** Test the stringsPlume class. */
public final class StringsPlumeTest {

  @Test
  public void test_replacePrefix() {

    // public static String replacePrefix(String target, String oldStr, String newStr)

    assertEquals("hijkdefg", StringsPlume.replacePrefix("abcdefg", "abc", "hijk"));
    assertEquals("abcdefg", StringsPlume.replacePrefix("abcdefg", "bcd", "hijk"));
    assertEquals("defg", StringsPlume.replacePrefix("abcdefg", "abc", ""));
    assertEquals("abcdefg", StringsPlume.replacePrefix("abcdefg", "bcd", ""));
  }

  @Test
  public void test_replaceSuffix() {

    // public static String replaceSuffix(String target, String oldStr, String newStr)

    assertEquals("abchijk", StringsPlume.replaceSuffix("abcdefg", "defg", "hijk"));
    assertEquals("abcdefg", StringsPlume.replaceSuffix("abcdefg", "cdef", "hijk"));
    assertEquals("abc", StringsPlume.replaceSuffix("abcdefg", "defg", ""));
    assertEquals("abcdefg", StringsPlume.replaceSuffix("abcdefg", "cdef", ""));
  }

  @Test
  public void test_replaceAll() {

    assertEquals("avarywhara", StringsPlume.replaceAll("everywhere", Pattern.compile("e"), "a"));
    assertEquals("aabaa", StringsPlume.replaceAll("abababa", Pattern.compile("aba"), "aa"));
    assertEquals(
        "abbababba", StringsPlume.replaceAll("abababa", Pattern.compile("a(b)a"), "a$1$1a"));
  }

  @Test
  public void test_prefixLines() {

    // public static String prefixLines(String prefix, String s) {

    assertEquals(
        StringsPlume.joinLines("  1", "  2", "  3"),
        StringsPlume.prefixLines("  ", StringsPlume.joinLines("1", "2", "3")));
    assertEquals(
        StringsPlume.joinLines("  ", "  1", "  ", "  2", "  "),
        StringsPlume.prefixLines("  ", StringsPlume.joinLines("", "1", "", "2", "")));
  }

  @Test
  public void test_indentLines() {

    // public static String indentLines(int indent, String s) {

    assertEquals(
        StringsPlume.prefixLines("  ", StringsPlume.joinLines("1", "2", "3")),
        StringsPlume.indentLines(2, StringsPlume.joinLines("1", "2", "3")));
    assertEquals(
        StringsPlume.prefixLines("  ", StringsPlume.joinLines("", "1", "", "2", "")),
        StringsPlume.indentLines(2, StringsPlume.joinLines("", "1", "", "2", "")));
  }

  @Test
  public void test_join() {

    // public static String join(Object[] a, String delim)
    // public static String join(ArrayList v, String delim)

    assertEquals("foo, bar, baz", StringsPlume.join(", ", new String[] {"foo", "bar", "baz"}));
    assertEquals("foo, bar, baz", StringsPlume.join(", ", "foo", "bar", "baz"));
    assertEquals("foo", StringsPlume.join(", ", new String[] {"foo"}));
    assertEquals("foo", StringsPlume.join(", ", "foo"));
    assertEquals("", StringsPlume.join(", ", new String[] {}));
    assertEquals("", StringsPlume.join(", "));
    assertEquals("01234", StringsPlume.join("", new Integer[] {0, 1, 2, 3, 4}));
    assertEquals("01234", StringsPlume.join("", 0, 1, 2, 3, 4));

    ArrayList<Object> potpourri = new ArrayList<>();
    potpourri.add("day");
    potpourri.add(2);
    potpourri.add("day");
    assertEquals("day 2 day", StringsPlume.join(" ", potpourri));
  }

  private void oneEscapeJava(String s, String escaped) {
    assertEquals(escaped, StringsPlume.escapeJava(s));
    assertEquals(s, StringsPlume.unescapeJava(escaped));
  }

  @SuppressWarnings("UnicodeEscape")
  @Test
  public void test_escapeJava() {

    // public static String escapeJava(String orig)
    // public static String escapeJava(char c)
    // public static String unescapeJava(String orig)
    // public static String unescapeJava(char c)

    oneEscapeJava("foobar", "foobar");
    oneEscapeJava("", "");
    oneEscapeJava("\\", "\\\\");
    oneEscapeJava("\"", "\\\"");
    oneEscapeJava("\n", "\\n");
    oneEscapeJava("\r", "\\r");
    oneEscapeJava("\\\n", "\\\\\\n");
    oneEscapeJava("\n\r", "\\n\\r");
    oneEscapeJava("\\\n\r\"", "\\\\\\n\\r\\\"");
    oneEscapeJava("split\nlines", "split\\nlines");
    oneEscapeJava("\\relax", "\\\\relax");
    oneEscapeJava("\"hello\"", "\\\"hello\\\"");
    oneEscapeJava("\"hello\" \"world\"", "\\\"hello\\\" \\\"world\\\"");
    oneEscapeJava("'hello'", "'hello'");
    oneEscapeJava("foo\\", "foo\\\\");
    oneEscapeJava("foo\0bar", "foo\\000bar");
    oneEscapeJava("foo\tbar", "foo\\tbar");
    oneEscapeJava("\b\f\n\r\t\1\377", "\\b\\f\\n\\r\\t\\001\\377");
    oneEscapeJava("\222", "\\222");
    oneEscapeJava("\300", "\\300");
    oneEscapeJava("\u12345", "\\u12345");
    oneEscapeJava("\u1234A", "\\u1234A");
    oneEscapeJava("\u54321", "\\u54321");
    oneEscapeJava("\u5432A", "\\u5432A");
    // Should add more tests here.

    // These tests are not symmetric because the argument is not a value that escapeJava would ever
    // return.
    assertEquals("\\", StringsPlume.unescapeJava("\\"));
    assertEquals("foo\\", StringsPlume.unescapeJava("foo\\"));
    assertEquals("*abc", StringsPlume.unescapeJava("\\*abc"));
    assertEquals("A", StringsPlume.unescapeJava("\\101"));
    assertEquals("ABC", StringsPlume.unescapeJava("A\\102C"));
    assertEquals("don't", StringsPlume.unescapeJava("don\\'t"));

    assertEquals("(1", StringsPlume.unescapeJava("\0501"));
    assertEquals("(1", StringsPlume.unescapeJava("\501"));
    assertEquals("?7", StringsPlume.unescapeJava("\0777")); // '?' = \077
    assertEquals("?7", StringsPlume.unescapeJava("\777")); // '?' = \077

    // public static String escapeNonASCII(String orig)

    assertEquals("foobar", StringsPlume.escapeNonASCII("foobar"));
    assertEquals("", StringsPlume.escapeNonASCII(""));
    assertEquals("\\\\", StringsPlume.escapeNonASCII("\\"));
    assertEquals("\\\\\\n\\r\\\"", StringsPlume.escapeNonASCII("\\\n\r\""));
    assertEquals("split\\nlines", StringsPlume.escapeNonASCII("split\nlines"));
    assertEquals("\\\\relax", StringsPlume.escapeNonASCII("\\relax"));
    assertEquals("\\\"hello\\\"", StringsPlume.escapeNonASCII("\"hello\""));
    assertEquals("\\\"hello\\\" \\\"world\\\"", StringsPlume.escapeNonASCII("\"hello\" \"world\""));
    assertEquals(
        "\\000\\001\\002\\007\\n8@I\\222",
        StringsPlume.escapeNonASCII("\0\1\2\7\12\70\100\111\222"));
    assertEquals(
        "\\u0100\\u1000\\ucafe\\uffff", StringsPlume.escapeNonASCII("\u0100\u1000\ucafe\uffff"));

    // private static String escapeNonASCII(char c)

    // Unfortunately, there isn't yet a unescapeNonASCII function.
    // If implemented, it should have the following behavior:
    // assertTrue(StringsPlume.unescapeNonASCII("\\115").equals("M"));
    // assertTrue(StringsPlume.unescapeNonASCII("\\115\\111\\124").equals("MIT"));
  }

  @Test
  public void test_charLiteral() {
    assertEquals("'a'", StringsPlume.charLiteral('a'));
    assertEquals("'\\''", StringsPlume.charLiteral('\''));
    assertEquals("'\"'", StringsPlume.charLiteral('\"'));
  }

  @Test
  public void test_removeWhitespace() {

    // public static String removeWhitespaceAround(String arg, String delimiter)
    // public static String removeWhitespaceAfter(String arg, String delimiter)
    // public static String removeWhitespaceBefore(String arg, String delimiter)

    assertEquals("a,b", StringsPlume.removeWhitespaceBefore("a,b", ","));
    assertEquals("a, b", StringsPlume.removeWhitespaceBefore("a, b", ","));
    assertEquals("a,b", StringsPlume.removeWhitespaceBefore("a ,b", ","));
    assertEquals("a, b", StringsPlume.removeWhitespaceBefore("a , b", ","));
    assertEquals("ab=>cd", StringsPlume.removeWhitespaceBefore("ab=>cd", "=>"));
    assertEquals("ab=> cd", StringsPlume.removeWhitespaceBefore("ab=> cd", "=>"));
    assertEquals("ab=>cd", StringsPlume.removeWhitespaceBefore("ab =>cd", "=>"));
    assertEquals("ab=> cd", StringsPlume.removeWhitespaceBefore("ab => cd", "=>"));
    assertEquals("123cd", StringsPlume.removeWhitespaceBefore("123cd", "123"));
    assertEquals("123 cd", StringsPlume.removeWhitespaceBefore(" 123 cd", "123"));
    assertEquals("123cd", StringsPlume.removeWhitespaceBefore(" 123cd", "123"));
    assertEquals("123 cd", StringsPlume.removeWhitespaceBefore("123 cd", "123"));
    assertEquals("cd123", StringsPlume.removeWhitespaceBefore("cd123", "123"));
    assertEquals("cd123 ", StringsPlume.removeWhitespaceBefore("cd 123 ", "123"));
    assertEquals("cd123 ", StringsPlume.removeWhitespaceBefore("cd123 ", "123"));
    assertEquals("cd123", StringsPlume.removeWhitespaceBefore("cd 123", "123"));

    assertEquals("a,b", StringsPlume.removeWhitespaceAfter("a,b", ","));
    assertEquals("a,b", StringsPlume.removeWhitespaceAfter("a, b", ","));
    assertEquals("a ,b", StringsPlume.removeWhitespaceAfter("a ,b", ","));
    assertEquals("a ,b", StringsPlume.removeWhitespaceAfter("a , b", ","));
    assertEquals("ab=>cd", StringsPlume.removeWhitespaceAfter("ab=>cd", "=>"));
    assertEquals("ab=>cd", StringsPlume.removeWhitespaceAfter("ab=> cd", "=>"));
    assertEquals("ab =>cd", StringsPlume.removeWhitespaceAfter("ab =>cd", "=>"));
    assertEquals("ab =>cd", StringsPlume.removeWhitespaceAfter("ab => cd", "=>"));
    assertEquals("123cd", StringsPlume.removeWhitespaceAfter("123cd", "123"));
    assertEquals(" 123cd", StringsPlume.removeWhitespaceAfter(" 123 cd", "123"));
    assertEquals(" 123cd", StringsPlume.removeWhitespaceAfter(" 123cd", "123"));
    assertEquals("123cd", StringsPlume.removeWhitespaceAfter("123 cd", "123"));
    assertEquals("cd123", StringsPlume.removeWhitespaceAfter("cd123", "123"));
    assertEquals("cd 123", StringsPlume.removeWhitespaceAfter("cd 123 ", "123"));
    assertEquals("cd123", StringsPlume.removeWhitespaceAfter("cd123 ", "123"));
    assertEquals("cd 123", StringsPlume.removeWhitespaceAfter("cd 123", "123"));

    assertEquals("a,b", StringsPlume.removeWhitespaceAround("a,b", ","));
    assertEquals("a,b", StringsPlume.removeWhitespaceAround("a, b", ","));
    assertEquals("a,b", StringsPlume.removeWhitespaceAround("a ,b", ","));
    assertEquals("a,b", StringsPlume.removeWhitespaceAround("a , b", ","));
    assertEquals("ab=>cd", StringsPlume.removeWhitespaceAround("ab=>cd", "=>"));
    assertEquals("ab=>cd", StringsPlume.removeWhitespaceAround("ab=> cd", "=>"));
    assertEquals("ab=>cd", StringsPlume.removeWhitespaceAround("ab =>cd", "=>"));
    assertEquals("ab=>cd", StringsPlume.removeWhitespaceAround("ab => cd", "=>"));
    assertEquals("123cd", StringsPlume.removeWhitespaceAround("123cd", "123"));
    assertEquals("123cd", StringsPlume.removeWhitespaceAround(" 123 cd", "123"));
    assertEquals("123cd", StringsPlume.removeWhitespaceAround(" 123cd", "123"));
    assertEquals("123cd", StringsPlume.removeWhitespaceAround("123 cd", "123"));
    assertEquals("cd123", StringsPlume.removeWhitespaceAround("cd123", "123"));
    assertEquals("cd123", StringsPlume.removeWhitespaceAround("cd 123 ", "123"));
    assertEquals("cd123", StringsPlume.removeWhitespaceAround("cd123 ", "123"));
    assertEquals("cd123", StringsPlume.removeWhitespaceAround("cd 123", "123"));
  }

  @Test
  public void test_isVersionNumberLE() {

    VersionNumberComparator vnc = new VersionNumberComparator();

    assertEquals(0, vnc.compare("123.456.789", "123.456.789"));
    assertEquals(-1, vnc.compare("113.456.789", "123.456.789"));
    assertEquals(-1, vnc.compare("123.416.789", "123.456.789"));
    assertEquals(-1, vnc.compare("123.456.719", "123.456.789"));
    assertEquals(-1, vnc.compare("123.456.789", "193.456.789"));
    assertEquals(-1, vnc.compare("123.456.789", "123.496.789"));
    assertEquals(-1, vnc.compare("123.456.789", "123.456.799"));
    assertEquals(-1, vnc.compare("123", "123.456.789"));
    assertEquals(-1, vnc.compare("123.456", "123.456.789"));
    assertEquals(1, vnc.compare("123.456.789", "123"));
    assertEquals(1, vnc.compare("123.456.789", "123.456"));
  }

  @Test
  public void test_nPlural() {

    // public static String nPlural(int n, String noun)

    assertEquals("0 fusses", StringsPlume.nPlural(0, "fuss"));
    assertEquals("1 fuss", StringsPlume.nPlural(1, "fuss"));
    assertEquals("2 fusses", StringsPlume.nPlural(2, "fuss"));
    assertEquals("0 foxes", StringsPlume.nPlural(0, "fox"));
    assertEquals("1 fox", StringsPlume.nPlural(1, "fox"));
    assertEquals("2 foxes", StringsPlume.nPlural(2, "fox"));
    assertEquals("0 wishes", StringsPlume.nPlural(0, "wish"));
    assertEquals("1 wish", StringsPlume.nPlural(1, "wish"));
    assertEquals("2 wishes", StringsPlume.nPlural(2, "wish"));
    assertEquals("0 fletches", StringsPlume.nPlural(0, "fletch"));
    assertEquals("1 fletch", StringsPlume.nPlural(1, "fletch"));
    assertEquals("2 fletches", StringsPlume.nPlural(2, "fletch"));
    assertEquals("0 funds", StringsPlume.nPlural(0, "fund"));
    assertEquals("1 fund", StringsPlume.nPlural(1, "fund"));
    assertEquals("2 funds", StringsPlume.nPlural(2, "fund"));
    assertEquals("0 f-stops", StringsPlume.nPlural(0, "f-stop"));
    assertEquals("1 f-stop", StringsPlume.nPlural(1, "f-stop"));
    assertEquals("2 f-stops", StringsPlume.nPlural(2, "f-stop"));
    assertEquals("0 facilities", StringsPlume.nPlural(0, "facility"));
    assertEquals("1 facility", StringsPlume.nPlural(1, "facility"));
    assertEquals("2 facilities", StringsPlume.nPlural(2, "facility"));
    assertEquals("0 factories", StringsPlume.nPlural(0, "factory"));
    assertEquals("1 factory", StringsPlume.nPlural(1, "factory"));
    assertEquals("2 factories", StringsPlume.nPlural(2, "factory"));
    assertEquals("0 fairways", StringsPlume.nPlural(0, "fairway"));
    assertEquals("1 fairway", StringsPlume.nPlural(1, "fairway"));
    assertEquals("2 fairways", StringsPlume.nPlural(2, "fairway"));
    assertEquals("0 fanboys", StringsPlume.nPlural(0, "fanboy"));
    assertEquals("1 fanboy", StringsPlume.nPlural(1, "fanboy"));
    assertEquals("2 fanboys", StringsPlume.nPlural(2, "fanboy"));

    // Exceptions
    assertEquals("0 fish", StringsPlume.nPlural(0, "fish"));
    assertEquals("1 fish", StringsPlume.nPlural(1, "fish"));
    assertEquals("2 fish", StringsPlume.nPlural(2, "fish"));

    // public static String nPlural(Collection c, String noun)

    Collection<String> size0 = Collections.emptyList();
    Map<Integer, Double> size1 = Collections.singletonMap(1, 2.0);
    String[] size2 = new String[] {"a", "string"};

    assertEquals("0 fusses", StringsPlume.nPlural(size0, "fuss"));
    assertEquals("1 fuss", StringsPlume.nPlural(size1, "fuss"));
    assertEquals("2 fusses", StringsPlume.nPlural(size2, "fuss"));
    assertEquals("0 foxes", StringsPlume.nPlural(size0, "fox"));
    assertEquals("1 fox", StringsPlume.nPlural(size1, "fox"));
    assertEquals("2 foxes", StringsPlume.nPlural(size2, "fox"));
    assertEquals("0 fish", StringsPlume.nPlural(size0, "fish"));
    assertEquals("1 fish", StringsPlume.nPlural(size1, "fish"));
    assertEquals("2 fish", StringsPlume.nPlural(size2, "fish"));
  }

  @Test
  public void test_vPlural() {

    // public static String [Plural(int n, String verb)

    assertEquals("were", StringsPlume.vPlural(0, "was"));
    assertEquals("was", StringsPlume.vPlural(1, "was"));
    assertEquals("were", StringsPlume.vPlural(2, "was"));
    assertEquals("are", StringsPlume.vPlural(0, "is"));
    assertEquals("is", StringsPlume.vPlural(1, "is"));
    assertEquals("are", StringsPlume.vPlural(2, "is"));
    assertEquals("eat", StringsPlume.vPlural(0, "eat"));
    assertEquals("eat", StringsPlume.vPlural(1, "eat"));
    assertEquals("eat", StringsPlume.vPlural(2, "eat"));
  }

  @Test
  public void test_nvPlural() {

    // public static String nvPlural(int n, String noun, String verb)

    assertEquals("0 foxes were", StringsPlume.nvPlural(0, "fox", "was"));
    assertEquals("1 fox was", StringsPlume.nvPlural(1, "fox", "was"));
    assertEquals("2 foxes were", StringsPlume.nvPlural(2, "fox", "was"));
    assertEquals("0 wishes are", StringsPlume.nvPlural(0, "wish", "is"));
    assertEquals("1 wish is", StringsPlume.nvPlural(1, "wish", "is"));
    assertEquals("2 wishes are", StringsPlume.nvPlural(2, "wish", "is"));
  }

  @Test
  public void test_conjunction() {

    // public static String conjunction(String conjunction, List<?> elements)

    assertEquals("a", StringsPlume.conjunction("and", Arrays.asList("a")));
    assertEquals("a and b", StringsPlume.conjunction("and", Arrays.asList("a", "b")));
    assertEquals("a, b, and c", StringsPlume.conjunction("and", Arrays.asList("a", "b", "c")));
    assertEquals(
        "a, b, c, and d", StringsPlume.conjunction("and", Arrays.asList("a", "b", "c", "d")));
    assertEquals("a", StringsPlume.conjunction("or", Arrays.asList("a")));
    assertEquals("a or b", StringsPlume.conjunction("or", Arrays.asList("a", "b")));
    assertEquals("a, b, or c", StringsPlume.conjunction("or", Arrays.asList("a", "b", "c")));
    assertEquals(
        "a, b, c, or d", StringsPlume.conjunction("or", Arrays.asList("a", "b", "c", "d")));
  }

  @Test
  public void test_rpad() {

    // public static String rpad(String s, int length)
    // public static String rpad(int num, int length)
    // public static String rpad(double num, int length)

    assertEquals("     ", StringsPlume.rpad("", 5));
    assertEquals("abcd ", StringsPlume.rpad("abcd", 5));
    assertEquals("abcde", StringsPlume.rpad("abcde", 5));
    assertEquals("ab...", StringsPlume.rpad("abcdef", 5));
    assertEquals("ab...", StringsPlume.rpad("abcde ghij", 5));
    assertEquals("10   ", StringsPlume.rpad(10, 5));
    assertEquals("3.14 ", StringsPlume.rpad(3.14, 5));
    assertEquals("3.141", StringsPlume.rpad(3.141592, 5));
    assertEquals("3141592", StringsPlume.rpad(3141592, 5));
    assertEquals("12", StringsPlume.rpad(12.34567, 1));
    assertEquals("12", StringsPlume.rpad(12.34567, 2));
    assertEquals("12 ", StringsPlume.rpad(12.34567, 3));
    assertEquals("12.3", StringsPlume.rpad(12.34567, 4));
    assertEquals("12.34", StringsPlume.rpad(12.34567, 5));
    assertEquals("12.345", StringsPlume.rpad(12.34567, 6));

    // public static class NullableStringComparator
    //   public int compare(Object o1, Object o2)

  }

  @Test
  public void test_count() {

    // public static int count(String s, int ch)
    // public static int count(String s, String sub)

    assertEquals(1, StringsPlume.count("abcde", 'a'));
    assertEquals(1, StringsPlume.count("abcde", 'c'));
    assertEquals(1, StringsPlume.count("abcde", 'e'));
    assertEquals(0, StringsPlume.count("abcde", 'z'));
    assertEquals(5, StringsPlume.count("abacadaea", 'a'));
    assertEquals(5, StringsPlume.count("aaa aea", 'a'));
    assertEquals(4, StringsPlume.count("daeaaa", 'a'));
  }

  // This will be easy to write tests for, when I get around to it.
  // public static ArrayList tokens(String str, String delim, boolean returnTokens)
  // public static ArrayList tokens(String str, String delim)
  // public static ArrayList tokens(String str)

  // This is tested by the tokens methods.
  // public static ArrayList makeArrayList(Enumeration e)

  @Test
  public void test_abbreviateNumber() {

    Locale.setDefault(Locale.US);
    assertEquals("5.00", StringsPlume.abbreviateNumber(5));
    assertEquals("5.00K", StringsPlume.abbreviateNumber(5000));
    assertEquals("5.00M", StringsPlume.abbreviateNumber(5000000));
    assertEquals("1.00G", StringsPlume.abbreviateNumber(1000000000));
    assertEquals("1.00", StringsPlume.abbreviateNumber(1));
    assertEquals("12.0", StringsPlume.abbreviateNumber(12));
    assertEquals("123", StringsPlume.abbreviateNumber(123));
    assertEquals("1.23K", StringsPlume.abbreviateNumber(1234));
    assertEquals("12.3K", StringsPlume.abbreviateNumber(12345));
    assertEquals("123K", StringsPlume.abbreviateNumber(123456));
    assertEquals("1.23M", StringsPlume.abbreviateNumber(1234567));
    assertEquals("12.3M", StringsPlume.abbreviateNumber(12345678));
    assertEquals("123M", StringsPlume.abbreviateNumber(123456789));
    assertEquals("1.23G", StringsPlume.abbreviateNumber(1234567890));
    assertEquals("9.00", StringsPlume.abbreviateNumber(9));
    assertEquals("98.0", StringsPlume.abbreviateNumber(98));
    assertEquals("987", StringsPlume.abbreviateNumber(987));
    assertEquals("9.88K", StringsPlume.abbreviateNumber(9876));
    assertEquals("98.8K", StringsPlume.abbreviateNumber(98765));
    assertEquals("988K", StringsPlume.abbreviateNumber(987654));
    assertEquals("9.88M", StringsPlume.abbreviateNumber(9876543));
    assertEquals("98.8M", StringsPlume.abbreviateNumber(98765432));
    assertEquals("988M", StringsPlume.abbreviateNumber(987654321));
    assertEquals("9.88G", StringsPlume.abbreviateNumber(9876543210L));
  }

  @Test
  public void testCountFormatArguments() {
    assertEquals(0, StringsPlume.countFormatArguments("No specifiier."));
    assertEquals(0, StringsPlume.countFormatArguments("This is 100%"));
    assertEquals(0, StringsPlume.countFormatArguments("This is 100%% excellent."));
    assertEquals(0, StringsPlume.countFormatArguments("Newline%n is not%na specifier."));
    assertEquals(1, StringsPlume.countFormatArguments("This is my %s"));
    assertEquals(1, StringsPlume.countFormatArguments("This is my %s."));
    assertEquals(2, StringsPlume.countFormatArguments("Two %d and %d"));
    assertEquals(3, StringsPlume.countFormatArguments("%f and %s and %d makes three"));
    assertEquals(
        3,
        StringsPlume.countFormatArguments("Hi! My name is %s and I have %d dogs and a %d cats."));

    assertEquals(2, StringsPlume.countFormatArguments("%f and %1$f and %d and %1$f makes two"));
    assertEquals(14, StringsPlume.countFormatArguments("%f and %14$f makes fourteen"));
  }

  @Test
  public void testSplitLines() {
    String str = "one\ntwo\n\rthree\r\nfour\rfive\n\n\nsix\r\n\r\n\r\n";
    @SuppressWarnings("value") // method that returns an array is not StaticallyExecutable
    String @ArrayLen(12) [] sa = StringsPlume.splitLines(str);
    // for (String s : sa)
    //   System.out.printf ("'%s'%n", s);
    assertEquals(12, sa.length);
    assertEquals("one", sa[0]);
    assertEquals("two", sa[1]);
    assertEquals("", sa[2]);
    assertEquals("three", sa[3]);
    assertEquals("four", sa[4]);
    assertEquals("five", sa[5]);
    assertEquals("", sa[6]);
    assertEquals("", sa[7]);
    assertEquals("six", sa[8]);
    assertEquals("", sa[9]);
    assertEquals("", sa[10]);
    assertEquals("", sa[11]);
  }

  @Test
  public void testFirstLineSeparator() {
    assertEquals(null, StringsPlume.firstLineSeparator("hello"));
    assertEquals("\n", StringsPlume.firstLineSeparator("hello\ngoodbye"));
    assertEquals("\n", StringsPlume.firstLineSeparator("hello\ngoodbye\rau revior"));
    assertEquals("\n", StringsPlume.firstLineSeparator("hello\ngoodbye\rau revior\r\nWindows"));
    assertEquals("\n", StringsPlume.firstLineSeparator("hello\n\rgoodbye\rau revior\r\nWindows"));

    assertEquals("\r", StringsPlume.firstLineSeparator("hello\rgoodbye"));
    assertEquals("\r", StringsPlume.firstLineSeparator("hello\rgoodbye\nau revior"));
    assertEquals("\r", StringsPlume.firstLineSeparator("hello\rgoodbye\nau revior\r\nWindows"));

    assertEquals("\r\n", StringsPlume.firstLineSeparator("hello\r\ngoodbye"));
    assertEquals("\r\n", StringsPlume.firstLineSeparator("hello\r\ngoodbye\nau revior"));
    assertEquals("\r\n", StringsPlume.firstLineSeparator("hello\r\ngoodbye\nau revior\rold MacOS"));
  }

  @Test
  void testSplitLinesRetainSeparators() {
    String text = "hello\rworld\nhello\r\nworld\n\rfoo";
    List<String> result = StringsPlume.splitLinesRetainSeparators(text);
    List<String> expected =
        Arrays.asList(new String[] {"hello\r", "world\n", "hello\r\n", "world\n", "\r", "foo"});
    assertEquals(expected, result);
  }

  @Test
  void testSplitRetainSeparators() {
    // There are two overloaded methods to test here.
  }

  @Test
  public void testToStringTruncated() {
    assertEquals("0123456789", StringsPlume.toStringTruncated("0123456789", 100));
    assertEquals("0123456789", StringsPlume.toStringTruncated("0123456789", 10));
    assertEquals("\"012...\"", StringsPlume.toStringTruncated("0123456789", 8));
    assertEquals("\"0...\"", StringsPlume.toStringTruncated("0123456789", 6));
    assertEquals("\"0...\"", StringsPlume.toStringTruncated("0123456", 6));
    assertEquals("012345", StringsPlume.toStringTruncated("012345", 6));
    assertEquals("01234", StringsPlume.toStringTruncated("01234", 6));
  }
}
