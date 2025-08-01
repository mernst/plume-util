// If you edit this file, you must also edit its tests.

package org.plumelib.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

// The list of features is everything that appears on a line starting with "  /// ".
/**
 * Utility functions that manipulate Strings: replacement; prefixing and indentation; splitting and
 * joining; quoting and escaping; whitespace; comparisons; StringTokenizer; debugging variants of
 * toString; diagnostic output; miscellaneous.
 */
public final class StringsPlume {

  /** This class is a collection of methods; it does not represent anything. */
  private StringsPlume() {
    throw new Error("do not instantiate");
  }

  /** The system-specific line separator string. */
  private static final String lineSep = System.lineSeparator();

  // //////////////////////////////////////////////////////////////////////
  // Replacement
  //

  /**
   * Returns the target with an occurrence of oldStr at the start replaced by newStr. Returns the
   * target if it does not strt with oldStr.
   *
   * <p>An alternative to this is to use regular expressions: {@code target.replaceFirst("^" +
   * Pattern.quote(oldStr), newStr)}
   *
   * @param target the string to do replacement in
   * @param oldStr the prefix to replace
   * @param newStr the replacement
   * @return the target with an occurrence of oldStr at the start replaced by newStr; returns the
   *     target if it does not start with oldStr
   */
  @SuppressWarnings("index:argument") // startsWith implies indexes fit
  @SideEffectFree
  public static String replacePrefix(String target, String oldStr, String newStr) {
    if (target.startsWith(oldStr)) {
      if (newStr.isEmpty()) {
        return target.substring(oldStr.length());
      } else {
        return newStr + target.substring(oldStr.length());
      }
    } else {
      return target;
    }
  }

  /**
   * Returns the target with an occurrence of oldStr at the end replaced by newStr. Returns the
   * target if it does not end with oldStr.
   *
   * <p>An alternative to this is to use regular expressions: {@code
   * target.replaceLast(Pattern.quote(oldStr) + "$", newStr)}
   *
   * @param target the string to do replacement in
   * @param oldStr the substring to replace
   * @param newStr the replacement
   * @return the target with an occurrence of oldStr at the start replaced by newStr; returns the
   *     target if it does not start with oldStr
   */
  @SuppressWarnings("lowerbound:argument") // endsWith implies indexes fit
  @SideEffectFree
  public static String replaceSuffix(String target, String oldStr, String newStr) {
    if (target.endsWith(oldStr)) {
      if (newStr.isEmpty()) {
        return target.substring(0, target.length() - oldStr.length());
      } else {
        return target.substring(0, target.length() - oldStr.length()) + newStr;
      }
    } else {
      return target;
    }
  }

  /**
   * Replaces every (non-overlapping) match for a regexp. Like {@code String.replaceAll}, but
   * slightly more efficient because the regex has been pre-compiled.
   *
   * @param s a string in which to replace
   * @param regex a regular expression
   * @param replacement the replacement for each match of the regular expression
   * @return the string, with each match for the regex replaced
   */
  @SideEffectFree
  public static String replaceAll(String s, Pattern regex, String replacement) {
    Matcher m = regex.matcher(s);
    return m.replaceAll(replacement);
  }

  // //////////////////////////////////////////////////////////////////////
  // Prefixing and indentation
  //

  /**
   * Returns the printed represenation of a value, with each line prefixed by another string.
   *
   * @param prefix the prefix to place before each line
   * @param o the value to be printed
   * @return the printed representation of {@code o}, with each line prefixed by the given prefix
   */
  @SideEffectFree
  public static String prefixLines(String prefix, @Nullable Object o) {
    return prefix + prefixLinesExceptFirst(prefix, o);
  }

  /**
   * Returns the printed represenation of a value, with each line (except the first) prefixed by
   * another string.
   *
   * @param prefix the prefix to place before each line
   * @param o the value to be printed
   * @return the printed representation of {@code o}, with each line (except the first) prefixed by
   *     the given prefix
   */
  @SideEffectFree
  public static String prefixLinesExceptFirst(String prefix, @Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace(lineSep, lineSep + prefix);
  }

  /**
   * Returns the printed representation of a value, with each line indented by {@code indent}
   * spaces.
   *
   * @param indent the number of spaces to indent
   * @param o the value whose printed representation string to increase indentation of
   * @return the printed representation of {@code o}, with each line prefixed by {@code indent}
   *     space characters
   */
  @SideEffectFree
  public static String indentLines(@NonNegative int indent, @Nullable Object o) {
    if (indent == 0) {
      return (o == null) ? "null" : o.toString();
    }
    String prefix = new String(new char[indent]).replace('\0', ' ');
    return prefixLines(prefix, o);
  }

  /**
   * Returns the printed representation of a value, with each line (except the first) indented by
   * {@code indent} spaces.
   *
   * @param indent the number of spaces to indent
   * @param o the value whose printed representation string to increase indentation of
   * @return the printed representation of {@code o}, with each line (except the first) prefixed by
   *     {@code indent} space characters
   */
  @SideEffectFree
  public static String indentLinesExceptFirst(@NonNegative int indent, @Nullable Object o) {
    if (indent == 0) {
      return (o == null) ? "null" : o.toString();
    }
    String prefix = new String(new char[indent]).replace('\0', ' ');
    return prefixLinesExceptFirst(prefix, o);
  }

  // //////////////////////////////////////////////////////////////////////
  // Splitting and joining
  //

  /** A pattern that matches all common line separators: lf, cr, cr-lf. */
  private static Pattern allLineSeparators = Pattern.compile("\\R");

  /**
   * Returns an array of Strings, one for each line in the argument. The strings do <b>not</b> end
   * with line separators. Always returns an array of length at least 1 (it might contain only the
   * empty string). All common line separators (lf, cr, cr-lf) are supported. Note that a string
   * that ends with a line separator will return an empty string as the last element of the array.
   *
   * <p>It is probably better to use {@code String.lines()} rather than this method.
   *
   * <p>Alternately, you could use {@link #firstLineSeparator} and split on its return value.
   *
   * @param s the string to split
   * @return an array of Strings, one for each line in the argument
   */
  @SuppressWarnings("value:statically.executable.not.pure") // pure wrt `equals()` but not `==`
  @SideEffectFree
  @StaticallyExecutable
  public static String[] splitLines(String s) {
    return allLineSeparators.split(s, -1);
  }

  /**
   * Returns the first line separator in the given string, or null if the string contains none.
   *
   * @param s a string
   * @return the first line separator in the given string
   */
  @SuppressWarnings({
    "regex:return", // all matches of allLineSeparators are regexes
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @SideEffectFree
  public static @Nullable @Regex String firstLineSeparator(String s) {
    Matcher m = allLineSeparators.matcher(s);
    if (m.find()) {
      return m.group();
    } else {
      return null;
    }
  }

  /**
   * Splits a String into lines, keeping the line separator at the end of each substring.
   *
   * @param input the input String
   * @return the split string
   */
  @SideEffectFree
  public static List<String> splitLinesRetainSeparators(String input) {
    return splitRetainSeparators(input, allLineSeparators);
  }

  /**
   * Splits a String according to a regex, keeping the separator at the end of each substring.
   *
   * @param input the input String
   * @param regex the regular expression upon which to split the input
   * @return the split string
   */
  @SideEffectFree
  public static List<String> splitRetainSeparators(String input, @Regex String regex) {
    return splitRetainSeparators(input, Pattern.compile(regex));
  }

  /**
   * Splits a String according to a pattern, keeping the separator at the end of each substring.
   *
   * @param input the input String
   * @param p the pattern upon which to split the input
   * @return the split string
   */
  @SuppressWarnings({
    "index:argument", // m.end is @LTLengthOf("index")
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // needs JDK annotations
  })
  @SideEffectFree
  public static List<String> splitRetainSeparators(String input, Pattern p) {
    List<String> result = new ArrayList<String>();
    Matcher m = p.matcher(input);
    int pos = 0;
    while (m.find()) {
      result.add(input.substring(pos, m.end()));
      pos = m.end();
    }
    if (pos < input.length()) {
      result.add(input.substring(pos));
    }
    return result;
  }

  /**
   * Concatenate the string representations of the array elements, placing the delimiter between
   * them.
   *
   * <p>This differs from the built-in {@code String.join()} method added in Java 8, in that this
   * takes any arbitrary array but that method takes an array of CharSequences. Use the String
   * method when the arguments are CharSequences.
   *
   * @param <T> the type of array elements
   * @param a array of values to concatenate
   * @param delim delimiter to place between printed representations
   * @return the concatenation of the string representations of the values, with the delimiter
   *     between
   */
  @SuppressWarnings({
    "lock:method.guarantee.violated", // side effect to local state
    "allcheckers:purity.not.sideeffectfree.call" // side effect to local state
  })
  @SafeVarargs
  @SideEffectFree
  public static <T extends @MustCallUnknown Object> String join(
      CharSequence delim, @Signed T... a) {
    if (a.length == 0) {
      return "";
    }
    if (a.length == 1) {
      return String.valueOf(a[0]);
    }
    StringBuilder sb = new StringBuilder(String.valueOf(a[0]));
    for (int i = 1; i < a.length; i++) {
      sb.append(delim).append(a[i]);
    }
    return sb.toString();
  }

  /**
   * Concatenate the string representations of the objects, placing the system-specific line
   * separator between them.
   *
   * @param <T> the type of array elements
   * @param a array of values to whose string representation to concatenate
   * @return the concatenation of the string representations of the values, each on its own line
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  @SideEffectFree
  public static <T extends @MustCallUnknown Object> String joinLines(@Signed T... a) {
    return join(lineSep, a);
  }

  /**
   * Concatenate the string representations of the objects, placing the delimiter between them.
   *
   * <p>This differs from the {@code String.join()} method added in Java 8, in that this takes any
   * Iterable but that method takes only {@code Iterable<? extends CharSequence>}.
   *
   * @see java.util.AbstractCollection#toString()
   * @param v collection of values to concatenate
   * @param delim delimiter to place between printed representations
   * @return the concatenation of the string representations of the values, with the delimiter
   *     between
   */
  @SuppressWarnings({
    "lock:method.guarantee.violated", // side effect to local state
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
  })
  @SideEffectFree
  public static String join(
      CharSequence delim,
      @MustCallUnknown Iterable<? extends @Signed @PolyNull @MustCallUnknown Object> v) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    Iterator<? extends @Signed @PolyNull @MustCallUnknown Object> itor = v.iterator();
    while (itor.hasNext()) {
      if (first) {
        first = false;
      } else {
        sb.append(delim);
      }
      sb.append(itor.next());
    }
    return sb.toString();
  }

  /**
   * Concatenate the string representations of the objects, placing the system-specific line
   * separator between them.
   *
   * @see java.util.AbstractCollection#toString()
   * @param v list of values to concatenate
   * @return the concatenation of the string representations of the values, each on its own line
   */
  @SideEffectFree
  public static String joinLines(
      @MustCallUnknown Iterable<? extends @Signed @PolyNull @MustCallUnknown Object> v) {
    return join(lineSep, v);
  }

  // //////////////////////////////////////////////////////////////////////
  // Quoting and escaping
  //

  /**
   * Escapes a String so that it is expressible in a string literal in Java source code. By
   * surrounding the return value with double quote marks, the result will be a Java string literal
   * denoting the original string.
   *
   * <p>Returns a new string only if any modifications were necessary.
   *
   * <p>Compared to the `escapeJava` method in Apache Commons Text StringEscapeUtils, this one
   * correctly handles non-printable ASCII characters.
   *
   * @param orig string to quote
   * @return quoted version of orig
   */
  @SuppressWarnings({
    "lock:method.guarantee.violated", // side effect to local state
    "allcheckers:purity.not.sideeffectfree.call" // side effect to local state
  })
  @SideEffectFree
  public static String escapeJava(String orig) {
    StringBuilder sb = new StringBuilder();
    // The previous escape character was seen right before this position.
    @IndexOrHigh("orig") int postEsc = 0;
    int origLen = orig.length();
    for (int i = 0; i < origLen; i++) {
      char c = orig.charAt(i);
      switch (c) {
        case '\"':
          if (postEsc < i) {
            sb.append(orig.substring(postEsc, i));
          }
          sb.append("\\\"");
          postEsc = i + 1;
          break;
        case '\\':
          if (postEsc < i) {
            sb.append(orig.substring(postEsc, i));
          }
          sb.append("\\\\");
          postEsc = i + 1;
          break;
        case '\b':
          if (postEsc < i) {
            sb.append(orig.substring(postEsc, i));
          }
          sb.append("\\b");
          postEsc = i + 1;
          break;
        case '\f':
          if (postEsc < i) {
            sb.append(orig.substring(postEsc, i));
          }
          sb.append("\\f");
          postEsc = i + 1;
          break;
        case '\n': // not lineSep
          if (postEsc < i) {
            sb.append(orig.substring(postEsc, i));
          }
          sb.append("\\n"); // not lineSep
          postEsc = i + 1;
          break;
        case '\r':
          if (postEsc < i) {
            sb.append(orig.substring(postEsc, i));
          }
          sb.append("\\r");
          postEsc = i + 1;
          break;
        case '\t':
          if (postEsc < i) {
            sb.append(orig.substring(postEsc, i));
          }
          sb.append("\\t");
          postEsc = i + 1;
          break;

        default:
          if (c >= ' ' && c <= '~') {
            // Nothing to do: i gets incremented
          } else if (c <= '\377') {
            if (postEsc < i) {
              sb.append(orig.substring(postEsc, i));
            }
            sb.append("\\");
            int cAsInt = (int) c;
            sb.append(String.format("%03o", cAsInt));
            postEsc = i + 1;
            break;
          } else {
            sb.append("\\u");
            sb.append(String.format("%04x", (int) c));
            postEsc = i + 1;
            break;
          }
      }
    }
    if (sb.length() == 0) {
      return orig;
    }
    sb.append(orig.substring(postEsc));
    return sb.toString();
  }

  /**
   * Like {@link #escapeJava(String)}, but for a single character. Note that this quotes its
   * argument for inclusion in a string literal, not in a character literal.
   *
   * @param ch character to quote
   * @return quoted version of ch
   * @deprecated use {@link #escapeJava(String)} or {@link #charLiteral(Character)}
   */
  @Deprecated // 2021-03-14
  @SideEffectFree
  public static String escapeJava(Character ch) {
    return escapeJava(ch.charValue());
  }

  /**
   * Like {@link #escapeJava(String)}, but for a single character. Note that this quotes its
   * argument for inclusion in a string literal, not in a character literal.
   *
   * @param c character to quote
   * @return quoted version of ch
   * @deprecated use {@link #escapeJava(String)} or {@link #charLiteral(char)}
   */
  @Deprecated // 2021-03-14
  @SideEffectFree
  public static String escapeJava(char c) {
    switch (c) {
      case '\"':
        return "\\\"";
      case '\\':
        return "\\\\";
      case '\b':
        return "\\b";
      case '\f':
        return "\\f";
      case '\n': // not lineSep
        return "\\n"; // not lineSep
      case '\r':
        return "\\r";
      case '\t':
        return "\\t";
      default:
        return new String(new char[] {c});
    }
  }

  /**
   * Given a character, returns a Java character literal denoting the character.
   *
   * @param ch character to quote
   * @return quoted version of ch
   */
  @SideEffectFree
  public static String charLiteral(Character ch) {
    return charLiteral(ch.charValue());
  }

  /**
   * Given a character, returns a Java character literal denoting the character.
   *
   * @param c character to quote
   * @return quoted version of ch
   */
  @SideEffectFree
  public static String charLiteral(char c) {
    switch (c) {
      case '\'':
        return "'\\''";
      case '\\':
        return "'\\\\'";
      case '\b':
        return "'\\b'";
      case '\f':
        return "'\\f'";
      case '\n': // not lineSep
        return "'\\n'"; // not lineSep
      case '\r':
        return "'\\r'";
      case '\t':
        return "'\\t'";
      default:
        return "'" + c + "'";
    }
  }

  /**
   * Escape unprintable characters in the target, following the usual Java backslash conventions, so
   * that the result is sure to be printable ASCII. Returns a new string.
   *
   * @param orig string to quote
   * @return quoted version of orig
   */
  @SuppressWarnings({
    "lock:method.guarantee.violated", // side effect to local state
    "allcheckers:purity.not.sideeffectfree.call" // side effect to local state
  })
  @SideEffectFree
  public static String escapeNonASCII(String orig) {
    StringBuilder sb = new StringBuilder();
    int origLen = orig.length();
    for (int i = 0; i < origLen; i++) {
      char c = orig.charAt(i);
      sb.append(escapeNonASCII(c));
    }
    return sb.toString();
  }

  /**
   * Like escapeJava(), but quote more characters so that the result is sure to be printable ASCII.
   *
   * <p>This implementation is not particularly optimized.
   *
   * @param c character to quote
   * @return quoted version of c
   */
  @SideEffectFree
  private static String escapeNonASCII(char c) {
    if (c == '"') {
      return "\\\"";
    } else if (c == '\\') {
      return "\\\\";
    } else if (c == '\n') { // not lineSep
      return "\\n"; // not lineSep
    } else if (c == '\r') {
      return "\\r";
    } else if (c == '\t') {
      return "\\t";
    } else if (c >= ' ' && c <= '~') {
      return new String(new char[] {c});
    } else if (c < 256) {
      String octal = Integer.toOctalString(c);
      while (octal.length() < 3) {
        octal = '0' + octal;
      }
      return "\\" + octal;
    } else {
      String hex = Integer.toHexString(c);
      while (hex.length() < 4) {
        hex = "0" + hex;
      }
      return "\\u" + hex;
    }
  }

  /**
   * Convert a string from Java source code format (with escape sequences) into the string it would
   * represent at run time. This is the inverse operation of {@link #escapeJava}, but it is
   * <em>not</em> a general unescaping mechanism for Java strings.
   *
   * <p>Compared to the `unescapeJava` method in Apache Commons Text StringEscapeUtils, this one
   * correctly handles non-printable ASCII characters.
   *
   * @param orig string to quote
   * @return quoted version of orig
   */
  @SuppressWarnings({
    "lock:method.guarantee.violated", // side effect to local state
    "allcheckers:purity.not.sideeffectfree.call" // side effect to local state
  })
  @SideEffectFree
  public static String unescapeJava(String orig) {
    StringBuilder sb = new StringBuilder();
    // The previous escape character was seen just before this position.
    @LTEqLengthOf("orig") int postEsc = 0;
    int thisEsc = orig.indexOf('\\');
    while (thisEsc != -1) {
      if (thisEsc == orig.length() - 1) {
        sb.append(orig.substring(postEsc, thisEsc + 1));
        postEsc = thisEsc + 1;
        break;
      }
      switch (orig.charAt(thisEsc + 1)) {
        case 'b':
          sb.append(orig.substring(postEsc, thisEsc));
          sb.append('\b');
          postEsc = thisEsc + 2;
          break;
        case 'f':
          sb.append(orig.substring(postEsc, thisEsc));
          sb.append('\f');
          postEsc = thisEsc + 2;
          break;
        case 'n':
          sb.append(orig.substring(postEsc, thisEsc));
          sb.append('\n'); // not lineSep
          postEsc = thisEsc + 2;
          break;
        case 'r':
          sb.append(orig.substring(postEsc, thisEsc));
          sb.append('\r');
          postEsc = thisEsc + 2;
          break;
        case 't':
          sb.append(orig.substring(postEsc, thisEsc));
          sb.append('\t');
          postEsc = thisEsc + 2;
          break;
        case '\\':
          // This is not in the default case because the search would find
          // the quoted backslash.  Here we include the first backslash in
          // the output, but not the first.
          sb.append(orig.substring(postEsc, thisEsc + 1));
          postEsc = thisEsc + 2;
          break;

        case 'u':
          // Unescape Unicode characters.
          sb.append(orig.substring(postEsc, thisEsc));
          char unicodeChar = 0;
          int ii = thisEsc + 2;
          // The specification permits one or more 'u' characters.
          while (ii < orig.length() && orig.charAt(ii) == 'u') {
            ii++;
          }
          // The specification requires exactly 4 hexadecimal characters.
          // This is more liberal.  (Should it be?)
          int limit = Math.min(ii + 4, orig.length());
          while (ii < limit) {
            int thisDigit = Character.digit(orig.charAt(ii), 16);
            if (thisDigit == -1) {
              break;
            }
            unicodeChar = (char) ((unicodeChar * 16) + thisDigit);
            ii++;
          }
          sb.append(unicodeChar);
          postEsc = ii;
          break;

        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
          // Unescape octal characters.
          sb.append(orig.substring(postEsc, thisEsc));
          char octalChar = 0;
          int iii = thisEsc + 1;
          while (iii < Math.min(thisEsc + 4, orig.length())) {
            int thisDigit = Character.digit(orig.charAt(iii), 8);
            if (thisDigit == -1) {
              break;
            }
            int newValue = (octalChar * 8) + thisDigit;
            if (newValue > 0377) {
              break;
            }
            octalChar = (char) newValue;
            iii++;
          }
          sb.append(octalChar);
          postEsc = iii;
          break;

        default:
          // In the default case, retain the character following the backslash,
          // but discard the backslash itself.  "\*" is just a one-character string.
          sb.append(orig.substring(postEsc, thisEsc));
          postEsc = thisEsc + 1;
          break;
      }
      thisEsc = orig.indexOf('\\', postEsc);
    }
    if (postEsc == 0) {
      return orig;
    }
    sb.append(orig.substring(postEsc));
    return sb.toString();
  }

  // //////////////////////////////////////////////////////////////////////
  // Whitespace
  //

  /**
   * Returns true if the string contains only white space codepoints, otherwise false.
   *
   * <p>In Java 11, use {@code String.isBlank()} instead.
   *
   * @param s a string
   * @return true if the string contains only white space codepoints, otherwise false
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "allcheckers:purity.not.deterministic.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @Pure
  public static boolean isBlank(String s) {
    return s.chars().allMatch(Character::isWhitespace);
  }

  /**
   * Remove all whitespace before or after instances of delimiter.
   *
   * @param arg string to remove whitespace in
   * @param delimiter string to remove whitespace abutting
   * @return version of arg, with whitespace abutting delimiter removed
   */
  @SideEffectFree
  public static String removeWhitespaceAround(String arg, String delimiter) {
    arg = removeWhitespaceBefore(arg, delimiter);
    arg = removeWhitespaceAfter(arg, delimiter);
    return arg;
  }

  /**
   * Remove all whitespace after instances of delimiter.
   *
   * @param arg string to remove whitespace in
   * @param delimiter a non-empty string to remove whitespace after
   * @return version of arg, with whitespace after delimiter removed
   */
  @SideEffectFree
  public static String removeWhitespaceAfter(String arg, String delimiter) {
    if (delimiter.isEmpty()) {
      throw new IllegalArgumentException("Bad delimiter: \"" + delimiter + "\"");
    }
    // String orig = arg;
    int delimLen = delimiter.length();
    int delimIndex = arg.indexOf(delimiter);
    while (delimIndex > -1) {
      int nonWsIndex = delimIndex + delimLen;
      while (nonWsIndex < arg.length() && Character.isWhitespace(arg.charAt(nonWsIndex))) {
        nonWsIndex++;
      }
      // if (nonWsIndex == arg.length()) {
      //   System.out.println("No nonspace character at end of: " + arg);
      // } else {
      //   System.out.println("'" + arg.charAt(nonWsIndex) + "' not a space character at " +
      //       nonWsIndex + " in: " + arg);
      // }
      if (nonWsIndex != delimIndex + delimLen) {
        arg = arg.substring(0, delimIndex + delimLen) + arg.substring(nonWsIndex);
      }
      delimIndex = arg.indexOf(delimiter, delimIndex + 1);
    }
    return arg;
  }

  /**
   * Remove all whitespace before instances of delimiter.
   *
   * @param arg string to remove whitespace in
   * @param delimiter a non-empty string to remove whitespace before
   * @return version of arg, with whitespace before delimiter removed
   */
  @SideEffectFree
  public static String removeWhitespaceBefore(String arg, String delimiter) {
    if (delimiter.isEmpty()) {
      throw new IllegalArgumentException("Bad delimiter: \"" + delimiter + "\"");
    }
    // System.out.println("removeWhitespaceBefore(\"" + arg + "\", \"" + delimiter + "\")");
    // String orig = arg;
    int delimIndex = arg.indexOf(delimiter);
    while (delimIndex > -1) {
      int nonWsIndex = delimIndex - 1;
      while (nonWsIndex >= 0 && Character.isWhitespace(arg.charAt(nonWsIndex))) {
        nonWsIndex--;
      }
      // if (nonWsIndex == -1) {
      //   System.out.println("No nonspace character at front of: " + arg);
      // } else {
      //   System.out.println("'" + arg.charAt(nonWsIndex) + "' not a space character at " +
      //       nonWsIndex + " in: " + arg);
      // }
      if (nonWsIndex != delimIndex - 1) {
        arg = arg.substring(0, nonWsIndex + 1) + arg.substring(delimIndex);
      }
      delimIndex = arg.indexOf(delimiter, nonWsIndex + 2);
    }
    return arg;
  }

  /**
   * Returns a string of the specified length, truncated if necessary (in which case the last 3
   * non-truncated characters are replaced by "..."), and padded with spaces to the left if
   * necessary.
   *
   * @param s string to truncate or pad
   * @param length goal length
   * @param c the character to use for padding
   * @param truncate if false, no truncation is done, only padding
   * @return {@code s} truncated or padded to {@code length} characters
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @SideEffectFree
  public static String lpad(String s, @NonNegative int length, char c, boolean truncate) {
    int sLength = s.length();
    if (sLength == length) {
      return s;
    } else if (sLength < length) {
      StringBuilder buf = new StringBuilder();
      for (int i = sLength; i < length; i++) {
        buf.append(c);
      }
      buf.append(s);
      return buf.toString();
    } else {
      if (truncate && length > 3) {
        return s.substring(0, length - 3) + "...";
      } else {
        return s;
      }
    }
  }

  /**
   * Returns a string of the specified length, truncated if necessary (in which case the last 3
   * non-truncated characters are replaced by "..."), and padded with `c` to the left if necessary.
   *
   * @param s string to truncate or pad
   * @param length goal length
   * @param c character to use for padding
   * @return {@code s} truncated or padded to {@code length} characters
   */
  @SideEffectFree
  public static String lpad(String s, @NonNegative int length, char c) {
    return lpad(s, length, c, true);
  }

  /**
   * Returns a string of the specified length, truncated if necessary (in which case the last 3
   * non-truncated characters are replaced by "..."), and padded with spaces to the left if
   * necessary.
   *
   * @param s string to truncate or pad
   * @param length goal length
   * @return {@code s} truncated or padded to {@code length} characters
   */
  @SideEffectFree
  public static String lpad(String s, @NonNegative int length) {
    return lpad(s, length, ' ', true);
  }

  /**
   * Returns a string of the specified length, truncated if necessary (in which case the last 3
   * non-truncated characters are replaced by "..."), and padded with the given character to the
   * right if necessary.
   *
   * @param s string to truncate or pad
   * @param length goal length
   * @param c character to use for padding
   * @param truncate if false, no truncation is done, only padding
   * @return {@code s} truncated or padded to {@code length} characters
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @SideEffectFree
  public static String rpad(String s, @NonNegative int length, char c, boolean truncate) {
    int sLength = s.length();
    if (sLength == length) {
      return s;
    } else if (sLength < length) {
      StringBuilder buf = new StringBuilder(s);
      for (int i = sLength; i < length; i++) {
        buf.append(c);
      }
      return buf.toString();
    } else {
      if (truncate && length > 3) {
        return s.substring(0, length - 3) + "...";
      } else {
        return s;
      }
    }
  }

  /**
   * Returns a string of the specified length, truncated if necessary (in which case the last 3
   * non-truncated characters are replaced by "..."), and padded with the given character to the
   * right if necessary.
   *
   * @param s string to truncate or pad
   * @param length goal length
   * @param c character to use for padding
   * @return {@code s} truncated or padded to {@code length} characters
   */
  @SideEffectFree
  public static String rpad(String s, @NonNegative int length, char c) {
    return rpad(s, length, c, true);
  }

  /**
   * Returns a string of the specified length, truncated if necessary (in which case the last 3
   * non-truncated characters are replaced by "..."), and padded with spaces to the right if
   * necessary.
   *
   * @param s string to truncate or pad
   * @param length goal length
   * @return {@code s} truncated or padded to {@code length} characters
   */
  @SideEffectFree
  public static String rpad(String s, @NonNegative int length) {
    return rpad(s, length, ' ');
  }

  /**
   * Converts the int to a String, then formats it using {@link #rpad(String,int)}.
   *
   * @param num int whose string representation to truncate or pad
   * @param length goal length
   * @return a string representation of {@code num} padded to {@code length} characters
   */
  @SideEffectFree
  public static String rpad(int num, @NonNegative int length) {
    return rpad(String.valueOf(num), length, ' ', /* truncate= */ false);
  }

  /**
   * Converts the double to a String, then formats it using {@link #rpad(String,int)}. Does not do
   * truncation that is after a decimal point.
   *
   * @param num double whose string representation to truncate or pad
   * @param length goal length
   * @return a string representation of {@code num} padded to {@code length} characters
   */
  @SuppressWarnings({
    "lock:method.guarantee.violated", // side effect to local state
    "allcheckers:purity.not.sideeffectfree.call"
  }) // side effect to local state
  @SideEffectFree
  public static String rpad(double num, @NonNegative int length) {
    if (length == 0) {
      throw new IllegalArgumentException(String.format("rpad(%s, %s)", num, length));
    }
    String numString = String.valueOf(num);
    int dotIndex = numString.indexOf('.');
    if (dotIndex >= length) {
      return numString.substring(0, dotIndex);
    } else if (dotIndex == length - 1) {
      // Pad instead of having the last character in the output be the decimal period.
      return numString.substring(0, dotIndex) + " ";
    } else
    // now: dotIndex < length - 1
    if (length < numString.length()) {
      return numString.substring(0, length);
    } else {
      // This is guaranteed to pad only, so inline rather than calling a method.
      StringBuilder result = new StringBuilder(numString);
      for (int i = numString.length(); i < length; i++) {
        result.append(' ');
      }
      return result.toString();
    }
  }

  // //////////////////////////////////////////////////////////////////////
  // Comparisons
  //

  /**
   * Same as built-in String comparison, but accept null arguments, and place them at the beginning.
   *
   * @deprecated use {@code Comparator.nullsFirst(Comparator.naturalOrder())}
   */
  @Deprecated // deprecated 2021-02-27
  public static class NullableStringComparator
      implements Comparator<@Nullable String>, Serializable {
    /** Unique identifier for serialization. If you add or remove fields, change this number. */
    static final long serialVersionUID = 20150812L;

    /** Create a new NullableStringComparator. */
    @SideEffectFree
    public NullableStringComparator() {}

    /**
     * Compare two Strings lexicographically. Null is considered less than any non-null String.
     *
     * @param s1 first string to compare
     * @param s2 second string to compare
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     *     equal to, or greater than the second
     */
    @SuppressWarnings("ReferenceEquality") // comparator method uses ==
    @Pure
    @Override
    public int compare(@Nullable String s1, @Nullable String s2) {
      if (s1 == s2) {
        return 0;
      }
      if (s1 == null) {
        return -1;
      }
      if (s2 == null) {
        return 1;
      }
      return s1.compareTo(s2);
    }
  }

  /**
   * Orders Objects according to their {@code toString()} representation. Null is considered less
   * than any non-null Object.
   *
   * <p>Note: if toString returns a nondeterministic value, such as one that depends on the result
   * of {@code hashCode()}, then this comparator may yield different orderings from run to run of a
   * program.
   *
   * <p>This cannot be replaced by {@code Comparator.nullsFirst(Comparator.naturalOrder())} becase
   * {@code Object} is not {@code Comparable}.
   */
  public static class ObjectComparator implements Comparator<@Nullable Object>, Serializable {

    /** The canonical ObjectComparator. */
    public static final ObjectComparator it = new ObjectComparator();

    /**
     * Create a new ObjectComparator.
     *
     * @deprecated use {@link #it}.
     */
    @Deprecated // 2022-07-25; to make private
    public ObjectComparator() {}

    /** Unique identifier for serialization. If you add or remove fields, change this number. */
    static final long serialVersionUID = 20170420L;

    /**
     * Compare two Objects based on their string representations. Null is considered less than any
     * non-null Object.
     *
     * @param o1 first object to compare
     * @param o2 second object to compare
     * @return a negative integer, zero, or a positive integer as the first argument's {@code
     *     toString()} representation is less than, equal to, or greater than the second argument's
     *     {@code toString()} representation
     */
    @SuppressWarnings({
      "allcheckers:purity.not.deterministic.call",
      "lock"
    }) // toString is being used in a deterministic way
    @Pure
    @Override
    public int compare(@Nullable Object o1, @Nullable Object o2) {
      // Make null compare smaller than anything else
      if (o1 == o2) {
        return 0;
      }
      if (o1 == null) {
        return -1;
      }
      if (o2 == null) {
        return 1;
      }
      if (o1.equals(o2)) {
        return 0;
      }
      // Don't compare output of hashCode() because it is non-deterministic from run to run.
      String s1 = o1.toString();
      String s2 = o2.toString();
      return s1.compareTo(s2);
    }
  }

  // //////////////////////////////////////////////////////////////////////
  // StringTokenizer
  //

  /**
   * Returns a ArrayList of the Strings returned by {@link
   * java.util.StringTokenizer#StringTokenizer(String,String,boolean)} with the given arguments.
   *
   * <p>The static type is {@code ArrayList<Object>} because StringTokenizer extends {@code
   * Enumeration<Object>} instead of {@code Enumeration<String>} as it should (probably due to
   * backward-compatibility).
   *
   * @param str a string to be parsed
   * @param delim the delimiters
   * @param returnDelims flag indicating whether to return the delimiters as tokens
   * @return vector of strings resulting from tokenization
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @SideEffectFree
  public static List<Object> tokens(String str, String delim, boolean returnDelims) {
    return CollectionsPlume.makeArrayList(new StringTokenizer(str, delim, returnDelims));
  }

  /**
   * Returns a ArrayList of the Strings returned by {@link
   * java.util.StringTokenizer#StringTokenizer(String,String)} with the given arguments.
   *
   * @param str a string to be parsed
   * @param delim the delimiters
   * @return vector of strings resulting from tokenization
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @SideEffectFree
  public static List<Object> tokens(String str, String delim) {
    return CollectionsPlume.makeArrayList(new StringTokenizer(str, delim));
  }

  /**
   * Returns a ArrayList of the Strings returned by {@link
   * java.util.StringTokenizer#StringTokenizer(String)} with the given arguments.
   *
   * @param str a string to be parsed
   * @return vector of strings resulting from tokenization
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @SideEffectFree
  public static List<Object> tokens(String str) {
    return CollectionsPlume.makeArrayList(new StringTokenizer(str));
  }

  // //////////////////////////////////////////////////////////////////////
  // Version numbers
  //

  /** Matches a version number, of the form N.N or N.N.N, etc., where each N consists of digits. */
  public static final @Regex String versionNumberRegex = "\\d+(\\.\\d+)+";

  /** Matches a version number, of the form N.N or N.N.N, etc., where each N consists of digits. */
  public static final Pattern versionNumberPattern = Pattern.compile("\\d+(\\.\\d+)+");

  /**
   * Returns true if the given text is a version number. It has the form N.N or N.N.N, etc., where
   * each N consists of digits.
   *
   * @param text a string
   * @return true if the given text is a version number
   */
  // "protected" to permit tests to access it.
  public static boolean isVersionNumber(String text) {
    return versionNumberPattern.matcher(text).matches();
  }

  /**
   * A comparator that compares version numbers. It must only be invoked on strings that are version
   * numbers.
   */
  public static class VersionNumberComparator implements Comparator<String> {

    /** Creates a new VersionNumberComparator. */
    public VersionNumberComparator() {}

    @SuppressWarnings("StringSplitter") // OK given that the arguments are version numbers.
    @Override
    public int compare(String s1, String s2) {
      if (s1.equals(s2)) {
        return 0;
      }
      String[] components1 = s1.split("\\.");
      String[] components2 = s2.split("\\.");
      int len = Math.min(components1.length, components2.length);
      for (int i = 0; i < len; i++) {
        int int1 = Integer.valueOf(components1[i]);
        int int2 = Integer.valueOf(components2[i]);
        if (int1 < int2) {
          return -1;
        } else if (int1 > int2) {
          return 1;
        }
      }
      if (components1.length < components2.length) {
        return -1;
      } else {
        assert components2.length < components1.length;
        return 1;
      }
    }
  }

  /** A VersionNumberComparator. */
  private static VersionNumberComparator vnc = new VersionNumberComparator();

  /**
   * Returns true if the first version number is less than or equal to the second version number.
   *
   * @param v1 a version number
   * @param v2 a version number
   * @return true if the given text is a version number
   */
  // "protected" to permit tests to access it.
  public static boolean isVersionNumberLE(String v1, String v2) {
    int compare = vnc.compare(v1, v2);
    return compare <= 0;
  }

  // //////////////////////////////////////////////////////////////////////
  // Debugging variants of toString
  //

  /**
   * Gives a string representation of the value and its class. Shows elements of collections; to
   * omit them, pass false to {@link #toStringAndClass(Object, boolean)}.
   *
   * @param v a value; may be null
   * @return the value's toString and its class
   */
  @SideEffectFree
  public static String toStringAndClass(@Nullable Object v) {
    return toStringAndClass(v, false);
  }

  /**
   * Gives a string representation of the value and its class. Intended for debugging.
   *
   * @param v a value; may be null
   * @param shallow if true, do not show elements of arrays and lists
   * @return the value's toString and its class
   */
  @SideEffectFree
  public static String toStringAndClass(@Nullable Object v, boolean shallow) {
    if (v == null) {
      return "null";
    }
    if (v.getClass() == Object.class) {
      return "a value of class " + v.getClass();
    }
    if (!shallow) {
      if (v.getClass().isArray()) {
        return arrayToStringAndClass(v);
      }
      if (v instanceof List) {
        return listToStringAndClass((List<? extends @PolyNull @Signed Object>) v);
      }
      if (v instanceof Map) {
        return mapToStringAndClass(
            (Map<? extends @PolyNull @Signed Object, ? extends @PolyNull @Signed Object>) v);
      }
    }
    try {
      String formatted = escapeJava(v.toString());
      return String.format("%s [%s]", formatted, v.getClass());
    } catch (Exception e) {
      return String.format("exception_when_calling_toString [%s]", v.getClass());
    }
  }

  /**
   * Gives a string representation of the value and its class. Intended for debugging.
   *
   * @param lst a value; may be null
   * @return the value's toString and its class
   */
  @SideEffectFree
  public static String listToStringAndClass(
      @Nullable List<? extends @Signed @PolyNull Object> lst) {
    if (lst == null) {
      return "null";
    } else {
      return listToString(lst) + " [" + lst.getClass() + "]";
    }
  }

  /**
   * For use by toStringAndClass. Calls toStringAndClass on each element, but does not add the class
   * of the list itself.
   *
   * @param lst the list to print
   * @return a string representation of each element and its class
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @SideEffectFree
  public static String listToString(@Nullable List<? extends @Signed @PolyNull Object> lst) {
    if (lst == null) {
      return "null";
    }
    StringJoiner sj = new StringJoiner(", ", "[", "]");
    for (Object o : lst) {
      sj.add(toStringAndClass(o, true));
    }
    return sj.toString();
  }

  /**
   * Returns a string representation of the contents of the specified array. The argument must be an
   * array or null. This just dispatches one of the 9 overloaded versions of {@code
   * java.util.Arrays.toString()}.
   *
   * @param a an array
   * @return a string representation of the array
   * @throws IllegalArgumentException if a is not an array
   */
  @SideEffectFree
  public static String arrayToStringAndClass(@Nullable Object a) {

    if (a == null) {
      return "null";
    }
    String theClass = " [" + a.getClass() + "]";

    if (a instanceof boolean[]) {
      return Arrays.toString((boolean[]) a) + theClass;
    } else if (a instanceof byte[]) {
      return Arrays.toString((byte[]) a) + theClass;
    } else if (a instanceof char[]) {
      return Arrays.toString((char[]) a) + theClass;
    } else if (a instanceof double[]) {
      return Arrays.toString((double[]) a) + theClass;
    } else if (a instanceof float[]) {
      return Arrays.toString((float[]) a) + theClass;
    } else if (a instanceof int[]) {
      return Arrays.toString((int[]) a) + theClass;
    } else if (a instanceof long[]) {
      return Arrays.toString((long[]) a) + theClass;
    } else if (a instanceof short[]) {
      return Arrays.toString((short[]) a) + theClass;
    }

    if (a instanceof Object[]) {
      try {
        return listToString(Arrays.asList((Object[]) a)) + theClass;
      } catch (Exception e) {
        return "exception_when_printing_array" + theClass;
      }
    }

    throw new IllegalArgumentException(
        "Argument is not an array; its class is " + a.getClass().getName());
  }

  //
  // Diagnostic output
  //

  /**
   * Convert a map to a string, printing the runtime class of keys and values.
   *
   * @param m a map
   * @return a string representation of the map
   * @deprecated use {@link CollectionsPlume#mapToStringAndClassMultiLine}
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @Deprecated // 2025-06-21
  @SideEffectFree
  public static String mapToStringAndClass(
      Map<? extends @Signed @PolyNull Object, ? extends @Signed @PolyNull Object> m) {
    StringJoiner result = new StringJoiner(System.lineSeparator());
    for (Map.Entry<? extends @Signed @PolyNull Object, ? extends @Signed @PolyNull Object> e :
        m.entrySet()) {
      result.add("  " + toStringAndClass(e.getKey()) + " => " + toStringAndClass(e.getValue()));
    }
    return result.toString();
  }

  /**
   * Convert a map to a string, printing each key-value pair on its own line, with no indentation.
   *
   * @param m a map
   * @return a string representation of the map
   * @deprecated use {@link CollectionsPlume#mapToStringAndClassMultiLine}
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @Deprecated // 2025-06-21
  @SideEffectFree
  public static String mapToStringLinewise(
      Map<? extends @Signed @PolyNull Object, ? extends @Signed @PolyNull Object> m) {
    StringJoiner result = new StringJoiner(System.lineSeparator());
    for (Map.Entry<? extends @Signed @PolyNull Object, ? extends @Signed @PolyNull Object> e :
        m.entrySet()) {
      result.add(e.getKey() + " => " + e.getValue());
    }
    return result.toString();
  }

  // //////////////////////////////////////////////////////////////////////
  // English text
  //

  /**
   * Returns either "n <em>noun</em>" or "n <em>noun</em>s" depending on n. Adds "es" to words
   * ending with "ch", "s", "sh", or "x", adds "ies" to words ending with "y" when the previous
   * letter is a consonant.
   *
   * @param n count of nouns
   * @param noun word being counted; must not be the empty string
   * @return {@code noun}, if n==1; otherwise, pluralization of {@code noun}
   * @throws IllegalArgumentException if the length of {@code noun} is 0
   * @deprecated use {@link #nPlural(int, String)}
   */
  @Deprecated // 2025-07-16
  @SideEffectFree
  public static String nplural(int n, String noun) {
    return nPlural(n, noun);
  }

  /** Exceptions to the usual English noun pluralization rules. */
  private static final Map<String, String> nPluralExceptions = new HashMap<>();

  static {
    // No change
    nPluralExceptions.put("bison", "bison");
    nPluralExceptions.put("buffalo", "buffalo");
    nPluralExceptions.put("carp", "carp");
    nPluralExceptions.put("chassis", "chassis");
    nPluralExceptions.put("cod", "cod");
    nPluralExceptions.put("deer", "deer");
    nPluralExceptions.put("fish", "fish");
    nPluralExceptions.put("kakapo", "kakapo");
    nPluralExceptions.put("neat", "neat");
    nPluralExceptions.put("pike", "pike");
    nPluralExceptions.put("salmon", "salmon");
    nPluralExceptions.put("series", "series");
    nPluralExceptions.put("sheep", "sheep");
    nPluralExceptions.put("shrimp", "shrimp");
    nPluralExceptions.put("species", "species");
    nPluralExceptions.put("squid", "squid");
    nPluralExceptions.put("trout", "trout");

    // Native American tribe names

    nPluralExceptions.put("Cherokee", "Cherokee");
    nPluralExceptions.put("Cree", "Cree");
    nPluralExceptions.put("Comanche", "Comanche");
    nPluralExceptions.put("Delaware", "Delaware");
    nPluralExceptions.put("Hopi", "Hopi");
    nPluralExceptions.put("Iroquois", "Iroquois");
    nPluralExceptions.put("Kiowa", "Kiowa");
    nPluralExceptions.put("Navajo", "Navajo");
    nPluralExceptions.put("Ojibwa", "Ojibwa");
    nPluralExceptions.put("Sioux", "Sioux");
    nPluralExceptions.put("Zuni", "Zuni");

    // Ending in "y"
    nPluralExceptions.put("lay-by", "lay-bys");
    nPluralExceptions.put("stand-by", "stand-bys");

    // Ending in "i"
    nPluralExceptions.put("alkali", "alkalies");

    // Plural ending in "en"
    nPluralExceptions.put("ox", "oxen");
    nPluralExceptions.put("child", "children");

    // Apophonic plurals
    nPluralExceptions.put("foot", "feet");
    nPluralExceptions.put("goose", "geese");
    nPluralExceptions.put("louse", "lice");
    nPluralExceptions.put("dormouse", "dormice");
    nPluralExceptions.put("man", "men");
    nPluralExceptions.put("mouse", "mice");
    nPluralExceptions.put("tooth", "teeth");
    nPluralExceptions.put("woman", "women");

    // Miscellaneous irregular plurals
    nPluralExceptions.put("person", "people");
  }

  /**
   * Returns either "n <em>noun</em>" or "n <em>noun</em>s" depending on {@code n}. Adds "es" to
   * words ending with "ch", "s", "sh", or "x". Adds "ies" to words ending with "y" when the
   * previous letter is a consonant. Handles some irregular nouns.
   *
   * @param n count of nouns
   * @param noun word being counted; must not be the empty string
   * @return {@code noun}, if n==1; otherwise, pluralization of {@code noun}
   * @throws IllegalArgumentException if the length of {@code noun} is 0
   */
  @SideEffectFree
  public static String nPlural(int n, String noun) {
    if (noun.isEmpty()) {
      throw new IllegalArgumentException(
          "The second argument to nPlural must not be an empty string");
    }
    if (n == 1) {
      return n + " " + noun;
    }
    String irregular = nPluralExceptions.get(noun);
    if (irregular != null) {
      return n + " " + irregular;
    }

    // TODO: handle more from https://en.wikipedia.org/wiki/English_plurals ,
    // and organize `nPluralExceptions` and the code the same as it.

    char lastLetter = noun.charAt(noun.length() - 1);
    char penultimateLetter = (noun.length() == 1) ? '\u0000' : noun.charAt(noun.length() - 2);
    if ((penultimateLetter == 'c' && lastLetter == 'h')
        || lastLetter == 's'
        || (penultimateLetter == 's' && lastLetter == 'h')
        || lastLetter == 'x') {
      return n + " " + noun + "es";
    }
    if (lastLetter == 'y'
        && (penultimateLetter != 'a'
            && penultimateLetter != 'e'
            && penultimateLetter != 'i'
            && penultimateLetter != 'o'
            && penultimateLetter != 'u')) {
      return n + " " + noun.substring(0, noun.length() - 1) + "ies";
    }

    // TODO:  Change nouns ending in "f" or "fe" to "ves" (knives, leaves, lives, wolves, calves),
    // but beware that there are exceptions (roofs, fifes).

    // TODO:
    // Singular nouns ending in o preceded by a consonant in many cases spell the plural by adding
    // -es: heroes,	potatoes, echoes.
    // However, many nouns of foreign origin, including almost all Italian loanwords, add only -s:
    // And also some other exceptions like volcanos.

    return n + " " + noun + "s";
  }

  /**
   * Returns either "n <em>noun</em>" or "n <em>noun</em>s" depending on the size of the collection.
   * Adds "es" to words ending with "ch", "s", "sh", or "x". Adds "ies" to words ending with "y"
   * when the previous letter is a consonant.
   *
   * @param c a collection whose size to test
   * @param noun word being counted; must not be the empty string
   * @return {@code noun}, if {@code c} has size 1; otherwise, pluralization of {@code noun}
   * @throws IllegalArgumentException if the length of {@code noun} is 0
   */
  @SideEffectFree
  public static String nPlural(Collection<?> c, String noun) {
    return nPlural(c.size(), noun);
  }

  /**
   * Returns either "n <em>noun</em>" or "n <em>noun</em>s" depending on the size of the collection.
   * Adds "es" to words ending with "ch", "s", "sh", or "x". Adds "ies" to words ending with "y"
   * when the previous letter is a consonant.
   *
   * @param m a map whose size to test
   * @param noun word being counted; must not be the empty string
   * @return {@code noun}, if {@code m} has size 1; otherwise, pluralization of {@code noun}
   * @throws IllegalArgumentException if the length of {@code noun} is 0
   */
  @SideEffectFree
  public static String nPlural(Map<?, ?> m, String noun) {
    return nPlural(m.size(), noun);
  }

  /**
   * Returns either "n <em>noun</em>" or "n <em>noun</em>s" depending on the size of the collection.
   * Adds "es" to words ending with "ch", "s", "sh", or "x". Adds "ies" to words ending with "y"
   * when the previous letter is a consonant.
   *
   * @param <T> the type of array elements
   * @param a an array whose size to test
   * @param noun word being counted; must not be the empty string
   * @return {@code noun}, if {@code a} has size 1; otherwise, pluralization of {@code noun}
   * @throws IllegalArgumentException if the length of {@code noun} is 0
   */
  @SideEffectFree
  public static <T> String nPlural(T[] a, String noun) {
    return nPlural(a.length, noun);
  }

  /** Exceptions to the usual English verb pluralization rules. */
  private static final Map<String, String> vPluralExceptions = new HashMap<>();

  static {
    vPluralExceptions.put("is", "are");
    vPluralExceptions.put("was", "were");
  }

  /**
   * Returns either the singular or plural form of the given verb, depending on {@code n}. Most
   * English verbs have the same singular and plural form.
   *
   * <p>The implementation of this method hard-codes some irregular verbs, and otherwise returns its
   * argument.
   *
   * @param n count
   * @param verb verb whose subject is one or more things, depending on {@code n}
   * @return {@code verb}, if n==1; otherwise, pluralization of {@code verb}
   */
  @SideEffectFree
  public static String vPlural(int n, String verb) {
    if (n == 1) {
      return verb;
    }
    return vPluralExceptions.getOrDefault(verb, verb);
  }

  /**
   * Returns either the singular or plural form of the given verb, depending on the size of the
   * collection. Most English verbs have the same singular and plural form.
   *
   * <p>The implementation of this method hard-codes some irregular verbs, and otherwise returns its
   * argument.
   *
   * @param c a collection
   * @param verb verb whose subject is one or more things, depending on the size of the collection
   * @return {@code verb}, if n==1; otherwise, pluralization of {@code verb}
   */
  @SideEffectFree
  public static String vPlural(Collection<?> c, String verb) {
    return vPlural(c.size(), verb);
  }

  /**
   * Returns either the singular or plural form of the given verb, depending on the size of the
   * collection. Most English verbs have the same singular and plural form.
   *
   * <p>The implementation of this method hard-codes some irregular verbs, and otherwise returns its
   * argument.
   *
   * @param m a map
   * @param verb verb whose subject is one or more things, depending on the size of the collection
   * @return {@code verb}, if n==1; otherwise, pluralization of {@code verb}
   */
  @SideEffectFree
  public static String vPlural(Map<?, ?> m, String verb) {
    return vPlural(m.size(), verb);
  }

  /**
   * Returns either the singular or plural form of the given verb, depending on the size of the
   * collection. Most English verbs have the same singular and plural form.
   *
   * <p>The implementation of this method hard-codes some irregular verbs, and otherwise returns its
   * argument.
   *
   * @param <T> the type of array elements
   * @param a an array
   * @param verb verb whose subject is one or more things, depending on the size of the collection
   * @return {@code verb}, if n==1; otherwise, pluralization of {@code verb}
   */
  @SideEffectFree
  public static <T> String vPlural(T[] a, String verb) {
    return vPlural(a.length, verb);
  }

  /**
   * Returns either the singular or plural form of the noun and verb, depending on {@code n}.
   *
   * @param n count
   * @param noun word being counted; must not be the empty string
   * @param verb verb whose subject is one or more things, depending on {@code n}
   * @return {@code noun verb}, if n==1; otherwise, its pluralization
   * @see #nPlural(int, String)
   * @see #vPlural(int, String)
   */
  @SideEffectFree
  public static String nvPlural(int n, String noun, String verb) {
    return nPlural(n, noun) + " " + vPlural(n, verb);
  }

  /**
   * Returns either the singular or plural form of the given noun and verb, depending on the size of
   * the collection.
   *
   * @param c a collection
   * @param noun word being counted; must not be the empty string
   * @param verb verb whose subject is one or more things, depending on the size of the collection
   * @return {@code noun verb}, if n==1; otherwise, its pluralization
   * @see #nPlural(int, String)
   * @see #vPlural(int, String)
   */
  @SideEffectFree
  public static String nvPlural(Collection<?> c, String noun, String verb) {
    return nvPlural(c.size(), noun, verb);
  }

  /**
   * Returns either the singular or plural form of the given noun and verb, depending on the size of
   * the collection.
   *
   * @param m a map
   * @param noun word being counted; must not be the empty string
   * @param verb verb whose subject is one or more things, depending on the size of the collection
   * @return {@code noun verb}, if n==1; otherwise, its pluralization
   * @see #nPlural(int, String)
   * @see #vPlural(int, String)
   */
  @SideEffectFree
  public static String nvPlural(Map<?, ?> m, String noun, String verb) {
    return nvPlural(m.size(), noun, verb);
  }

  /**
   * Returns either the singular or plural form of the given noun and verb, depending on the size of
   * the collection.
   *
   * @param <T> the type of array elements
   * @param a an array
   * @param noun word being counted; must not be the empty string
   * @param verb verb whose subject is one or more things, depending on the size of the collection
   * @return {@code noun verb}, if n==1; otherwise, its pluralization
   * @see #nPlural(int, String)
   * @see #vPlural(int, String)
   */
  @SideEffectFree
  public static <T> String nvPlural(T[] a, String noun, String verb) {
    return nvPlural(a.length, noun, verb);
  }

  /**
   * Creates a conjunction or disjunction, like "a", "a or b", and "a, b, or c". Obeys the "serial
   * comma" or "Oxford comma" rule: when the list has size 3 or larger, puts a comma after every
   * element (except the last one, which ends the list).
   *
   * @param conjunction the conjunction word, like "and" or "or"
   * @param elements the elements of the conjunction or disjunction
   * @return a conjunction or disjunction string
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // needs JDK annotations
  })
  @SideEffectFree
  public static String conjunction(
      String conjunction, List<? extends @Signed @PolyNull Object> elements) {
    int size = elements.size();
    if (size == 0) {
      throw new IllegalArgumentException("no elements passed to conjunction()");
    } else if (size == 1) {
      return Objects.toString(elements.get(0));
    } else if (size == 2) {
      return elements.get(0) + " " + conjunction + " " + elements.get(1);
    }

    StringJoiner sj = new StringJoiner(", ");
    for (int i = 0; i < size - 1; i++) {
      sj.add(Objects.toString(elements.get(i)));
    }
    sj.add(conjunction + " " + elements.get(size - 1));
    return sj.toString();
  }

  // //////////////////////////////////////////////////////////////////////
  // Miscellaneous
  //

  /**
   * Returns the number of times the character appears in the string.
   *
   * @param s string to search in
   * @param ch character to search for
   * @return number of times the character appears in the string
   */
  @Pure
  @StaticallyExecutable
  public static int count(String s, int ch) {
    int result = 0;
    int pos = s.indexOf(ch);
    while (pos > -1) {
      result++;
      pos = s.indexOf(ch, pos + 1);
    }
    return result;
  }

  /**
   * Returns the number of times the second string appears in the first.
   *
   * @param s string to search in
   * @param sub non-empty string to search for
   * @return number of times the substring appears in the string
   */
  @Pure
  @StaticallyExecutable
  public static int count(String s, String sub) {
    if (sub.equals("")) {
      throw new IllegalArgumentException("second argument must not be empty");
    }
    int result = 0;
    int pos = s.indexOf(sub);
    while (pos > -1) {
      result++;
      pos = s.indexOf(sub, pos + 1);
    }
    return result;
  }

  /**
   * Convert a number into an abbreviation such as "5.00K" for 5000 or "65.0M" for 65000000. K
   * stands for 1000, not 1024; M stands for 1000000, not 1048576, etc. There are always exactly 3
   * decimal digits of precision in the result (counting both sides of the decimal point).
   *
   * @param val a numeric value
   * @return an abbreviated string representation of the value
   */
  @SideEffectFree
  public static String abbreviateNumber(long val) {

    double dval = (double) val;
    String mag = "";

    if (val < 1000) {
      // nothing to do
    } else if (val < 1000000) {
      dval = val / 1000.0;
      mag = "K";
    } else if (val < 1000000000) {
      dval = val / 1000000.0;
      mag = "M";
    } else {
      dval = val / 1000000000.0;
      mag = "G";
    }

    String precision = "0";
    if (dval < 10) {
      precision = "2";
    } else if (dval < 100) {
      precision = "1";
    }

    @SuppressWarnings("formatter") // format string computed from precision and mag
    String result = String.format("%,1." + precision + "f" + mag, dval);
    return result;
  }

  // From
  // https://stackoverflow.com/questions/37413816/get-number-of-placeholders-in-formatter-format-string
  /** Regex that matches a format specifier. Some correspond to arguments and some do not. */
  private static @Regex(6) Pattern formatSpecifier =
      Pattern.compile("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

  /**
   * Returns the number of arguments that the given format string takes. This is the number of
   * specifiers that take arguments (some, like {@code %n} and {@code %%}, do not take arguments).
   *
   * @param s a string
   * @return the number of format specifiers in the string
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "allcheckers:purity.not.deterministic.not.sideeffectfree.call", // side effect to local state
    "allcheckers:purity.not.deterministic.call", // deterministic up to equals, which is sufficient
    "lock:method.guarantee.violated" // side effect to local state
  })
  @Pure
  @StaticallyExecutable
  public static int countFormatArguments(String s) {
    int result = 0;
    int maxIndex = 0;
    Matcher matcher = formatSpecifier.matcher(s);
    while (matcher.find()) {
      String argumentIndex = matcher.group(1);
      if (argumentIndex != null) {
        @SuppressWarnings("lowerbound:argument") // group contains >= 2 chars
        int thisIndex = Integer.parseInt(argumentIndex.substring(0, argumentIndex.length() - 1));
        maxIndex = Math.max(maxIndex, thisIndex);
        continue;
      }
      String conversion = matcher.group(6);
      assert conversion != null : "@AssumeAssertion(nullness): nonempty capturing group";
      if (!(conversion.equals("%") || conversion.equals("n"))) {
        result++;
      }
    }
    return Math.max(maxIndex, result);
  }

  /**
   * If the string representation of the given object is greater than the given length, truncate it
   * to that length.
   *
   * @param o an object
   * @param length the maximum length for the string representation; must be 6 or more
   * @return the string representation of the object, no more than the given length
   */
  @SideEffectFree
  public static String toStringTruncated(Object o, int length) {
    if (length < 6) {
      throw new IllegalArgumentException(
          "toStringTruncated: length argument must be 6 or more, got " + length);
    }
    String result = o.toString();
    if (result.length() <= length) {
      return result;
    } else {
      // The quoting increases the likelihood that all delimiters are balanced in the result.
      // That makes it easier to manipulate the result (such as skipping over it) in an
      // editor.  The quoting also makes clear that the value is truncated.
      return "\"" + result.substring(0, length - 5) + "...\"";
    }
  }
}
